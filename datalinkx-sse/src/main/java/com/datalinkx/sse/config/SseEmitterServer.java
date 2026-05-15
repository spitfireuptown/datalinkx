package com.datalinkx.sse.config;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.datalinkx.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
public class SseEmitterServer {

    private static final String SSE_USER_KEY = "sse:user:node";
    private static final String SSE_CHANNEL_PREFIX = "sse:channel:";
    private static final String SSE_MESSAGE_QUEUE_PREFIX = "sse:queue:";
    private static final long SSE_TIMEOUT = 0L;
    private static final int MAX_QUEUE_SIZE = 100;
    private static final long MESSAGE_EXPIRE_SECONDS = 3600;

    private static final AtomicInteger count = new AtomicInteger(0);

    private static final Map<String, SseEmitter> sseEmitterMap = new ConcurrentHashMap<>();
    private static final Set<String> subscribedChannels = ConcurrentHashMap.newKeySet();

    private static StringRedisTemplate redisTemplate;
    private static RedisMessageListenerContainer listenerContainer;
    private static String currentNodeId;
    private static SharedSseMessageListener sharedMessageListener;

    public static void setRedisTemplate(StringRedisTemplate redisTemplate) {
        SseEmitterServer.redisTemplate = redisTemplate;
    }

    public static void setListenerContainer(RedisMessageListenerContainer listenerContainer) {
        SseEmitterServer.listenerContainer = listenerContainer;
        sharedMessageListener = new SharedSseMessageListener();
    }

    public static void setCurrentNodeId(String nodeId) {
        SseEmitterServer.currentNodeId = nodeId;
    }

    public static SseEmitter connect(String employeeCode) {
        return connect(employeeCode, true);
    }

    public static SseEmitter connect(String employeeCode, boolean isDistributed) {
        SseEmitter existingEmitter = sseEmitterMap.get(employeeCode);
        if (!ObjectUtils.isEmpty(existingEmitter)) {
            return existingEmitter;
        }

        SseEmitter sseEmitter = new SseEmitter(SSE_TIMEOUT);
        sseEmitter.onCompletion(completionCallBack(employeeCode));
        sseEmitter.onError(errorCallBack(employeeCode));
        sseEmitter.onTimeout(timeoutCallBack(employeeCode));

        sseEmitterMap.put(employeeCode, sseEmitter);
        count.getAndIncrement();

        if (isDistributed && redisTemplate != null && currentNodeId != null) {
            try {
                redisTemplate.opsForHash().put(SSE_USER_KEY, employeeCode, currentNodeId);
                ensureRedisSubscriber(employeeCode);
                flushOfflineMessages(employeeCode);
                log.info("SSE user[{}] registered to node[{}]", employeeCode, currentNodeId);
            } catch (Exception e) {
                log.warn("Failed to register SSE user to Redis: {}", employeeCode, e);
            }
        }

        return sseEmitter;
    }

    private static synchronized void ensureRedisSubscriber(String employeeCode) {
        if (listenerContainer == null || redisTemplate == null || sharedMessageListener == null) {
            return;
        }

        String channel = SSE_CHANNEL_PREFIX + employeeCode;
        if (subscribedChannels.contains(channel)) {
            return;
        }

        try {
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(sharedMessageListener);
            listenerAdapter.afterPropertiesSet();
            listenerContainer.addMessageListener(listenerAdapter, new ChannelTopic(channel));
            subscribedChannels.add(channel);
            log.info("Subscribed to SSE channel: {}", channel);
        } catch (Exception e) {
            log.warn("Failed to subscribe SSE channel[{}]: {}", channel, e.getMessage());
        }
    }

    public static void sendMessage(String employeeCode, String jsonMsg) {
        sendMessage(employeeCode, jsonMsg, true);
    }

    public static void sendMessage(String employeeCode, String jsonMsg, boolean isDistributed) {
        if (!isDistributed || redisTemplate == null || currentNodeId == null) {
            sendMessageLocal(employeeCode, jsonMsg);
            return;
        }

        try {
            Object nodeInfoObj = redisTemplate.opsForHash().get(SSE_USER_KEY, employeeCode);
            if (nodeInfoObj == null) {
                log.debug("User[{}] not found in Redis, storing offline message", employeeCode);
                storeOfflineMessage(employeeCode, jsonMsg);
                return;
            }

            String nodeInfo = nodeInfoObj.toString();
            String nodeId = nodeInfo.split(":")[0];

            if (currentNodeId.equals(nodeId)) {
                sendMessageLocal(employeeCode, jsonMsg);
            } else {
                String channel = SSE_CHANNEL_PREFIX + employeeCode;
                redisTemplate.convertAndSend(channel, jsonMsg);
                log.debug("Published SSE message to channel[{}] for node[{}]", channel, nodeId);
            }
        } catch (Exception e) {
            log.error("Failed to send SSE message to user[{}]: {}", employeeCode, e.getMessage());
            log.error(e.getMessage(), e);
            storeOfflineMessage(employeeCode, jsonMsg);
        }
    }

    private static void storeOfflineMessage(String employeeCode, String jsonMsg) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String queueKey = SSE_MESSAGE_QUEUE_PREFIX + employeeCode;
            redisTemplate.opsForList().rightPush(queueKey, jsonMsg);
            redisTemplate.expire(queueKey, MESSAGE_EXPIRE_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            Long size = redisTemplate.opsForList().size(queueKey);
            if (size != null && size > MAX_QUEUE_SIZE) {
                redisTemplate.opsForList().leftPop(queueKey);
            }
            log.debug("Stored offline message for user[{}], queue size: {}", employeeCode, size);
        } catch (Exception e) {
            log.warn("Failed to store offline message for user[{}]: {}", employeeCode, e.getMessage());
        }
    }

    private static void flushOfflineMessages(String employeeCode) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String queueKey = SSE_MESSAGE_QUEUE_PREFIX + employeeCode;
            List<String> messages = redisTemplate.opsForList().range(queueKey, 0, -1);
            if (messages != null && !messages.isEmpty()) {
                SseEmitter emitter = sseEmitterMap.get(employeeCode);
                if (emitter != null) {
                    for (String msg : messages) {
                        try {
                            emitter.send(msg, MediaType.APPLICATION_JSON);
                            log.debug("Flushed offline message to user[{}]", employeeCode);
                        } catch (IOException e) {
                            log.error("Failed to flush message to user[{}]: {}", employeeCode, e.getMessage());
                            break;
                        }
                    }
                }
                redisTemplate.delete(queueKey);
                log.info("Flushed {} offline messages for user[{}]", messages.size(), employeeCode);
            }
        } catch (Exception e) {
            log.warn("Failed to flush offline messages for user[{}]: {}", employeeCode, e.getMessage());
        }
    }

    private static void sendMessageLocal(String employeeCode, String jsonMsg) {
        SseEmitter emitter = sseEmitterMap.get(employeeCode);
        if (emitter == null) {
            emitter = connect(employeeCode, false);
        }
        try {
            emitter.send(jsonMsg, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            log.error("SSE user[{}] push error: {}", employeeCode, e.getMessage());
            removeUser(employeeCode);
        }
    }

    public static void removeUser(String employeeCode) {
        SseEmitter emitter = sseEmitterMap.remove(employeeCode);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for user[{}]: {}", employeeCode, e.getMessage());
            }
        }
        count.getAndDecrement();

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForHash().delete(SSE_USER_KEY, employeeCode);
                log.debug("Removed user[{}] from Redis", employeeCode);
            } catch (Exception e) {
                log.warn("Failed to remove user[{}] from Redis: {}", employeeCode, e.getMessage());
            }
        }
    }

    private static Runnable completionCallBack(String employeeCode) {
        return () -> {
            log.info("SSE connection completed: {}", employeeCode);
            removeUser(employeeCode);
        };
    }

    private static Runnable timeoutCallBack(String employeeCode) {
        return () -> {
            log.info("SSE connection timeout: {}", employeeCode);
            removeUser(employeeCode);
        };
    }

    private static Consumer<Throwable> errorCallBack(String employeeCode) {
        return throwable -> {
            log.info("SSE connection error: {}", employeeCode);
            removeUser(employeeCode);
        };
    }

    public static class SharedSseMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            try {
                String body = new String(message.getBody());
                String channel = new String(message.getChannel());
                String userId = channel.substring(SSE_CHANNEL_PREFIX.length());
                log.debug("Received SSE message for user[{}] on node[{}]", userId, currentNodeId);
                handleRemoteMessage(userId, body);
            } catch (Exception e) {
                log.error("Error handling SSE message: {}", e.getMessage());
            }
        }
    }

    private static void handleRemoteMessage(String employeeCode, String jsonMsg) {
        SseEmitter emitter = sseEmitterMap.get(employeeCode);
        if (emitter != null) {
            try {
                emitter.send(jsonMsg, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                log.error("Failed to send SSE message to user[{}]: {}", employeeCode, e.getMessage());
                removeUser(employeeCode);
            }
        } else {
            storeOfflineMessage(employeeCode, jsonMsg);
        }
    }
}
package com.datalinkx.messagehub.service.redis;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.datalinkx.messagehub.bean.form.ConsumerAdapterForm;
import com.datalinkx.messagehub.bean.form.ProducerAdapterForm;
import com.datalinkx.messagehub.service.MessageHubServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.tokens.ScalarToken;


@Slf4j
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Service("redisStreamProcessor")
public class RedisStreamProcessor extends MessageHubServiceImpl {
    // 添加静态锁对象
    private static final Object GROUP_LOCK = new Object();

    @Override
    public void produce(ProducerAdapterForm producerAdapterForm) {
        ObjectRecord<String, Object> stringObjectRecord = ObjectRecord.create(producerAdapterForm.getTopic(), producerAdapterForm.getMessage());
        stringRedisTemplate.opsForStream().add(stringObjectRecord);
    }

    @Override
    public void consume(ConsumerAdapterForm messageForm) {

        String topic = messageForm.getTopic();
        String group = messageForm.getGroup();
        String consumerName = messageForm.getInvokeMethod().getName();
        Object consumerBean = messageForm.getBean();
        Method invokeMethod = messageForm.getInvokeMethod();

        if (ObjectUtils.isEmpty(group)) {
            throw new Error("REDIS_STREAM消费类型未指定消费者组");
        }

        new Thread(() -> {
            String lastOffset = null;
            StreamOperations<String, String, Object> streamOperations = this.stringRedisTemplate.opsForStream();
            // 对消费者组创建逻辑加锁
            synchronized (GROUP_LOCK) {
                if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(topic))) {
                    StreamInfo.XInfoGroups groups = streamOperations.groups(topic);
                    AtomicReference<Boolean> groupHasKey = new AtomicReference<>(false);
                    groups.forEach(groupInfo -> {
                        if (Objects.equals(group, groupInfo.getRaw().get("name"))) {
                            groupHasKey.set(true);
                        }
                    });
                    if (groups.isEmpty() || !groupHasKey.get()) {
                        String groupName = streamOperations.createGroup(topic, group);
                        log.info("messagehub stream creatGroup:{}", groupName);
                    }
                } else {
                    String groupName = streamOperations.createGroup(topic, group);
                    log.info("messagehub stream creatGroup:{}", groupName);
                }
            }

            while (true) {
                try {
                    ReadOffset readOffset = ReadOffset.lastConsumed();
                    List<ObjectRecord<String, String>> messageList = streamOperations.read(
                            String.class,
                            Consumer.from(group, consumerName),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(5)),
                            StreamOffset.create(topic, readOffset));

                    if (ObjectUtils.isEmpty(messageList)) {
                        // 如果为null，说明没有消息，继续下一次循环
                        Thread.sleep(1000);
                        continue;
                    }

                    for (ObjectRecord<String, String> record : messageList) {
                        lastOffset = record.getId().getValue();
                        invokeMethod.invoke(consumerBean, record.getValue());

                        stringRedisTemplate.opsForStream().acknowledge(group, record);
                    }
                } catch (Throwable e) {
                    log.error("messagehub stream consumer {} consume error, last offset: {}", consumerName, lastOffset);
                    log.error(e.getMessage(), e);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }
}

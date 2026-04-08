<template>
  <a-popover
    v-model="visible"
    trigger="click"
    placement="bottomRight"
    overlayClassName="header-notice-wrapper"
    :getPopupContainer="() => $refs.noticeRef.parentElement"
    :autoAdjustOverflow="true"
    :arrowPointAtCenter="true"
    :overlayStyle="{ width: '300px', top: '50px', maxHeight: '400px', overflow: 'auto' }"
  >
    <template slot="content">
      <a-spin :spinning="loading" tip="加载中...">
        <div class="notice-content">
          <a-list v-if="messages.length > 0" itemLayout="horizontal" :dataSource="messages">
            <a-list-item slot="renderItem" slot-scope="item" @click="markAsRead(item.id)" class="notice-item" :class="{ 'unread': !item.read }">
              <a-list-item-meta :title="item.title" :description="item.time">
                <a-avatar style="background-color: white" slot="avatar" :src="item.avatar"/>
              </a-list-item-meta>
              <a-tag v-if="!item.read" color="blue">未读</a-tag>
            </a-list-item>
          </a-list>
          <div v-else class="empty-notice">暂无消息</div>
          <div v-if="messages.length > 0" class="notice-footer">
            <a @click="markAllAsRead">全部标记为已读</a>
          </div>
        </div>
      </a-spin>
    </template>
    <span @click="fetchNotice" :class="[prefixCls, 'header-notice']" ref="noticeRef">
      <a-badge :count="unreadCount" :dot="unreadCount > 0">
        <a-icon type="bell" />
      </a-badge>
    </span>
  </a-popover>
</template>

<script>
import { getInSiteMessageList, markInSiteMessageAsRead, markAllInSiteMessagesAsRead } from '@/api/alarm/alarm'

export default {
  name: 'HeaderNotice',
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-drop-down'
    }
  },
  data () {
    return {
      loading: false,
      visible: false,
      messages: [],
      eventSource: null,
      reconnectCount: 0,
      maxReconnectCount: 5,
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  computed: {
    unreadCount () {
      const unreadMessages = this.messages.filter(item => !item.read).length
      return unreadMessages
    }
  },
  mounted () {
    // 初始加载通知数据
    this.fetchNoticeData()
    // 建立SSE连接
    this.setupSSE()
  },
  beforeDestroy () {
    // 关闭SSE连接
    if (this.eventSource) {
      this.eventSource.close()
    }
  },
  methods: {
    fetchNotice () {
      if (!this.visible) {
        this.loading = true
        this.fetchNoticeData()
      } else {
        this.loading = false
      }
      this.visible = !this.visible
    },
    fetchNoticeData () {
      const userId = '1'
      getInSiteMessageList(userId, this.currentPage, this.pageSize).then(response => {
        if (response && response.result) {
          const result = response.result
          if (result.data) {
            this.messages = result.data
          }
          this.total = result.total || 0
        }
        this.loading = false
      }).catch(error => {
        console.error('Failed to fetch notice data:', error)
        this.loading = false
      })
    },
    setupSSE () {
      // 建立SSE连接
      try {
        // 使用正确的SSE连接接口
        const userId = '1'
        this.eventSource = new EventSource(`/api/api/sse/connect/${userId}`)
        
        // 监听消息事件
        this.eventSource.addEventListener('message', (event) => {
          try {
            const data = JSON.parse(event.data)
            this.handleSSEMessage(data)
          } catch (error) {
            console.error('Error parsing SSE message:', error)
          }
        })
        
        // 监听错误事件
        this.eventSource.addEventListener('error', (error) => {
          console.error('SSE connection error:', error)
          // 尝试重连，但限制重连次数
          this.reconnectCount++ 
          if (this.reconnectCount < this.maxReconnectCount) {
            setTimeout(() => {
              this.setupSSE()
            }, 5000)
          } else {
            console.error('Max reconnection attempts reached, stopping')
          }
        })
        
        console.log('SSE connection established')
        // 重置重连计数器
        this.reconnectCount = 0
      } catch (error) {
        console.error('Error setting up SSE:', error)
        // 尝试重连，但限制重连次数
        this.reconnectCount++
        if (this.reconnectCount < this.maxReconnectCount) {
          setTimeout(() => {
            this.setupSSE()
          }, 5000)
        } else {
          console.error('Max reconnection attempts reached, stopping')
        }
      }
    },
    handleSSEMessage (data) {
      // 处理从后端接收的消息
      if (!data || !data.type) return
      
      if (data.type === 'message') {
        // 添加新消息
        this.addMessage(data)
      } else {
        console.log('Unknown message type:', data.type)
      }
    },
    addMessage (data) {
      // 添加新消息
      const newMessage = {
        id: data.id || Date.now(),
        title: data.title || '新消息',
        time: data.time || this.formatTime(new Date()),
        avatar: data.avatar || 'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png',
        read: false
      }
      this.messages.unshift(newMessage)
    },
    formatTime (date) {
      // 格式化时间
      const now = new Date()
      const diff = now - date
      const days = Math.floor(diff / (1000 * 60 * 60 * 24))
      
      if (days === 0) {
        // 今天
        return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      } else if (days === 1) {
        // 昨天
        return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      } else if (days < 7) {
        // 一周内
        const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
        return weekdays[date.getDay()] + ' ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      } else {
        // 其他
        return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
      }
    },
    markAsRead (id) {
      const userId = '1'
      markInSiteMessageAsRead(userId, id).then(() => {
        const item = this.messages.find(item => item.id === id)
        if (item) {
          item.read = true
        }
      }).catch(error => {
        console.error('Failed to mark message as read:', error)
      })
    },
    markAllAsRead () {
      const userId = '1'
      markAllInSiteMessagesAsRead(userId).then(() => {
        this.messages.forEach(item => {
          item.read = true
        })
      }).catch(error => {
        console.error('Failed to mark all messages as read:', error)
      })
    }
  }
}
</script>

<style lang="css">
  .header-notice-wrapper {
    top: 50px !important;
  }
  .empty-notice {
    padding: 20px;
    text-align: center;
    color: #999;
  }
  .notice-content {
    max-height: 300px;
    overflow-y: auto;
  }
  .notice-footer {
    padding: 10px;
    text-align: right;
    border-top: 1px solid #f0f0f0;
    margin-top: 10px;
  }
  .notice-footer a {
    font-size: 12px;
    color: #1890ff;
  }
  .notice-item {
    cursor: pointer;
    transition: all 0.3s;
  }
  .notice-item:hover {
    background-color: #f5f5f5;
  }
  .notice-item.unread {
    background-color: #e6f7ff;
  }
</style>
<style lang="less" scoped>
  .header-notice{
    display: inline-block;
    transition: all 0.3s;

    span {
      vertical-align: initial;
    }
    &:hover {
      color: #1890ff;
    }
  }
</style>
<style lang="less">
@import '~ant-design-vue/es/style/themes/default';

@header-drop-down-prefix-cls: ~'@{ant-prefix}-pro-drop-down';

.@{header-drop-down-prefix-cls} {
  line-height: @layout-header-height;
  vertical-align: top;
  cursor: pointer;

  > i {
    font-size: 16px !important;
    transform: none !important;

    svg {
      position: relative;
      top: -1px;
    }
  }
}
</style>

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
      eventSource: null
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
      // 模拟API请求获取消息数据
      setTimeout(() => {
        this.messages = [
          {
            id: 1,
            title: '张三给你发送了一条消息',
            time: '今天 09:30',
            avatar: 'https://gw.alipayobjects.com/zos/rmsportal/BiazfanxmamNRoxxVxka.png',
            read: false
          }
        ]
        
        this.loading = false
      }, 1000)
    },
    setupSSE () {
      // 建立SSE连接
      try {
        // 使用现有的SSE连接接口
        const userId = '1' // 这里应该从登录用户信息中获取
        this.eventSource = new EventSource(`/api/sse/connect/${userId}`)
        
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
          // 尝试重连
          setTimeout(() => {
            this.setupSSE()
          }, 5000)
        })
        
        console.log('SSE connection established')
      } catch (error) {
        console.error('Error setting up SSE:', error)
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
        time: this.formatTime(new Date()),
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
      const item = this.messages.find(item => item.id === id)
      if (item) {
        item.read = true
        console.log(`Marked message ${id} as read`)
      }
    },
    markAllAsRead () {
      this.messages.forEach(item => {
        item.read = true
      })
      console.log('Marked all messages as read')
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

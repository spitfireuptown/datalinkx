<template>
  <a-list
    itemLayout="horizontal"
    :dataSource="notificationSettings"
  >
    <a-list-item slot="renderItem" slot-scope="item">
      <a-list-item-meta
        :title="item.title"
        :description="item.description"
      />
      <a-switch
        :checked="item.value"
        @change="handleSwitchChange(item.key, $event)"
      />
    </a-list-item>
  </a-list>
</template>

<script>
export default {
  data () {
    return {
      notificationSettings: [
        {
          key: 'password',
          title: this.$t('account.settings.notification.password'),
          description: this.$t('account.settings.notification.password-description'),
          value: true
        },
        {
          key: 'messages',
          title: this.$t('account.settings.notification.messages'),
          description: this.$t('account.settings.notification.messages-description'),
          value: true
        },
        {
          key: 'todo',
          title: this.$t('account.settings.notification.todo'),
          description: this.$t('account.settings.notification.todo-description'),
          value: true
        }
      ]
    }
  },
  methods: {
    handleSwitchChange (key, checked) {
      const setting = this.notificationSettings.find(item => item.key === key)
      if (setting) {
        setting.value = checked
        // 这里可以添加保存设置的逻辑
        console.log(`Notification setting ${key} changed to ${checked}`)
      }
    }
  }
}
</script>

<style scoped>

</style>

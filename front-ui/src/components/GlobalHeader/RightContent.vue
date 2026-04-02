<template>
  <div :class="wrpCls">
    <notice-icon :class="prefixCls" />
    <select-lang :class="prefixCls" />
  </div>
</template>

<script>
import SelectLang from '@/components/SelectLang'
import NoticeIcon from '@/components/NoticeIcon/NoticeIcon'

export default {
  name: 'RightContent',
  components: {
    SelectLang,
    NoticeIcon
  },
  props: {
    prefixCls: {
      type: String,
      default: 'ant-pro-global-header-index-action'
    },
    isMobile: {
      type: Boolean,
      default: () => false
    },
    topMenu: {
      type: Boolean,
      required: true
    },
    theme: {
      type: String,
      required: true
    }
  },
  data () {
    return {
      showMenu: true,
      currentUser: {}
    }
  },
  computed: {
    wrpCls () {
      return {
        'ant-pro-global-header-index-right': true,
        [`ant-pro-global-header-index-${(this.isMobile || !this.topMenu) ? 'light' : this.theme}`]: true
      }
    }
  },
  mounted () {
    setTimeout(() => {
      this.currentUser = {
        name: 'Serati Ma'
      }
    }, 1500)
  }
}
</script>

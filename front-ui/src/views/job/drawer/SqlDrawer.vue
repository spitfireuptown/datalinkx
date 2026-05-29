<template>
  <a-drawer
    title="SQL算子"
    placement="right"
    :closable="false"
    :visible="visible"
    :after-visible-change="afterVisibleChange"
    @close="onClose"
    width="500"
  >
    <div class="sql-hint">
      <p>1、当前画布仅支持单SQL算子</p>
      <p>2、字符串必须使用单引号''</p>
    </div>
    <div class="sql-content">
      <span class="sql-label">
        SQL语句
      </span>
      <a-textarea
        @drop="handleDrop"
        @dragover.prevent
        :value="sql"
        placeholder="请输入符合来源数据源的完整的SQL语句"
        :auto-size="{ minRows: 15, maxRows: 25 }"
        @input="handleSqlInput"
      />
    </div>
  </a-drawer>
</template>

<script>
import { validateTransformMeta } from '@/api/job/transform'

export default {
  name: 'SqlDrawer',

  props: {
    visible: {
      type: Boolean,
      default: false
    },
    tags: {
      type: Array,
      default: () => []
    },
    sql: {
      type: String,
      default: ''
    },
    graph: {
      default: null
    },
    type: {
      type: String,
      default: 'sql'
    },
    dsId: {
      type: String,
      default: ''
    }
  },

  data () {
    return {
      inputVisible: false,
      inputValue: ''
    }
  },

  methods: {
    handleDragStart (tag, event) {
      event.dataTransfer.setData('text', tag)
    },

    handleDrop (event) {
      event.preventDefault()
      const tag = event.dataTransfer.getData('text')
      const newValue = this.sql
        ? `${this.sql} ${tag}`
        : tag
      this.$emit('sql-change', newValue)
      this.$emit('tag-add', tag)
    },

    handleSqlInput (e) {
      this.$emit('sql-change', e.target.value)
    },

    cleanSql () {
      this.$emit('sql-change', '')
    },

    afterVisibleChange (val) {
      this.$emit('visible-change', val)
    },

    onClose () {
      if (this.graph) {
        this.$emit('before-close')
        const graphData = this.graph.toJSON()
        validateTransformMeta({ graph: JSON.stringify(graphData), type: this.type, dsId: this.dsId })
          .then(res => {
            const result = res.result
            if (result.valid) {
              // 将outputFields传递给父组件
              if (result.outputFields && result.outputFields.length > 0) {
                this.$emit('validate-success', result.outputFields)
              }
              this.$emit('close')
            } else {
              this.$message.warning('校验失败：' + result.message + '，仍将保存当前配置')
              this.$emit('close')
            }
          })
          .catch(() => {
            this.$emit('close')
          })
      } else {
        this.$emit('close')
      }
    }
  }
}
</script>

<style lang="less" scoped>
.sql-hint {
  margin-bottom: 12px;
  padding: 8px;
  background-color: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 4px;

  p {
    margin: 0;
    color: #d48806;
  }
}

.tag-container {
  margin-bottom: 12px;
}

.new-field-btn {
  margin-bottom: 12px;
}

.sql-content {
  .sql-label {
    display: block;
    margin: 8px 0;
    font-weight: 500;
  }
}
</style>
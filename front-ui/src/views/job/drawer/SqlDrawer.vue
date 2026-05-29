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

    <div class="tag-container">
      <a-tag
        v-for="tag in tags"
        :key="tag"
        draggable
        @dragstart="handleDragStart(tag, $event)"
      >
        {{ tag }}
      </a-tag>
    </div>

    <div class="new-field-btn">
      <a-input
        v-if="inputVisible"
        ref="inputRef"
        type="text"
        size="small"
        :style="{ width: '120px' }"
        :value="inputValue"
        @change="handleInputChange"
        @blur="handleInputConfirm"
        @keyup.enter="handleInputConfirm"
      />
      <a-tag
        v-else
        style="background: #fff; border-style: dashed;"
        @click="showInput"
      >
        <a-icon type="plus" /> New Field
      </a-tag>
    </div>

    <div class="sql-content">
      <span class="sql-label">
        select
        <a-icon type="sync" @click="cleanSelect" />
      </span>
      <a-textarea
        @drop="handleDrop"
        @dragover.prevent
        :value="sqlValue"
        :disabled="disabledTrue"
        placeholder="基于上游节点字段, 从上面的标签中拖拽至此处"
        @input="handleSqlInput"
      />

      <span class="sql-label">from</span>
      <a-input
        :value="fromTable"
        :disabled="disabledTrue"
        @input="handleFromInput"
      />

      <span class="sql-label">where</span>
      <a-textarea
        :value="whereValue"
        placeholder=""
        @input="handleWhereInput"
      />

      <span class="sql-label">group</span>
      <a-textarea
        :value="groupValue"
        placeholder=""
        @input="handleGroupInput"
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
    sqlValue: {
      type: String,
      default: ''
    },
    fromTable: {
      type: String,
      default: ''
    },
    whereValue: {
      type: String,
      default: ''
    },
    groupValue: {
      type: String,
      default: ''
    },
    disabledTrue: {
      type: Boolean,
      default: true
    },
    graph: {
      default: null
    },
    type: {
      type: String,
      default: 'sql'
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
      const newValue = this.sqlValue
        ? `${this.sqlValue}, ${tag}`
        : tag
      this.$emit('sql-change', newValue)
      this.$emit('tag-add', tag)
    },

    handleSqlInput (e) {
      this.$emit('sql-change', e.target.value)
    },

    handleFromInput (e) {
      this.$emit('from-change', e.target.value)
    },

    handleWhereInput (e) {
      this.$emit('where-change', e.target.value)
    },

    handleGroupInput (e) {
      this.$emit('group-change', e.target.value)
    },

    cleanSelect () {
      this.$emit('clean-select')
    },

    showInput () {
      this.inputVisible = true
      this.$nextTick(() => {
        this.$refs.inputRef && this.$refs.inputRef.focus()
      })
    },

    handleInputChange (e) {
      this.inputValue = e.target.value
    },

    handleInputConfirm () {
      const value = this.inputValue
      if (value && !this.tags.includes(value)) {
        this.$emit('tag-add', value)
      }
      this.inputValue = ''
      this.inputVisible = false
    },

    afterVisibleChange (val) {
      this.$emit('visible-change', val)
    },

    onClose () {
      if (this.graph) {
        const graphData = this.graph.toJSON()
        validateTransformMeta({ graph: JSON.stringify(graphData), type: this.type })
          .then(res => {
            const result = res.result
            if (result.valid) {
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

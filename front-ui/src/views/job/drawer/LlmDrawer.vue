<template>
  <a-drawer
    title="大模型算子"
    placement="right"
    :closable="false"
    :visible="visible"
    :after-visible-change="afterVisibleChange"
    @close="onClose"
    width="500"
  >
    <div class="llm-hint">
      <p>1、如果需要引用来源表字段使用${}，例：${division_name}</p>
      <p>2、使用OPENAI模型代理地址需配置完整的模型对话url</p>
    </div>

    <div class="llm-content">
      <span class="llm-label">模型提供商</span>
      <a-select
        :value="modelProvider"
        style="width: 100%; margin-bottom: 12px;"
        placeholder="请选择模型提供商"
        @change="handleModelProviderChange"
      >
        <a-select-option value="OPENAI">OPENAI</a-select-option>
        <a-select-option value="DOUBAO">DOUBAO</a-select-option>
        <a-select-option value="KIMIAI">KIMIAI</a-select-option>
        <a-select-option value="CUSTOM">CUSTOM</a-select-option>
      </a-select>
    </div>

    <div class="llm-content">
      <span class="llm-label">模型名称 <span class="required">*</span></span>
      <a-input
        :value="model"
        style="width: 100%; margin-bottom: 12px;"
        placeholder="请输入模型名称"
        @input="handleModelInput"
        :required="true"
      />
    </div>

    <div class="llm-content">
      <span class="llm-label">API Key <span class="required">*</span></span>
      <a-input-password
        :value="apiKey"
        style="width: 100%; margin-bottom: 12px;"
        placeholder="请输入API Key"
        @input="handleApiKeyInput"
        :required="true"
      />
    </div>

    <div class="llm-content">
      <span class="llm-label">API Path</span>
      <a-input
        :value="apiPath"
        style="width: 100%; margin-bottom: 12px;"
        placeholder="请输入API Path"
        @input="handleApiPathInput"
      />
    </div>

    <div class="llm-content">
      <span class="llm-label">大模型prompt</span>
      <a-textarea
        :value="prompt"
        style="height: 121px;"
        placeholder="所有字段都将可以用作模型输入，直接使用字段名称编写prompt，只支持英文，例：Determine whether someone is Chinese or American by their name(根据名字判断某人是中国人还是美国人)"
        @input="handlePromptInput"
      />
    </div>
  </a-drawer>
</template>

<script>
import { validateTransformMeta } from '@/api/job/transform'

export default {
  name: 'LlmDrawer',

  props: {
    visible: {
      type: Boolean,
      default: false
    },
    modelProvider: {
      type: String,
      default: 'OPENAI'
    },
    model: {
      type: String,
      default: ''
    },
    apiKey: {
      type: String,
      default: ''
    },
    apiPath: {
      type: String,
      default: ''
    },
    prompt: {
      type: String,
      default: ''
    },
    graph: {
      default: null
    },
    nodeId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: 'llm'
    }
  },

  methods: {
    handleModelProviderChange (value) {
      this.$emit('model-provider-change', value)
    },

    handleModelInput (e) {
      this.$emit('model-change', e.target.value)
    },

    handleApiKeyInput (e) {
      this.$emit('api-key-change', e.target.value)
    },

    handleApiPathInput (e) {
      this.$emit('api-path-change', e.target.value)
    },

    handlePromptInput (e) {
      this.$emit('prompt-change', e.target.value)
    },

    afterVisibleChange (val) {
      this.$emit('visible-change', val)
    },

    onClose () {
      // 先将配置保存到节点
      if (this.graph && this.nodeId) {
        const node = this.graph.getNodes().find(n => n.id === this.nodeId)
        if (node) {
          const currentData = node.getData() || {}
          node.setData({
            ...currentData,
            modelProvider: this.modelProvider,
            model: this.model,
            apiKey: this.apiKey,
            apiPath: this.apiPath,
            prompt: this.prompt
          })
        }
      }

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
.llm-hint {
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

.llm-content {
  .llm-label {
    display: block;
    margin-bottom: 8px;
    font-weight: 500;
  }
  
  .required {
    color: #ff4d4f;
  }
}
</style>

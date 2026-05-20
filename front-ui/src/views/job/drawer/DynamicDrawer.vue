<template>
  <a-drawer
    title="动态编译算子"
    placement="right"
    :closable="false"
    :visible="visible"
    :after-visible-change="afterVisibleChange"
    @close="onClose"
    width="500"
  >
    <div class="dynamic-hint">
      <p>
      动态编译算子允许您编写自定义代码进行处理。<a href="https://github.com/apache/seatunnel/tree/dev/seatunnel-e2e/seatunnel-transforms-v2-e2e/seatunnel-transforms-v2-e2e-part-2/src/test/resources/dynamic_compile/conf" target="_blank">更多复杂例子参考</a>
      </p>
    </div>

    <div class="dynamic-content">
      <div class="label-wrapper">
        <span class="dynamic-label">
          Java源代码
          <a-tooltip :mouse-enter-delay="0.3">
            <template #title>
              <div class="rule-tooltip">
                <p>在源代码中，您必须实现两个方法：</p>
                <p class="method-name">Column[] getInlineOutputColumns(CatalogTable inputCatalogTable)</p>
                <p class="method-name">Object[] getInlineOutputFieldValues(SeaTunnelRowAccessor inputRow)</p>
                <p>getInlineOutputColumns方法，输入参数为CatalogTable，返回类型为Column[]。</p>
                <p>您可以从获取当前表的模式CatalogTable。</p>
                <p>如果返回的列存在于当前模式中，则它将被返回值（字段类型、注释等）覆盖；如果是新列，它将被添加到当前模式中。</p>
                <p>getInlineOutputFieldValues方法，输入参数为SeaTunnelRowAccessor，返回类型为Object[]。</p>
                <p>您可以从中获取记录SeaTunnelRowAccessor，并进行自定义数据处理逻辑。</p>
                <p>返回Object[]数组的长度应与方法结果的长度匹配getInlineOutputColumns，顺序也必须匹配。</p>
              </div>
            </template>
            <span class="rule-icon">
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
            </span>
          </a-tooltip>
        </span>
        <a-button
          type="primary"
          size="small"
          :loading="validating"
          @click="handleValidate"
        >
          语法校验
        </a-button>
      </div>
      <a-textarea
          :value="localSourceCode"
          style="height: 600px;"
          placeholder="请输入源代码..."
          @input="handleSourceCodeInput"
        />
    </div>
  </a-drawer>
</template>

<script>
import { validateDynamicCode } from '@/api/job/dynamicCode'

const DEFAULT_SOURCE_CODE = 'import org.apache.seatunnel.api.table.catalog.Column;\nimport org.apache.seatunnel.transform.common.SeaTunnelRowAccessor;\nimport org.apache.seatunnel.api.table.catalog.*;\nimport org.apache.seatunnel.api.table.type.*;\nimport java.util.ArrayList;\n\npublic Column[] getInlineOutputColumns(CatalogTable inputCatalogTable) {\n    PhysicalColumn col1 =\n        PhysicalColumn.of(\n            "compile_language",\n            BasicType.STRING_TYPE,\n            10L,\n            true,\n            "",\n            "");\n    PhysicalColumn col2 =\n        PhysicalColumn.of(\n            "age",\n            BasicType.INT_TYPE,\n            0L,\n            false,\n            false,\n            ""\n        );\n    return new Column[] {\n        col1, col2\n    };\n}\n\npublic Object[] getInlineOutputFieldValues(SeaTunnelRowAccessor inputRow) {\n    Object[] fieldValues = new Object[2];\n    // get age\n    Object ageField = inputRow.getField(1);\n    fieldValues[0] = "JAVA";\n    if (Integer.parseInt(ageField.toString()) == 20) {\n        fieldValues[1] = 40;\n    } else {\n        fieldValues[1] = ageField;\n    }\n    return fieldValues;\n}'

export default {
  name: 'DynamicDrawer',

  props: {
    visible: {
      type: Boolean,
      default: false
    },
    sourceCode: {
      type: String,
      default: ''
    }
  },

  data () {
    return {
      localSourceCode: '',
      validating: false
    }
  },

  watch: {
    visible (val) {
      if (val) {
        this.localSourceCode = this.sourceCode || DEFAULT_SOURCE_CODE
      }
    }
  },

  methods: {
    handleSourceCodeInput (e) {
      this.localSourceCode = e.target.value
      this.$emit('source-code-change', e.target.value)
    },

    handleValidate () {
      if (!this.localSourceCode.trim()) {
        this.$message.warning('请输入源代码')
        return
      }

      this.validating = true
      validateDynamicCode({ source_code: this.localSourceCode })
        .then(res => {
          const result = res.result
          if (result.valid) {
            this.$message.success(result.message)
            // 将解析的字段返回给父组件
            if (result.outputFields && result.outputFields.length > 0) {
              this.$emit('validate-success', result.outputFields)
              this.$message.success(`成功解析到 ${result.outputFields.length} 个输出字段`)
            }
          } else {
            this.$message.error(result.message)
          }
        })
        .catch(err => {
          this.$message.error('语法校验失败：' + (err.message || '未知错误'))
        })
        .finally(() => {
          this.validating = false
        })
    },

    afterVisibleChange (val) {
      this.$emit('visible-change', val)
    },

    onClose () {
      // 关闭前自动进行语法校验
      if (this.localSourceCode.trim()) {
        validateDynamicCode({ source_code: this.localSourceCode })
          .then(res => {
            const result = res.result
            if (result.valid) {
              // 校验成功，解析字段
              if (result.outputFields && result.outputFields.length > 0) {
                this.$emit('validate-success', result.outputFields)
              }
              this.$emit('close')
            } else {
              // 校验失败，提示用户但仍然关闭
              this.$message.warning('语法校验失败：' + result.message + '，仍将保存当前代码')
              this.$emit('close')
            }
          })
          .catch(() => {
            // 请求失败，直接关闭
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
.dynamic-hint {
  margin-bottom: 12px;
  padding: 8px;
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
  border-radius: 4px;

  p {
    margin: 0;
    color: #0050b3;
  }
}

.dynamic-content {
  .label-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
  }

  .dynamic-label {
    font-weight: 500;
  }

  .rule-icon {
    cursor: pointer;
    color: #1890ff;
    transition: color 0.3s;

    &:hover {
      color: #40a9ff;
    }
  }
}

.rule-tooltip {
  max-width: 400px;
  white-space: pre-wrap;
  word-break: break-all;

  p {
    margin: 4px 0;
    font-size: 13px;
    line-height: 1.6;
    color: #fff;
  }

  .method-name {
    font-family: 'Monaco', 'Menlo', monospace;
    background-color: rgba(255, 255, 255, 0.15);
    padding: 2px 6px;
    border-radius: 3px;
    font-size: 12px;
    color: #fff;
    margin: 6px 0;
  }
}
</style>

<template>
  <a-modal
    title="钉钉告警组件"
    :width="640"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <a-form-item
          v-show="false"
          label="ds_id"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            v-decorator="['alarmId']"
            :disabled="editable || showable"
          />
        </a-form-item>
        <a-form-item
          label="名称"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            type="text"
            :disabled="showable"
            v-decorator="['name', {rules: [{required: true, message: '请输入名称'}]}]" />
        </a-form-item>
        <a-form-item
          label="webhook地址"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            type="text"
            :disabled="showable"
            v-decorator="['webhook', {rules: [{required: true, message: '请输入webhook地址'}]}]" />
        </a-form-item>
        <a-form-item
          label="secret"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            type="text"
            :disabled="showable"
            v-decorator="['secret', {rules: [{required: false, message: '请输入secret地址'}]}]" />
        </a-form-item>
      </a-form>
    </a-spin>
    <template slot="footer" >
      <div
        v-show="editable || addable"
      >
        <a-button key="cancel" @click="handleCancel">取消</a-button>
        <a-button key="forward" :loading="confirmLoading" type="primary" @click="handleOk">保存</a-button>
      </div>
    </template>
  </a-modal>

</template>

<script>
import pick from 'lodash.pick'
import { getObj, addObj, putObj } from '@/api/alarm/alarm'
let selectTables = []

export default {
  name: 'DsSaveOrUpdate',
  data () {
    return {
      labelCol: {
        xs: { span: 24 },
        sm: { span: 7 }
      },
      wrapperCol: {
        xs: { span: 24 },
        sm: { span: 13 }
      },
      longWrapperCol: {
        xs: { span: 44 },
        sm: { span: 13 }
      },
      visible: false,
      confirmLoading: false,
      form: this.$form.createForm(this),
      editable: false,
      addable: false,
      showable: false,
      type: 'add'
    }
  },
  created () {
    this.init()
  },
  methods: {
    // 获取用户信息
    edit (id, type) {
      this.visible = true
      if (type && type === 'add') {
        this.addable = true
        this.type = type
      }
      if (type === 'edit') {
        this.editable = true
        this.type = type
      }
      if (type === 'show') {
        this.showable = true
        this.type = type
      }

      const { form: { setFieldsValue, resetFields } } = this
      if (['edit', 'show'].includes(type)) {
        this.confirmLoading = true
        getObj(id).then(res => {
          const record = res.result.config
          record['alarmId'] = res.result['alarm_id']
          record['name'] = res.result['name']
          console.log(record)
          this.confirmLoading = false
          this.$nextTick(() => {
            setFieldsValue(pick(record, ['alarmId', 'type', 'name', 'webhook', 'secret']))
          })
        })
      } else {
        resetFields()
      }
    },
    handleChange (value) {
      selectTables.push(value)
      console.log(`Selected: ${value}`)
      console.log(selectTables)
    },
    // 结束保存用户信息
    handleOk () {
      this.form.validateFields(async (err, values) => {
        if (!err) {
          values['type'] = 1
          values['config'] = JSON.stringify({
            'type': 1,
            'webhook': values['webhook'],
            'secret': values['secret']
          })
          this.confirmLoading = true
          if (this.type === 'add') {
            addObj(values).then(res => {
              if (res.status === '0') {
                this.$emit('ok')
                this.confirmLoading = false
                // 清楚表单数据
                this.handleCancel()
                this.$message.success('新增成功')
              } else {
                this.confirmLoading = false
                this.$message.error(res.errstr)
              }
            }).catch(err => {
              this.confirmLoading = false
              this.$message.error(err.errstr)
            })
          } else if (this.type === 'edit') {
            putObj(values).then(res => {
              if (res.status === '0') {
                this.$emit('ok')
                this.confirmLoading = false
                // 清楚表单数据
                this.handleCancel()
                this.$message.success('更新成功')
              } else {
                this.confirmLoading = false
                this.$message.error(res.errstr)
              }
            }).catch(err => {
              this.confirmLoading = false
              this.$message.error(err.errstr)
            })
          }
        }
      })
      selectTables = []
    },
    handleCancel () {
      this.visible = false
      setTimeout(() => {
        this.addable = false
        this.showable = false
        this.editable = false
      }, 200)
    },
    init () {
    },
    handleSearch (value) {

    }
  }
}
</script>

<style scoped>

</style>

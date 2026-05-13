<template>
  <a-modal
    :title="title"
    :width="640"
    :visible="visible"
    :confirmLoading="confirmLoading"
    :maskClosable="false"
    @cancel="handleCancel"
  >
    <a-spin :spinning="confirmLoading">
      <a-form :form="form">
        <!-- <a-form-item
          v-show="false"
          label="ds_id"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            v-decorator="['dsId']"
            :disabled="editable || showable"
          />
        </a-form-item> -->

        <a-form-item
          label="当前数据源"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <span>{{ currentDs.label }}</span>
        </a-form-item>

        <a-form-item
          v-for="(item) in currentDsList"
          :key="item.key"
          :label="item.label"
          :labelCol="labelCol"
          :wrapperCol="wrapperCol"
        >
          <a-input
            type="text"
            v-if="item.type === 'input'"
            :disabled="showable"
            v-decorator="[item.key, {rules: [{required: item.isRequired, message: item.decorator}]}]" />
          <a-input-password
            type="text"
            v-if="item.type === 'password'"
            :disabled="showable"
            v-decorator="[item.key, {rules: [{required: item.isRequired, message: item.decorator}]}]" />
          <a-textarea
            type="text"
            v-if="item.type === 'textarea'"
            :auto-size="{ minRows: 2, maxRows: 8 }"
            :disabled="showable"
            v-decorator="[item.key, {rules: [{required: item.isRequired, message: item.decorator}]}]" />
          <a-select
            :disabled="showable"
            v-if="item.type === 'select' && currentDs.dsTypeKey === 'oracle'"
            v-decorator="[item.key, {rules: [{required: item.isRequired, message: item.decorator}]}]"
            :options="OracleServerTypes"
          >
          </a-select>
          <a-select
            :disabled="showable"
            mode="tags"
            v-if="item.type === 'select' && currentDs.dsTypeKey === 'mysqlcdc'"
            v-decorator="[item.key, {rules: [{required: item.isRequired, message: item.decorator}]}]"
            :options="MysqlCDCTypes"
          >
          </a-select>
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
import { getObj, addObj, putObj } from '@/api/datasource/datasource'
import { dsConfigOriginList, OracleServerTypes, MysqlCDCTypes, DataSourceType } from './const'
import cloneDeep from 'lodash.clonedeep'

let selectTables = []

export default {
  name: 'DsConfig',
  props: {
    currentDs: {
      type: Object,
      default: () => {
        return {
          value: 'MySQL',
          label: 'MySQL',
          dsTypeKey: 'mysql'
        }
      }
    }
  },
  data () {
    return {
      dsConfigOriginList,
      OracleServerTypes,
      MysqlCDCTypes,
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
      DataSourceType: DataSourceType,
      type: 'add',
      dsId: ''
    }
  },
  computed: {
    title () {
      switch (this.type) {
        case 'add':
          return '新建数据源'
        case 'edit':
          return '修改数据源'
        default:
          return '查看数据源'
      }
    },
    currentDsList () {
      let list = cloneDeep(this.dsConfigOriginList)
      if (this.currentDs.dsTypeKey === 'mysql') return list
      if (this.currentDs.dsTypeKey === 'es') {
        list = list.filter(item => {
          return item.key !== 'database'
        })
      }
      list.forEach(item => {
        switch (this.currentDs.dsTypeKey) {
          case 'redis':
          case 'es': {
            item.isRequired = ['host', 'port'].includes(item.key)
            break
          }
          case 'oracle': {
            item.isRequired = ['host', 'port', 'username', 'password', 'database'].includes(item.key)
            break
          }
          case 'kafka': {
            item.isRequired = ['host', 'port'].includes(item.key)
          }
        }
      })
      if (this.currentDs.dsTypeKey === 'oracle') {
        list.push({
          label: '服务器类型',
          key: 'servertype',
          type: 'select',
          decorator: '请选择',
          isRequired: true
        }, {
          label: 'SID或服务器名',
          key: 'sid',
          type: 'input',
          decorator: '请输入',
          isRequired: true
        })
      } else if (this.currentDs.dsTypeKey === 'mysqlcdc') {
        list.push({
          label: '监听事件',
          key: 'listentype',
          type: 'select',
          decorator: '请选择',
          isRequired: true
        })
      }
      return list
    }
  },
  methods: {
    show (id, type, info) {
      console.log(this.currentDs)
      this.type = type
      this.dsId = id
      switch (type) {
        case 'add':
          this.showable = false
          this.addable = true
          break
        case 'edit':
          this.showable = false
          this.editable = true
          break
        default:
          this.showable = true
          break
      }
      this.visible = true
      const { form: { setFieldsValue, resetFields } } = this
      if (['edit', 'show'].includes(type)) {
        if (!info) {
          this.confirmLoading = true
          getObj(id).then(res => {
            const record = res.result
            this.confirmLoading = false
            this.$nextTick(() => {
              setFieldsValue(this.formatData(record))
            })
          })
        } else {
          this.$nextTick(() => {
            setFieldsValue(this.formatData(info))
          })
        }
      } else {
        this.$nextTick(() => {
          resetFields()
        })
      }
    },
    // 格式化复显表单数据
    formatData (info) {
      const temp = pick(info, ['name', 'host', 'port', 'username', 'database', 'password', 'config'])
      if (this.currentDs.dsTypeKey === 'oracle') {
        const config = JSON.parse(info.config)
        if (config) {
          const servertype = Object.keys(config)[0]
          temp['servertype'] = servertype
          temp[servertype] = config[servertype]

          temp['sid'] = config['servername']
        }
      }
      if (this.currentDs.dsTypeKey === 'mysqlcdc') {
        const config = JSON.parse(info.config)
        if (config) {
          temp['listentype'] = config['listentype']
        }
      }
      return temp
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
          values.type = this.currentDs.dsTypeKey
          values.ds_id = this.dsId
          if (this.currentDs.dsTypeKey === 'oracle') {
            const temp = {}
            temp[values.servertype] = values['sid']
            values.config = JSON.stringify(temp)
          }
          if (this.currentDs.dsTypeKey === 'mysqlcdc') {
            const temp = {}
            temp['listentype'] = values['listentype']
            values.config = JSON.stringify(temp)
          }
          this.confirmLoading = true
          if (this.type === 'add') {
            console.log(values)
            addObj(values).then(res => {
              if (res.status === '0') {
                this.$emit('ok')
                this.confirmLoading = false
                // 清楚表单数据
                this.handleCancel()
                this.$message.success('新增成功')
                // 触发全局事件，通知其他组件刷新统计信息
                this.$emit('dsCreated', { type: 'add' })
                // 刷新统计信息
                this.refreshStats()
              } else {
                this.confirmLoading = false
                this.$message.error(res.errstr)
              }
            }).catch(err => {
              this.confirmLoading = false
              const errorMessage = err.errstr || err.response?.data?.errstr || '保存失败'
              this.$message.error(errorMessage)
            })
          } else if (this.type === 'edit') {
            console.log('----', values)
            await putObj(values).then(res => {
              if (res.status === '0') {
                this.$emit('ok')
                this.confirmLoading = false
                // 清楚表单数据
                this.handleCancel()
                this.$message.success('修改成功')
              } else {
                this.confirmLoading = false
                this.$message.error(res.errstr)
              }
            }).catch(err => {
              this.confirmLoading = false
              const errorMessage = err.errstr || err.response?.data?.errstr || '修改失败'
              this.$message.error(errorMessage)
            })
          }
        }
      })
      selectTables = []
    },

    // 刷新统计信息
    refreshStats () {
      // 这里可以发出事件或其他方式通知全局统计刷新
      // 例如，可以通过 Vuex 或者总线模式通知其他组件
      this.$emit('refreshStats')
    },
    handleCancel () {
      this.visible = false
      setTimeout(() => {
        this.addable = false
        this.showable = false
        this.editable = false
      }, 200)
    }
  }
}
</script>

<style scoped>

</style>
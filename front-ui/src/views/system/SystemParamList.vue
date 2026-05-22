<template>
  <div class="system-param-list">
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-item label="参数Key">
                <a-input v-model="queryParam.param_key" placeholder="请输入参数Key" allowClear/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-form-item label="参数范围">
                <a-select v-model="queryParam.param_scope" placeholder="请选择参数范围" allowClear>
                  <a-select-option value="patch_job">批式任务</a-select-option>
                  <a-select-option value="stream_job">流式任务</a-select-option>
                  <a-select-option value="compute_job">计算任务</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-button @click="queryData" type="primary" icon="search">查询</a-button>
              <a-button @click="resetQuery" style="margin-left: 12px" icon="reload">重置</a-button>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <div class="table-operator">
        <a-button @click="showModal" type="primary" icon="plus">新增参数</a-button>
      </div>
      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="loading"
        :pagination="pagination"
        :rowKey="record => record.id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'paramValue'">
            <span :title="record.paramValue" class="ellipsis">{{ record.paramValue }}</span>
          </template>
          <template v-if="column.key === 'paramDesc'">
            <span :title="record.paramDesc" class="ellipsis">{{ record.paramDesc }}</span>
          </template>
        </template>
      </a-table>

      <a-modal
        :title="modalTitle"
        :visible="visible"
        :confirmLoading="confirmLoading"
        @ok="handleOk"
        @cancel="handleCancel"
      >
        <a-form :form="form" layout="vertical">
          <a-form-item label="ID" :required="false" style="display: none">
            <a-input v-decorator="['id']" />
          </a-form-item>
          <a-form-item label="参数Key" :required="true">
            <a-input
              v-decorator="['paramKey', { rules: [{ required: true, message: '请输入参数Key' }] }]"
              placeholder="请输入参数Key"
            />
          </a-form-item>
          <a-form-item label="参数值" :required="true">
            <a-input
              v-decorator="['paramValue', { rules: [{ required: true, message: '请输入参数值' }] }]"
              placeholder="请输入参数值"
            />
          </a-form-item>
          <a-form-item label="参数描述">
            <a-input
              v-decorator="['paramDesc']"
              placeholder="请输入参数描述"
            />
          </a-form-item>
          <a-form-item label="参数范围">
            <a-select v-decorator="['paramScope']" placeholder="请选择参数范围">
              <a-select-option value="patch_job">批式任务</a-select-option>
              <a-select-option value="stream_job">流式任务</a-select-option>
              <a-select-option value="compute_job">计算任务</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-modal>
    </a-card>
  </div>
</template>

<script>
import { Card, Form, Input, Select, Button, Modal, Table, Switch, Divider, Popconfirm, Tag } from 'ant-design-vue'
import { pageQuery, create, modify, del, info, toggleEnable } from '@/api/system/systemParam'

export default {
  name: 'SystemParamList',
  components: {
    'a-card': Card,
    'a-form': Form,
    'a-form-item': Form.Item,
    'a-input': Input,
    'a-select': Select,
    'a-select-option': Select.Option,
    'a-button': Button,
    'a-modal': Modal,
    'a-table': Table,
    'a-switch': Switch,
    'a-tag': Tag,
    'a-divider': Divider,
    'a-popconfirm': Popconfirm
  },
  data () {
    return {
      loading: false,
      visible: false,
      confirmLoading: false,
      isEdit: false,
      modalTitle: '新增系统参数',
      form: this.$form.createForm(this),
      scopeMap: {
        'patch_job': '批式任务',
        'stream_job': '流式任务',
        'compute_job': '计算任务'
      },
      columns: [
        {
          title: '参数Key',
          dataIndex: 'paramKey',
        },
        {
          title: '参数值',
          dataIndex: 'paramValue',
          key: 'paramValue',
        },
        {
          title: '参数描述',
          dataIndex: 'paramDesc',
          key: 'paramDesc',
        },
        {
          title: '参数范围',
          dataIndex: 'paramScope',
          customRender: (text) => {
            return this.scopeMap[text] || text
          }
        },
        {
          title: '状态',
          dataIndex: 'enable',
          customRender: (text) => {
            return text ? <a-tag color="green">启用</a-tag> : <a-tag color="red">禁用</a-tag>
          }
        },
        {
          title: '操作',
          customRender: (record) => {
            return (
              <div class="table-actions">
                <a-button type="link" onClick={() => this.edit(record)}>修改</a-button>
                <a-divider type="vertical" />
                <a-button type="link" onClick={() => this.toggleStatus(record)}>
                  {record.enable ? '禁用' : '启用'}
                </a-button>
                <a-divider type="vertical" />
                <a-popconfirm
                  title="确定要删除该参数吗?"
                  onConfirm={() => this.delete(record)}
                  okText="确定"
                  cancelText="取消"
                >
                  <a-button type="link" class="delete-btn">删除</a-button>
                </a-popconfirm>
              </div>
            )
          }
        }
      ],
      tableData: [],
      pagination: {
        pageSize: 10,
        current: 1,
        total: 0,
        showSizeChanger: true,
        showQuickJumper: true
      },
      pages: {
        pageSize: 10,
        pageNo: 1
      },
      queryParam: {}
    }
  },
  methods: {
    init () {
      this.loading = true
      pageQuery({
        ...this.queryParam,
        ...this.pages
      }).then(res => {
        if (res.status === '0') {
          this.tableData = res.result.data
          this.pagination.total = +res.result.total
        }
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    showModal () {
      this.isEdit = false
      this.modalTitle = '新增系统参数'
      this.form.resetFields()
      this.visible = true
    },
    edit (record) {
      this.isEdit = true
      this.modalTitle = '编辑任务参数'
      info(record.id).then(res => {
        if (res.status === '0') {
          this.form.setFieldsValue({
            id: res.result.id,
            paramKey: res.result.paramKey,
            paramValue: res.result.paramValue,
            paramDesc: res.result.paramDesc,
            paramScope: res.result.paramScope
          })
        }
      })
      this.visible = true
    },
    toggleStatus (record) {
      this.loading = true
      toggleEnable(record.id).then(res => {
        if (res.status === '0') {
          this.$message.success(record.enable ? '已禁用' : '已启用')
          this.init()
        } else {
          this.$message.error(res.errstr || res.error || (record.enable ? '禁用失败' : '启用失败'))
        }
      }).catch(() => {
        this.$message.error(record.enable ? '禁用失败' : '启用失败')
      }).finally(() => {
        this.loading = false
      })
    },
    delete (record) {
      this.loading = true
      del(record.id).then(res => {
        if (res.status === '0') {
          this.$message.success('删除成功')
          this.init()
        } else {
          this.$message.error(res.errstr || res.error || '删除失败')
        }
      }).catch(() => {
        this.$message.error('删除失败')
      }).finally(() => {
        this.loading = false
      })
    },
    handleOk () {
      this.form.validateFields((err, values) => {
        if (!err) {
          this.confirmLoading = true
          const data = {
            "id": values.id,
            "param_key": values.paramKey,
            "param_value": values.paramValue,
            "param_desc": values.paramDesc,
            "param_scope": values.paramScope,
            "enable": true
          }
          const request = this.isEdit ? modify(data) : create(data)
          request.then(res => {
            if (res.status === '0') {
              this.$message.success(this.isEdit ? '修改成功' : '创建成功')
              this.handleCancel()
              this.init()
            } else {
              this.$message.error(res.errstr || res.error || (this.isEdit ? '修改失败' : '创建失败'))
            }
          }).catch(() => {
            this.$message.error(this.isEdit ? '修改失败' : '创建失败')
          }).finally(() => {
            this.confirmLoading = false
          })
        }
      })
    },
    handleCancel () {
      this.visible = false
      this.form.resetFields()
    },
    handleTableChange (pagination) {
      this.pagination = pagination
      this.pages.pageSize = pagination.pageSize
      this.pages.pageNo = pagination.current
      this.init()
    },
    queryData () {
      this.pages.pageNo = 1
      this.init()
    },
    resetQuery () {
      this.queryParam = {}
      this.pages.pageNo = 1
      this.init()
    }
  },
  created () {
    this.init()
  }
}
</script>

<style scoped lang="less">
.system-param-list {
  padding: 24px;
  background: #f0f2f5;

  .table-page-search-wrapper {
    padding: 24px 24px 0;
  }

  .table-operator {
    margin-bottom: 16px;
    padding: 0 24px;
  }

  .table-actions {
    button {
      padding: 0 4px;
    }

    .delete-btn {
      color: #ff4d4f;

      &:hover {
        color: #ff7875;
      }
    }
  }

  .ellipsis {
    display: inline-block;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
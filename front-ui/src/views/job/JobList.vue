<template>
  <a-card :bordered="false">
    <div class="table-operator">
      <a-button @click="$refs.JobSaveOrUpdate.edit('add', '')" icon="plus" type="primary">新建</a-button>
    </div>
    <a-table
      :columns="columns"
      :dataSource="tableData"
      :loading="loading"
      :pagination="pagination"
      :rowKey="record => record.job_id"
      @change="handleTableChange"
    >
    </a-table>
    <job-save-or-update
      @ok="handleOk"
      ref="JobSaveOrUpdate"
    />
  </a-card>
</template>

<script>
import { pageQuery, delObj, exec, stop } from '@/api/job/job'
import { closeConnect } from '@/api/job/sse'
import JobSaveOrUpdate from '../job/JobSaveOrUpdate.vue'
// 0:CREATE|1:SYNCING|2:SYNC_FINISH|3:SYNC_ERROR|4:QUEUING
const StatusType = [
  {
    label: '新建',
    value: 0,
    color: '#1890ff'
  },
  {
    label: '流转中',
    value: 1,
    color: '#1ac4c4'
  },
  {
    label: '流转完成',
    value: 2,
    color: '#52c41a'
  },
  {
    label: '流转失败',
    value: 3,
    color: '#f5222d'
  },
  {
    label: '流转停止',
    value: 4,
    color: '#faad14'
  },
  {
    label: '排队中',
    value: 5,
    color: '#ad14fa'
  }
]
export default {
  name: 'JobList',
  components: {
    JobSaveOrUpdate
  },
  data () {
    return {
      loading: false,
      columns: [
        {
          title: 'job_id',
          width: '10%',
          dataIndex: 'job_id'
        },
        {
          title: '任务名称',
          width: '10%',
          dataIndex: 'job_name'
        },
        {
          title: '来源表',
          width: '10%',
          dataIndex: 'from_tb_name'
        },
        {
          title: '目标表',
          width: '10%',
          dataIndex: 'to_tb_name',
          sorter: true
        },
        {
          title: '上次任务执行时间',
          width: '10%',
          dataIndex: 'start_time',
          sorter: true
        },
        {
          title: '任务状态',
          width: '10%',
          dataIndex: 'status',
          customRender: (text) => {
            return (
              <div>
                {StatusType.map(item => {
                  if (item.value === text) {
                    return (
                      <span>
                        <span class="status-dot" style={'background-color: ' + item.color}></span>
                        {item.label}
                      </span>
                    )
                  }
                  return null
                })}
              </div>
            )
          }
        },
        {
          title: '流转进度(r/w)',
          width: '10%',
          dataIndex: 'progress'
        },
        {
          title: '操作',
          width: '15%',
          customRender: (record) => {
            return (
              <div>
                <a onClick={(e) => this.edit(record)}>修改</a>
                <a-divider type="vertical" />
                <a-popconfirm title="是否删除" onConfirm={() => this.delete(record)} okText="是" cancelText="否">
                  <a-icon slot="icon" type="question-circle-o" style="color: red" />
                  <a href="javascript:;" style="color: red">删除</a>
                </a-popconfirm>
                <a-divider type="vertical" />
                <a href="javascript:;"onClick={(e) => this.execJob(record)}>手动触发</a>
                <a-divider type="vertical" />
                <a href="javascript:;"onClick={(e) => this.stopJob(record)}>停止流转</a>
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
        showSizeChanger: true
      },
      pages: {
        size: 10,
        current: 1
      },
      queryParam: {
        'type': 0
      },
      source: null
    }
  },
  methods: {
    init () {
      this.loading = true
      pageQuery({
        ...this.queryParam,
        ...this.pages
      }).then(res => {
        this.tableData = res.result.data
        this.pagination.total = +res.result.total
        this.loading = false
      }).finally(() => {
        this.loading = false
      })
    },
    handleTableChange (pagination) {
      this.pagination = pagination
      this.pages.size = pagination.pageSize
      this.pages.current = pagination.current
      this.init()
    },
    edit (record) {
      this.$refs.JobSaveOrUpdate.edit('edit', record.job_id)
    },
    delete (record) {
      this.loading = true
      delObj(record.job_id).then(res => {
        if (res.status === '0') {
          this.$message.info('删除成功')
          this.init()
        } else {
          this.$message.error(res.errstr)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    show (record) {
      this.$refs.JobSaveOrUpdate.edit(record.job_id, 'show')
    },
    execJob (record) {
      this.eventSource.close()
      this.createEventSource()
      exec(record.job_id).then(res => {
        if (res.status === '0') {
          this.$message.info('触发成功')
          this.init()
        } else {
          this.$message.error(res.errstr)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    stopJob (record) {
      stop(record.job_id).then(res => {
        if (res.status === '0') {
          this.$message.info('停止成功')
          this.init()
        } else {
          this.$message.error(res.errstr)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    handleOk () {
      this.init()
    },
    queryData () {
      this.pages.current = 1
      this.init()
    },
    createEventSource () {
      if (window.EventSource) {
        this.eventSource = new EventSource(
          `api/api/sse/connect/jobList`, {
            // 设置重连时间
            heartbeatTimeout: 60 * 60 * 1000
            // 添加token
          })
        this.eventSource.onopen = (e) => {
          console.log('connect success')
        }
        this.eventSource.onmessage = (e) => {
          console.log('from server data:', e.data)
          const flashData = JSON.parse(e.data)
          console.log(flashData)
          for (const i of this.tableData) {
            if (i.job_id === flashData.job_id) {
              i.status = flashData.status
              if (flashData.status === 1) {
                i.progress = (flashData.read_records + '/' + flashData.write_records)
              }
            }
          }
        }
      } else {
        console.log('browser not support SSE')
      }
    }
  },
  beforeDestroy () {
    this.eventSource.close()
    closeConnect('jobList')
  },
  created () {
    this.init()
    this.createEventSource()
  }
}
</script>

<style scoped>
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}
</style>

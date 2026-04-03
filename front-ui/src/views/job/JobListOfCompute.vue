<template>
  <a-card :bordered="false">
    <div class="table-operator">
      <a-button @click="newCompute" icon="plus" type="primary">新建</a-button>
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
    <div v-if="showJobCompute" class="job-compute-overlay">
      <job-compute ref="JobCompute" :jobId="this.currentJobId"></job-compute>
    </div>
  </a-card>
</template>

<script>
import { pageQuery, delObj, exec, stop } from '@/api/job/job'
import JobCompute from '../job/JobCompute.vue'
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
    JobCompute
  },
  data () {
    return {
      loading: false,
      currentJobId: '',
      showJobCompute: false,
      refreshTimer: null,
      eventSource: null,
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
                          <span class="status-dot" style={{ backgroundColor: item.color }}></span>
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
                <a href="javascript:;" onClick={(e) => this.execJob(record)}>手动触发</a>
                <a-divider type="vertical" />
                <a href="javascript:;" onClick={(e) => this.stopJob(record)}>停止流转</a>
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
        'type': 2
      },
      source: null
    }
  },
  provide () {
    return {
      closeDraw: this.closeDraw
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
    newCompute () {
      this.showJobCompute = true
    },
    handleTableChange (pagination) {
      this.pagination = pagination
      this.pages.size = pagination.pageSize
      this.pages.current = pagination.current
      this.init()
    },
    edit (record) {
      console.log(record.job_id)
      this.showJobCompute = true
      setTimeout(() => {
        this.$refs.JobCompute.edit(record.job_id)
      }, 10)
    },
    closeDraw () {
      this.showJobCompute = false
      this.init()
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
    execJob (record) {
      exec(record.job_id).then(res => {
        if (res.status === '0') {
          this.$message.info('触发成功')
          this.init()
        } else {
          this.$message.error(res.errstr)
        }
      }).catch(err => {
        const errData = err.response?.data || {}
        this.$message.error(errData.errstr || '操作失败')
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
    }
  },
  beforeDestroy () {
    if (this.eventSource) {
      this.eventSource.close()
    }
    // 清理定时刷新
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
      this.refreshTimer = null
    }
  },
  created () {
    this.init()
    // 启动定时刷新，每10秒刷新一次任务列表
    console.log('启动定时刷新定时器')
    this.refreshTimer = setInterval(() => {
      console.log('执行定时刷新')
      this.init()
    }, 15000)
  }
}
</script>

<style scoped>
.job-compute-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 1);
  z-index: 19;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}
</style>

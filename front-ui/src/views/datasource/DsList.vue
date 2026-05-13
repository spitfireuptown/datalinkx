<template>
  <div class="ds-list-root">
    <div class="list-left">
      <ul>
        <li
          v-for="ds in dsTypeList"
          :key="ds.value"
          class="ds-list"
          :class="{active: ds.dsTypeKey === currentDs.dsTypeKey}"
          @click="selectDs(ds)"
        >
          <span class="ds-icon">
            <img :src="ds.img" alt="">
          </span>
          <div class="ds-name">
            <div class="nowrap">
              <span class="name">{{ ds.label }}</span>
              <span class="num-badge">{{ dsGroupNumber[ds.dsTypeKey] }}</span>
            </div>
          </div>
        </li>
      </ul>
    </div>
    <a-card :bordered="false" class="list-card">
      <div class="table-page-search-wrapper">
        <a-form layout="inline">
          <a-row :gutter="48">
            <a-col :md="8" :sm="24">
              <a-form-item label="数据源名称">
                <a-input v-model="queryParam.name" placeholder="请输入数据源名称" allowClear/>
              </a-form-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-button @click="() => {this.queryData()}" type="primary" icon="search">查询</a-button>
              <a-button @click="() => queryParam = {}" style="margin-left: 12px" icon="reload">重置</a-button>
            </a-col>
          </a-row>
        </a-form>
      </div>
      <div class="table-operator">
        <a-button @click="createDS" type="primary" icon="plus">新建数据源</a-button>
      </div>
      <a-table
        :columns="columns"
        :dataSource="tableData"
        :loading="loading"
        :pagination="pagination"
        :rowKey="record => record.id"
        @change="handleTableChange"
        :rowClassName="() => 'table-row'"
      >
      </a-table>
      <ds-config
        @ok="handleOk"
        :currentDs="currentDs"
        ref="refDsConfig"
      />
      <http-ds-save-or-update
        @ok="handleOk"
        ref="httpDsSaveOrUpdate"
      />
      <custom-ds-save-or-update @ok="handleOk" ref="customDsSaveOrUpdate" />
    </a-card>
  </div>
</template>

<script>
import { pageQuery, delObj, getDsGroup } from '@/api/datasource/datasource'
import HttpDsSaveOrUpdate from './HttpDsSaveOrUpdate.vue'
import CustomDsSaveOrUpdate from './CustomDsSaveOrUpdate.vue'
import DsConfig from './DsConfig.vue'
import { dsTypeList, DataSourceType, DsGroupDefaultNumber } from './const'

export default {
  name: 'ContainerBottom',
  components: {
    HttpDsSaveOrUpdate,
    CustomDsSaveOrUpdate,
    DsConfig
  },
  data () {
    return {
      loading: false,
      columns: [
        {
          title: 'ds_id',
          width: '10%',
          dataIndex: 'dsId'
        }, {
          title: '数据源名称',
          width: '10%',
          dataIndex: 'name'
        },
        {
          title: '数据源类型',
          width: '10%',
          dataIndex: 'type',
          customRender: (text) => {
            const matchedItem = DataSourceType.find(item => item.value === text);
            return (
              <div>
                {matchedItem ? <span>{matchedItem.label}</span> : <span>custom</span>}
              </div>
            )
          }
        },
        {
          title: '创建时间',
          width: '10%',
          dataIndex: 'ctime',
          sorter: true
        }, {
          title: '操作',
          width: '15%',
          customRender: (record) => {
            return (
              <div class="table-actions">
                <a-button type="link" onClick={(e) => this.edit(record)}>修改</a-button>
                <a-divider type="vertical" />
                <a-popconfirm
                  title="确定要删除该数据源吗?"
                  onConfirm={() => this.delete(record)}
                  okText="确定"
                  cancelText="取消"
                >
                  <a-button type="link" class="delete-btn">删除</a-button>
                </a-popconfirm>
                <a-divider type="vertical" />
                <a-button type="link" onClick={(e) => this.show(record)}>查看</a-button>
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
        size: 10,
        current: 1
      },
      queryParam: {},
      dsTypeList,
      dsGroupNumber: DsGroupDefaultNumber,
      currentDs: {
        value: 'MySQL',
        label: 'MySQL',
        dsTypeKey: 'mysql'
      }
    }
  },
  methods: {
    init () {
      this.loading = true
      pageQuery({
        type: this.currentDs.dsTypeKey,
        ...this.queryParam,
        ...this.pages
      }).then(res => {
        this.tableData = res.result.data
        console.log(res)
        this.pagination.total = +res.result.total
        this.loading = false
      }).catch(reason => {
        this.loading = false
      })
    },
    createDS () {
      if (this.currentDs.dsTypeKey === 'http') {
        this.$refs.httpDsSaveOrUpdate.show('', 'add')
      } else if (this.currentDs.dsTypeKey === 'custom') {
        this.$refs.customDsSaveOrUpdate.edit('', 'add')
      } else {
        this.$refs.refDsConfig.show('', 'add')
      }
    },
    getAllDsNumber () {
      this.loading = true
      getDsGroup().then(res => {
        if (res.status === '0') {
          // 合并返回的数据，保留默认值以防某些类型未返回
          this.dsGroupNumber = Object.assign({}, DsGroupDefaultNumber, res.result)
        } else {
          this.$message.error(res.error || res.errstr || '获取数据源统计失败')
        }
      }).catch(reason => {
        console.error('获取数据源统计失败:', reason)
      }).finally(() => {
        this.loading = false
      })
    },
    selectDs (item) {
      this.currentDs = item
      this.pages.current = 1
      this.init()
    },
    handleTableChange (pagination, filters, sorter) {
      console.log(sorter.field)
      console.log(sorter.order)
      this.pagination = pagination
      this.pages.size = pagination.pageSize
      this.pages.current = pagination.current
      this.init()
    },

    edit (record) {
      if (this.currentDs.dsTypeKey === 'http') {
        this.$refs.httpDsSaveOrUpdate.show(record.dsId, 'edit')
      } else if (this.currentDs.dsTypeKey === 'custom') {
        this.$refs.customDsSaveOrUpdate.edit(record.dsId, 'edit')
      } else {
        this.$refs.refDsConfig.show(record.dsId, 'edit', record)
      }
    },
    delete (record) {
      this.loading = true
      delObj(record.dsId).then(res => {
        if (res.status === '0') {
          this.$message.success('删除成功')
          // 刷新当前类型的数据源列表
          this.init()
        } else {
          this.$message.error(res.errstr || res.error || '删除失败')
        }
      }).catch(reason => {
        console.error('删除数据源失败:', reason)
        this.$message.error('删除失败')
      }).finally(() => {
        // 刷新所有数据源类型的统计数量
        this.getAllDsNumber()
      })
    },
    show (record) {
      if (this.currentDs.dsTypeKey === 'http') {
        this.$refs.httpDsSaveOrUpdate.show(record.dsId, 'show')
      } else if (this.currentDs.dsTypeKey === 'custom') {
        this.$refs.customDsSaveOrUpdate.edit(record.dsId, 'show')
      } else {
        this.$refs.refDsConfig.show(record.dsId, 'show', record)
      }
    },
    handleOk (data) {
      // 先刷新当前类型的数据源列表
      this.init()
      // 然后刷新所有数据源类型的统计数量
      this.getAllDsNumber()
    },
    queryData () {
      this.pages.current = 1
      this.init()
    }
  },
  created () {
    this.init()
    this.getAllDsNumber()
  }
}
</script>

<style scoped lang="less">
.ds-list-root {
  position: relative;
  display: flex;
  height: 100%;
  width: 100%;
  background: #f0f2f5;

  .list-left {
    width: 220px;
    height: 100%;
    margin: 24px 0 16px 24px;
    border-radius: 8px;
    padding: 16px;
    overflow: auto;
    cursor: pointer;
    background-color: #fff;
    box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.16),
    0 3px 6px 0 rgba(0, 0, 0, 0.12),
    0 5px 12px 4px rgba(0, 0, 0, 0.09);

    ul, li {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .ds-list {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      margin-bottom: 8px;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        background-color: #f5f5f5;
      }

      .ds-icon {
        width: 32px;
        height: 32px;
        margin-right: 12px;

        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }
      }

      .ds-name {
        flex: 1;

        .name {
          font-size: 14px;
          color: rgba(0, 0, 0, 0.85);
          font-weight: 500;
        }

        .num-badge {
          margin-left: 8px;
          padding: 2px 8px;
          font-size: 12px;
          line-height: 16px;
          border-radius: 10px;
          background: #e6f7ff;
          color: #1890ff;
        }
      }
    }

    .active {
      background-color: #e6f7ff;
      border-right: 3px solid #1890ff;
    }
  }

  .list-card {
    flex: 1;
    margin: 24px;
    border-radius: 8px;

    .table-page-search-wrapper {
      padding: 24px 24px 0;
    }

    .table-operator {
      margin-bottom: 16px;
      padding: 0 24px;
    }

    .table-row {
      &:hover {
        background-color: #f5f5f5;
      }
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
  }
}
</style>
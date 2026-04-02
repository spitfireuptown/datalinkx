<template>
  <a-modal
    :title="type === 'edit' ? '编辑数据流转任务' : '新增数据流转任务'"
    :width="800"
    :visible="visible"
    :maskClosable="false"
    @cancel="handleCancel"
    class="job-save-root"
  >
    <a-form :form="form" @submit="handleSubmit" layout="vertical">
      <!-- 基本信息 -->
      <a-divider orientation="left">基本信息</a-divider>
      <a-row :gutter="24">
        <a-col :span="24">
          <a-form-item label="任务名称">
            <a-input
              placeholder="请输入任务名称"
              v-decorator="['jobName', {rules: [{required: true, message: '请输入任务名称'}],initialValue: jobName}]"
            />
          </a-form-item>
        </a-col>
        <a-col :span="24" v-show="!isStreaming">
          <a-form-item>
            <span slot="label">
              定时配置 (Spring crontab)
              <a-tooltip title="点击查看 crontab 工具">
                <a href="https://tool.lu/crontab" target="_blank" style="margin-left: 8px">
                  <a-icon type="question-circle-o" />
                </a>
              </a-tooltip>
            </span>
            <a-input
              placeholder="例如: 0 0/5 * * * ?"
              v-decorator="['schedulerConf', {rules: [{required: false, message: '请输入crontab表达式', trigger: 'blur'}],initialValue: schedulerConf}]"/>
          </a-form-item>
        </a-col>
      </a-row>

      <!-- 数据源配置 -->
      <a-divider orientation="left">数据源配置</a-divider>
      <div class="ds-guide-wrapper">
        <div class="ds-guide-panel source-panel">
          <div class="panel-header">
            <a-icon type="database" /> 来源端 (From)
          </div>
          <a-form-item label="数据源" class="guide-item">
            <a-select
              placeholder="请选择来源数据源"
              @change="handleFromChange"
              v-decorator="['selectedDataSource', {rules: [{required: true, message: '请选择来源数据源'}],initialValue: selectedDataSource}]">
              <a-select-option v-for="table in fromDsList" :value="table.dsId" :key="table.name">
                <div class="ds-option">
                  <span class="ds-icon">
                    <img :src="dsImgObj[table.type]" alt="">
                  </span>
                  <span>{{ table.name }}</span>
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="数据表" class="guide-item">
            <a-select
              show-search
              placeholder="请选择来源表"
              @change="handleFromTbChange"
              v-decorator="['selectedSourceTable', {rules: [{required: true, message: '请选择来源数据源表'}],initialValue: selectedSourceTable}]">
              <a-select-option v-for="table in sourceTables" :value="table" :key="table">
                {{ table }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>

        <div class="ds-guide-connector">
          <div class="connector-arrow">
            <a-icon type="double-right" />
          </div>
        </div>

        <div class="ds-guide-panel target-panel">
          <div class="panel-header">
            <a-icon type="cloud-upload" /> 目标端 (To)
          </div>
          <a-form-item label="数据源" class="guide-item">
            <a-select
              placeholder="请选择目标数据源"
              @change="handleToDsChange"
              v-decorator="['selectedTargetSource', {rules: [{required: true, message: '请选择目标数据源'}],initialValue: selectedTargetSource}]">
              <a-select-option v-for="table in toDsList" :value="table.dsId" :key="table.name">
                <div class="ds-option">
                  <span class="ds-icon">
                    <img :src="dsImgObj[table.type]" alt="">
                  </span>
                  <span>{{ table.name }}</span>
                </div>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="!isRedisTo" label="数据表" class="guide-item">
            <a-select
              show-search
              placeholder="请选择目标表"
              @change="handleToTbChange"
              v-decorator="['selectedTargetTable', {rules: [{required: true, message: '请选择目标数据源表'}],initialValue: selectedTargetTable}]">
              <a-select-option v-for="table in targetTables" :value="table" :key="table">
                {{ table }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <div v-else class="redis-config-row">
            <a-form-item label="Redis Type" :required="true" :validateStatus="redis_type_validateStatus" :help="redis_type_help" class="redis-type-item guide-item">
              <a-select v-model="redisToType" @change="changeToRedisType;validate_redis">
                <a-select-option v-for="item in RedisTypes" :value="item.value" :key="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="Redis Key" :required="true" :validateStatus="redis_value_validateStatus" :help="redis_value_help" class="redis-key-item guide-item">
              <a-input
                placeholder="请输入 Redis Key"
                @blur="validate_redis"
                v-model="redisToValue"/>
            </a-form-item>
          </div>
        </div>
      </div>

      <!-- 同步策略 -->
      <a-divider orientation="left" v-show="!isRedisTo && !isStreaming">同步策略</a-divider>
      <div class="sync-strategy-box" v-show="!isRedisTo && !isStreaming">
        <a-row :gutter="24" type="flex" align="middle">
          <a-col :span="8">
            <span class="sync-label">同步模式:</span>
            <a-radio-group v-model="syncMode" button-style="solid" @change="changeSyncConfig" size="small">
              <a-radio-button value="overwrite">全量</a-radio-button>
              <a-radio-button value="increment">增量</a-radio-button>
            </a-radio-group>
          </a-col>
          <a-col :span="16">
            <div v-if="!isIncrement" class="sync-detail-item">
              <span class="sync-label">开启数据覆盖:</span>
              <a-switch v-model="trans_cover" @change="changeCover" />
            </div>
            <div v-else class="sync-detail-item">
              <span class="sync-label">增量字段:</span>
              <a-select v-model="incrementField" placeholder="请选择增量字段" style="width: 200px" size="small">
                <a-select-option v-for="field in sourceFields" :value="field.name" :key="field.name">
                  {{ field.name }}
                </a-select-option>
              </a-select>
            </div>
          </a-col>
        </a-row>
      </div>

      <!-- 字段映射关系 -->
      <a-divider orientation="left">字段映射关系</a-divider>
      <div class="mapping-container">
        <div class="mapping-header">
          <div class="header-item">来源字段</div>
          <div class="header-icon"></div>
          <div class="header-item">目标字段</div>
          <div class="header-action">操作</div>
        </div>
        <div v-for="(mapping, index) in mappings" :key="index" class="mapping-row">
          <div class="mapping-field">
            <a-select v-model="mapping.sourceField" placeholder="来源字段" show-search style="width: 100%">
              <a-select-option v-for="field in sourceFields" :value="field.name" :key="field.name">
                {{ field.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="mapping-arrow">
            <a-icon type="swap-right" />
          </div>
          <div class="mapping-field">
            <a-select v-model="mapping.targetField" placeholder="目标字段" show-search style="width: 100%">
              <a-select-option v-for="field in targetFields" :value="field.name" :key="field.name">
                {{ field.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="mapping-action">
            <a-button type="link" @click="removeMapping(index)" v-show="mappings.length > 1" class="delete-btn">
              <a-icon type="delete" />
            </a-button>
          </div>
        </div>
        <a-button type="dashed" block icon="plus" @click="addMapping" class="add-mapping-btn">
          添加字段映射关系
        </a-button>
      </div>

      <LoadingDx size="'size-1x'" v-if="selectloading"></LoadingDx>
    </a-form>

    <template slot="footer">
      <a-button key="cancel" @click="handleCancel">取消</a-button>
      <a-button key="forward" v-show="onlyRead" :loading="confirmLoading" type="primary" @click="handleSubmit">保存</a-button>
    </template>
  </a-modal>
</template>

<script>
  import { fetchTables, getDsTbFieldsInfo, listQuery } from '@/api/datasource/datasource'
  import { addObj, getObj, modifyObj } from '@/api/job/job'
  import LoadingDx from './../../components/common/loading-dx.vue'
  import { RedisTypes, dsImgObj } from './../datasource/const'
  export default {
    components: {
      LoadingDx
    },
    data () {
      return {
        RedisTypes,
        dsImgObj,
        jobType: 'default',
        form: this.$form.createForm(this),
        selectedDataSource: null,
        selectedTargetSource: null,
        selectedSourceTable: null,
        selectedTargetTable: null,
        schedulerConf: '',
        confirmLoading: false,
        jobName: '',
        onlyRead: true,
        dataSources: [
          { id: 1, name: '数据源1' },
          { id: 2, name: '数据源2' },
          { id: 3, name: '数据源3' }
          // 其他数据源选项
        ],
        visible: false,
        type: '',
        fromDsList: [],
        toDsList: [],
        sourceTables: [],
        targetTables: [],
        redisSpitKey: '!-!-!',
        mappings: [
          { sourceField: '', targetField: '' }
        ],
        sourceFields: [],
        jobId: '',
        queryParam: {},
        targetFields: [],
        isIncrement: false,
        syncMode: 'overwrite',
        cover: 0,
        incrementField: '',
        selectloading: false,
        redisToType: 'string',
        redisToValue: '',
        redis_type_validateStatus: '',
        redis_value_validateStatus: '',
        redis_type_help: '',
        redis_value_help: ''
      }
    },
    computed: {
      isRedisTo () {
        let temp = false
        if (this.selectedTargetSource) {
          temp = this.toDsList.find(item => { return item.dsId === this.selectedTargetSource })?.type === 'redis'
        }
        return temp
      },
      isStreaming () {
        return this.jobType === 'streaming'
      },
      // 是否覆盖
      trans_cover () {
        return Boolean(this.cover)
      }
    },
    methods: {
      // 校验redis
      validate_redis () {
        if (!this.redisToType) {
          this.redis_type_validateStatus = 'error'
          this.redis_type_help = '请选择Type'
        } else {
          this.redis_type_validateStatus = ''
          this.redis_type_help = ''
        }
        if (!this.redisToValue) {
          this.redis_value_validateStatus = 'error'
          this.redis_value_help = '请输入Value'
        } else {
          this.redis_value_validateStatus = ''
          this.redis_value_help = ''
        }
      },
      handleSubmit () {
        // 如果目标数据源是redis 则设置为全量
        if (this.isRedisTo) {
          // 同步redis数据源表单数据
          this.form.setFieldsValue({ 'selectedTargetTable': this.redisToType + this.redisSpitKey + this.redisToValue })
          // 则校验redis数据
          this.validate_redis()
        }
        this.form.validateFields(async (err, values) => {
          if (!err) {
            this.confirmLoading = true
            const formData = {
              'job_id': this.jobId,
              // 提交表单数据
              'from_ds_id': this.form.getFieldValue('selectedDataSource'),
              'to_ds_id': this.form.getFieldValue('selectedTargetSource'),
              'from_tb_name': this.form.getFieldValue('selectedSourceTable'),
              'to_tb_name': this.form.getFieldValue('selectedTargetTable'),
              'job_name': this.form.getFieldValue('jobName'),
              'scheduler_conf': this.form.getFieldValue('schedulerConf'),
              'field_mappings': this.mappings,
              'cover': this.cover,
              'sync_mode': {
                'mode': this.syncMode,
                'increate_field': this.incrementField
              }
            }
            console.log(this.jobId)
            if (this.type === 'edit') {
              modifyObj(formData).then(res => {
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
                this.$message.error(err.errstr)
              })
            } else {
              addObj(formData).then(res => {
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
            }
            // setTimeout(() => {
            //   this.confirmLoading = false
            //   this.$emit('ok')
            //   this.visible = false
            // }, 1500)
          }
        })
      },
      readOnly (id) {
        this.onlyRead = false
        console.log(this.onlyRead)
        this.edit('edit', id)
      },
      edit (type, id, jobType = 'default') {
        this.visible = true
        this.jobType = jobType
        this.type = type
        this.selectloading = true
        listQuery().then(res => {
          this.selectloading = false
          const record = res.result
          const excludeFromDs = ['redis', 'http', 'kafka']
          const excludeToDs = ['http', 'kafka']
          for (var a of record) {
            // redis数据源暂不支持读
            if (!excludeFromDs.includes(a.type)) {
              this.fromDsList.push({
                dsId: a.dsId,
                name: a.name,
                type: a.type
              })
            }
            if (!excludeToDs.includes(a.type)) {
              this.toDsList.push({
                dsId: a.dsId,
                name: a.name,
                type: a.type
              })
            }
          }
        })
        console.log(this.fromDsList)
        if (type === 'edit') {
          getObj(id).then(res => {
            const record = res.result
            this.selectedTargetSource = record.to_ds_id
            this.selectedDataSource = record.from_ds_id
            this.selectedSourceTable = record.from_tb_name
            this.selectedTargetTable = record.to_tb_name
            this.schedulerConf = record.scheduler_conf
            this.mappings = record.field_mappings
            this.jobId = record.job_id
            this.syncMode = record.sync_mode.mode
            this.jobName = record.job_name
            this.cover = record.cover
            console.log(this.syncMode)
            if (this.selectedTargetTable.includes(this.redisSpitKey)) {
              const arr = this.selectedTargetTable.split(this.redisSpitKey)
              this.redisToValue = arr[1]
              this.redisToType = arr[0]
            }

            this.incrementField = record.sync_mode.increate_field
            if (this.syncMode === 'increment') {
              this.isIncrement = true
            }
            fetchTables(this.selectedTargetSource).then(res => {
              this.targetTables = res.result
            })
            this.handleFromTbChange(this.selectedSourceTable)
            fetchTables(this.selectedDataSource).then(res => {
              this.sourceTables = res.result
            })
            this.handleToTbChange(this.selectedTargetTable)
          })
        }
      },
      handleFromChange (value) {
        this.selectedDataSource = value
        // 切换数据源同步表单数据
        this.form.setFieldsValue({ 'selectedDataSource': value })
        // 清空表单中来源表
        this.form.setFieldsValue({ 'selectedSourceTable': '' })
        this.selectloading = true
        fetchTables(value).then(res => {
          this.selectloading = false
          this.sourceTables = res.result
        })
      },
      handleToDsChange (value) {
        this.selectedTargetSource = value
        // 切换目标数据源同步表单数据
        this.form.setFieldsValue({ 'selectedTargetSource': value })
        // 清空表单中目标表
        this.form.setFieldsValue({ 'selectedTargetTable': '' })
        console.log('当前选中数据源类型', this.selectedTargetSource, this.isRedisTo)
        // 如果目标数据源是redis 则设置为全量
        if (this.toDsList.find(item => { return item.dsId === this.selectedTargetSource })?.type === 'redis') {
          this.syncMode = 'overwrite'
          this.isIncrement = false
        } else {
          this.selectloading = true
          fetchTables(value).then(res => {
            this.selectloading = false
            this.targetTables = res.result
          })
        }
      },
      handleFromTbChange (value) {
        this.selectloading = true
        // 切换来源表同步表单数据
        this.form.setFieldsValue({ 'selectedSourceTable': value })
        this.selectedSourceTable = value
        this.queryParam = {
          'ds_id': this.selectedDataSource,
          'name': value
        }
        getDsTbFieldsInfo({
          ...this.queryParam
        }).then(res => {
          this.selectloading = false
          this.sourceFields = res.result
        })
      },
      // 目标数据源为redis时切换类型
      changeToRedisType (val) {
        console.log(val)
      },
      changeCover (checked) {
        this.cover = checked ? 1 : 0
      },
      changeSyncConfig (value) {
        if (value.target.value === 'increment') {
          this.isIncrement = true
          this.syncMode = 'increment'
        } else {
          this.isIncrement = false
          this.incrementField = ''
          this.syncMode = 'overwrite'
        }
      },
      handleToTbChange (value) {
        this.selectloading = true
        this.selectedTargetTable = value
        // 切换目标表同步表单数据
        this.form.setFieldsValue({ 'selectedTargetTable': value })
        this.queryParam = {
          'ds_id': this.selectedTargetSource,
          'name': value
        }
        getDsTbFieldsInfo({
          ...this.queryParam
        }).then(res => {
          this.selectloading = false
          this.targetFields = res.result
        })
      },
      addMapping () {
        this.mappings.push({ sourceField: '', targetField: '' })
      },
      handleCancel () {
        this.visible = false
        this.selectedDataSource = null
        this.selectedTargetSource = null
        this.selectedSourceTable = null
        this.selectedTargetTable = null
        this.fromDsList = []
        this.toDsList = []
        this.sourceTables = []
        this.targetTables = []
        this.isIncrement = false
        this.mappings = []
        this.syncMode = ''
        this.cover = 0
        this.incrementField = ''
        this.schedulerConf = ''
        this.jobName = ''
        this.syncMode = 'overwrite'
        this.form.resetFields()
        this.redis_value_help = ''
        this.redis_type_help = ''
        this.redis_value_validateStatus = ''
        this.redis_type_validateStatus = ''
      },
      removeMapping (index) {
        this.mappings.splice(index, 1)
      }
    }
  }
</script>

<style scoped lang="less">
  .job-save-root {
    /deep/ .ant-modal-body {
      padding: 16px 24px 24px;
      background-color: #fbfbfb;
    }

    /deep/ .ant-form-item {
      margin-bottom: 12px;
      .ant-form-item-label {
        line-height: 24px;
        padding-bottom: 4px;
        label {
          color: #595959;
          font-weight: 500;
        }
      }
    }

    /deep/ .ant-divider-horizontal.ant-divider-with-text-left {
      margin: 24px 0 16px;
      font-weight: 600;
      font-size: 14px;
      color: #262626;
      border-top-color: #f0f0f0;
      &::before {
        border-top-color: #f0f0f0;
      }
      &::after {
        border-top-color: #f0f0f0;
      }
    }
  }

  .ds-option {
    display: flex;
    align-items: center;
    padding: 2px 0;
    .ds-icon {
      width: 18px;
      height: 18px;
      margin-right: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
    }
  }

  .ds-guide-wrapper {
    display: flex;
    align-items: stretch;
    gap: 16px;
    margin-bottom: 24px;
    background: #fdfdfd;
    padding: 16px;
    border-radius: 8px;
    border: 1px solid #f0f0f0;

    .ds-guide-panel {
      flex: 1;
      background: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 6px;
      padding: 16px;
      transition: all 0.3s;

      &:hover {
        border-color: #91d5ff;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      }

      .panel-header {
        font-size: 13px;
        font-weight: 600;
        color: #595959;
        margin-bottom: 16px;
        display: flex;
        align-items: center;
        gap: 8px;
        padding-bottom: 8px;
        border-bottom: 1px solid #f5f5f5;
        
        .anticon {
          font-size: 16px;
          color: #1890ff;
        }
      }

      .guide-item {
        margin-bottom: 8px !important;
        &:last-child {
          margin-bottom: 0 !important;
        }
      }
    }

    .source-panel {
      border-left: 4px solid #1890ff;
    }

    .target-panel {
      border-left: 4px solid #52c41a;
    }

    .ds-guide-connector {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;

      .connector-arrow {
        color: #d9d9d9;
        font-size: 20px;
        animation: slideRight 2s infinite;
      }
    }
  }

  @keyframes slideRight {
    0% { transform: translateX(-4px); opacity: 0.3; }
    50% { transform: translateX(4px); opacity: 1; }
    100% { transform: translateX(-4px); opacity: 0.3; }
  }

  .redis-config-row {
    display: flex;
    gap: 12px;
    .redis-type-item {
      width: 120px;
      flex-shrink: 0;
    }
    .redis-key-item {
      flex: 1;
    }
  }

  .sync-strategy-box {
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    padding: 16px 20px;
    margin-bottom: 12px;

    .sync-label {
      font-size: 13px;
      color: #8c8c8c;
      margin-right: 12px;
    }

    .sync-detail-item {
      display: flex;
      align-items: center;
      height: 32px;
    }
  }

  .mapping-container {
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    padding: 12px;

    .mapping-header {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      padding: 0 12px;
      font-size: 12px;
      color: #bfbfbf;
      text-transform: uppercase;
      letter-spacing: 0.5px;

      .header-item {
        flex: 1;
      }
      .header-icon {
        width: 48px;
      }
      .header-action {
        width: 32px;
        text-align: center;
      }
    }

    .mapping-row {
      display: flex;
      align-items: center;
      margin-bottom: 8px;
      padding: 6px 12px;
      background: #fdfdfd;
      border: 1px solid #f0f0f0;
      border-radius: 4px;
      transition: all 0.2s ease;

      &:last-child {
        margin-bottom: 0;
      }

      &:hover {
        border-color: #d1e9ff;
        background: #f0f7ff;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
      }

      .mapping-field {
        flex: 1;
      }

      .mapping-arrow {
        width: 48px;
        display: flex;
        justify-content: center;
        color: #d9d9d9;
        font-size: 16px;
      }

      .mapping-action {
        width: 32px;
        display: flex;
        justify-content: center;
        .delete-btn {
          color: #ffccc7;
          font-size: 14px;
          transition: color 0.3s;
          &:hover {
            color: #ff4d4f;
          }
        }
      }
    }

    .add-mapping-btn {
      margin-top: 12px;
      height: 36px;
      color: #8c8c8c;
      border-color: #d9d9d9;
      font-size: 13px;
      &:hover {
        color: #40a9ff;
        border-color: #40a9ff;
      }
    }
  }
</style>
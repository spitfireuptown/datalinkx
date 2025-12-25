<template>
  <div class="g6-wrap">
    <a-drawer
      title="来源数据源"
      placement="right"
      :closable="false"
      :visible="visible"
      :after-visible-change="afterVisibleChange"
      @close="onClose"
      width="700"
    >
      <LoadingDx size="'size-1x'" v-if="selectloading"></LoadingDx>
      <div class="select-container">
        来源数据源
        <a-select
          class="input-full-width"
          @change="handleFromChange"
          v-model="selectedDataSource">
          <a-select-option v-for="table in fromDsList" :value="table.dsId" :key="table.name">
            <div>
              <span class="ds-icon">
                <img :src="dsImgObj[table.type]" alt="">
              </span>
              <span>{{ table.name }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>

      <div class="input-container">
        来源数据表
        <a-select class="input-full-width" @change="handleFromTbChange" v-model="selectedSourceTable">
          <a-select-option v-for="table in sourceTables" :value="table" :key="table">
            {{ table }}
          </a-select-option>
        </a-select>
      </div>
      <a-form-item
        label="同步配置"
        v-show="!isRedisTo && !isStreaming"
      >
        <a-row :gutter="16">
          <a-col :span="6">
            <a-radio-group v-model="syncMode" button-style="solid" @change="changeSyncConfig">
              <a-radio-button value="overwrite">
                全量
              </a-radio-button>
              <a-radio-button value="increment">
                增量
              </a-radio-button>
            </a-radio-group>
          </a-col>
          <a-col :span="6" v-show="!isIncrement">开启数据覆盖: <a-switch v-model="trans_cover" @change="changeCover" /></a-col>
          <a-col :span="6"><p v-show="isIncrement">请选择增量字段: </p></a-col>
          <a-col :span="6">
<!--            <div class="input-container">-->
              <a-select v-show="isIncrement" v-model="incrementField" placeholder="请选择增量字段" style="width: 100%">
                <a-select-option v-for="field in sourceFields" :value="field.name" :key="field.name">
                  {{ field.name }}
                </a-select-option>
              </a-select>
<!--            </div>-->
          </a-col>
        </a-row>
      </a-form-item>
      <a-form-item label="选择流转字段">
        <a-row :gutter="16" v-for="(mapping, index) in mappings" :key="index">
          <a-col :span="8">
            <a-select v-model="mapping.sourceField" placeholder="请选择来源字段" class="input-full-width">
              <a-select-option v-for="field in sourceFields" :value="field.name" :key="field.name">
                {{ field.name }}
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-icon type="minus-circle-o" @click="removeMapping(index)" v-show="mappings.length > 1" />
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-button type="dashed" @click="addMapping" style="width: 100%">
              <a-icon type="plus" /> 选择来源字段
            </a-button>
          </a-col>
        </a-row>
      </a-form-item>
    </a-drawer>
    <a-drawer
      title="SQL算子"
      placement="right"
      :closable="false"
      :visible="sqlVisible"
      :after-visible-change="afterVisibleChange"
      @close="onClose"
      width="500"
    >
      <div>
        <a-tag v-for="tag in tags" :key="tag" draggable @dragstart="handleDragStart(tag, $event)">{{ tag }}</a-tag>
      </div>
      <a-input
        v-if="inputVisible"
        ref="input"
        type="text"
        size="small"
        :style="{ width: '120px' }"
        :value="inputValue"
        @change="handleInputChange"
        @blur="handleInputConfirm"
        @keyup.enter="handleInputConfirm"
      />
      <a-tag v-else style="background: #fff; borderStyle: dashed;" @click="showInput">
        <a-icon type="plus" /> New Field
      </a-tag>
      <br>
      <span class="">
        select
        <a-icon type="sync"  @click="cleanSelect"/>
      </span>
      <a-textarea @drop="handleDrop" @dragover.prevent v-model="sqlOperatorValue" :disabled="disabledTrue" placeholder="基于上游节点字段, 从上面的标签中拖拽至此处"/>
      <span>from</span>
      <a-input v-model="selectedSourceTable" :disabled="disabledTrue"/>
      <span>where</span>
      <a-textarea v-model="sqlOperatorWhereValue" placeholder=""/>
      <span>group</span>
      <a-textarea v-model="sqlOperatorGroupValue" placeholder=""/>
    </a-drawer>
    <a-drawer
      title="大模型算子"
      placement="right"
      :closable="false"
      :visible="llmVisible"
      :after-visible-change="afterVisibleChange"
      @close="onClose"
      width="500"
    >
      <span>大模型prompt</span>
      <a-textarea v-model="llmPrompt" style="height: 121px;" placeholder="所有字段都将可以用作模型输入，直接使用字段名称编写prompt，只支持英文，例：Determine whether someone is Chinese or American by their name(根据名字判断某人是中国人还是美国人)"/>
    </a-drawer>
    <a-drawer
      title="目标数据源"
      placement="right"
      :closable="false"
      :visible="toDsVisible"
      :after-visible-change="afterVisibleChange"
      @close="onClose"
      width="500"
    >
      <LoadingDx size="'size-1x'" v-if="selectloading"></LoadingDx>
      <div class="select-container">任务名称<a-input v-model="jobName"/></div>
      <div class="select-container"><a href="https://tool.lu/crontab" target="_blank">定时配置（Spring crontab表达式）</a><a-input v-model="schedulerConf"/></div>
      <div class="select-container">
        目标数据源
        <a-select
          class="input-full-width"
          @change="handleToChange"
          v-model="selectedTargetSource">
          <a-select-option v-for="table in toDsList" :value="table.dsId" :key="table.name">
            <div>
              <span class="ds-icon">
                <img :src="dsImgObj[table.type]" alt="">
              </span>
              <span>{{ table.name }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>
      <div class="input-container">
        目标数据表
        <a-select class="input-full-width" @change="handleToTbChange" v-model="selectedTargetTable">
          <a-select-option v-for="table in targetTables" :value="table" :key="table">
            {{ table }}
          </a-select-option>
        </a-select>
      </div>

      <a-form-item label="字段映射关系">
        <a-row :gutter="16">
          <a-col :span="8">
            <span>来源字段</span>
          </a-col>
          <a-col :span="8">
            <span>目标字段</span>
          </a-col>
        </a-row>
        <a-row :gutter="16" v-for="(mapping, index) in targetMappings" :key="index">
          <a-col :span="8">
            <a-select v-model="mapping.sourceField" placeholder="请选择来源字段" class="input-full-width">
              <a-select-option v-for="field in toDsSourceFields" :value="field" :key="field">
                {{ field }}
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="8">
            <a-select v-model="mapping.targetField" placeholder="请选择目标字段" class="input-full-width">
              <a-select-option v-for="field in targetFields" :value="field.name" :key="field.name">
                {{ field.name }}
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="4">
            <a-icon type="minus-circle-o" @click="removeTargetMapping(index)" v-show="targetMappings.length > 1" />
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="24">
            <a-button type="dashed" @click="addTargetMapping" style="width: 100%">
              <a-icon type="plus" /> 添加字段映射关系
            </a-button>
          </a-col>
        </a-row>
      </a-form-item>
    </a-drawer>
    <a-layout>
      <a-layout-header>
        <div class="top-box">
          <div class="tools-box">
            <div
              v-for="tool in tools"
              :key="tool.key"
              class="tool"
              @click="handleTrigger(tool.key)">
              <img :src="require(`@/assets/icons/${tool.iconClass}.png`)" alt="">
              <div class="word">{{ tool.title }}</div>
            </div>
          </div>
        </div>
      </a-layout-header>
      <a-layout>
        <a-layout-sider style="background: transparent">
          <div id="stencil">
            <div class="dnd-rect" @mousedown="startDrag('start',$event)">
              <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA0IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgOWgtMnY0aC0ydi00aC0ybDMtM3oiLz48L3N2Zz4=" alt="" width="50px" height="50px">
              <span>来源数据源</span>
            </div>
            <div class="dnd-rect" @mousedown="startDrag('rect',$event)">
              <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9Im5vbmUiIHN0cm9rZT0iY3VycmVudENvbG9yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIHN0cm9rZS13aWR0aD0iMiIgZD0iTTEyIDhhMiAyIDAgMCAxIDIgMnY0YTIgMiAwIDEgMS00IDB2LTRhMiAyIDAgMCAxIDItMm01IDB2OGg0bS04LTFsMSAxTTMgMTVhMSAxIDAgMCAwIDEgMWgyYTEgMSAwIDAgMCAxLTF2LTJhMSAxIDAgMCAwLTEtMUg0YTEgMSAwIDAgMS0xLTFWOWExIDEgMCAwIDEgMS0xaDJhMSAxIDAgMCAxIDEgMSIvPjwvc3ZnPg==" alt="" width="50px" height="50px">
              <span>SQL算子</span>
            </div>
            <div class="dnd-rect" @mousedown="startDrag('polygon',$event)">
              <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIyLjI4MiA5LjgyMWE2IDYgMCAwIDAtLjUxNi00LjkxYTYuMDUgNi4wNSAwIDAgMC02LjUxLTIuOUE2LjA2NSA2LjA2NSAwIDAgMCA0Ljk4MSA0LjE4YTYgNiAwIDAgMC0zLjk5OCAyLjlhNi4wNSA2LjA1IDAgMCAwIC43NDMgNy4wOTdhNS45OCA1Ljk4IDAgMCAwIC41MSA0LjkxMWE2LjA1IDYuMDUgMCAwIDAgNi41MTUgMi45QTYgNiAwIDAgMCAxMy4yNiAyNGE2LjA2IDYuMDYgMCAwIDAgNS43NzItNC4yMDZhNiA2IDAgMCAwIDMuOTk3LTIuOWE2LjA2IDYuMDYgMCAwIDAtLjc0Ny03LjA3M00xMy4yNiAyMi40M2E0LjQ4IDQuNDggMCAwIDEtMi44NzYtMS4wNGwuMTQxLS4wODFsNC43NzktMi43NThhLjguOCAwIDAgMCAuMzkyLS42ODF2LTYuNzM3bDIuMDIgMS4xNjhhLjA3LjA3IDAgMCAxIC4wMzguMDUydjUuNTgzYTQuNTA0IDQuNTA0IDAgMCAxLTQuNDk0IDQuNDk0TTMuNiAxOC4zMDRhNC40NyA0LjQ3IDAgMCAxLS41MzUtMy4wMTRsLjE0Mi4wODVsNC43ODMgMi43NTlhLjc3Ljc3IDAgMCAwIC43OCAwbDUuODQzLTMuMzY5djIuMzMyYS4wOC4wOCAwIDAgMS0uMDMzLjA2Mkw5Ljc0IDE5Ljk1YTQuNSA0LjUgMCAwIDEtNi4xNC0xLjY0Nk0yLjM0IDcuODk2YTQuNSA0LjUgMCAwIDEgMi4zNjYtMS45NzNWMTEuNmEuNzcuNzcgMCAwIDAgLjM4OC42NzdsNS44MTUgMy4zNTRsLTIuMDIgMS4xNjhhLjA4LjA4IDAgMCAxLS4wNzEgMGwtNC44My0yLjc4NkE0LjUwNCA0LjUwNCAwIDAgMSAyLjM0IDcuODcyem0xNi41OTcgMy44NTVsLTUuODMzLTMuMzg3TDE1LjExOSA3LjJhLjA4LjA4IDAgMCAxIC4wNzEgMGw0LjgzIDIuNzkxYTQuNDk0IDQuNDk0IDAgMCAxLS42NzYgOC4xMDV2LTUuNjc4YS43OS43OSAwIDAgMC0uNDA3LS42NjdtMi4wMS0zLjAyM2wtLjE0MS0uMDg1bC00Ljc3NC0yLjc4MmEuNzguNzggMCAwIDAtLjc4NSAwTDkuNDA5IDkuMjNWNi44OTdhLjA3LjA3IDAgMCAxIC4wMjgtLjA2MWw0LjgzLTIuNzg3YTQuNSA0LjUgMCAwIDEgNi42OCA0LjY2em0tMTIuNjQgNC4xMzVsLTIuMDItMS4xNjRhLjA4LjA4IDAgMCAxLS4wMzgtLjA1N1Y2LjA3NWE0LjUgNC41IDAgMCAxIDcuMzc1LTMuNDUzbC0uMTQyLjA4TDguNzA0IDUuNDZhLjguOCAwIDAgMC0uMzkzLjY4MXptMS4wOTctMi4zNjVsMi42MDItMS41bDIuNjA3IDEuNXYyLjk5OWwtMi41OTcgMS41bC0yLjYwNy0xLjVaIi8+PC9zdmc+" alt="" width="50px" height="50px">
              <span>大模型算子</span>
            </div>
            <div class="dnd-rect" @mousedown="startDrag('end',$event)">
              <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA0IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgMTFsLTMgM2wtMy0zaDJ2LTRoMnY0eiIvPjwvc3ZnPg==" alt="" width="50px" height="50px">
              <span>目标数据源</span>
            </div>
          </div>
        </a-layout-sider>
        <a-layout-content style="height: calc(100vh - 64px)">
          <div id="container">
            <div id="graph-container">
            </div>
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
    <LoadingDx size="'size-1x'" v-if="selectloading"></LoadingDx>
  </div>
</template>

<script>
  import { dsImgObj } from './../datasource/const'
  import { Graph } from '@antv/x6'
  import LoadingDx from './../../components/common/loading-dx.vue'
  import { Selection } from '@antv/x6-plugin-selection'
  import { Snapline } from '@antv/x6-plugin-snapline'
  import { Keyboard } from '@antv/x6-plugin-keyboard'
  import { Clipboard } from '@antv/x6-plugin-clipboard'
  import { MiniMap } from '@antv/x6-plugin-minimap'
  import { Dnd } from '@antv/x6-plugin-dnd'
  import { History } from '@antv/x6-plugin-history'
  import { fetchTables, getDsTbFieldsInfo, listQuery } from '@/api/datasource/datasource'
  import { addObj, getObj, modifyObj } from '@/api/job/job'

  export default {
    components: {
      LoadingDx
    },
    data () {
      return {
        visible: false,
        sqlVisible: false,
        llmVisible: false,
        toDsVisible: false,
        parentNodeBlood: {},
        currentNodeId: '',
        jobName: '',
        jobId: '',
        dsImgObj,
        fromDsList: [],
        toDsList: [],
        sourceTables: [],
        targetTables: [],
        sourceFields: [],
        toDsSourceFields: [],
        targetFields: [],
        selectloading: false,
        disabledTrue: true,
        previousNode: '${previous_node}',
        selectedDataSourceName: '',
        selectedDataSource: '',
        nodeName: '',
        sqlOperatorValue: '',
        sqlOperatorWhereValue: '',
        sqlOperatorGroupValue: '',
        selectedSourceTable: '',
        selectedTargetSource: '',
        selectedTargetTable: '',
        llmPrompt: '',
        mappings: [
          // { sourceField: '' }
        ],
        targetMappings: [
        ],
        tags: [],
        sqlOperatorList: [],
        inputVisible: false,
        inputValue: '',
        syncMode: 'overwrite',
        cover: 0,
        isIncrement: false,
        incrementField: '',
        schedulerConf: '',
        tools: [
          {
            title: '保存',
            iconClass: 'save',
            key: 'save'
          },
          {
            title: '撤销',
            iconClass: 'left',
            key: 'onUndo'
          },
          {
            title: '前进',
            iconClass: 'right',
            key: 'onRedo'
          },
          {
            title: '放大',
            iconClass: 'zoomIn',
            key: 'zoomIn'
          },
          {
            title: '缩小',
            iconClass: 'zoomOut',
            key: 'zoomOut'
          },
          {
            title: '居中',
            iconClass: 'center',
            key: 'centerContent'
          },
          {
            title: '预览',
            iconClass: 'view',
            key: 'view'
          },
          {
            title: '开启框选',
            iconClass: 'select',
            key: 'select',
            status: false
          },
          {
            title: '开启平移',
            iconClass: 'move',
            key: 'move',
            status: false
          },
          {
            title: '退出',
            iconClass: 'exit',
            key: 'exit'
          }
        ]
      }
    },
    created () {
      this.sqlOperatorValue = ''
      this.sqlOperatorWhereValue = ''
      this.sqlOperatorGroupValue = ''
      this.llmPrompt = ''
      this.inputValue = ''
      this.mappings = []
    },
    mounted () {
      this.initGraph()
      this.selectloading = true
      listQuery().then(res => {
        this.selectloading = false
        const record = res.result
        const excludeFromDs = ['redis', 'kafka', 'custom']
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
    },
    inject: ['closeDraw'],
    computed: {
      isRedisTo () {
        let temp = false
        if (this.selectedTargetSource) {
          temp = this.toDsList.find(item => { return item.dsId === this.selectedTargetSource })?.type === 4
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
    beforeDestroy() {
      if (this.graph) {
        // 销毁graph实例，释放资源
        this.graph.dispose();
        // 清空引用，避免内存泄漏
        this.graph = null;
      }
      if (this.dnd) {
        // 销毁dnd实例
        this.dnd.dispose();
        this.dnd = null;
      }
    },
    methods: {
      handleFromTbChange (value) {
        for (const i in this.fromDsList) {
          if (this.fromDsList[i].dsId === this.selectedDataSource) {
            this.selectedDataSourceName = this.fromDsList[i].catalog
          }
        }
        this.selectedSourceTable = value
        this.selectloading = true
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
        this.sqlOperatorValue = ''
        this.sqlOperatorWhereValue = ''
        this.sqlOperatorGroupValue = ''
        this.sqlOperatorList = []
      },
      handleToTbChange (value) {
        this.selectloading = true
        this.selectedTargetTable = value
        // 切换目标表同步表单数据
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
      handleFromChange (value) {
        this.selectedDataSource = value
        this.selectloading = true
        console.log(this.cacheCell)
        fetchTables(value).then(res => {
          this.selectloading = false
          this.sourceTables = res.result
        })
      },
      handleToChange (value) {
        this.selectedTargetSource = value
        this.selectloading = true
        fetchTables(value).then(res => {
          this.selectloading = false
          this.targetTables = res.result
        })
      },
      cleanSelect () {
        this.sqlOperatorValue = ''
        this.toDsSourceFields = []
        for (const i in this.mappings) {
          if (!this.toDsSourceFields.includes(this.mappings[i].sourceField)) {
            this.toDsSourceFields.push(this.mappings[i].sourceField)
          }
        }
      },
      showInput () {
        this.inputVisible = true
        this.$nextTick(function () {
          this.$refs.input.focus()
        })
      },
      handleInputChange (e) {
        this.inputValue = e.target.value
      },
      handleInputConfirm () {
        const inputValue = this.inputValue
        let tags = this.tags
        if (inputValue && tags.indexOf(inputValue) === -1) {
          tags = [...tags, inputValue]
        }
        console.log(tags)
        Object.assign(this, {
          tags,
          inputVisible: false,
          inputValue: ''
        })
      },
      addMapping () {
        this.mappings.push({ sourceField: '' })
      },
      removeMapping (index) {
        const removeField = this.mappings[index].sourceField
        const sourceFieldIndex = this.toDsSourceFields.indexOf(removeField)
        if (sourceFieldIndex > -1) {
          this.toDsSourceFields.splice(sourceFieldIndex, 1)
        }
        for (const i in this.targetMappings) {
          if (this.targetMappings[i].sourceField === removeField) {
            this.targetMappings.splice(i, 1)
            break
          }
        }
        this.mappings.splice(index, 1)
      },
      addTargetMapping () {
        this.targetMappings.push({ sourceField: '' })
      },
      removeTargetMapping (index) {
        this.targetMappings.splice(index, 1)
      },
      afterVisibleChange (val) {
        console.log('visible', val)
      },
      handleDrop (event) {
        event.preventDefault()
        const tag = event.dataTransfer.getData('text')
        console.log('drag', this.sqlOperatorValue)
        if (this.sqlOperatorValue === '') {
          this.sqlOperatorValue += tag
        } else {
          this.sqlOperatorValue += ', '
          this.sqlOperatorValue += tag
        }
        this.sourceFields.push(tag)
        this.targetMappings.push({ sourceField: tag, targetField: '' })
      },
      handleDragStart (tag, event) {
        event.dataTransfer.setData('text', tag)
      },
      onClose () {
        this.visible = false
        this.sqlVisible = false
        this.toDsVisible = false
        this.llmVisible = false
        this.tags = []
        for (const i in this.mappings) {
          if (this.mappings[i].sourceField !== '') {
            this.tags.push(this.mappings[i].sourceField)
          }
        }

        for (const node of this.graph.getNodes()) {
          const nodeData = {}
          if (node.id === this.currentNodeId) {
            if (node.shape === 'sql') {
              nodeData['sqlOperatorValue'] = this.sqlOperatorValue
              nodeData['sqlOperatorWhereValue'] = this.sqlOperatorWhereValue
              nodeData['sqlOperatorGroupValue'] = this.sqlOperatorGroupValue
              nodeData['sqlOperatorFrom'] = this.selectedSourceTable
            }
            if (node.shape === 'llm') {
              nodeData['prompt'] = this.llmPrompt
            }
            if (node.shape === 'custom-start-node') {
              nodeData['mappings'] = this.mappings
            }
            nodeData['id'] = this.currentNodeId
            node.setData(nodeData)
            break
          }
        }
        console.log('this.sqlOperatorValue', this.sqlOperatorValue)
        // setTimeout(() => {
        //   this.llmPrompt = ''
        //   this.sqlOperatorValue = ''
        //   this.currentNodeId = ''
        //   this.sqlOperatorList = []
        // }, 100)
      },
      edit (jobId) {
        this.selectloading = true
        getObj(jobId).then(res => {
          const record = res.result
          this.selectedTargetSource = record.to_ds_id
          this.selectedDataSource = record.from_ds_id
          this.selectedSourceTable = record.from_tb_name
          this.selectedTargetTable = record.to_tb_name
          this.schedulerConf = record.scheduler_conf
          this.targetMappings = record.field_mappings
          this.jobId = record.job_id
          this.jobName = record.job_name
          this.syncMode = record.sync_mode.mode
          this.cover = record.cover
          this.incrementField = record.sync_mode.increate_field
          if (this.syncMode === 'increment') {
            this.isIncrement = true
          }
          for (const i in record.field_mappings) {
            this.mappings.push({ sourceField: record.field_mappings[i].sourceField })
            this.tags.push(this.mappings[i].sourceField)
          }

          fetchTables(this.selectedTargetSource).then(res => {
            this.targetTables = res.result
          })
          this.handleFromTbChange(this.selectedSourceTable)
          fetchTables(this.selectedDataSource).then(res => {
            this.sourceTables = res.result
          })
          this.handleToTbChange(this.selectedTargetTable)
          const graphData = JSON.parse(record.graph)
          this.graph.fromJSON(JSON.parse(record.graph))

          console.log('graphData', graphData)
          for (const node of graphData.cells) {
            if (node.shape === 'sql') {
              this.sqlOperatorValue = node.data.sqlOperatorValue
              this.sqlOperatorWhereValue = node.data.sqlOperatorWhereValue
              this.sqlOperatorGroupValue = node.data.sqlOperatorGroupValue
            }
            if (node.shape === 'llm') {
              console.log('node.data.prompt', node.data.prompt)
              this.llmPrompt = node.data.prompt
            }
            if (node.shape === 'custom-start-node') {
              this.mappings = node.data.mappings
            }
          }
        })
        this.selectloading = false
      },
      handleTrigger (command) {
        switch (command) {
          case 'save':
            this.handleSave()
            break
          case 'onUndo':
            this.graph.undo()
            break
          case 'onRedo':
            this.graph.redo()
            break
          case 'zoomIn':
            this.graph.zoom(0.2)
            break
          case 'zoomOut':
            this.graph.zoom(-0.2)
            break
          case 'centerContent':
            this.graph.centerContent()
            break
          case 'view':
            this.exportJson()
            break
          case 'select':
            this.changeRubberband(command)
            break
          case 'move':
            this.changePann(command)
            break
          case 'exit':
            this.closeDraw()
            this.$router.push('/compute/job')
            break
          default:
            break
        }
      },
      // 保存的方法 根据业务需要达到数据处理成想要的
      handleSave () {
        this.selectloading = true
        const data = this.graph.toJSON() // 可以拿到画完图的数s据
        const formData = {
          'job_id': this.jobId,
          // 提交表单数据
          'from_ds_id': this.selectedDataSource,
          'to_ds_id': this.selectedTargetSource,
          'from_tb_name': this.selectedSourceTable,
          'to_tb_name': this.selectedTargetTable,
          'job_name': this.jobName,
          'scheduler_conf': this.schedulerConf,
          'field_mappings': this.targetMappings,
          'graph': JSON.stringify(data),
          'cover': this.cover,
          'sync_mode': {
            'mode': this.syncMode,
            'increate_field': this.incrementField
          },
          'type': 2,
          'run': false
        }
        if (this.jobId !== '') {
          modifyObj(formData).then(res => {
            if (res.status === '0') {
              this.$message.success('修改成功')
              this.closeDraw()
            } else {
              this.$message.error(res.errstr)
            }
          }).catch(err => {
            this.$message.error(err.errstr)
          })
        } else {
          addObj(formData).then(res => {
            if (res.status === '0') {
              this.$message.success('新增成功')
              this.closeDraw()
            } else {
              this.$message.error(res.errstr)
            }
          }).catch(err => {
            this.$message.error(err.errstr)
          })
        }
        this.selectloading = false
        // const nodeArr = data.cells
        // const filterCell = nodeArr.filter(item => item.shape !== 'edge')// 这里过滤我们需要的数据，可以根据自己的业务需要来做
        // const rulesNodeDTOList = []
        // for (const item of filterCell) {
        //   const nodeAttribute = item.data ? item.data.nodeAttribute : {}
        //   if (nodeAttribute) {
        //     rulesNodeDTOList.push(nodeAttribute)
        //   }
        // }
      },
      // 预览的方法，根据业务我这里预览转成了G6
      exportJson () {
        const data = this.graph.toJSON()
        this.$store.dispatch('g6/setG6data', data)
        console.log(JSON.stringify(data))
        console.log(data)
        // this.$router.push('g6')
        this.dialogTableVisible = true
      },
      // 开/关框选的方法
      changeRubberband (key) {
        this.tools.forEach(item => {
          if (item.key === key) {
            item.status = !item.status
            item.status ? item.title = '关闭框选' : item.title = '开启框选'
            // this.graph.toggleSelection(item.status)
            this.graph.toggleRubberband(item.status)
            this.graph.toggleStrictRubberband(item.status)
            this.graph.cleanSelection(item.status)
          }
        })
      },
      // 开/关画布平移的方法
      changePann (key) {
        this.tools.forEach(item => {
          if (item.key === key) {
            item.status = !item.status
            item.status ? item.title = '关闭平移' : item.title = '开启平移'
            this.graph.togglePanning(item.status)
          }
        })
        // graph.togglePanning(val)
        // val ? graph.enablePanning() : graph.disablePanning();
      },
      startDrag (type, e) {
        this.startDragToGraph(this.graph, type, e)
      },
      startDragToGraph (graph, type, e) {
        const startNode = this.graph.createNode({
          shape: 'custom-start-node',
          width: 55,
          height: 55,
          data: '',
          attrs: {
            body: {
              strokeWidth: 1,
              stroke: '#000000',
              fill: '#ffffff',
              rx: 10,
              ry: 10
            }
          }
        })
        const polygonNode = this.graph.createNode({
          shape: 'llm',
          width: 55,
          height: 55,
          attrs: {
            body: {
              strokeWidth: 1,
              stroke: '#000000',
              fill: '#ffffff',
              rx: 10,
              ry: 10
            },
            label: {
              fontSize: 13,
              fontWeight: 'bold'
            }
          }
        })
        const rectNode = this.graph.createNode({
          shape: 'sql',
          width: 55,
          height: 55,
          attrs: {
            body: {
              strokeWidth: 1,
              stroke: '#000000',
              fill: '#ffffff',
              rx: 10,
              ry: 10
            },
            label: {
              fontSize: 13,
              fontWeight: 'bold'
            }
          }
        })
        const endNode = this.graph.createNode({
          shape: 'custom-end-node',
          width: 55,
          height: 55,
          data: '',
          attrs: {
            body: {
              strokeWidth: 1,
              stroke: '#000000',
              fill: '#ffffff',
              rx: 10,
              ry: 10
            },
            label: {
              fontSize: 13,
              fontWeight: 'bold'
            }
          }
        })
        let dragNode
        if (type === 'start') {
          dragNode = startNode
        } else if (type === 'end') {
          dragNode = endNode
        } else if (type === 'rect') {
          dragNode = rectNode
        } else if (type === 'polygon') {
          dragNode = polygonNode
        }

        this.dnd.start(dragNode, e)
      },
      initGraph: function () {
        const nodeWidth = 80
        const nodeHeight = 60

        this.graph = new Graph({
          container: document.getElementById('graph-container'),
          autoResize: true,
          translating: {
            restrict: true
          },
          mousewheel: {
            enabled: true,
            modifiers: 'Ctrl',
            maxScale: 4,
            minScale: 0.2
          },
          grid: {
            visible: true,
            type: 'mesh',
            args: [
              {
                color: '#c5c5c5', // 主网格线颜色
                thickness: 1 // 主网格线宽度
              }
            ]
          },
          connecting: {
            snap: true, // 是否自动吸附
            allowMulti: true, // 是否允许在相同的起始节点和终止之间创建多条边
            allowNode: false, // 是否允许边链接到节点（非节点上的链接桩）
            allowBlank: false, // 是否允许连接到空白点
            allowLoop: false, // 是否允许创建循环连线，即边的起始节点和终止节点为同一节点，
            allowEdge: false, // 是否允许边链接到另一个边
            highlight: true, // 拖动边时，是否高亮显示所有可用的连接桩或节点
            router: {
              name: 'manhattan',
              args: {
                startDirections: ['top', 'right', 'bottom', 'left'],
                endDirections: ['top', 'right', 'bottom', 'left']
              }
            },
            // connector: {
            //     name: 'rounded',
            //     // args: { radius: 10, },
            // },
            anchor: 'center',
            connectionPoint: 'anchor',
            validateConnection ({ targetMagnet }) {
              return !!targetMagnet
            }
          },
          highlighting: {
            // 连接桩可以被连接时在连接桩外围围渲染一个包围框
            magnetAvailable: {
              name: 'stroke',
              args: {
                attrs: {
                  fill: '#fff',
                  stroke: '#A4DEB1',
                  strokeWidth: 4
                }
              }
            },
            // 连接桩吸附连线时在连接桩外围围渲染一个包围框
            magnetAdsorbed: {
              name: 'stroke',
              args: {
                attrs: {
                  fill: '#fff',
                  stroke: '#31d0c6',
                  strokeWidth: 4
                }
              }
            }
          }
        })

        this.dnd = new Dnd({
          target: this.graph,
          scaled: false
        })

        this.graph
          .use(new Selection({ shelectionBox: true, pointerEvents: 'none' }))
          .use(new Snapline())
          .use(new Keyboard())
          .use(new Clipboard())
          .use(new History())
          .use(new MiniMap({ container: document.getElementById('minimap') }))
        // 连接桩配置
        const ports = {
          groups: {
            top: {
              position: 'top',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  fill: '#fff',
                  style: {
                    visibility: 'hidden'
                  }
                }
              }
            },
            right: {
              position: 'right',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  fill: '#fff',
                  style: {
                    visibility: 'hidden'
                  }
                }
              }
            },
            bottom: {
              position: 'bottom',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  fill: '#fff',
                  style: {
                    visibility: 'hidden'
                  }
                }
              }
            },
            left: {
              position: 'left',
              attrs: {
                circle: {
                  r: 4,
                  magnet: true,
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  fill: '#fff',
                  style: {
                    visibility: 'hidden'
                  }
                }
              }
            }
          },
          items: [
            // {
            //   group: 'top'
            // },
            {
              group: 'right'
            },
            // {
            //   group: 'bottom'
            // },
            {
              group: 'left'
            }
          ]
        }
        // 控制连接桩显示/隐藏
        const showPorts = (ports, show) => {
          for (let i = 0, len = ports.length; i < len; i += 1) {
            ports[i].style.visibility = show ? 'visible' : 'hidden'
          }
        }
        Graph.registerNode(
          'sql',
          {
            inherit: 'rect', // 继承于 rect 节点
            ports: { ...ports },
            width: 100,
            height: 40,
            data: [],
            markup: [
              {
                tagName: 'rect', // 标签名称
                selector: 'body' // 选择器
              },
              {
                tagName: 'image',
                selector: 'img'
              },
              {
                tagName: 'text',
                selector: 'label'
              }
            ],
            attrs: {
              body: {
                stroke: '#8f8f8f',
                strokeWidth: 1,
                fill: '#fff',
                rx: 6,
                ry: 6
              },
              img: {
                'xlink:href':
                  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9Im5vbmUiIHN0cm9rZT0iY3VycmVudENvbG9yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIHN0cm9rZS13aWR0aD0iMiIgZD0iTTEyIDhhMiAyIDAgMCAxIDIgMnY0YTIgMiAwIDEgMS00IDB2LTRhMiAyIDAgMCAxIDItMm01IDB2OGg0bS04LTFsMSAxTTMgMTVhMSAxIDAgMCAwIDEgMWgyYTEgMSAwIDAgMCAxLTF2LTJhMSAxIDAgMCAwLTEtMUg0YTEgMSAwIDAgMS0xLTFWOWExIDEgMCAwIDEgMS0xaDJhMSAxIDAgMCAxIDEgMSIvPjwvc3ZnPg==',
                width: 45,
                height: 45,
                x: 6,
                y: 6
              }
            }
          },
          true
        )

        Graph.registerNode(
          'llm',
          {
            inherit: 'rect',
            ports: { ...ports },
            width: 100,
            height: 40,
            data: [],
            markup: [
              {
                tagName: 'rect', // 标签名称
                selector: 'body' // 选择器
              },
              {
                tagName: 'image',
                selector: 'img'
              },
              {
                tagName: 'text',
                selector: 'label'
              }
            ],
            attrs: {
              body: {
                stroke: '#8f8f8f',
                strokeWidth: 1,
                fill: '#fff',
                rx: 6,
                ry: 6
              },
              img: {
                'xlink:href':
                  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIyLjI4MiA5LjgyMWE2IDYgMCAwIDAtLjUxNi00LjkxYTYuMDUgNi4wNSAwIDAgMC02LjUxLTIuOUE2LjA2NSA2LjA2NSAwIDAgMCA0Ljk4MSA0LjE4YTYgNiAwIDAgMC0zLjk5OCAyLjlhNi4wNSA2LjA1IDAgMCAwIC43NDMgNy4wOTdhNS45OCA1Ljk4IDAgMCAwIC41MSA0LjkxMWE2LjA1IDYuMDUgMCAwIDAgNi41MTUgMi45QTYgNiAwIDAgMCAxMy4yNiAyNGE2LjA2IDYuMDYgMCAwIDAgNS43NzItNC4yMDZhNiA2IDAgMCAwIDMuOTk3LTIuOWE2LjA2IDYuMDYgMCAwIDAtLjc0Ny03LjA3M00xMy4yNiAyMi40M2E0LjQ4IDQuNDggMCAwIDEtMi44NzYtMS4wNGwuMTQxLS4wODFsNC43NzktMi43NThhLjguOCAwIDAgMCAuMzkyLS42ODF2LTYuNzM3bDIuMDIgMS4xNjhhLjA3LjA3IDAgMCAxIC4wMzguMDUydjUuNTgzYTQuNTA0IDQuNTA0IDAgMCAxLTQuNDk0IDQuNDk0TTMuNiAxOC4zMDRhNC40NyA0LjQ3IDAgMCAxLS41MzUtMy4wMTRsLjE0Mi4wODVsNC43ODMgMi43NTlhLjc3Ljc3IDAgMCAwIC43OCAwbDUuODQzLTMuMzY5djIuMzMyYS4wOC4wOCAwIDAgMS0uMDMzLjA2Mkw5Ljc0IDE5Ljk1YTQuNSA0LjUgMCAwIDEtNi4xNC0xLjY0Nk0yLjM0IDcuODk2YTQuNSA0LjUgMCAwIDEgMi4zNjYtMS45NzNWMTEuNmEuNzcuNzcgMCAwIDAgLjM4OC42NzdsNS44MTUgMy4zNTRsLTIuMDIgMS4xNjhhLjA4LjA4IDAgMCAxLS4wNzEgMGwtNC44My0yLjc4NkE0LjUwNCA0LjUwNCAwIDAgMSAyLjM0IDcuODcyem0xNi41OTcgMy44NTVsLTUuODMzLTMuMzg3TDE1LjExOSA3LjJhLjA4LjA4IDAgMCAxIC4wNzEgMGw0LjgzIDIuNzkxYTQuNDk0IDQuNDk0IDAgMCAxLS42NzYgOC4xMDV2LTUuNjc4YS43OS43OSAwIDAgMC0uNDA3LS42NjdtMi4wMS0zLjAyM2wtLjE0MS0uMDg1bC00Ljc3NC0yLjc4MmEuNzguNzggMCAwIDAtLjc4NSAwTDkuNDA5IDkuMjNWNi44OTdhLjA3LjA3IDAgMCAxIC4wMjgtLjA2MWw0LjgzLTIuNzg3YTQuNSA0LjUgMCAwIDEgNi42OCA0LjY2em0tMTIuNjQgNC4xMzVsLTIuMDItMS4xNjRhLjA4LjA4IDAgMCAxLS4wMzgtLjA1N1Y2LjA3NWE0LjUgNC41IDAgMCAxIDcuMzc1LTMuNDUzbC0uMTQyLjA4TDguNzA0IDUuNDZhLjguOCAwIDAgMC0uMzkzLjY4MXptMS4wOTctMi4zNjVsMi42MDItMS41bDIuNjA3IDEuNXYyLjk5OWwtMi41OTcgMS41bC0yLjYwNy0xLjVaIi8+PC9zdmc+',
                width: 45,
                height: 45,
                x: 6,
                y: 6
              }
            }
          },
          true
        )

        // Graph.registerNode(
        //   'custom-start-node',
        //   {
        //     inherit: 'circle',
        //     ports: { ...ports }
        //   },
        //   true
        // )
        Graph.registerNode(
          'custom-start-node',
          {
            inherit: 'rect', // 继承于 rect 节点
            ports: { ...ports },
            width: 100,
            height: 40,
            markup: [
              {
                tagName: 'rect', // 标签名称
                selector: 'body' // 选择器
              },
              {
                tagName: 'image',
                selector: 'img'
              },
              {
                tagName: 'text',
                selector: 'label'
              }
            ],
            attrs: {
              body: {
                stroke: '#8f8f8f',
                strokeWidth: 1,
                fill: '#fff',
                rx: 6,
                ry: 6
              },
              img: {
                'xlink:href':
                  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA0IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgOWgtMnY0aC0ydi00aC0ybDMtM3oiLz48L3N2Zz4=',
                width: 45,
                height: 45,
                x: 6,
                y: 6
              }
            }
          },
          true
        )
        Graph.registerNode(
          'custom-end-node',
          {
            inherit: 'rect', // 继承于 rect 节点
            ports: { ...ports },
            width: 100,
            height: 40,
            markup: [
              {
                tagName: 'rect', // 标签名称
                selector: 'body' // 选择器
              },
              {
                tagName: 'image',
                selector: 'img'
              },
              {
                tagName: 'text',
                selector: 'label'
              }
            ],
            attrs: {
              body: {
                stroke: '#8f8f8f',
                strokeWidth: 1,
                fill: '#fff',
                rx: 6,
                ry: 6
              },
              img: {
                'xlink:href':
                  'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA0IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgMTFsLTMgM2wtMy0zaDJ2LTRoMnY0eiIvPjwvc3ZnPg==',
                width: 45,
                height: 45,
                x: 6,
                y: 6
              }
            }
          },
          true
        )
        this.graph.bindKey(['meta+c', 'ctrl+c'], () => {
          const cells = this.graph.getSelectedCells()
          if (cells.length) {
            this.graph.copy(cells)
          }
          return false
        })

        this.graph.bindKey(['meta+x', 'ctrl+x'], () => {
          const cells = this.graph.getSelectedCells()
          if (cells.length) {
            this.graph.cut(cells)
          }
          return false
        })

        this.graph.bindKey(['meta+v', 'ctrl+v'], () => {
          if (!this.graph.isClipboardEmpty()) {
            const cells = this.graph.paste({ offset: 32 })
            this.graph.cleanSelection()
            this.graph.select(cells)
          }
          return false
        })

        this.graph.bindKey(['meta+z', 'ctrl+z'], () => {
          if (this.graph.canUndo()) {
            this.graph.undo()
          }
          return false
        })

        this.graph.bindKey(['meta+shift+z', 'ctrl+shift+z'], () => {
          if (this.graph.canRedo()) {
            this.graph.redo()
          }
          return false
        })

        this.graph.bindKey(['meta+a', 'ctrl+a'], () => {
          const nodes = this.graph.getNodes()
          if (nodes) {
            this.graph.select(nodes)
          }
        })

        this.graph.bindKey('backspace', () => {
          const cells = this.graph.getSelectedCells()
          if (cells.length) {
            this.graph.removeCells(cells)
            console.log("remove cell####################")
          }
        })

        this.graph.bindKey(['ctrl+1', 'meta+1'], () => {
          const zoom = this.graph.zoom()
          if (zoom < 1.5) {
            this.graph.zoom(0.1)
          }
        })

        this.graph.bindKey(['ctrl+2', 'meta+2'], () => {
          const zoom = graph.zoom()
          if (zoom > 0.5) {
            this.graph.zoom(-0.1)
          }
        })

        this.graph.on('cell:mouseenter', ({ cell }) => {
          // console.log(cell.isNode(), '123')
          const container = document.getElementById('graph-container')
          const ports = container.querySelectorAll('.x6-port-body')
          showPorts(ports, !cell.attrs.typeName)
          if (cell.isNode()) {
            cell.addTools([
              {
                name: 'button-remove',
                args: {
                  x: 0,
                  y: 0,
                  offset: { x: 10, y: 10 }
                }
              }
            ])
          } else {
            cell.addTools([
              {
                name: 'button-remove',
                args: { distance: -40 }
              }
            ])
          }
        })

        this.graph.on('cell:mouseleave', ({ cell }) => {
          if (cell.hasTool('button-remove')) {
            cell.removeTool('button-remove')
          }
        })
        this.graph.on('node:click', ({ x, y, node, cell }) => {
          this.currentNodeId = node.id
          console.log('click node', node)
          const nodeData = node.getData()
          if (cell.shape === 'custom-start-node') {
            this.visible = true
          }
          if (cell.shape === 'sql') {
            this.sqlVisible = true
            // if (nodeData['sqlOperatorValue']) {
            //   this.sqlOperatorValue = nodeData['sqlOperatorValue']
            // }
            // if (nodeData['sqlOperatorWhereValue']) {
            //   this.sqlOperatorWhereValue = nodeData['sqlOperatorWhereValue']
            // }
            // if (nodeData['sqlOperatorGroupValue']) {
            //   this.sqlOperatorGroupValue = nodeData['sqlOperatorGroupValue']
            // }
          }
          if (cell.shape === 'llm') {
            this.llmVisible = true
          }
          if (cell.shape === 'custom-end-node') {
            this.toDsVisible = true
            // 父节点 Object.keys(this.graph.model.outgoings)[0]
            // 当前节点 Object.keys(this.graph.model.incomings)[0]
            const parentId = Object.keys(this.graph.model.outgoings)[0]
            for (const i in this.mappings) {
              if (!this.toDsSourceFields.includes(this.mappings[i].sourceField)) {
                this.toDsSourceFields.push(this.mappings[i].sourceField)
              }
            }

            // this.parentNodeBlood[Object.keys(this.graph.model.incomings)[0]] = Object.keys(this.graph.model.outgoings)[0]
            console.log(this.graph.getNodes())
            var hasLlm = 0
            for (const node of this.graph.getNodes()) {
              if (node.shape === 'llm') {
                hasLlm = 1
              }
            }
            if (hasLlm === 1) {
              if (!this.toDsSourceFields.includes('llm_output')) {
                this.toDsSourceFields.push('llm_output')
              }
            } else {
              const sourceFieldIndex = this.toDsSourceFields.indexOf('llm_output')
              if (sourceFieldIndex > -1) {
                this.toDsSourceFields.splice(sourceFieldIndex, 1)
              }
            }
          }
          if (cell.isNode() && !cell.attrs.typeName) {
            // 这可以写一些点击节点时和右侧表单交互的效果
            const selectedCell = this.graph.getSelectedCells()
          }
          if (cell.hasTool('button')) {
            cell.removeTool('button')
          } else {
            const markup = [
              {
                tagName: 'circle',
                selector: 'button',
                attrs: {
                  r: 10,
                  stroke: '#6d6d6d',
                  'stroke-width': 3,
                  fill: 'white',
                  cursor: 'pointer'
                }
              }
            ]
          }
        })
        this.graph.on('cell:delete', ({ cell, index, options }) => {
          console.log(cell)
        })
        this.graph.on('cell:added', ({ cell, index, options }) => {
          if (!cell.isNode()) {
          }
        })
        this.graph.on('blank:click', () => {
          // this.currentCell && this.currentCell.removeTools();
          if (this.currentCell) {
            this.currentCell.removeTool('button')
            this.currentCell.removeTool('button-move')
          }
          this.isClose = true
          this.isGloable = true
        })
      }
    }
  }
</script>

<style>
  .select-container {
    margin-bottom: 10px; /* 添加底部间距 */
  }

  .input-full-width {
    width: 100%;
  }
</style>
<style lang="less" scoped>
  ::v-deep .ds-icon {
    float: left;
    width: 24px;
    height: 24px;
    border-radius: 6px;
    overflow: hidden;
    margin-right: 4px;
    img {
      width: 24px;
      height: 24px;
      margin: 0;
      padding: 0;
      border: 0;
    }
  }
  .g6-wrap {
    width: 100%;
    height: 100vh;

    .el-dialog {
      display: flex;
      flex-direction: column;
      margin: 0 !important;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      /*height:600px;*/
      max-height: calc(100% - 30px);
      max-width: calc(100% - 30px);
      border-radius: 2px;
    }

    .el-dialog .el-dialog__body {
      flex: 1;
      overflow: auto;
    }

    position: relative;

    .top-box {
      height: 40px;
      z-index: 999;
      //background: #FFFFFF;
      //position: absolute;
      //top: 0;
      //left: 100px;

      .tools-box {
        display: flex;
        align-items: center;

        .tool {
          width: 50px;
          margin-right: 5px;
          cursor: pointer;
          padding: 2px;
          text-align: center;

          img {
            width: 20px;
            height: 20px;
          }

          .word {
            font-size: 10px;
          }
        }

        .tool:hover {
          background: #d7d7d7;
          border-radius: 2px;
        }
      }

      .goBack {
        height: 38px;
        line-height: 38px;
      }
    }

    #container {
      display: flex;
      height: 100%;
      margin: 0 10px;

      .x6-widget-selection-box {
        border: 3px dashed #239edd;
        border-radius: 1px;
      }

      .x6-widget-selection-inner {
        border: 2px solid #239edd;
        border-radius: 10px;
      }

      #minimap {
        position: absolute;
        top: 30px;
        right: 30%;
      }
    }

    #stencil {
      width: 100%;
      height: 100%;
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      border-right: 1px solid #dfe3e8;
      text-align: center;
      font-size: 12px;
      padding: 5px 10px;

      .dnd-rect {
        cursor: pointer;
        border: .5px solid #000;
        width: 100%;
        border-radius: 10px;
        margin: 5px 0;
        /*   width: 50px;
           height: 30px;
           line-height: 40px;
           text-align: center;
           border: 2px solid #000000;
           border-radius: 6px;
           cursor: move;
           font-size: 12px;
           margin-top: 30px;*/
      }

      .dnd-polygon {
        width: 35px;
        height: 35px;
        border: 2px solid #000000;
        transform: rotate(45deg);
        cursor: move;
        font-size: 12px;
        margin-top: 30px;
        margin-bottom: 10px;
      }

      .dnd-circle {
        width: 35px;
        height: 35px;
        line-height: 45px;
        text-align: center;
        border: 5px solid #000000;
        border-radius: 100%;
        cursor: move;
        font-size: 12px;
        margin-top: 30px;
      }

      .dnd-start {
        //border: 2px solid #000000;

      }

      .x6-widget-stencil {
        background-color: #f8f9fb;
      }

      .x6-widget-stencil-title {
        background: #eee;
        font-size: 1rem;
      }

      .x6-widget-stencil-group-title {
        font-size: 1rem !important;
        background-color: #fff !important;
        height: 40px !important;
      }

      .x6-widget-transform {
        margin: -1px 0 0 -1px;
        padding: 0px;
        border: 1px solid #239edd;
      }

      .x6-widget-transform > div {
        border: 1px solid #239edd;
      }

      .x6-widget-transform > div:hover {
        background-color: #3dafe4;
      }

      .x6-widget-transform-active-handle {
        background-color: #3dafe4;
      }

      .x6-widget-transform-resize {
        border-radius: 0;
      }

    }

    .right-box {
      display: block;
      position: absolute;
      top: 0;
      right: 0;
      width: 30%;
      height: 100%;
      border-left: 2px solid #ccc;
      background: #FFFFFF;
      overflow: auto;
      transition: display .3s;
    }

    .close {
      display: none;
    }

    .open {
      position: absolute;
      top: 50%;
      right: 30%;
      width: 20px;
      height: 100px;
      font-size: 25px;
      background: #FFFFFF;
      text-align: center;
      line-height: 100px;
      border: 2px solid #ccc;
      border-right: none;
      border-radius: 2px;
      cursor: pointer;
      transition: right;
    }

    .right0 {
      right: 10px;
    }

    #graph-container {
      width: calc(100% - 220px);
      border-right: 2px solid #ccc;
      /*margin: 45px 10px 10px;*/
    }

    .el-drawer__header {
      margin-bottom: 0;
      font-weight: bold;
      font-size: 20px;
      color: #000;
    }
  }
</style>

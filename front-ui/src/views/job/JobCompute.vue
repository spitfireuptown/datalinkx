<template>
  <div class="g6-wrap">
    <!-- 来源数据源抽屉 -->
    <SourceDrawer
      :visible="drawerVisible.source"
      :ds-list="fromDsList"
      :loading="selectloading"
      :source="sourceConfig"
      :tables="sourceTables"
      :source-fields="sourceFields"
      :mappings="sourceConfig.mappings"
      :sync-mode="syncConfig.mode"
      :trans-cover="transCover"
      :increment-field="syncConfig.incrementField"
      :show-sync-config="showSyncConfig"
      @ds-change="handleFromChange"
      @table-change="handleFromTbChange"
      @sync-mode-change="handleSyncModeChange"
      @cover-change="changeCover"
      @increment-field-change="handleIncrementFieldChange"
      @mapping-change="handleSourceMappingChange"
      @add-mapping="addMapping"
      @remove-mapping="removeMapping"
      @visible-change="afterVisibleChange"
      @close="handleDrawerClose('source')"
    />

    <!-- SQL算子抽屉 -->
    <SqlDrawer
      :visible="drawerVisible.sql"
      :tags="sqlConfig.tags"
      :sql="sqlConfig.sqlValue"
      :graph="graph"
      :ds-id="sourceConfig.dsId"
      @sql-change="handleSqlChange"
      @tag-add="handleTagAdd"
      @visible-change="afterVisibleChange"
      @before-close="saveNodeData"
      @validate-success="handleSqlValidateSuccess"
      @close="handleDrawerClose('sql')"
    />

    <!-- 大模型算子抽屉 -->
    <LlmDrawer
      :visible="drawerVisible.llm"
      :model-provider="llmConfig.modelProvider"
      :model="llmConfig.model"
      :api-key="llmConfig.apiKey"
      :api-path="llmConfig.apiPath"
      :prompt="llmConfig.prompt"
      :graph="graph"
      :node-id="currentNodeId"
      @model-provider-change="handleModelProviderChange"
      @model-change="handleModelChange"
      @api-key-change="handleApiKeyChange"
      @api-path-change="handleApiPathChange"
      @prompt-change="handlePromptChange"
      @visible-change="afterVisibleChange"
      @close="handleDrawerClose('llm')"
    />

    <!-- 动态编译算子抽屉 -->
    <DynamicDrawer
      :visible="drawerVisible.dynamic"
      :source-code="dynamicConfig.sourceCode"
      :output-fields="dynamicConfig.outputFields"
      :graph="graph"
      :node-id="currentNodeId"
      @source-code-change="handleSourceCodeChange"
      @output-fields-change="handleOutputFieldsChange"
      @validate-success="handleValidateSuccess"
      @visible-change="afterVisibleChange"
      @close="handleDrawerClose('dynamic')"
    />

    <!-- 目标数据源抽屉 -->
    <TargetDrawer
      :visible="drawerVisible.target"
      :ds-list="toDsList"
      :loading="selectloading"
      :job-name="jobInfo.name"
      :scheduler-conf="jobInfo.schedulerConf"
      :target="targetConfig"
      :tables="targetTables"
      :source-fields="toDsSourceFields"
      :target-fields="targetFields"
      :mappings="targetMappings"
      @job-name-change="handleJobNameChange"
      @scheduler-change="handleSchedulerChange"
      @ds-change="handleToChange"
      @table-change="handleToTbChange"
      @source-field-change="handleToSourceFieldChange"
      @target-field-change="handleToTargetFieldChange"
      @add-mapping="addTargetMapping"
      @remove-mapping="removeTargetMapping"
      @visible-change="afterVisibleChange"
      @close="handleDrawerClose('target')"
    />

    <!-- 画布区域 -->
    <a-layout>
      <a-layout-header>
        <div class="top-box">
          <div class="tools-box">
            <div
              v-for="tool in tools"
              :key="tool.key"
              class="tool"
              @click="handleToolTrigger(tool.key)"
            >
              <img :src="getToolIcon(tool.iconClass)" :alt="tool.title">
              <div class="word">{{ tool.title }}</div>
            </div>
          </div>
        </div>
      </a-layout-header>

      <a-layout>
        <a-layout-sider style="background: transparent">
          <div id="stencil">
            <div
              v-for="node in nodeTemplates"
              :key="node.type"
              class="dnd-rect"
              @mousedown="startDrag(node.type, $event)"
            >
              <img :src="node.icon" :alt="node.label" width="50px" height="50px">
              <span>{{ node.label }}</span>
            </div>
          </div>
        </a-layout-sider>

        <a-layout-content style="height: calc(100vh - 64px)">
          <div id="container">
            <div id="graph-container"></div>
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>

    <LoadingDx v-if="selectloading" size="'size-1x'" />
  </div>
</template>

<script>
import { Graph } from '@antv/x6'
import { Selection } from '@antv/x6-plugin-selection'
import { Snapline } from '@antv/x6-plugin-snapline'
import { Keyboard } from '@antv/x6-plugin-keyboard'
import { Clipboard } from '@antv/x6-plugin-clipboard'
import { MiniMap } from '@antv/x6-plugin-minimap'
import { Dnd } from '@antv/x6-plugin-dnd'
import { History } from '@antv/x6-plugin-history'
import { fetchTables, getDsTbFieldsInfo, listQuery } from '@/api/datasource/datasource'
import { addObj, getObj, modifyObj } from '@/api/job/job'
import LoadingDx from '@/components/common/loading-dx.vue'
import SourceDrawer from './drawer/SourceDrawer.vue'
import SqlDrawer from './drawer/SqlDrawer.vue'
import LlmDrawer from './drawer/LlmDrawer.vue'
import TargetDrawer from './drawer/TargetDrawer.vue'
import DynamicDrawer from './drawer/DynamicDrawer.vue'
import {
  EXCLUDE_FROM_DS,
  EXCLUDE_TO_DS,
  NODE_SHAPE,
  NODE_ICONS,
  TOOLS_CONFIG,
  KEYBOARD_SHORTCUTS,
  PORT_GROUPS,
  GRAPH_CONFIG
} from './drawer/constants'

export default {
  name: 'JobCompute',

  components: {
    LoadingDx,
    SourceDrawer,
    SqlDrawer,
    LlmDrawer,
    TargetDrawer,
    DynamicDrawer
  },

  inject: ['closeDraw'],

  data () {
    // 抽屉可见性状态
    const drawerVisible = {
      source: false,
      sql: false,
      llm: false,
      target: false,
      dynamic: false
    }

    // 来源配置
    const sourceConfig = {
      dsId: '',
      dsName: '',
      tableName: '',
      mappings: []
    }

    // 同步配置
    const syncConfig = {
      mode: 'overwrite',
      cover: 0,
      incrementField: '',
      isIncrement: false
    }

    // SQL算子配置
    const sqlConfig = {
      sqlValue: '',
      whereValue: '',
      groupValue: '',
      tags: [],
      outputFields: []
    }

    // LLM算子配置
    const llmConfig = {
      modelProvider: '',
      model: '',
      apiKey: '',
      apiPath: '',
      prompt: ''
    }

    // 动态编译算子配置
    const dynamicConfig = {
      sourceCode: '',
      outputFields: []
    }

    // 目标配置
    const targetConfig = {
      dsId: '',
      tableName: ''
    }

    // 任务信息
    const jobInfo = {
      name: '',
      id: '',
      schedulerConf: ''
    }

    return {
      // 图实例
      graph: null,
      dnd: null,

      // 状态
      drawerVisible,
      sourceConfig,
      syncConfig,
      sqlConfig,
      llmConfig,
      dynamicConfig,
      targetConfig,
      jobInfo,

      // 数据列表
      fromDsList: [],
      toDsList: [],
      sourceTables: [],
      targetTables: [],
      sourceFields: [],
      toDsSourceFields: [],
      targetFields: [],
      targetMappings: [],

      // UI状态
      selectloading: false,
      disabledTrue: true,
      selectedSourceTable: '',

      // 工具栏
      tools: TOOLS_CONFIG,

      // 节点模板
      nodeTemplates: [
        { type: 'start', label: '来源数据源', icon: NODE_ICONS.START },
        { type: 'rect', label: 'SQL算子', icon: NODE_ICONS.SQL },
        { type: 'polygon', label: '大模型算子', icon: NODE_ICONS.LLM },
        { type: 'dynamic', label: '动态编译算子', icon: NODE_ICONS.DYNAMIC },
        { type: 'end', label: '目标数据源', icon: NODE_ICONS.END }
      ],

      // 当前编辑节点ID
      currentNodeId: ''
    }
  },

  computed: {
    // 是否显示同步配置
    showSyncConfig () {
      const targetType = this.toDsList.find(
        item => item.dsId === this.targetConfig.dsId
      )?.type
      return targetType !== 4 && this.jobInfo.type !== 'streaming'
    },

    // 数据覆盖布尔值
    transCover () {
      return Boolean(this.syncConfig.cover)
    }
  },

  created () {
    this.resetFormState()
  },

  mounted () {
    this.initGraph()
    this.loadDataSourceList()
  },

  beforeDestroy () {
    this.disposeGraph()
  },

  methods: {
    // ========== 工具方法 ==========

    /**
     * 转义JSON字符串中的特殊字符
     */
    escapeForJson (input) {
      if (!input) {
        return input
      }
      return input.replace(/\\/g, '\\\\')
                  .replace(/"/g, '\\"')
                  .replace(/\n/g, '\\n')
                  .replace(/\r/g, '\\r')
    },

    /**
     * 反转义JSON字符串中的特殊字符
     */
    unescapeFromJson (input) {
      if (!input) {
        return input
      }
      return input.replace(/\\n/g, '\n')
                  .replace(/\\r/g, '\r')
                  .replace(/\\"/g, '"')
                  .replace(/\\\\/g, '\\')
    },

    // ========== 初始化方法 ==========

    /**
     * 初始化Graph画布
     */
    initGraph () {
      this.graph = new Graph({
        container: document.getElementById('graph-container'),
        autoResize: true,
        translating: { restrict: true },
        mousewheel: {
          enabled: true,
          modifiers: 'Ctrl',
          maxScale: GRAPH_CONFIG.MAX_SCALE,
          minScale: GRAPH_CONFIG.MIN_SCALE
        },
        grid: {
          visible: true,
          type: 'mesh',
          args: [{ color: '#c5c5c5', thickness: 1 }]
        },
        connecting: {
          snap: true,
          allowMulti: true,
          allowNode: false,
          allowBlank: false,
          allowLoop: false,
          allowEdge: false,
          highlight: true,
          router: {
            name: 'manhattan',
            args: {
              startDirections: ['top', 'right', 'bottom', 'left'],
              endDirections: ['top', 'right', 'bottom', 'left']
            }
          },
          anchor: 'center',
          connectionPoint: 'anchor',
          validateConnection ({ targetMagnet }) {
            return !!targetMagnet
          }
        },
        highlighting: {
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

      this.initPlugins()
      this.initPorts()
      this.registerNodes()
      this.bindKeyboard()
      this.bindGraphEvents()
    },

    /**
     * 初始化插件
     */
    initPlugins () {
      this.dnd = new Dnd({
        target: this.graph,
        scaled: false
      })

      this.graph
        .use(new Selection({ rubberbox: true, pointerEvents: 'none' }))
        .use(new Snapline())
        .use(new Keyboard())
        .use(new Clipboard())
        .use(new History())
        .use(new MiniMap({ container: document.getElementById('minimap') }))
    },

    /**
     * 初始化连接桩配置
     */
    initPorts () {
      const portGroups = {}
      PORT_GROUPS.forEach(group => {
        portGroups[group] = {
          position: group,
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 2,
              fill: '#fff',
              style: { visibility: 'hidden' }
            }
          }
        }
      })

      this.portConfig = {
        groups: portGroups,
        items: PORT_GROUPS.map(group => ({ group }))
      }
    },

    /**
     * 注册自定义节点
     */
    registerNodes () {
      const nodeConfigs = [
        { name: NODE_SHAPE.SQL, icon: NODE_ICONS.SQL },
        { name: NODE_SHAPE.LLM, icon: NODE_ICONS.LLM },
        { name: NODE_SHAPE.START, icon: NODE_ICONS.START },
        { name: NODE_SHAPE.END, icon: NODE_ICONS.END },
        { name: NODE_SHAPE.DYNAMIC, icon: NODE_ICONS.DYNAMIC }
      ]

      nodeConfigs.forEach(config => {
        Graph.registerNode(config.name, {
          inherit: 'rect',
          ports: { ...this.portConfig },
          width: 100,
          height: 40,
          markup: [
            { tagName: 'rect', selector: 'body' },
            { tagName: 'image', selector: 'img' }
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
              'xlink:href': config.icon,
              width: 45,
              height: 45,
              x: 6,
              y: 6
            }
          }
        }, true)
      })
    },

    /**
     * 绑定快捷键
     */
    bindKeyboard () {
      const shortcuts = KEYBOARD_SHORTCUTS

      // 复制
      this.graph.bindKey(shortcuts.COPY, () => {
        const cells = this.graph.getSelectedCells()
        if (cells.length) {
          this.graph.copy(cells)
        }
        return false
      })

      // 剪切
      this.graph.bindKey(shortcuts.CUT, () => {
        const cells = this.graph.getSelectedCells()
        if (cells.length) {
          this.graph.cut(cells)
        }
        return false
      })

      // 粘贴
      this.graph.bindKey(shortcuts.PASTE, () => {
        if (!this.graph.isClipboardEmpty()) {
          const cells = this.graph.paste({ offset: 32 })
          this.graph.cleanSelection()
          this.graph.select(cells)
        }
        return false
      })

      // 撤销
      this.graph.bindKey(shortcuts.UNDO, () => {
        if (this.graph.canUndo()) {
          this.graph.undo()
        }
        return false
      })

      // 重做
      this.graph.bindKey(shortcuts.REDO, () => {
        if (this.graph.canRedo()) {
          this.graph.redo()
        }
        return false
      })

      // 全选
      this.graph.bindKey(shortcuts.SELECT_ALL, () => {
        const nodes = this.graph.getNodes()
        if (nodes) {
          this.graph.select(nodes)
        }
      })

      // 删除
      this.graph.bindKey(shortcuts.DELETE, () => {
        const cells = this.graph.getSelectedCells()
        if (cells.length) {
          this.graph.removeCells(cells)
        }
      })

      // 放大
      this.graph.bindKey(shortcuts.ZOOM_IN, () => {
        const zoom = this.graph.zoom()
        if (zoom < GRAPH_CONFIG.ZOOM_THRESHOLD.IN) {
          this.graph.zoom(GRAPH_CONFIG.ZOOM_STEP)
        }
      })

      // 缩小
      this.graph.bindKey(shortcuts.ZOOM_OUT, () => {
        const zoom = this.graph.zoom()
        if (zoom > GRAPH_CONFIG.ZOOM_THRESHOLD.OUT) {
          this.graph.zoom(-GRAPH_CONFIG.ZOOM_STEP)
        }
      })
    },

    /**
     * 绑定画布事件
     */
    bindGraphEvents () {
      // 鼠标进入节点
      this.graph.on('node:mouseenter', ({ cell }) => {
        const container = document.getElementById('graph-container')
        const ports = container.querySelectorAll('.x6-port-body')
        this.showPorts(ports, !cell.attrs.typeName)
        if (cell.isNode()) {
          cell.addTools([{ name: 'button-remove', args: { x: 0, y: 0, offset: { x: 10, y: 10 } } }])
        } else {
          cell.addTools([{ name: 'button-remove', args: { distance: -40 } }])
        }
      })

      // 鼠标离开节点
      this.graph.on('node:mouseleave', ({ cell }) => {
        if (cell.hasTool('button-remove')) {
          cell.removeTool('button-remove')
        }
      })

      // 鼠标进入边
      this.graph.on('edge:mouseenter', ({ cell }) => {
        if (cell.isEdge()) {
          cell.addTools([{ name: 'button-remove', args: { distance: -40 } }])
        }
      })

      // 鼠标离开边
      this.graph.on('edge:mouseleave', ({ cell }) => {
        if (cell.hasTool('button-remove')) {
          cell.removeTool('button-remove')
        }
      })

      // 节点点击
      this.graph.on('node:click', ({ node, cell }) => {
        this.currentNodeId = node.id
        const nodeShape = cell.shape
        this.openDrawerByNodeType(nodeShape, cell)
      })

      // 元素被选中（框选或点击）
      this.graph.on('cell:selected', ({ cell }) => {
        if (cell.isNode()) {
          cell.addTools([{ name: 'button-remove', args: { x: 0, y: 0, offset: { x: 10, y: 10 } } }])
        } else if (cell.isEdge()) {
          cell.addTools([{ name: 'button-remove', args: { distance: -40 } }])
        }
      })

      // 元素取消选中
      this.graph.on('cell:unselected', ({ cell }) => {
        if (cell.hasTool('button-remove')) {
          cell.removeTool('button-remove')
        }
      })

      // 空白点击
      this.graph.on('blank:click', () => {
        if (this.currentCell) {
          this.currentCell.removeTool('button')
          this.currentCell.removeTool('button-move')
        }
      })

      // 节点被删除
      this.graph.on('cell:removed', ({ cell }) => {
        if (cell.isNode()) {
          // 如果删除的是当前编辑的节点，清空相关配置
          if (cell.id === this.currentNodeId) {
            this.currentNodeId = ''
            this.resetFormState()
          }

          // 如果删除的是动态编译节点，清理其输出字段
          if (cell.shape === NODE_SHAPE.DYNAMIC) {
            const outputFields = cell.getData()?.outputFields || []
            outputFields.forEach(field => {
              const index = this.toDsSourceFields.indexOf(field.name)
              if (index > -1) {
                this.toDsSourceFields.splice(index, 1)
              }
            })
          }
        }
      })
    },

    /**
     * 显示/隐藏连接桩
     */
    showPorts (ports, show) {
      for (let i = 0, len = ports.length; i < len; i++) {
        ports[i].style.visibility = show ? 'visible' : 'hidden'
      }
    },

    // ========== 数据加载 ==========

    /**
     * 加载数据源列表
     */
    loadDataSourceList () {
      this.selectloading = true
      listQuery().then(res => {
        this.selectloading = false
        const records = res.result || []

        this.fromDsList = records
          .filter(item => !EXCLUDE_FROM_DS.includes(item.type))
          .map(item => ({
            dsId: item.dsId,
            name: item.name,
            type: item.type
          }))

        this.toDsList = records
          .filter(item => !EXCLUDE_TO_DS.includes(item.type))
          .map(item => ({
            dsId: item.dsId,
            name: item.name,
            type: item.type
          }))
      })
    },

    // ========== 来源数据源相关 ==========

    /**
     * 数据源变更
     */
    handleFromChange (dsId) {
      this.sourceConfig.dsId = dsId
      this.sourceConfig.tableName = ''
      this.sourceConfig.mappings = []
      this.sourceFields = []

      this.selectloading = true
      fetchTables(dsId).then(res => {
        this.selectloading = false
        this.sourceTables = res.result || []
      })
    },

    /**
     * 数据表变更
     */
    handleFromTbChange (tableName) {
      const ds = this.fromDsList.find(item => item.dsId === this.sourceConfig.dsId)
      this.sourceConfig.dsName = ds?.catalog || ''
      this.sourceConfig.tableName = tableName
      this.selectedSourceTable = tableName
      this.sqlConfig.sqlValue = ''
      this.sqlConfig.whereValue = ''
      this.sqlConfig.groupValue = ''
      this.sqlConfig.tags = []

      this.selectloading = true
      getDsTbFieldsInfo({
        ds_id: this.sourceConfig.dsId,
        name: tableName
      }).then(res => {
        this.selectloading = false
        this.sourceFields = res.result || []
      })
    },

    /**
     * 同步模式变更
     */
    handleSyncModeChange ({ mode, isIncrement }) {
      this.syncConfig.mode = mode
      this.syncConfig.isIncrement = isIncrement
      if (!isIncrement) {
        this.syncConfig.incrementField = ''
      }
    },

    /**
     * 数据覆盖变更
     */
    changeCover (checked) {
      this.syncConfig.cover = checked ? 1 : 0
    },

    /**
     * 增量字段变更
     */
    handleIncrementFieldChange (value) {
      this.syncConfig.incrementField = value
    },

    /**
     * 来源映射变更
     */
    handleSourceMappingChange (index) {
      // 映射变更处理
    },

    /**
     * 添加映射
     */
    addMapping () {
      this.sourceConfig.mappings.push({ sourceField: '' })
    },

    /**
     * 删除映射
     */
    removeMapping (index) {
      const removeField = this.sourceConfig.mappings[index]?.sourceField
      const fieldIndex = this.toDsSourceFields.indexOf(removeField)
      if (fieldIndex > -1) {
        this.toDsSourceFields.splice(fieldIndex, 1)
      }

      // 同步删除目标映射中的关联项
      const targetIndex = this.targetMappings.findIndex(
        m => m.sourceField === removeField
      )
      if (targetIndex > -1) {
        this.targetMappings.splice(targetIndex, 1)
      }

      this.sourceConfig.mappings.splice(index, 1)
    },

    // ========== SQL算子相关 ==========

    /**
     * SQL内容变更
     */
    handleSqlChange (value) {
      this.sqlConfig.sqlValue = value
    },

    /**
     * SQL算子校验成功，保存输出字段
     */
    handleSqlValidateSuccess (outputFields) {
      // 保存SQL算子的输出字段
      this.sqlConfig.outputFields = outputFields
      // 将字段添加到toDsSourceFields供目标数据源使用
      outputFields.forEach(field => {
        if (!this.toDsSourceFields.includes(field.name)) {
          this.toDsSourceFields.push(field.name)
        }
      })
    },

    /**
     * 添加标签
     */
    handleTagAdd (tag) {
      if (!this.sqlConfig.tags.includes(tag)) {
        this.sqlConfig.tags.push(tag)
      }
      if (!this.sourceFields.find(f => f.name === tag)) {
        this.sourceFields.push({ name: tag })
      }
      if (!this.toDsSourceFields.includes(tag)) {
        this.toDsSourceFields.push(tag)
      }
      if (!this.targetMappings.find(m => m.sourceField === tag)) {
        this.targetMappings.push({ sourceField: tag, targetField: '' })
      }
    },

    // ========== LLM算子相关 ==========

    /**
     * 模型提供商变更
     */
    handleModelProviderChange (value) {
      this.llmConfig.modelProvider = value
    },

    /**
     * 模型名称变更
     */
    handleModelChange (value) {
      this.llmConfig.model = value
    },

    /**
     * API Key变更
     */
    handleApiKeyChange (value) {
      this.llmConfig.apiKey = value
    },

    /**
     * API Path变更
     */
    handleApiPathChange (value) {
      this.llmConfig.apiPath = value
    },

    /**
     * Prompt变更
     */
    handlePromptChange (value) {
      this.llmConfig.prompt = value
    },

    // ========== 动态编译算子相关 ==========

    handleSourceCodeChange (value) {
      this.dynamicConfig.sourceCode = value
    },

    handleOutputFieldsChange (fields) {
      this.dynamicConfig.outputFields = fields
    },

    /**
     * 语法校验成功，自动添加解析的字段到输出字段列表
     */
    handleValidateSuccess (outputFields) {
      // 将解析的字段添加到输出字段列表
      outputFields.forEach(field => {
        // 检查字段是否已存在
        const exists = this.dynamicConfig.outputFields.some(f => f.name === field.name)
        if (!exists) {
          this.dynamicConfig.outputFields.push({
            name: field.name,
            type: field.type
          })
        }
      })
      // 触发字段变更事件，更新节点数据
      this.saveNodeData()
    },

    // ========== 目标数据源相关 ==========

    /**
     * 任务名称变更
     */
    handleJobNameChange (value) {
      this.jobInfo.name = value
    },

    /**
     * 定时配置变更
     */
    handleSchedulerChange (value) {
      this.jobInfo.schedulerConf = value
    },

    /**
     * 目标数据源变更
     */
    handleToChange (dsId) {
      this.targetConfig.dsId = dsId
      this.targetConfig.tableName = ''
      this.targetFields = []

      this.selectloading = true
      fetchTables(dsId).then(res => {
        this.selectloading = false
        this.targetTables = res.result || []
      })
    },

    /**
     * 目标表变更
     */
    handleToTbChange (tableName) {
      this.targetConfig.tableName = tableName
      this.selectloading = true
      getDsTbFieldsInfo({
        ds_id: this.targetConfig.dsId,
        name: tableName
      }).then(res => {
        this.selectloading = false
        this.targetFields = res.result || []
      })
    },

    handleToSourceFieldChange (index) {
      // 来源字段变更处理
    },

    handleToTargetFieldChange (index) {
      // 目标字段变更处理
    },

    /**
     * 添加目标映射
     */
    addTargetMapping () {
      this.targetMappings.push({ sourceField: '', targetField: '' })
    },

    /**
     * 删除目标映射
     */
    removeTargetMapping (index) {
      this.targetMappings.splice(index, 1)
    },

    // ========== 抽屉控制 ==========

    /**
     * 根据节点类型打开对应抽屉
     */
    openDrawerByNodeType (nodeShape, cell) {
      const drawerMap = {
        [NODE_SHAPE.START]: 'source',
        [NODE_SHAPE.SQL]: 'sql',
        [NODE_SHAPE.LLM]: 'llm',
        [NODE_SHAPE.END]: 'target',
        [NODE_SHAPE.DYNAMIC]: 'dynamic'
      }

      const drawerKey = drawerMap[nodeShape]
      if (drawerKey) {
        this.drawerVisible[drawerKey] = true
      }

      // 加载节点数据
      const nodeData = cell.getData() || {}
      if (nodeShape === NODE_SHAPE.DYNAMIC) {
        this.dynamicConfig.sourceCode = nodeData.sourceCode || ''
      }

      // 目标节点特殊处理
      if (nodeShape === NODE_SHAPE.END) {
        this.syncToDsSourceFields()
      }
    },

    /**
     * 同步可用来源字段到目标
     */
    syncToDsSourceFields () {
      // 从来源映射中收集字段
      this.sourceConfig.mappings.forEach(mapping => {
        if (mapping.sourceField && !this.toDsSourceFields.includes(mapping.sourceField)) {
          this.toDsSourceFields.push(mapping.sourceField)
        }
      })

      // 检查是否存在LLM节点
      const hasLlm = this.graph.getNodes().some(node => node.shape === NODE_SHAPE.LLM)
      if (hasLlm) {
        if (!this.toDsSourceFields.includes('llm_output')) {
          this.toDsSourceFields.push('llm_output')
        }
      } else {
        const index = this.toDsSourceFields.indexOf('llm_output')
        if (index > -1) {
          this.toDsSourceFields.splice(index, 1)
        }
      }

      // 收集SQL算子的输出字段
      const sqlNodes = this.graph.getNodes().filter(node => node.shape === NODE_SHAPE.SQL)
      const sqlOutputFields = []
      sqlNodes.forEach(node => {
        const outputFields = node.getData()?.outputFields || []
        outputFields.forEach(field => {
          if (!sqlOutputFields.includes(field.name)) {
            sqlOutputFields.push(field.name)
          }
        })
      })

      // 添加SQL算子的输出字段
      sqlOutputFields.forEach(fieldName => {
        if (!this.toDsSourceFields.includes(fieldName)) {
          this.toDsSourceFields.push(fieldName)
        }
      })

      // 收集动态编译算子的输出字段
      const dynamicNodes = this.graph.getNodes().filter(node => node.shape === NODE_SHAPE.DYNAMIC)
      const dynamicOutputFields = []
      dynamicNodes.forEach(node => {
        const outputFields = node.getData()?.outputFields || []
        outputFields.forEach(field => {
          if (!dynamicOutputFields.includes(field.name)) {
            dynamicOutputFields.push(field.name)
          }
        })
      })

      // 添加动态编译算子的输出字段
      dynamicOutputFields.forEach(fieldName => {
        if (!this.toDsSourceFields.includes(fieldName)) {
          this.toDsSourceFields.push(fieldName)
        }
      })

      // 移除已不存在的字段
      this.toDsSourceFields = this.toDsSourceFields.filter(field => {
        return this.sourceConfig.mappings.some(m => m.sourceField === field) ||
               field === 'llm_output' ||
               sqlOutputFields.includes(field) ||
               dynamicOutputFields.includes(field)
      })
    },

    /**
     * 抽屉关闭
     */
    handleDrawerClose (type) {
      this.drawerVisible[type] = false
      this.saveNodeData()
    },

    /**
     * 保存节点数据到图
     */
    saveNodeData () {
      const node = this.graph.getNodes().find(n => n.id === this.currentNodeId)
      if (!node) return

      const nodeData = { id: this.currentNodeId }
      const shape = node.shape

      if (shape === NODE_SHAPE.SQL) {
        Object.assign(nodeData, {
          sqlOperatorValue: this.sqlConfig.sqlValue,
          sqlOperatorFrom: this.selectedSourceTable,
          tags: this.sqlConfig.tags,
          outputFields: [...this.sqlConfig.outputFields]
        })

        // 同步tags到mappings
        this.sqlConfig.tags.forEach(tag => {
          if (!this.sourceConfig.mappings.find(m => m.sourceField === tag)) {
            this.sourceConfig.mappings.push({ sourceField: tag })
          }
        })
      }

      if (shape === NODE_SHAPE.LLM) {
        nodeData.modelProvider = this.llmConfig.modelProvider
        nodeData.model = this.llmConfig.model
        nodeData.apiKey = this.llmConfig.apiKey
        nodeData.apiPath = this.llmConfig.apiPath
        nodeData.prompt = this.llmConfig.prompt
      }

      if (shape === NODE_SHAPE.DYNAMIC) {
        nodeData.sourceCode = this.dynamicConfig.sourceCode
        nodeData.outputFields = [...this.dynamicConfig.outputFields]
      }

      if (shape === NODE_SHAPE.START) {
        nodeData.dsId = this.sourceConfig.dsId
        nodeData.mappings = [...this.sourceConfig.mappings]
      }

      node.setData(nodeData)
    },

    /**
     * 抽屉显示状态变更
     */
    afterVisibleChange (val) {
      // 可用于日志记录
    },

    // ========== 拖拽相关 ==========

    /**
     * 获取工具图标
     */
    getToolIcon (iconClass) {
      return require(`@/assets/icons/${iconClass}.png`)
    },

    /**
     * 开始拖拽节点
     */
    startDrag (type, event) {
      this.startDragToGraph(this.graph, type, event)
    },

    /**
     * 执行拖拽到画布
     */
    startDragToGraph (graph, type, event) {
      const shapeMap = {
        start: NODE_SHAPE.START,
        end: NODE_SHAPE.END,
        rect: NODE_SHAPE.SQL,
        polygon: NODE_SHAPE.LLM,
        dynamic: NODE_SHAPE.DYNAMIC
      }

      const nodeShape = shapeMap[type] || NODE_SHAPE.START

      const dragNode = this.graph.createNode({
        shape: nodeShape,
        width: GRAPH_CONFIG.NODE_WIDTH,
        height: GRAPH_CONFIG.NODE_HEIGHT,
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

      this.dnd.start(dragNode, event)
    },

    // ========== 工具栏 ==========

    /**
     * 处理工具栏点击
     */
    handleToolTrigger (command) {
      const handlers = {
        save: this.handleSave,
        onUndo: () => this.graph.undo(),
        onRedo: () => this.graph.redo(),
        zoomIn: () => this.graph.zoom(GRAPH_CONFIG.ZOOM_STEP * 2),
        zoomOut: () => this.graph.zoom(-GRAPH_CONFIG.ZOOM_STEP * 2),
        centerContent: () => this.graph.centerContent(),
        view: this.exportJson,
        select: () => this.changeRubberband('select'),
        move: () => this.changePann('move'),
        exit: this.handleExit
      }

      const handler = handlers[command]
      if (handler) {
        handler()
      }
    },

    /**
     * 切换框选
     */
    changeRubberband (key) {
      this.tools.forEach(item => {
        if (item.key === key) {
          item.status = !item.status
          item.title = item.status ? '关闭框选' : '开启框选'
          this.graph.toggleRubberband(item.status)
          this.graph.toggleStrictRubberband(item.status)
          this.graph.cleanSelection(item.status)
        }
      })
    },

    /**
     * 切换画布平移
     */
    changePann (key) {
      this.tools.forEach(item => {
        if (item.key === key) {
          item.status = !item.status
          item.title = item.status ? '关闭平移' : '开启平移'
          this.graph.togglePanning(item.status)
        }
      })
    },

    // ========== 保存与加载 ==========

    /**
     * 保存任务
     */
    handleSave () {
      this.selectloading = true
      const graphData = this.graph.toJSON()

      const formData = {
        job_id: this.jobInfo.id,
        from_ds_id: this.sourceConfig.dsId,
        to_ds_id: this.targetConfig.dsId,
        from_tb_name: this.sourceConfig.tableName,
        to_tb_name: this.targetConfig.tableName,
        job_name: this.jobInfo.name,
        scheduler_conf: this.jobInfo.schedulerConf,
        field_mappings: this.targetMappings,
        graph: JSON.stringify(graphData),
        cover: this.syncConfig.cover,
        sync_mode: {
          mode: this.syncConfig.mode,
          increate_field: this.syncConfig.incrementField
        },
        type: 2,
        run: false
      }

      const apiMethod = this.jobInfo.id ? modifyObj : addObj

      apiMethod(formData).then(res => {
        this.selectloading = false
        if (res.status === '0') {
          this.$message.success(this.jobInfo.id ? '修改成功' : '新增成功')
          this.closeDraw()
        } else {
          this.$message.error(res.errstr)
        }
      }).catch(err => {
        this.selectloading = false
        const errData = err.response?.data || {}
        this.$message.error(errData.errstr || '操作失败')
      })
    },

    /**
     * 预览
     */
    exportJson () {
      const data = this.graph.toJSON()
      this.$store.dispatch('g6/setG6data', data)
    },

    /**
     * 退出
     */
    handleExit () {
      this.closeDraw()
      this.$router.push('/compute/job')
    },

    /**
     * 编辑任务
     */
    edit (jobId) {
      this.selectloading = true
      this.jobInfo.id = jobId

      getObj(jobId).then(res => {
        this.selectloading = false
        const record = res.result

        // 基本信息
        this.targetConfig.dsId = record.to_ds_id
        this.sourceConfig.dsId = record.from_ds_id
        this.sourceConfig.tableName = record.from_tb_name
        this.targetConfig.tableName = record.to_tb_name
        this.jobInfo.name = record.job_name
        this.jobInfo.schedulerConf = record.scheduler_conf
        this.targetMappings = record.field_mappings || []
        this.syncConfig.mode = record.sync_mode?.mode || 'overwrite'
        this.syncConfig.cover = record.cover || 0
        this.syncConfig.incrementField = record.sync_mode?.increate_field || ''
        this.syncConfig.isIncrement = this.syncConfig.mode === 'increment'

        // 加载表格数据
        fetchTables(this.targetConfig.dsId).then(res => {
          this.targetTables = res.result || []
        })
        fetchTables(this.sourceConfig.dsId).then(res => {
          this.sourceTables = res.result || []
        })

        this.handleFromTbChange(this.sourceConfig.tableName)
        this.handleToTbChange(this.targetConfig.tableName)

        // 加载图数据
        const graphData = JSON.parse(record.graph)
        this.graph.fromJSON(graphData)

        // 恢复节点数据
        graphData.cells.forEach(cell => {
          if (cell.shape === NODE_SHAPE.SQL) {
            this.sqlConfig.sqlValue = cell.data?.sqlOperatorValue || ''
            this.sqlConfig.whereValue = cell.data?.sqlOperatorWhereValue || ''
            this.sqlConfig.groupValue = cell.data?.sqlOperatorGroupValue || ''
            this.sqlConfig.tags = cell.data?.tags || []
            this.sqlConfig.outputFields = cell.data?.outputFields || []
          }
          if (cell.shape === NODE_SHAPE.LLM) {
            this.llmConfig.modelProvider = cell.data?.modelProvider || ''
            this.llmConfig.model = cell.data?.model || ''
            this.llmConfig.apiKey = cell.data?.apiKey || ''
            this.llmConfig.apiPath = cell.data?.apiPath || ''
            this.llmConfig.prompt = cell.data?.prompt || ''
          }
          if (cell.shape === NODE_SHAPE.DYNAMIC) {
            this.dynamicConfig.sourceCode = cell.data?.sourceCode || ''
            this.dynamicConfig.outputFields = cell.data?.outputFields || []
          }
          if (cell.shape === NODE_SHAPE.START) {
            this.sourceConfig.dsId = cell.data?.dsId || ''
            this.sourceConfig.mappings = cell.data?.mappings || []
          }
        })
      })
    },

    // ========== 工具方法 ==========

    /**
     * 重置表单状态
     */
    resetFormState () {
      this.sqlConfig.sqlValue = ''
      this.sqlConfig.whereValue = ''
      this.sqlConfig.groupValue = ''
      this.sqlConfig.tags = []
      this.sqlConfig.outputFields = []
      this.llmConfig.modelProvider = ''
      this.llmConfig.model = ''
      this.llmConfig.apiKey = ''
      this.llmConfig.apiPath = ''
      this.llmConfig.prompt = ''
      this.dynamicConfig.sourceCode = ''
      this.dynamicConfig.outputFields = []
      this.sourceConfig.mappings = []
    },

    /**
     * 销毁图实例
     */
    disposeGraph () {
      if (this.graph) {
        this.graph.dispose()
        this.graph = null
      }
      if (this.dnd) {
        this.dnd.dispose()
        this.dnd = null
      }
    }
  }
}
</script>

<style lang="less" scoped>
.g6-wrap {
  width: 100%;
  height: 100vh;
  position: relative;

  .top-box {
    height: 40px;
    z-index: 999;

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
      border: 0.5px solid #000;
      width: 100%;
      border-radius: 10px;
      margin: 5px 0;
      display: flex;
      align-items: center;
      padding: 5px;
      gap: 10px;
    }
  }

  #graph-container {
    width: calc(100% - 220px);
    border-right: 2px solid #ccc;
  }
}

.el-drawer__header {
  margin-bottom: 0;
  font-weight: bold;
  font-size: 20px;
  color: #000;
}
</style>

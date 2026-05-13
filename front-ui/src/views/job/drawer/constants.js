/**
 * JobCompute 页面常量配置
 */

// 禁止作为来源的数据源类型
export const EXCLUDE_FROM_DS = ['es', 'oracle', 'redis', 'kafka', 'custom', 'mysqlcdc']

// 禁止作为目标的数据源类型
export const EXCLUDE_TO_DS = ['oracle', 'kafka', 'mysqlcdc', 'redis', 'custom', 'es']

// 节点类型
export const NODE_SHAPE = {
  START: 'custom-start-node',
  END: 'custom-end-node',
  SQL: 'SQL',
  LLM: 'LLM',
  DYNAMIC: 'DynamicCompile'
}

// 节点类型对应抽屉
export const NODE_DRAWER_MAP = {
  [NODE_SHAPE.START]: 'visible',
  [NODE_SHAPE.SQL]: 'sqlVisible',
  [NODE_SHAPE.LLM]: 'llmVisible',
  [NODE_SHAPE.END]: 'toDsVisible',
  [NODE_SHAPE.DYNAMIC]: 'dynamicVisible'
}

// 同步模式
export const SYNC_MODE = {
  OVERWRITE: 'overwrite',
  INCREMENT: 'increment'
}

// 节点默认尺寸
export const NODE_SIZE = {
  WIDTH: 100,
  HEIGHT: 40
}

// 节点默认样式
export const NODE_DEFAULT_STYLE = {
  stroke: '#8f8f8f',
  strokeWidth: 1,
  fill: '#fff',
  rx: 6,
  ry: 6
}

// 连接桩位置
export const PORT_GROUPS = ['left', 'right']

// 画布配置
export const GRAPH_CONFIG = {
  NODE_WIDTH: 60,
  NODE_HEIGHT: 60,
  MAX_SCALE: 4,
  MIN_SCALE: 0.2,
  ZOOM_STEP: 0.1,
  ZOOM_THRESHOLD: {
    IN: 1.5,
    OUT: 0.5
  }
}

// 工具栏配置
export const TOOLS_CONFIG = [
  { title: '保存', iconClass: 'save', key: 'save' },
  { title: '撤销', iconClass: 'left', key: 'onUndo' },
  { title: '前进', iconClass: 'right', key: 'onRedo' },
  { title: '放大', iconClass: 'zoomIn', key: 'zoomIn' },
  { title: '缩小', iconClass: 'zoomOut', key: 'zoomOut' },
  { title: '居中', iconClass: 'center', key: 'centerContent' },
  { title: '预览', iconClass: 'view', key: 'view' },
  { title: '开启框选', iconClass: 'select', key: 'select', status: false },
  { title: '开启平移', iconClass: 'move', key: 'move', status: false },
  { title: '退出', iconClass: 'exit', key: 'exit' }
]

// 快捷键配置
export const KEYBOARD_SHORTCUTS = {
  COPY: ['meta+c', 'ctrl+c'],
  CUT: ['meta+x', 'ctrl+x'],
  PASTE: ['meta+v', 'ctrl+v'],
  UNDO: ['meta+z', 'ctrl+z'],
  REDO: ['meta+shift+z', 'ctrl+shift+z'],
  SELECT_ALL: ['meta+a', 'ctrl+a'],
  DELETE: 'backspace',
  ZOOM_IN: ['ctrl+1', 'meta+1'],
  ZOOM_OUT: ['ctrl+2', 'meta+2']
}

// 抽屉宽度配置
export const DRAWER_WIDTH = {
  SOURCE: 700,
  SQL: 500,
  LLM: 500,
  TARGET: 500,
  DYNAMIC: 500
}

// 节点图标 SVG Base64
export const NODE_ICONS = {
  START: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA0IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgOWgtMnY0aC0ydi00aC0ybDMtM3oiLz48L3N2Zz4=',
  END: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIwIDEzLjA5VjdjMC0yLjIxLTMuNTgtNC04LTRTNCA0Ljc5IDQgN3YxMGMwIDIuMjEgMy41OSA0IDggNGMuNDYgMCAuOSAwIDEuMzMtLjA2QTYgNiAwIDAgMSAxMyAxOXYtLjA1Yy0uMzIuMDUtLjY1LjA1LTEgLjA1Yy0zLjg3IDAtNi0xLjUtNi0ydi0yLjIzYzEuNjEuNzggMy43MiAxLjIzIDYgMS4yM2MuNjUgMCAxLjI3LS4wNCAxLjg4LS4xMUE1Ljk5IDUuOTkgMCAwIDEgMTkgMTNjLjM0IDAgLjY3LjA4IDEgLjA5bS0yLS42NGMtMS4zLjk1LTMuNTggMS41NS02IDEuNTVzLTQuNy0uNi02LTEuNTVWOS42NGMxLjQ3LjgzIDMuNjEgMS4zNiA2IDEuMzZzNC41My0uNTMgNi0xLjM2ek0xMiA5QzguMTMgOSA2IDcuNSA2IDdzMi4xMy0yIDYtMnM2IDEuNSA2IDJzLTIuMTMgMi02IDJtMTAgMTFsLTMgM2wtMy0zaDJ2LTRoMnY0eiIvPjwvc3ZnPg==',
  SQL: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9Im5vbmUiIHN0cm9rZT0iY3VycmVudENvbG9yIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIHN0cm9rZS13aWR0aD0iMiIgZD0iTTEyIDhhMiAyIDAgMCAxIDIgMnY0YTIgMiAwIDEgMS00IDB2LTRhMiAyIDAgMCAxIDItMm01IDB2OGg0bS04LTFsMSAxTTMgMTVhMSAxIDAgMCAwIDEgMWgyYTEgMSAwIDAgMCAxLTF2LTJhMSAxIDAgMCAwLTEtMUg0YTEgMSAwIDAgMS0xLTFWOWExIDEgMCAwIDEgMS0xaDJhMSAxIDAgMCAxIDEgMSIvPjwvc3ZnPg==',
  LLM: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0iTTIyLjI4MiA5LjgyMWE2IDYgMCAwIDAtLjUxNi00LjkxYTYuMDUgNi4wNSAwIDAgMC02LjUxLTIuOUE2LjA2NSA2LjA2NSAwIDAgMCA0Ljk4MSA0LjE4YTYgNiAwIDAgMC0zLjk5OCAyLjlhNi4wNSA2LjA1IDAgMCAwIC43NDMgNy4wOTdhNS45OCA1Ljk4IDAgMCAwIC41MSA0LjkxMWE2LjA1IDYuMDUgMCAwIDAgNi41MTUgMi45QTYgNiAwIDAgMCAxMy4yNiAyNGE2LjA2IDYuMDYgMCAwIDAgNS43NzItNC4yMDZhNiA2IDAgMCAwIDMuOTk3LTIuOWE2LjA2IDYuMDYgMCAwIDAtLjc0Ny03LjA3M00xMy4yNiAyMi40M2E0LjQ4IDQuNDggMCAwIDEtMi44NzYtMS4wNGwuMTQxLS4wODFsNC43NzktMi43NThhLjguOCAwIDAgMCAuMzkyLS42ODF2LTYuNzM3bDIuMDIgMS4xNjhhLjA3LjA3IDAgMCAxIC4wMzguMDUydjUuNTgzYTQuNTA0IDQuNTA0IDAgMCAxLTQuNDk0IDQuNDk0TTMuNiAxOC4zMDRhNC40NyA0LjQ3IDAgMCAxLS41MzUtMy4wMTRsLjE0Mi4wODVsNC43ODMgMi43NTlhLjc3Ljc3IDAgMCAwIC43OCAwbDUuODQzLTMuMzY5djIuMzMyYS4wOC4wOCAwIDAgMS0uMDMzLjA2Mkw5Ljc0IDE5Ljk1YTQuNSA0LjUgMCAwIDEtNi4xNC0xLjY0Nk0yLjM0IDcuODk2YTQuNSA0LjUgMCAwIDEgMi4zNjYtMS45NzNWMTEuNmEuNzcuNzcgMCAwIDAgLjM4OC42NzlsNS44MTUgMy4zNTRsLTIuMDIgMS4xNjhhLjA4LjA4IDAgMCAxLS4wNzEgMGwtNC44My0yLjc4NkE0LjUwNCA0LjUwNCAwIDAgMSAyLjM0IDcuODcyem0xNi41OTcgMy44NTVsLTUuODMzLTMuMzg3TDE1LjExOSA3LjJhLjA4LjA4IDAgMCAxIC4wNzEgMGw0LjgzIDIuNzkxYTQuNDk0IDQuNDk0IDAgMCAxLS42NzYgOC4xMDV2LTUuNjc4YS43OS43OSAwIDAgMC0uNDA3LS42NjdtMi4wMS0zLjAyM2wtLjE0MS0uMDg1bC00Ljc3NC0yLjc4MmEuNzguNzggMCAwIDAtLjc4NSAwTDkuNDA5IDkuMjNWNi44OTdhLjA3LjA3IDAgMCAxIC4wMjgtLjA2MWw0LjgzLTIuNzg3YTQuNSA0LjUgMCAwIDEgNi42OCA0LjY2em0tMTIuNjQgNC4xMzVsLTIuMDItMS4xNjRhLjA4LjA4IDAgMCAxLS4wMzgtLjA1N1Y2LjA3NWE0LjUgNC41IDAgMCAxIDcuMzc1LTMuNDUzbC0uMTQyLjA4TDguNzA0IDUuNDZhLjguOCAwIDAgMC0uMzkzLjY4MXptMS4wOTctMi4zNjVsMi42MDItMS41bDIuNjA3IDEuNXYyLjk5OWwtMi41OTcgMS41bC0yLjYwNy0xLjVaIi8+PC9zdmc+',
  DYNAMIC: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjgiIGhlaWdodD0iMTI4IiB2aWV3Qm94PSIwIDAgMjQgMjQiPjxwYXRoIGZpbGw9ImN1cnJlbnRDb2xvciIgZD0ibTguODI1IDEybDEuNDc1LTEuNDc1cS4zLS4zLjMtLjd0LS4zLS43dC0uNzEyLS4zdC0uNzEzLjNMNi43IDExLjNxLS4xNS4xNS0uMjEzLjMyNVQ2LjQyNSAxMnQuMDYyLjM3NXQuMjEzLjMyNWwyLjE3NSAyLjE3NXEuMy4zLjcxMy4zdC43MTItLjN0LjMtLjd0LS4zLS43em02LjM1IDBMMTMuNyAxMy40NzVxLS4zLjMtLjMuN3QuMy43dC43MTMuM3QuNzEyLS4zTDE3LjMgMTIuN3EuMTUtLjE1LjIxMy0uMzI1dC4wNjItLjM3NXQtLjA2Mi0uMzc1dC0uMjEzLS4zMjVsLTIuMTc1LTIuMTc1cS0uMTUtLjE1LS4zMzctLjIyNXQtLjM3Ni0uMDc1dC0uMzc1LjA3NXQtLjMzNy4yMjVxLS4zLjMtLjMuN3QuMy43ek01IDIxcS0uODI1IDAtMS40MTItLjU4N1QzIDE5VjVxMC0uODI1LjU4OC0xLjQxMlQ1IDNoMTRxLjgyNSAwIDEuNDEzLjU4OFQyMSA1djE0cTAgLjgyNS0uNTg3IDEuNDEzVDE5IDIxem0wLTJoMTRWNUg1ek01IDV2MTR6Ii8+PC9zdmc+'
}

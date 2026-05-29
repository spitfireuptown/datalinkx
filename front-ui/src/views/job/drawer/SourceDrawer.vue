<template>
  <a-drawer
    title="来源数据源"
    placement="right"
    :closable="false"
    :visible="visible"
    :after-visible-change="afterVisibleChange"
    @close="onClose"
    width="700"
  >
    <LoadingDx v-if="loading" size="'size-1x'" />

    <div class="select-container">
      来源数据源
      <a-select
        class="input-full-width"
        :value="source.dsId"
        @change="handleDsChange"
      >
        <a-select-option v-for="ds in dsList" :key="ds.dsId" :value="ds.dsId">
          <div class="ds-option">
            <span class="ds-icon">
              <img :src="dsImgObj[ds.type]" :alt="ds.name">
            </span>
            <span>{{ ds.name }}</span>
          </div>
        </a-select-option>
      </a-select>
    </div>

    <div class="input-container">
      来源数据表
      <a-select
        class="input-full-width"
        :value="source.tableName"
        @change="handleTableChange"
      >
        <a-select-option v-for="table in tables" :key="table" :value="table">
          {{ table }}
        </a-select-option>
      </a-select>
    </div>

    <a-form-item label="同步配置" v-show="showSyncConfig">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-radio-group
            :value="syncMode"
            button-style="solid"
            @change="handleSyncModeChange"
          >
            <a-radio-button value="overwrite">全量</a-radio-button>
            <a-radio-button value="increment">增量</a-radio-button>
          </a-radio-group>
        </a-col>
        <a-col :span="6" v-show="!isIncrement">
          开启数据覆盖:
          <a-switch :checked="transCover" @change="handleCoverChange" />
        </a-col>
        <a-col :span="6">
          <p v-show="isIncrement">请选择增量字段:</p>
        </a-col>
        <a-col :span="6">
          <a-select
            v-show="isIncrement"
            :value="incrementField"
            placeholder="请选择增量字段"
            style="width: 100%"
            @change="handleIncrementFieldChange"
          >
            <a-select-option
              v-for="field in sourceFields"
              :key="field.name"
              :value="field.name"
            >
              {{ field.name }}
            </a-select-option>
          </a-select>
        </a-col>
      </a-row>
    </a-form-item>

    <a-form-item label="选择流转字段">
      <a-row :gutter="16" v-for="(mapping, index) in mappings" :key="index">
        <a-col :span="8">
          <a-select
            v-model="mapping.sourceField"
            placeholder="请选择来源字段"
            class="input-full-width"
            @change="handleMappingChange(index)"
          >
            <a-select-option
              v-for="field in sourceFields"
              :key="field.name"
              :value="field.name"
            >
              {{ field.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-icon
            type="minus-circle-o"
            @click="removeMapping(index)"
            v-show="mappings.length > 1"
          />
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
</template>

<script>
import LoadingDx from '@/components/common/loading-dx.vue'
import { dsImgObj } from '@/views/datasource/const'
import { EXCLUDE_FROM_DS } from './constants'

export default {
  name: 'SourceDrawer',

  components: {
    LoadingDx
  },

  props: {
    visible: {
      type: Boolean,
      default: false
    },
    dsList: {
      type: Array,
      default: () => []
    },
    loading: {
      type: Boolean,
      default: false
    },
    source: {
      type: Object,
      default: () => ({
        dsId: '',
        tableName: ''
      })
    },
    tables: {
      type: Array,
      default: () => []
    },
    sourceFields: {
      type: Array,
      default: () => []
    },
    mappings: {
      type: Array,
      default: () => []
    },
    syncMode: {
      type: String,
      default: 'overwrite'
    },
    transCover: {
      type: Boolean,
      default: false
    },
    incrementField: {
      type: String,
      default: ''
    },
    showSyncConfig: {
      type: Boolean,
      default: true
    }
  },

  computed: {
    isIncrement () {
      return this.syncMode === 'increment'
    }
  },

  data () {
    return {
      dsImgObj
    }
  },

  methods: {
    handleDsChange (value) {
      this.$emit('ds-change', value)
    },

    handleTableChange (value) {
      this.$emit('table-change', value)
    },

    handleSyncModeChange (e) {
      const mode = e.target.value
      this.$emit('sync-mode-change', {
        mode,
        isIncrement: mode === 'increment'
      })
    },

    handleCoverChange (checked) {
      this.$emit('cover-change', checked)
    },

    handleIncrementFieldChange (value) {
      this.$emit('increment-field-change', value)
    },

    handleMappingChange (index) {
      this.$emit('mapping-change', index)
    },

    addMapping () {
      this.$emit('add-mapping')
    },

    removeMapping (index) {
      this.$emit('remove-mapping', index)
    },

    afterVisibleChange (val) {
      this.$emit('visible-change', val)
    },

    onClose () {
      this.$emit('close', {
        dsId: this.source.dsId,
        dsName: this.source.dsName,
        tableName: this.source.tableName,
        mappings: this.mappings,
        syncMode: this.syncMode,
        transCover: this.transCover,
        incrementField: this.incrementField
      })
    }
  }
}
</script>

<style lang="less" scoped>
.ds-option {
  display: flex;
  align-items: center;
}

.ds-icon {
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

.select-container,
.input-container {
  margin-bottom: 10px;
}

.input-full-width {
  width: 100%;
}
</style>

<template>
  <a-modal
    title="添加数据源"
    :width="840"
    :visible="visible"
    :maskClosable="false"
    @cancel="handleCancel"
  >
    <LoadingDx size="'size-1x'" v-if="selectloading"></LoadingDx>
    <div>
      <a-card style="width: 100%">
        <a-row>
          <a-col :span="4">
            <a-space>
              <a-select
                ref="select"
                defaultValue="get"
                style="width: 150px"
                @focus="focus"
                @change="handleChange"
              >
                <a-select-option value="get">GET</a-select-option>
                <a-select-option value="post">POST</a-select-option>
              </a-select>
            </a-space>
          </a-col>
          <a-col :span="20">
            <a-input v-model="api_url" style="width: calc(100% - 250px)" :disabled="editable"/>
            <a-space>
              <a-button type="primary" @click="onSubmit" >请求此API</a-button>
            </a-space>
          </a-col>
        </a-row>
        <br />
        <a-row>
          <a-col :span="24">
            <!--       activeKey属性通过“v-model”实现表单与data数据的双向绑定, 需在data中定义该属性-->
            <a-tabs v-model="activeKey">
              <a-tab-pane key="1" tab="Params">
                <a-table :columns="columns" :dataSource="paramData" :pagination="false" bordered>
                  <template v-for="col in ['index', 'key', 'value']" :slot="col" slot-scope="text, record, index">
                    <div :key="col">
                      <a-input
                        v-if="record.editable"
                        style="margin: -5px 0"
                        :value="text"
                        @change="e => handleChanged('param', e.target.value, record.index, col,0)"
                      />
                      <template v-else>{{ text }}</template>
                    </div>
                  </template>
                  <template slot="operation" slot-scope="text, record, index">
                    <div class="editable-row-operations">
                      <span v-if="record.editable">
                        <a-space>
                          <a @click="() => save('param', record.index,index)">Save</a>
                          <a-popconfirm title="Sure to cancel?" @confirm="() => cancel('param', record.index,index)">
                            <a>Cancel</a>
                          </a-popconfirm>
                        </a-space>
                      </span>
                      <span v-else>
                        <a @click="() => edit('param', record.index,index)">Edit</a>
                      </span>
                      <a-divider type="vertical" />
                      <a @click="() => onParamDelete('param', record.index)">Delete</a>
                    </div>
                  </template>
                </a-table>
                <a-button class="editable-add-btn" style="width: 100%" @click="handleAdd('param')">+ 添加一行数据</a-button>
              </a-tab-pane>
              <a-tab-pane key="2" tab="Headers" force-render>
                <a-table :columns="columns" :dataSource="headerData" :pagination="false" bordered>
                  <template v-for="col in ['index', 'key', 'value']" :slot="col" slot-scope="text, record, index">
                    <div :key="col">
                      <a-input
                        v-if="record.editable"
                        style="margin: -5px 0"
                        :value="text"
                        @change="e => handleChanged('header', e.target.value, record.index, col,0)"
                      />
                      <template v-else>{{ text }}</template>
                    </div>
                  </template>
                  <template slot="operation" slot-scope="text, record, index">
                    <div class="editable-row-operations">
                      <span v-if="record.editable">
                        <a-space>
                          <a @click="() => save('header', record.index,index)">Save</a>
                          <a-popconfirm title="Sure to cancel?" @confirm="() => cancel('header', record.index,index)">
                            <a>Cancel</a>
                          </a-popconfirm>
                        </a-space>
                      </span>
                      <span v-else>
                        <a @click="() => edit('header', record.index,index)">Edit</a>
                      </span>
                      <a-divider type="vertical" />
                      <a @click="() => onParamDelete('header', record.index)">Delete</a>
                    </div>
                  </template>
                </a-table>
                <a-button class="editable-add-btn" style="width: 100%" @click="handleAdd('header')">+ 添加一行数据</a-button>
              </a-tab-pane>
              <a-tab-pane key="3" tab="Body">
                <a-col :span="24">
                  <a-radio-group defaultValue="none" :select="body_type" name="radioGroup" @change="onChange" style="width: 100%" >
                    <a-radio value="none">none</a-radio>
                    <a-radio value="form-data">form-data</a-radio>
                    <a-radio value="x-www-form-urlencoded">x-www-form-urlencoded</a-radio>
                    <a-radio value="raw">raw</a-radio>
                    <a-card style="width: 100%" >
                      <p v-if="body_type=='none'">This request does not have a body</p>
                      <div v-else-if="body_type=='form-data' || body_type=='x-www-form-urlencoded'">
                        <a-table :columns="columns" :dataSource="data" :pagination="false" bordered>
                          <template v-for="col in ['index', 'key', 'value']" :slot="col" slot-scope="text, record, index">
                            <div :key="col">
                              <a-input
                                v-if="record.editable"
                                style="margin: -5px 0"
                                :value="text"
                                @change="e => handleChanged('body', e.target.value, record.index, col,0)"
                              />
                              <template v-else>{{ text }}</template>
                            </div>
                          </template>
                          <template slot="operation" slot-scope="text, record, index">
                            <div class="editable-row-operations">
                              <span v-if="record.editable">
                                <a-space>
                                  <a @click="() => save('body', record.index,index)">Save</a>
                                  <a-popconfirm title="Sure to cancel?" @confirm="() => cancel('body', record.index,index)">
                                    <a>Cancel</a>
                                  </a-popconfirm>
                                </a-space>
                              </span>
                              <span v-else>
                                <a @click="() => edit('body', record.index,index)">Edit</a>
                              </span>
                              <a-divider type="vertical" />
                              <a @click="() => onParamDelete('body', record.index)">Delete</a>
                            </div>
                          </template>
                        </a-table>
                        <a-button class="editable-add-btn" style="width: 100%" @click="handleAdd('body')">+ 添加一行数据</a-button>
                      </div>
                      <div v-else-if="body_type=='raw'">
                        <a-textarea
                          v-model="rawValue"
                          placeholder="application/json"
                          :auto-size="{ minRows: 3, maxRows: 5 }"
                        />
                      </div>
                    </a-card>
                  </a-radio-group>
                </a-col>
              </a-tab-pane>
            </a-tabs>
          </a-col>
        </a-row>
        <br />
        <a-row>
          <a-col :span="4">
            <a-space>
              <a-button style="width: 150px; text-align: left">配置Json_Path</a-button>
            </a-space>
          </a-col>
          <a-col :span="20">
            <a-input v-model="json_path" placeholder="根据json_path解析接口结果，配置到结果集的上一层即可"/>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="4">
            <a-space>
              <a-button style="width: 150px; text-align: left">数据源名称</a-button>
            </a-space>
          </a-col>
          <a-col :span="20">
            <a-input v-model="ds_name"/>
          </a-col>
        </a-row>
        <a-row>
          <a-card title="Response" style="width: 100%">
            <p>{{ rev_data }}</p>
          </a-card>
        </a-row>
      </a-card>

    </div>
    <template slot="footer">
      <a-button key="cancel" @click="handleCancel">取消</a-button>
      <a-button key="forward" v-show="onlyRead" :disabled=editable :loading="confirmLoading" type="primary" @click="handleSubmit">保存</a-button>
    </template>
  </a-modal>
</template>

<script>
  import { httpGo } from '@/api/postman'
  import { addObj, getObj, putObj } from '@/api/datasource/datasource'

  // 表头数据,title 为表头的标题 dataIndex为列数据在数据项中对应的 key
  const columns = [
    // {
    //   title: 'index',
    //   dataIndex: 'index',
    //   scopedSlots: { customRender: 'index' },
    //   width: '25%'
    // },
    {
      title: 'key',
      dataIndex: 'key',
      scopedSlots: { customRender: 'key' },
      width: '25%'
    },
    {
      title: 'value',
      dataIndex: 'value',
      scopedSlots: { customRender: 'value' },
      width: '25%'
    },
    {
      title: 'operation',
      dataIndex: 'operation',
      scopedSlots: { customRender: 'operation' }, // 值跟dataIndex对应，支持操作列插槽
      width: '25%'
    }
  ]

  export default {
    name: 'Postman',
    data () {
      return {
        paramCount: 0,
        headerCount: 0,
        bodyCount: 0,
        api_url: '',
        rev_data: {},
        json_path: '',
        ds_name: '',
        activeKey: '1', // 控制标签页params、headers、body
        data: [],
        headerData: [],
        paramData: [],
        dataSource: [],
        method: 'GET',
        columns: columns,
        body_type: 'none',
        rawValue: '',
        type: '',
        editableData: {},
        code: '', // 编辑器绑定的值,对应v-model
        visible: false,
        selectloading: false,
        confirmLoading: false,
        onlyRead: true,
        editable: false,
        dsId: '',
        // 默认配置
        options: {
          tabSize: 4, // 缩进格式
          theme: 'material', // 主题，对应主题库 JS 需要提前引入
          lineNumbers: true, // 显示行号
          line: true, // 检查格式
          autocorrect: true, // 自动更正
          spellcheck: true, // 拼写检查
          mode: { // 模式, 可查看 /mode 中的所有模式,运行代码类型
            name: 'python',
            json: true
          },
          styleActiveLine: true, // 高亮选中行
          hintOptions: {
            completeSingle: true // 当匹配只有一项的时候是否自动补全
          }
        }
      }
    },
    methods: {
      onSubmit () {
        const httpConfig = {
          'method': this.method,
          'api_url': this.api_url,
          'header': this.headerData,
          'param': this.paramData,
          'body': this.data,
          'content_type': this.body_type,
          'json_path': this.json_path,
          'raw': this.rawValue
        }
        httpGo(httpConfig)
          .then((result) => {
            if (result.status === '0') {
              this.rev_data = result.result
              console.log(this.rev_data)
            } else {
              this.$message.error(result.errstr)
            }
          })
          .catch(err => {
            this.$message.error('请求失败：' + (err.message || '未知错误'))
          })
      },
      handleSubmit () {
        const httpConfig = {
          'method': this.method,
          'url': this.api_url,
          'header': this.headerData,
          'param': this.paramData,
          'body': this.data,
          'content_type': this.body_type,
          'raw': this.rawValue,
          'json_path': this.json_path,
          'rev_data': JSON.stringify(this.rev_data)
        }

        const formData = {
          'ds_id': this.dsId,
          'name': this.ds_name,
          'type': 'http',
          'config': JSON.stringify(httpConfig)
        }
        if (this.api_url === '') {
          this.$message.error('接口数据不能为空')
          return
        }
        if (this.json_path === '') {
          this.$message.error('json_path不能为空')
          return
        }
        if (this.ds_name === '') {
          this.$message.error('数据源名称不能为空')
          return
        }
        this.selectloading = true
        if (this.dsId === '') {
          addObj(formData).then(res => {
              if (res.status === '0') {
                this.$emit('ok')
                this.confirmLoading = false
                // 清楚表单数据
                this.$message.success('保存成功')
                this.visible = false
              } else {
                this.confirmLoading = false
                this.$message.error(res.errstr)
              }
            }).catch(err => {
              this.confirmLoading = false
              this.$message.error('保存失败：' + (err.errstr || err.message || '未知错误'))
          })
        } else {
          putObj(formData).then(res => {
            if (res.status === '0') {
              this.$emit('ok')
              this.confirmLoading = false
              // 清楚表单数据
              this.$message.success('修改成功')
              this.visible = false
            } else {
              this.confirmLoading = false
              this.$message.error(res.errstr)
            }
          }).catch(err => {
            this.confirmLoading = false
            this.$message.error('修改失败：' + (err.errstr || err.message || '未知错误'))
          })
        }
        this.selectloading = false
      },
      focus (key) {
        console.log(key)
      },
      handleChange (key) {
        console.log(key)
        this.method = key
      },
      onChange (e) {
        // TODO json需要单独处理
        if (e.target.value === 'none') {
          this.body_type = 'none'
          console.log('radio-----', e)
        } else if (e.target.value === 'form-data') {
          this.body_type = 'form-data'
        } else if (e.target.value === 'x-www-form-urlencoded') {
          this.body_type = 'x-www-form-urlencoded'
        } else if (e.target.value === 'raw') {
          this.body_type = 'raw'
        }
      },
      confirm () {},
      handleChanged (type, value, key, column, index) {
        if (type === 'param') {
          const newData = [...this.paramData]
          const target = newData.filter(item => key === item.index)[index]
          if (target) {
            target[column] = value
            this.paramData = newData
          }
        } else if (type === 'header') {
          const newData = [...this.headerData]
          const target = newData.filter(item => key === item.index)[index]
          if (target) {
            target[column] = value
            this.headerData = newData
          }
        } else {
          const newData = [...this.data]
          const target = newData.filter(item => key === item.index)[index]
          if (target) {
            target[column] = value
            this.data = newData
          }
        }
      },
      edit (type, key, index) {
        if (type === 'param') {
          const newData = [...this.paramData]
          console.log('newData----', newData)
          const target = newData.filter(item => key === item.index)[0]
          console.log(target)
          if (target) {
            target.editable = true
            this.paramData = newData
          }
        } else if (type === 'header') {
          const newData = [...this.headerData]
          console.log('newData----', newData)
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            target.editable = true
            this.headerData = newData
          }
        } else {
          const newData = [...this.data]
          console.log('newData----', newData)
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            target.editable = true
            this.data = newData
          }
        }
      },
      onParamDelete (type, key) {
        if (type === 'param') {
          this.paramData = this.paramData.filter(item => item.index !== key)
        } else if (type === 'header') {
          this.headerData = this.headerData.filter(item => item.index !== key)
        } else {
          this.data = this.data.filter(item => item.index !== key)
        }
      },
      save (type, key, index) {
        if (type === 'param') {
          const newData = [...this.paramData]
          console.log(newData)
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            delete target.editable
            this.paramData = newData
            this.cacheData = newData.map(item => ({ ...item }))
          }
        } else if (type === 'header') {
          const newData = [...this.headerData]
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            delete target.editable
            this.headerData = newData
            this.cacheData = newData.map(item => ({ ...item }))
          }
        } else {
          const newData = [...this.data]
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            delete target.editable
            this.data = newData
            this.cacheData = newData.map(item => ({ ...item }))
          }
        }
      },
      cancel (type, key, index) {
        if (type === 'param') {
          const newData = [...this.paramData]
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            Object.assign(target, this.cacheData.filter(item => key === item.index)[0])
            delete target.editable
            this.paramData = newData
          }
        } else if (type === 'header') {
          const newData = [...this.headerData]
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            Object.assign(target, this.cacheData.filter(item => key === item.index)[0])
            delete target.editable
            this.headerData = newData
          }
        } else {
          const newData = [...this.data]
          const target = newData.filter(item => key === item.index)[0]
          if (target) {
            Object.assign(target, this.cacheData.filter(item => key === item.index)[0])
            delete target.editable
            this.data = newData
          }
        }
      },
      itemClick (node) {
        console.log(node.model.text + ' clicked !')
      },
      handleAdd (key) {
        if (key === 'param') {
          this.paramData.push({
            index: this.paramCount,
            key: '',
            value: ''
          })
          this.paramCount++
        } else if (key === 'header') {
          this.headerData.push({
            index: this.headerCount,
            key: '',
            value: ''
          })
          this.headerCount++
        } else {
          this.data.push({
            index: this.bodyCount,
            key: '',
            value: ''
          })
          this.bodyCount++
        }
      },
      show (dsId, type) {
        this.type = type
        this.dsId = dsId
        switch (type) {
          case 'add':
            this.editable = false
            break
          case 'edit':
            this.editable = false
            break
          default:
            this.editable = true
            break
        }
        this.visible = true
        if (['edit', 'show'].includes(type)) {
          getObj(dsId).then(res => {
            const record = res.result
            const dsConfig = JSON.parse(record.config)
            this.api_url = dsConfig.url
            this.method = dsConfig.method
            this.headerData = dsConfig.header
            this.paramData = dsConfig.param
            this.data = dsConfig.body
            this.body_type = dsConfig.content_type
            this.rawValue = dsConfig.raw
            this.json_path = dsConfig.json_path
            this.rev_data = JSON.parse(dsConfig.rev_data)
            this.ds_name = record.name
          })
          .catch(err => {
            this.$message.error('获取数据源信息失败：' + (err.errstr || err.message || '未知错误'))
          })

          this.confirmLoading = false
        }
      },
      handleCancel () {
        this.visible = false
        this.api_url = ''
        this.json_path = ''
        this.ds_name = ''
        this.rev_data = {}
        this.data = []
        this.headerData = []
        this.paramData = []
        this.dataSource = []
        this.method = 'GET'
        this.body_type = 'none'
        this.rawValue = ''
        this.dsId = ''
        setTimeout(() => {
          this.addable = false
          this.showable = false
          this.editable = false
        }, 200)
      }
    },
    components: {
    }
  }

</script>

<style scoped>

</style>

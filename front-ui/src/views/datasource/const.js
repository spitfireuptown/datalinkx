import {
  mysqlPng,
  oraclePng,
  ESPng,
  redisPng,
  kafkaPng,
  httpPng,
  mysqlcdcPng,
  customPng,
  ckPng
} from '@/core/icons'

const DataSourceType = [
  {
    label: 'MySQL',
    value: 'mysql'
  },
  {
    label: 'ELASTICSEARCH',
    value: 'es'
  },
  {
    label: 'ORACLE',
    value: 'oracle'
  },
  {
    label: 'REDIS',
    value: 'redis'
  },
  {
    label: 'HTTP',
    value: 'http'
  },
  {
    label: 'MYSQLCDC',
    value: 'mysqlcdc'
  },
  {
    label: 'KAFKA',
    value: 'kafka'
  },
  {
    label: 'CUSTOM',
    value: 'custom'
  },
  {
    label: 'CK',
    value: 'clickhouse'
  }
]

const DsGroupDefaultNumber = {
    'mysql': 0,
    'es': 0,
    'oracle': 0,
    'redis': 0,
    'http': 0,
    'custom': 0,
    'mysqlcdc': 0,
    'kafka': 0,
    'clickhouse': 0
}

const dsTypeList = [
  {
    value: 'MySQL',
    label: 'MySQL',
    dsTypeKey: 'mysql',
    img: mysqlPng
  },
  {
    value: 'es',
    label: 'ElasticSearch',
    dsTypeKey: 'es',
    img: ESPng
  },
  {
    value: 'mysqlcdc',
    label: 'MYSQLCDC',
    dsTypeKey: 'mysqlcdc',
    img: mysqlcdcPng
  },
  {
    value: 'oracle',
    label: 'Oracle',
    dsTypeKey: 'oracle',
    img: oraclePng
  },
  {
    value: 'redis',
    label: 'Redis',
    dsTypeKey: 'redis',
    img: redisPng
  },
  {
    value: 'clickhouse',
    label: 'ClickHouse',
    dsTypeKey: 'clickhouse',
    img: ckPng
  },
  {
    value: 'kafka',
    label: 'Kafka',
    dsTypeKey: 'kafka',
    img: kafkaPng
  },
  {
    value: 'http',
    label: 'HTTP',
    dsTypeKey: 'http',
    img: httpPng
  },
  {
    value: 'custom',
    label: 'CUSTOM',
    dsTypeKey: 'custom',
    img: customPng
  }
]
const dsImgObj = {
  'mysql': mysqlPng,
  'es': ESPng,
  'oracle': oraclePng,
  'redis': redisPng,
  'http': httpPng,
  'custom': customPng,
  'mysqlcdc': mysqlcdcPng,
  'kafka': kafkaPng,
  'clickhouse': ckPng
}
// 目标数据源 redis 类型
const RedisTypes = [
  {
    label: 'string',
    value: 'string'
  },
  {
    label: 'list-lpush',
    value: 'list-lpush'
  },
  {
    label: 'list-rpush',
    value: 'list-rpush'
  },
  {
    label: 'set-sadd',
    value: 'set-sadd'
  },
  {
    label: 'zset-zadd',
    value: 'zset-zadd'
  },
  {
    label: 'hash-hset',
    value: 'hash-hset'
  }
]

const dsConfigOriginList = [
  {
    label: '数据源名称',
    key: 'name',
    type: 'input',
    decorator: '请输入数据源名称',
    isRequired: true
  },
  {
    label: 'host',
    key: 'host',
    type: 'input',
    decorator: '请输入 host',
    isRequired: true
  },
  {
    label: 'port',
    key: 'port',
    type: 'input',
    decorator: '请输入 port',
    isRequired: true
  },
  {
    label: 'username',
    key: 'username',
    type: 'input',
    decorator: '请输入 username',
    isRequired: true
  },
  {
    label: 'password',
    key: 'password',
    type: 'password',
    decorator: '请输入 password',
    isRequired: true
  },
  {
    label: 'database',
    key: 'database',
    type: 'input',
    decorator: '请输入 database',
    isRequired: true
  }
  // {
  //   label: '附加配置',
  //   key: 'config',
  //   type: 'textarea',
  //   decorator: '请输入附加配置',
  //   isRequired: false
  // }
]

const OracleServerTypes = [
  {
    label: 'SID',
    value: 'sid'
  },
  {
    label: '服务名',
    value: 'servername'
  }
]

const MysqlCDCTypes = [
  {
    label: 'INSERT',
    value: 'INSERT'
  },
  {
    label: 'UPDATE',
    value: 'UPDATE'
  }
]

export { DataSourceType, dsTypeList, RedisTypes, dsConfigOriginList, OracleServerTypes, dsImgObj, MysqlCDCTypes, DsGroupDefaultNumber }

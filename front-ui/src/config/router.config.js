// eslint-disable-next-line
import { UserLayout, BasicLayout, BlankLayout } from '@/layouts'
import {
  dataManage,
  dataList,
  task,
  taskList,
  taskLog
} from '@/core/icons'

const RouteView = {
  name: 'RouteView',
  render: h => h('router-view')
}

export const asyncRouterMap = [
  {
    path: '/',
    name: 'index',
    component: BasicLayout,
    meta: { title: 'menu.home' },
    redirect: '/analysis',
    children: [
      {
        path: '/analysis',
        name: 'analysis',
        component: () => import('@/views/dashboard/Analysis.vue'),
        meta: { title: 'menu.analysis', keepAlive: true, icon: task }
      },
      {
        path: '/datasource',
        name: 'datasource',
        redirect: '/datasource/workplace',
        component: RouteView,
        meta: { title: 'menu.batchDataSource', keepAlive: true, icon: dataManage },
        children: [
          {
            path: '/dashboard',
            name: 'dashboard',
            component: () => import('@/views/datasource/DsList.vue'),
            meta: { title: 'menu.datasourceList', keepAlive: false, icon: dataList }
          }
        ]
      },
      {
        path: '/transferTask',
        name: 'transferTask',
        redirect: '/transferTask/workplace',
        component: RouteView,
        meta: { title: 'menu.transferTask', keepAlive: true, icon: task },
        children: [
          {
            path: '/job',
            name: 'job',
            component: () => import('@/views/job/JobList.vue'),
            meta: { title: 'menu.taskList', keepAlive: false, icon: taskList }
          }
        ]
      },
      {
        path: '/streaming/transferTask',
        name: 'streamingTransferTask',
        redirect: '/streamingTransferTask/workplace',
        component: RouteView,
        meta: { title: 'menu.streamingTransferTask', keepAlive: true, icon: task },
        children: [
          {
            path: '/streaming/job',
            name: 'StreamingJob',
            component: () => import('@/views/job/JobListOfStreaming.vue'),
            meta: { title: 'menu.streamingTaskList', keepAlive: false, icon: taskList }
          }
        ]
      },
      {
        path: '/compute/transferTask',
        name: 'computeTransferTask',
        redirect: '/computeTransferTask/workplace',
        component: RouteView,
        meta: { title: 'menu.computeTransferTask', keepAlive: true, icon: task },
        children: [
          {
            path: '/compute/job',
            name: 'ComputeJob',
            component: () => import('@/views/job/JobListOfCompute.vue'),
            meta: { title: 'menu.computeTaskList', keepAlive: false, icon: taskList }
          }
        ]
      },
      {
        path: '/job_relation',
        name: 'job_relation',
        component: () => import('@/views/jobrelation/JobRelationList.vue'),
        meta: { title: 'menu.taskrelationlist', keepAlive: false, icon: taskLog }
      },
      {
        path: '/job_relation_map',
        name: 'job_map',
        component: () => import('@/views/jobrelation/JobRelationBloodMap.vue'),
        meta: { title: 'menu.taskrelationmap', keepAlive: false, icon: taskLog }
      },
      {
        path: '/job_log',
        name: 'joblog',
        component: () => import('@/views/joblog/JobLogList.vue'),
        meta: { title: 'menu.tasklistlog', keepAlive: false, icon: taskLog }
      },
      {
        path: '/alarm',
        name: 'alarm',
        redirect: '/alarm/center',
        component: RouteView,
        meta: { title: 'menu.alarmCenter', keepAlive: true, icon: task },
        children: [
          {
            path: '/alarm/config',
            name: 'AlarmConfig',
            component: () => import('@/views/alarm/alarmConfig.vue'),
            meta: { title: 'menu.alarmComponentConfig', keepAlive: false, icon: taskList }
          },
          {
            path: '/alarm/rule',
            name: 'AlarmRule',
            component: () => import('@/views/alarm/alarmRule.vue'),
            meta: { title: 'menu.alarmRuleConfig', keepAlive: false, icon: taskList }
          }
        ]
      },
      {
        path: '/system/param',
        name: 'taskParam',
        component: () => import('@/views/system/SystemParamList.vue'),
        meta: { title: 'menu.taskParam', keepAlive: true, icon: task }
      },
      {
        path: '/system/monitor',
        name: 'systemMonitor',
        component: () => import('@/views/system/systemMonitor.vue'),
        meta: { title: 'menu.systemMonitor', keepAlive: true, icon: task }
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    hidden: true
  }
]

/**
 * 基础路由
 * @type { *[] }
 */
export const constantRouterMap = [
  {
    path: '/user',
    component: UserLayout,
    redirect: '/user/login',
    hidden: true,
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import(/* webpackChunkName: "user" */ '@/views/user/Login')
      },
      {
        path: 'register',
        name: 'register',
        component: () => import(/* webpackChunkName: "user" */ '@/views/user/Register')
      },
      {
        path: 'register-result',
        name: 'registerResult',
        component: () => import(/* webpackChunkName: "user" */ '@/views/user/RegisterResult')
      },
      {
        path: 'recover',
        name: 'recover',
        component: undefined
      }
    ]
  },

  {
    path: '/404',
    component: () => import(/* webpackChunkName: "fail" */ '@/views/exception/404')
  }
]

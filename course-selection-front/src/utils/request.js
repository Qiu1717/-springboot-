import axios from 'axios'
import { Message } from 'element-ui'  // Element UI 的消息提示组件
import router from '@/router'        // Vue Router 实例（用于401时跳转登录页）

// ===== 创建axios实例 =====
const instance = axios.create({
  baseURL: '',       // 空字符串: 使用相对路径，由 vue.config.js 代理到后端
  timeout: 10000     // 10秒超时，超时触发响应拦截器的error分支
})

// ===== 请求拦截器: 每个请求发出前自动执行 =====
instance.interceptors.request.use(
  config => {
    const token = sessionStorage.getItem('token')  // 从sessionStorage取Token（Login.vue登录成功后存储）
    if (token) {
      config.headers.Authorization = `Bearer ${token}`  // 按JWT标准格式: "Bearer <token字符串>"
    }
    return config  // 继续发送请求
  },
  error => Promise.reject(error)  // 请求配置错误直接拒绝
)

// ===== 响应拦截器: 每个响应回来时自动执行 =====
instance.interceptors.response.use(
  response => {
    // 业务成功: HTTP 200 + res.code === 200
    const res = response.data  // axios包装层: response.data 才是后端返回的 Result 对象
    if (res.code !== 200) {    // 业务异常: 如"课程已满员"、"选课已达上限"
      Message.error(res.message || '请求失败')  // 弹出红色错误提示
      return Promise.reject(new Error(res.message))  // 转为rejected，调用方.catch()捕获
    }
    return res  // code=200 → 直接返回Result对象，调用方.then(res => res.data取数据)
  },
  error => {
    // HTTP错误: 网络故障、超时、401、403、500等
    if (error.response) {
      const status = error.response.status  // HTTP状态码
      if (status === 401) {  // 未认证: Token过期或无效
        Message.error('登录已过期，请重新登录')
        sessionStorage.removeItem('token')       // 清除过期Token
        sessionStorage.removeItem('userInfo')    // 清除用户信息
        router.push('/login')                    // 强制跳转登录页
      } else if (status === 403) {  // 无权限: 角色不匹配
        Message.error('没有权限访问')
      } else {
        Message.error('服务器错误: ' + status)  // 其他HTTP错误
      }
    } else {
      Message.error('网络连接失败，请检查网络')  // 无response: 网络不通或超时
    }
    return Promise.reject(error)  // 继续向外传递错误
  }
)

export default instance  // 导出配置好的axios实例，所有Vue组件 import request from '@/utils/request' 使用

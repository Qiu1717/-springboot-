import Vue from 'vue'
import App from './App.vue'          // 根组件
import router from './router'        // 路由配置
import ElementUI from 'element-ui'   // Element UI组件库(el-table/el-form/el-button等)
import 'element-ui/lib/theme-chalk/index.css'  // Element UI样式

Vue.config.productionTip = false  // 关闭生产环境提示
Vue.use(ElementUI)  // 全局注册Element UI: 所有.vue文件可直接使用<el-xxx>组件

// 全局Vue错误处理: 静默错误信息，避免开发模式下红色全屏覆盖层
// 业务错误已由 request.js 响应拦截器通过 Message.error() 向用户提示
Vue.config.errorHandler = (err) => {
  console.error('[Vue Error]', err)  // 仅控制台输出，不显示红色遮罩
}
window.addEventListener('unhandledrejection', event => {
  event.preventDefault()  // 阻止未处理的Promise rejection弹窗
})

// 创建Vue实例，挂载到 #app (public/index.html中的根元素)
new Vue({
  router,              // 注入路由
  render: h => h(App)  // 渲染App.vue(包含<router-view/>)
}).$mount('#app')

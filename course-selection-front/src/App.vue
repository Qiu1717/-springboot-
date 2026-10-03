<template>
  <div id="app">
    <router-view />
    <!-- AI 悬浮助手：登录后才显示 -->
    <AIChat v-if="isLoggedIn" />
  </div>
</template>

<script>
import AIChat from '@/views/common/AIChat.vue'

export default {
  name: 'App',
  components: { AIChat },
  computed: {
    isLoggedIn() {
      // $route 是 Vue 响应式的，路由跳转到 /login 时自动消失
      return this.$route.path !== '/login'
    }
  }
}
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}
</style>

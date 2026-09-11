<template><div class="app-shell" :class="{ 'is-auth-page': isAuthPage }"><AppHeader v-if="!isAuthPage"/><div class="shell-body"><DesktopSidebar v-if="!isAuthPage"/><main class="main-content"><RouterView/></main><aside v-if="!isAuthPage" class="desktop-rail" aria-label="城市灵感"><p class="eyebrow">杭州 · 今日灵感</p><h2>把日常过成一场小旅行</h2><p>从一杯咖啡到一条街巷，记录你与城市相遇的瞬间。</p><div class="rail-tags"><span>咖啡馆</span><span>街区漫步</span><span>夜宵</span></div></aside></div><BottomNav v-if="!isAuthPage"/></div></template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, RouterView } from 'vue-router'
import AppHeader from '@/components/navigation/AppHeader.vue'
import BottomNav from '@/components/navigation/BottomNav.vue'
import DesktopSidebar from '@/components/navigation/DesktopSidebar.vue'
const route = useRoute()
const isAuthPage = computed(() => route.name === 'login' || route.name === 'password-login')
</script>

<style scoped>
.is-auth-page .shell-body {
  display: block;
}
</style>

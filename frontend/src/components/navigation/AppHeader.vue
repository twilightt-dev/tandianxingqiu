<template><header class="app-header"><RouterLink class="brand" :to="{ name: 'home' }" aria-label="探店星球首页"><BrandMark/><span>探店星球</span></RouterLink><span class="city"><LocationInformation class="icon" aria-hidden="true"/>杭州</span><RouterLink v-if="!auth.isAuthenticated" class="header-action" :to="{ name: 'login' }">登录</RouterLink><RouterLink v-else class="header-action" :to="{ name: 'me' }">{{ auth.user?.nickName || '我的' }}</RouterLink></header></template>
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { LocationInformation } from '@element-plus/icons-vue'
import BrandMark from '@/components/common/BrandMark.vue'
import { useAuthStore } from '@/stores/auth'
const auth = useAuthStore()
</script>

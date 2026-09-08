<template><aside class="desktop-sidebar" aria-label="桌面主导航"><nav><RouterLink v-for="item in items" :key="item.name" :to="{ name: item.name }" class="side-nav-item" active-class="is-active"><component :is="item.icon" aria-hidden="true"/><span>{{ item.label }}</span></RouterLink></nav><div class="discovery-hint" aria-disabled="true"><Compass class="icon" aria-hidden="true"/><span>发现更多<br/><small>城市灵感即将上线</small></span></div></aside></template>
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { HomeFilled, EditPen, UserFilled, Compass } from '@element-plus/icons-vue'
const items = [{ name:'home', label:'首页', icon:HomeFilled }, { name:'publish', label:'发布', icon:EditPen }, { name:'me', label:'我的', icon:UserFilled }]
</script>

<template><nav class="bottom-nav" aria-label="主导航"><RouterLink v-for="item in items" :key="item.name" :to="{ name: item.name }" class="bottom-nav-item" active-class="is-active"><component :is="item.icon" class="nav-icon" aria-hidden="true"/><span>{{ item.label }}</span></RouterLink></nav></template>
<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { HomeFilled, EditPen, UserFilled } from '@element-plus/icons-vue'
const items = [{ name:'home', label:'首页', icon:HomeFilled }, { name:'publish', label:'发布', icon:EditPen }, { name:'me', label:'我的', icon:UserFilled }]
</script>

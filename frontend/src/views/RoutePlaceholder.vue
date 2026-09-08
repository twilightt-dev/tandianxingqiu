<template><section class="placeholder" aria-labelledby="placeholder-title"><p class="eyebrow">探店星球</p><h1 id="placeholder-title">{{ title }}</h1><p>这片内容正在准备中，很快就会和你见面。</p></section></template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
const route = useRoute()
const title = computed(() => String(route.name ?? '页面'))
</script>

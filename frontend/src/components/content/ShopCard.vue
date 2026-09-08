<template>
  <article class="shop-card">
    <button type="button" class="shop-card__open" data-test="shop-card-open" @click="$emit('open', shop)">
      <img v-if="imageUrl" data-test="shop-image" class="shop-card__image" :src="imageUrl" :alt="`${shop.name || '门店'}的图片`" width="144" height="120" loading="lazy">
      <span v-else class="shop-card__placeholder" aria-label="暂无门店图片"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19 9 13l3 3 3-4 5 7H4Zm0-14h16v10l-5-6-3 4-3-3-5 6V5Z" fill="currentColor"/></svg></span>
      <span class="shop-card__body"><strong>{{ shop.name || '未命名门店' }}</strong><span class="shop-card__score">★ {{ scoreText }} <span>{{ shop.comments || 0 }} 条评价</span></span><span class="shop-card__address">{{ [shop.area, shop.address].filter(Boolean).join(' · ') || '地址待补充' }}</span><span class="shop-card__meta"><b v-if="shop.avgPrice !== undefined">¥{{ shop.avgPrice }}/人</b><b v-if="distance !== ''" data-test="shop-distance">{{ distance }}</b></span></span>
    </button>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Shop } from '@/types/api'
import { formatDistance } from '@/utils/format'
const props = defineProps<{ shop: Shop }>()
defineEmits<{ open: [shop: Shop] }>()
const imageUrl = computed(() => props.shop.images?.split(',').map((item) => item.trim()).find(Boolean) || '')
const distance = computed(() => formatDistance(props.shop.distance))
const scoreText = computed(() => Number.isFinite(props.shop.score) ? (Number(props.shop.score) / 10).toFixed(1) : '暂无评分')
</script>

<style scoped>
.shop-card{overflow:hidden;border:1px solid var(--color-border);border-radius:var(--radius-card);background:var(--color-surface);box-shadow:var(--shadow-card)}.shop-card__open{display:flex;width:100%;min-height:132px;padding:0;border:0;background:transparent;color:inherit;font:inherit;text-align:left;cursor:pointer}.shop-card__open:focus-visible{outline:3px solid var(--color-focus);outline-offset:-3px}.shop-card__image,.shop-card__placeholder{width:38%;min-width:116px;object-fit:cover;background:#fde9dc}.shop-card__placeholder{display:grid;place-items:center;color:var(--color-primary)}.shop-card__placeholder svg{width:32px}.shop-card__body{display:grid;align-content:center;gap:7px;min-width:0;padding:14px}.shop-card__body strong{overflow:hidden;font-size:1rem;white-space:nowrap;text-overflow:ellipsis}.shop-card__score{color:#b56a09;font-size:.84rem;font-weight:700}.shop-card__score span{margin-left:6px;color:var(--color-muted);font-weight:400}.shop-card__address{overflow:hidden;color:var(--color-muted);font-size:.82rem;white-space:nowrap;text-overflow:ellipsis}.shop-card__meta{display:flex;justify-content:space-between;gap:10px;color:var(--color-primary);font-size:.82rem}.shop-card__meta b{font-weight:700}@media(min-width:768px){.shop-card__image,.shop-card__placeholder{width:34%}}
</style>

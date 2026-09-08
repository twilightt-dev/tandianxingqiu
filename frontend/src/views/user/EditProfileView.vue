<template>
  <section class="edit-profile"><header><p>资料管理</p><h1>个人资料</h1></header><section class="readonly-notice" role="status"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 7v5m0 4h.01M5 20h14a2 2 0 0 0 1.7-3L13.7 5a2 2 0 0 0-3.4 0L3.3 17A2 2 0 0 0 5 20Z" fill="none" stroke="currentColor" stroke-width="1.8"/></svg><p>当前版本仅支持查看资料，服务端暂未提供资料保存能力。</p></section><PageState v-if="state !== 'ready'" :state="state" :message="errorMessage" @retry="load"/><section v-else class="profile-fields"><div class="profile-avatar"><img v-if="user?.icon" :src="user.icon" :alt="`${user.nickName || '用户'}的头像`" width="72" height="72" loading="lazy"><span v-else aria-hidden="true">{{ (user?.nickName || '探').slice(0, 1) }}</span></div><dl><div><dt>昵称</dt><dd>{{ user?.nickName || '未设置' }}</dd></div><div><dt>简介</dt><dd>{{ info.introduce || '未设置' }}</dd></div><div><dt>性别</dt><dd>{{ info.gender === undefined ? '未设置' : info.gender ? '男' : '女' }}</dd></div><div><dt>城市</dt><dd>{{ info.city || '未设置' }}</dd></div><div><dt>生日</dt><dd>{{ info.birthday || '未设置' }}</dd></div><div><dt>积分</dt><dd>{{ info.credits ?? 0 }}</dd></div><div><dt>等级</dt><dd>{{ info.level === undefined ? '未设置' : info.level ? '是' : '否' }}</dd></div></dl></section></section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getUserInfo } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import PageState from '@/components/common/PageState.vue'
import type { User, UserInfo } from '@/types/api'
type ViewState = 'loading' | 'error' | 'empty' | 'ready'
const auth = useAuthStore(); const user = ref<User>(); const info = ref<UserInfo>({}); const state = ref<ViewState>('loading'); const errorMessage = ref('')
async function load() { state.value = 'loading'; errorMessage.value = ''; try { const current = auth.user || await auth.fetchCurrentUser(); if (current.id === undefined) throw new Error('缺少用户标识'); user.value = current; info.value = await getUserInfo(current.id) || {}; state.value = 'ready' } catch { state.value = 'error'; errorMessage.value = '资料加载失败，请稍后重试。' } }
void load()
</script>

<style scoped>
.edit-profile{max-width:720px;margin:0 auto}.edit-profile header p{margin:0;color:var(--color-primary);font-size:.85rem;font-weight:700}.edit-profile h1{margin:4px 0 18px;font-size:clamp(1.7rem,4vw,2.4rem)}.readonly-notice{display:flex;gap:9px;margin-bottom:18px;padding:13px;border:1px solid #edc594;border-radius:var(--radius-control);background:#fff8e9;color:#80510d}.readonly-notice svg{width:22px;flex:none}.readonly-notice p{margin:0;font-size:.9rem}.profile-fields{padding:20px;border:1px solid var(--color-border);border-radius:var(--radius-card);background:var(--color-surface);box-shadow:var(--shadow-card)}.profile-avatar{display:grid;place-items:center;margin-bottom:18px}.profile-avatar img,.profile-avatar span{width:72px;height:72px;border-radius:50%;object-fit:cover}.profile-avatar span{display:grid;place-items:center;background:#f7c8af;color:#884625;font-size:1.5rem;font-weight:800}.profile-fields dl{margin:0}.profile-fields dl div{display:grid;grid-template-columns:86px 1fr;gap:12px;padding:13px 0;border-top:1px solid var(--color-border)}.profile-fields dt{color:var(--color-muted)}.profile-fields dd{margin:0;white-space:pre-wrap;overflow-wrap:anywhere}
</style>

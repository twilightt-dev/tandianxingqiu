<template>
  <section class="auth-view" aria-labelledby="register-title">
    <RouterLink class="auth-view__brand" :to="{ name: 'home' }"><BrandMark />探店星球</RouterLink>
    <div class="auth-panel">
      <p class="auth-panel__kicker">创建账号，开始记录城市生活</p>
      <h1 id="register-title">注册</h1>

      <form novalidate :aria-describedby="submitError ? 'register-submit-error' : undefined" @submit.prevent="submit">
        <label for="register-phone">手机号</label>
        <input id="register-phone" v-model.trim="form.phone" data-test="phone" type="tel" inputmode="numeric" autocomplete="tel" placeholder="请输入手机号" :aria-invalid="!!errors.phone" :aria-describedby="errors.phone ? 'register-phone-error' : undefined" @input="errors.phone = ''">
        <p v-if="errors.phone" id="register-phone-error" class="field-error">{{ errors.phone }}</p>

        <div class="code-field">
          <div>
            <label for="register-code">验证码</label>
            <input id="register-code" v-model.trim="form.verifyCode" data-test="verify-code" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位数字" maxlength="6" :aria-invalid="!!errors.verifyCode" :aria-describedby="errors.verifyCode ? 'register-code-error' : undefined" @input="errors.verifyCode = ''">
          </div>
          <button type="button" data-test="send-code" :disabled="cooldown > 0 || sending" @click="requestCode">{{ cooldown ? `${cooldown} 秒后重试` : sending ? '发送中…' : '获取验证码' }}</button>
        </div>
        <p v-if="errors.verifyCode" id="register-code-error" class="field-error">{{ errors.verifyCode }}</p>

        <label for="register-password">密码</label>
        <input id="register-password" v-model="form.password" data-test="password" type="password" autocomplete="new-password" placeholder="8–64 位，且包含字母和数字" :aria-invalid="!!errors.password" :aria-describedby="errors.password ? 'register-password-error' : undefined" @input="errors.password = ''">
        <p v-if="errors.password" id="register-password-error" class="field-error">{{ errors.password }}</p>

        <label for="register-confirm-password">确认密码</label>
        <input id="register-confirm-password" v-model="form.confirmPassword" data-test="confirm-password" type="password" autocomplete="new-password" placeholder="请再次输入密码" :aria-invalid="!!errors.confirmPassword" :aria-describedby="errors.confirmPassword ? 'register-confirm-error' : undefined" @input="errors.confirmPassword = ''">
        <p v-if="errors.confirmPassword" id="register-confirm-error" class="field-error">{{ errors.confirmPassword }}</p>

        <p v-if="submitError" id="register-submit-error" class="field-error" role="alert">{{ submitError }}</p>
        <button class="auth-submit" type="submit" :disabled="submitting">{{ submitting ? '注册中…' : '注册' }}</button>
      </form>

      <RouterLink class="login-link" :to="{ name: 'login' }">已有账号？返回登录</RouterLink>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import BrandMark from '@/components/common/BrandMark.vue'
import { register, sendRegisterCode } from '@/api/user'

const phonePattern = /^1[3-9]\d{9}$/
const codePattern = /^\d{6}$/
const passwordPattern = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/
const router = useRouter()
const form = reactive({ phone: '', verifyCode: '', password: '', confirmPassword: '' })
const errors = reactive({ phone: '', verifyCode: '', password: '', confirmPassword: '' })
const submitError = ref('')
const cooldown = ref(0)
const sending = ref(false)
const submitting = ref(false)
let timer: ReturnType<typeof setInterval> | undefined

function validatePhone() {
  errors.phone = phonePattern.test(form.phone) ? '' : '请输入有效的中国大陆手机号'
  return !errors.phone
}

function validateForm() {
  const phoneValid = validatePhone()
  errors.verifyCode = codePattern.test(form.verifyCode) ? '' : '请输入 6 位数字验证码'
  errors.password = passwordPattern.test(form.password) ? '' : '密码需为 8–64 位，并同时包含字母和数字'
  errors.confirmPassword = form.confirmPassword === form.password ? '' : '两次输入的密码不一致'
  return phoneValid && !errors.verifyCode && !errors.password && !errors.confirmPassword
}

function startCooldown() {
  cooldown.value = 60
  timer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0 && timer) {
      clearInterval(timer)
      timer = undefined
    }
  }, 1000)
}

async function requestCode() {
  if (!validatePhone() || cooldown.value) return
  sending.value = true
  errors.verifyCode = ''
  try {
    await sendRegisterCode(form.phone)
    startCooldown()
  } catch (error) {
    errors.verifyCode = error instanceof Error ? error.message : '验证码发送失败，请稍后重试'
  } finally {
    sending.value = false
  }
}

async function submit() {
  submitError.value = ''
  if (!validateForm()) return
  submitting.value = true
  try {
    await register({ ...form })
    await router.push({ name: 'login', query: { phone: form.phone, registered: '1' } })
  } catch (error) {
    submitError.value = error instanceof Error ? error.message : '注册失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.auth-view{max-width:460px;margin:clamp(20px,8vw,72px) auto;padding:0 16px}.auth-view__brand{display:inline-flex;align-items:center;gap:8px;color:var(--color-primary);font-size:1.12rem;font-weight:800}.auth-panel{margin-top:28px;padding:clamp(22px,5vw,38px);background:var(--color-surface);border:1px solid var(--color-border);border-radius:var(--radius-card);box-shadow:0 16px 34px rgba(91,48,26,.08)}.auth-panel__kicker{margin:0;color:var(--color-muted);font-size:.9rem}.auth-panel h1{margin:8px 0 28px;font-size:1.85rem}.auth-panel form{display:grid;gap:8px}.auth-panel label{font-weight:700}.auth-panel input{width:100%;min-height:46px;padding:10px 12px;border:1px solid var(--color-border);border-radius:var(--radius-control);background:#fff;font:inherit}.code-field{display:grid;grid-template-columns:1fr auto;gap:10px;align-items:end}.code-field>div{display:grid;gap:8px}.code-field button,.auth-submit{min-height:46px;padding:10px 14px;border:0;border-radius:var(--radius-control);background:#fff0ea;color:var(--color-primary);font:inherit;font-weight:700;cursor:pointer}.auth-submit{margin-top:10px;background:var(--color-primary);color:#fff}.auth-submit:disabled,.code-field button:disabled{opacity:.6;cursor:not-allowed}.field-error{margin:0;color:#b42318;font-size:.84rem}.login-link{display:inline-flex;min-height:44px;align-items:center;margin-top:14px;color:var(--color-primary);font-weight:700}@media(max-width:420px){.code-field{grid-template-columns:1fr}.code-field button{width:100%}}
</style>

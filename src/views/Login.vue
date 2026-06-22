<template>
  <div class="login-container">
    <div class="login-box">
      <h1>外卖平台管理后台</h1>
      <p class="subtitle">仅限管理员登录</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入管理员用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <p v-if="errorMsg" style="color: #f56c6c; text-align: center; font-size: 13px;">
        {{ errorMsg }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (loading.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  errorMsg.value = ''
  loading.value = true

  try {
    const result = await userStore.handleLogin(form.username, form.password)
    if (!result.success) {
      errorMsg.value = result.message
      ElMessage.error(result.message)
    }
  } catch (e) {
    errorMsg.value = '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-container style="height: 100vh">
    <!-- 侧边栏 -->
    <el-aside width="220px" style="background: #304156; overflow: hidden">
      <div class="sidebar-header">
        <div class="logo-icon">🍔</div>
        <span class="logo-text">外卖管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
        style="border-right: none"
      >
        <el-menu-item index="/admin/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>

        <el-menu-item index="/admin/users">
          <el-icon><UserFilled /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/merchants">
          <el-icon><Shop /></el-icon>
          <span>商家管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/riders">
          <el-icon><Van /></el-icon>
          <span>骑手管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/qualifications">
          <el-icon><Checked /></el-icon>
          <span>资质审核</span>
        </el-menu-item>

        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon>
          <span>全平台订单</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header style="height: 56px; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,0.08); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; z-index: 10">
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-icon size="18"><Fold /></el-icon>
          <span style="font-size: 15px; color: #606266;">{{ currentTitle }}</span>
        </div>

        <div style="display: flex; align-items: center; gap: 16px;">
          <el-tag type="danger" size="small" effect="dark">管理员</el-tag>
          <el-button type="danger" text size="small" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main style="background: #f5f7fa; padding: 24px; overflow-y: auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  DataAnalysis, UserFilled, Shop, Van, Checked, List, Fold, SwitchButton
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '管理后台')

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.handleLogout()
  } catch {
    // 用户取消
  }
}
</script>

<style scoped>
.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
}
</style>

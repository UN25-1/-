<template>
  <div>
    <!-- 搜索与筛选 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <el-row :gutter="16" style="align-items: center;">
        <el-col :span="6">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名"
            clearable
            :prefix-icon="Search"
            @clear="fetchUsers"
            @keyup.enter="fetchUsers"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchUsers">
            <el-option label="全部" value="" />
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
            <el-option label="待审核" :value="2" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="fetchUsers">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="userList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ ROLE_MAP[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="togglingId === row.id"
              :before-change="() => handleToggle(row)"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确定禁用该用户？"
              v-if="row.status === 1"
              @confirm="handleToggle(row)"
            >
              <template #reference>
                <el-button type="danger" size="small" link>禁用</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确定启用该用户？"
              v-else
              @confirm="handleToggle(row)"
            >
              <template #reference>
                <el-button type="success" size="small" link>启用</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ROLE_MAP } from '@/utils/format'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const statusFilter = ref('')
const userList = ref([])
const loading = ref(false)
const togglingId = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function fetchUsers() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: keyword.value || ''
    }
    // 只在有值时传 status，避免空字符串导致后端类型转换异常
    if (statusFilter.value !== '' && statusFilter.value !== null) {
      params.status = statusFilter.value
    }
    const { data } = await request.get('/api/admin/users', { params })
    // 兼容分页和列表两种返回格式
    if (data.content) {
      userList.value = data.content
      total.value = data.totalElements || 0
    } else {
      userList.value = Array.isArray(data) ? data : []
      total.value = userList.value.length
    }
  } catch (e) {
    userList.value = []
  } finally {
    loading.value = false
  }
}

async function handleToggle(row) {
  const newStatus = row.status === 1 ? 0 : 1

  // 乐观更新
  const oldStatus = row.status
  row.status = newStatus
  togglingId.value = row.id

  try {
    await request.put(`/api/admin/users/${row.id}/status`, { status: newStatus })
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch (e) {
    // 回滚
    row.status = oldStatus
  } finally {
    togglingId.value = null
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<template>
  <div>
    <!-- 搜索与筛选 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <el-row :gutter="16" style="align-items: center;">
        <el-col :span="6">
          <el-input
            v-model="keyword"
            placeholder="搜索店铺名"
            clearable
            :prefix-icon="Search"
            @clear="fetchMerchants"
            @keyup.enter="fetchMerchants"
          />
        </el-col>
        <el-col :span="4">
          <el-select v-model="enabledFilter" placeholder="状态筛选" clearable @change="fetchMerchants">
            <el-option label="全部" value="" />
            <el-option label="已启用" :value="true" />
            <el-option label="已禁用" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="fetchMerchants">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="merchantList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" />
        <el-table-column prop="shopPhone" label="联系电话" min-width="140">
          <template #default="{ row }">{{ row.shopPhone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.rating != null">
              ⭐ {{ Number(row.rating).toFixed(1) }}
            </span>
            <span v-else style="color: #c0c4cc;">暂无</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="订单数" width="100" align="center">
          <template #default="{ row }">{{ row.totalOrders || 0 }}</template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === true"
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
              title="确定禁用该商家？"
              v-if="row.enabled === true"
              @confirm="handleToggle(row)"
            >
              <template #reference>
                <el-button type="danger" size="small" link>禁用</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确定启用该商家？"
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
          @size-change="fetchMerchants"
          @current-change="fetchMerchants"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const enabledFilter = ref('')
const merchantList = ref([])
const loading = ref(false)
const togglingId = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function fetchMerchants() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: keyword.value || ''
    }
    if (enabledFilter.value !== '' && enabledFilter.value !== null) {
      params.enabled = enabledFilter.value
    }
    const { data } = await request.get('/api/admin/merchants', { params })
    if (data.content) {
      merchantList.value = data.content
      total.value = data.totalElements || 0
    } else {
      merchantList.value = Array.isArray(data) ? data : []
      total.value = merchantList.value.length
    }
  } catch (e) {
    merchantList.value = []
  } finally {
    loading.value = false
  }
}

async function handleToggle(row) {
  const newEnabled = !(row.enabled === true)
  const oldEnabled = row.enabled
  row.enabled = newEnabled
  togglingId.value = row.id

  try {
    await request.put(`/api/admin/merchants/${row.id}/status`, { enabled: newEnabled })
    ElMessage.success(newEnabled ? '已启用' : '已禁用')
  } catch (e) {
    row.enabled = oldEnabled
  } finally {
    togglingId.value = null
  }
}

onMounted(() => {
  fetchMerchants()
})
</script>

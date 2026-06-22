<template>
  <div>
    <!-- 筛选 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <el-row :gutter="16" style="align-items: center;">
        <el-col :span="6">
          <el-select
            v-model="statusFilter"
            placeholder="订单状态筛选"
            clearable
            style="width: 100%"
            @change="fetchOrders"
          >
            <el-option label="全部订单" value="" />
            <el-option
              v-for="s in orderStatuses"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="fetchOrders">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
        </el-col>
        <el-col :span="4" :offset="10" style="text-align: right;">
          <el-button @click="fetchOrders">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 数据表格 -->
    <el-card shadow="never">
      <el-table :data="orderList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="订单号" width="100" fixed="left" />
        <el-table-column prop="orderStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.orderStatus)" size="small">
              {{ getStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="merchantName" label="商家" min-width="140">
          <template #default="{ row }">{{ row.merchantName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户" min-width="120">
          <template #default="{ row }">{{ row.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="120" align="right">
          <template #default="{ row }">
            <span style="color: #f56c6c; font-weight: 600;">
              ¥{{ formatPrice(row.totalAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="viewDetail(row)">
              详情
            </el-button>
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
          @size-change="fetchOrders"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { formatPrice, formatDate, getStatusText, getStatusType } from '@/utils/format'
import { ElMessage } from 'element-plus'

const statusFilter = ref('')
const orderList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const orderStatuses = [
  { label: '待支付', value: 'pending_payment' },
  { label: '待接单', value: 'pending' },
  { label: '已接单', value: 'accepted' },
  { label: '备餐中', value: 'preparing' },
  { label: '待取餐', value: 'prepared' },
  { label: '配送中', value: 'delivering' },
  { label: '已送达', value: 'delivered' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
  { label: '异常', value: 'exception' },
  { label: '已拒收', value: 'rejected' }
]

async function fetchOrders() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const { data } = await request.get('/api/admin/orders', { params })
    if (data.content) {
      orderList.value = data.content
      total.value = data.totalElements || 0
    } else {
      orderList.value = Array.isArray(data) ? data : []
      total.value = orderList.value.length
    }
  } catch (e) {
    orderList.value = []
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  ElMessage.info(`订单 ${row.id} 详情功能待扩展 — 可对接 /api/orders/${row.id}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

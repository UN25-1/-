<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <el-col :span="4" v-for="card in statCards" :key="card.title">
        <div class="stat-card">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value">
            {{ card.value }}<span class="stat-unit">{{ card.unit }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表和排行榜 -->
    <el-row :gutter="20">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">订单状态分布</span>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <span style="font-weight: 600;">🏆 热门商家 Top 5</span>
          </template>
          <div v-if="topMerchants.length === 0" style="text-align: center; color: #909399; padding: 40px 0;">
            暂无数据
          </div>
          <div v-else>
            <div
              v-for="(m, idx) in topMerchants"
              :key="m.merchantId"
              style="display: flex; align-items: center; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f2f2f2;"
            >
              <div style="display: flex; align-items: center; gap: 10px;">
                <span
                  :style="{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    background: idx < 3 ? ['#f56c6c', '#e6a23c', '#409eff'][idx] : '#c0c4cc',
                    color: '#fff',
                    fontSize: '12px',
                    fontWeight: '700'
                  }"
                >
                  {{ idx + 1 }}
                </span>
                <span style="font-size: 14px; color: #303133;">{{ m.shopName }}</span>
              </div>
              <div style="text-align: right;">
                <div style="font-size: 13px; color: #606266;">{{ m.orderCount }} 单</div>
                <div style="font-size: 12px; color: #909399;">¥{{ formatPrice(m.totalRevenue) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import { formatPrice, getStatusText } from '@/utils/format'

const chartRef = ref(null)
let chartInstance = null

const statCards = reactive([
  { title: '总用户数', value: 0, unit: '人', key: 'totalUsers' },
  { title: '总商家数', value: 0, unit: '家', key: 'totalMerchants' },
  { title: '总骑手数', value: 0, unit: '人', key: 'totalRiders' },
  { title: '今日订单', value: 0, unit: '单', key: 'todayOrderCount' },
  { title: '今日GMV', value: '0', unit: '元', key: 'todayGMV' },
  { title: '待处理订单', value: 0, unit: '单', key: 'pendingOrderCount' }
])

const topMerchants = ref([])

async function fetchStats() {
  try {
    const { data } = await request.get('/api/admin/stats')
    statCards[0].value = data.totalUsers || 0
    statCards[1].value = data.totalMerchants || 0
    statCards[2].value = data.totalRiders || 0
    statCards[3].value = data.todayOrderCount || 0
    statCards[4].value = formatPrice(data.todayGMV)
    statCards[5].value = data.pendingOrderCount || 0

    topMerchants.value = data.topMerchants || []

    // 渲染图表
    await nextTick()
    renderChart(data.orderStatusDistribution || {})
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

function renderChart(distribution) {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(chartRef.value)

  const keys = Object.keys(distribution)
  if (keys.length === 0) {
    keys.push('暂无数据')
  }

  const names = keys.map(k => getStatusText(k))
  const values = keys.map(k => distribution[k] || 0)

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: values,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409eff' },
            { offset: 1, color: '#79bbff' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%'
      }
    ]
  })
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  fetchStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})
</script>

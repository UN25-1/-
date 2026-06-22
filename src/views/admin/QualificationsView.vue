<template>
  <div>
    <el-card shadow="never" style="margin-bottom: 16px;">
      <div style="display: flex; align-items: center; justify-content: space-between;">
        <span style="font-size: 16px; font-weight: 600;">资质审核 · 待处理列表</span>
        <el-button type="primary" @click="fetchPending">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </el-card>

    <!-- 审核列表 -->
    <div v-loading="loading">
      <el-empty v-if="pendingList.length === 0 && !loading" description="暂无待审核项目" />

      <div v-for="item in pendingList" :key="item.userId" style="margin-bottom: 16px;">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <div>
                <span style="font-weight: 600;">{{ item.username }}</span>
                <el-tag :type="item.userRole === 'merchant' ? 'warning' : 'success'" size="small" style="margin-left: 8px;">
                  {{ ROLE_MAP[item.userRole] || item.userRole }}
                </el-tag>
                <span style="color: #909399; font-size: 13px; margin-left: 8px;">
                  ID: {{ item.userId }}
                </span>
              </div>
              <span style="color: #909399; font-size: 13px; display: flex; align-items: center; gap: 4px;">
                {{ formatDate(item.createdAt) }}
              </span>
            </div>
          </template>

          <!-- 证照预览 -->
          <div v-if="item.documents && item.documents.length > 0" style="display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 16px;">
            <div v-for="doc in item.documents" :key="doc.id" style="text-align: center;">
              <el-image
                :src="getImageUrl(doc.docUrl)"
                :preview-src-list="[getImageUrl(doc.docUrl)]"
                fit="cover"
                class="doc-image"
                :preview-teleported="true"
                :initial-index="0"
              />
              <div style="font-size: 12px; color: #606266; margin-top: 4px;">
                {{ DOC_TYPE_MAP[doc.docType] || doc.docType }}
              </div>
            </div>
          </div>
          <div v-else style="color: #c0c4cc; font-size: 13px; margin-bottom: 16px;">
            暂未上传证照
          </div>

          <!-- 操作按钮 -->
          <div style="display: flex; gap: 12px;">
            <el-button type="success" @click="handleApprove(item)">
              <el-icon><Check /></el-icon>
              审核通过
            </el-button>
            <el-button type="danger" @click="handleReject(item)">
              <el-icon><Close /></el-icon>
              驳回
            </el-button>
          </div>

          <!-- 驳回记录显示 -->
          <div v-if="item.rejectReason" style="margin-top: 8px; padding: 8px 12px; background: #fef0f0; border-radius: 4px; color: #f56c6c; font-size: 13px;">
            驳回原因：{{ item.rejectReason }}
          </div>
        </el-card>
      </div>
    </div>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="驳回原因" width="480px" :close-on-click-modal="false">
      <el-form>
        <el-form-item label="请输入驳回原因">
          <el-input
            v-model="rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请说明具体驳回原因，以便申请人修改后重新提交"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="confirmReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh, Check, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ROLE_MAP, DOC_TYPE_MAP, formatDate } from '@/utils/format'
import { getImageUrl } from '@/utils/image'
import { ElMessage, ElMessageBox } from 'element-plus'

const pendingList = ref([])
const loading = ref(false)
const actionLoading = ref(false)

const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRejectUser = ref(null)

async function fetchPending() {
  loading.value = true
  try {
    const { data } = await request.get('/api/qualification/admin/pending')
    pendingList.value = Array.isArray(data) ? data : (data.content || [])
  } catch (e) {
    console.error('获取待审核列表失败', e)
    pendingList.value = []
  } finally {
    loading.value = false
  }
}

async function handleApprove(item) {
  try {
    await ElMessageBox.confirm(
      `确定通过 <b>${item.username}</b> (${ROLE_MAP[item.userRole] || item.userRole}) 的资质审核吗？`,
      '确认通过',
      {
        confirmButtonText: '确定通过',
        cancelButtonText: '取消',
        type: 'success',
        dangerouslyUseHTMLString: true
      }
    )
    actionLoading.value = true
    await request.post(`/api/qualification/admin/approve/${item.userId}`)
    ElMessage.success(`${item.username} 审核已通过`)
    // 从列表中移除
    pendingList.value = pendingList.value.filter(i => i.userId !== item.userId)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    actionLoading.value = false
  }
}

function handleReject(item) {
  currentRejectUser.value = item
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  actionLoading.value = true
  try {
    await request.post(`/api/qualification/admin/reject/${currentRejectUser.value.userId}`, {
      reason: rejectReason.value
    })
    ElMessage.success(`${currentRejectUser.value.username} 已驳回`)
    // 更新列表中的驳回记录
    const idx = pendingList.value.findIndex(i => i.userId === currentRejectUser.value.userId)
    if (idx >= 0) {
      pendingList.value[idx].rejectReason = rejectReason.value
    }
    rejectVisible.value = false
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  } finally {
    actionLoading.value = false
  }
}

onMounted(() => {
  fetchPending()
})
</script>

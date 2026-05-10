<script setup>
import { onMounted, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDailySummary, getDietRecords, getExerciseRecords, deleteDietRecord, deleteExerciseRecord, getWeeklyTrend } from '../api/health'

const router = useRouter()
const today = new Date().toISOString().slice(0, 10)

const selectedDate = ref(today)
const summary = ref(null)
const dietRecords = ref([])
const exerciseRecords = ref([])
const loading = ref(false)
const noProfile = ref(false)
const chartRef = ref(null)
let chartInstance = null

const MEAL_LABEL = { BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }
const INTENSITY_LABEL = { LOW: '低强度', MODERATE: '中强度', HIGH: '高强度' }

function isToday() {
  return selectedDate.value === today
}

function dateLabel() {
  return isToday() ? '今日' : selectedDate.value
}

async function loadAll() {
  loading.value = true
  noProfile.value = false
  chartInstance = null
  let chartNeeded = false
  try {
    const date = selectedDate.value
    const [summaryRes, dietRes, exerciseRes] = await Promise.all([
      getDailySummary(date),
      getDietRecords(date),
      getExerciseRecords(date),
    ])
    if (summaryRes.data.success) {
      summary.value = summaryRes.data.data
    } else if (summaryRes.data.message?.includes('健康档案')) {
      noProfile.value = true
    } else {
      ElMessage.error(summaryRes.data.message || '加载失败')
    }
    dietRecords.value = dietRes.data.success ? (dietRes.data.data || []) : []
    exerciseRecords.value = exerciseRes.data.success ? (exerciseRes.data.data || []) : []
    chartNeeded = !noProfile.value
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
  // 必须在 loading=false、DOM 恢复后再初始化图表
  if (chartNeeded) {
    await loadTrendChart()
  }
}

async function loadTrendChart() {
  try {
    const { data } = await getWeeklyTrend()
    if (!data.success || !data.data?.length) return
    const trend = data.data
    await nextTick()
    if (!chartRef.value) return
    const existing = echarts.getInstanceByDom(chartRef.value)
    if (existing) {
      chartInstance = existing
    } else {
      chartInstance = echarts.init(chartRef.value)
    }
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['摄入热量', '总消耗'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: trend.map(d => d.date.slice(5)),
        axisLabel: { fontSize: 11 },
      },
      yAxis: { type: 'value', name: 'kcal', axisLabel: { fontSize: 11 } },
      series: [
        {
          name: '摄入热量',
          type: 'line',
          smooth: true,
          data: trend.map(d => Number(d.intakeCalories).toFixed(1)),
          itemStyle: { color: '#f59e0b' },
          areaStyle: { opacity: 0.1 },
        },
        {
          name: '总消耗',
          type: 'line',
          smooth: true,
          data: trend.map(d => Number(d.totalBurnedCalories).toFixed(1)),
          itemStyle: { color: '#3b82f6' },
          areaStyle: { opacity: 0.1 },
        },
      ],
    })
  } catch {
    // chart is optional
  }
}

async function removeDiet(id) {
  try {
    await deleteDietRecord(id)
    dietRecords.value = dietRecords.value.filter(r => r.id !== id)
    const { data } = await getDailySummary(selectedDate.value)
    if (data.success) summary.value = data.data
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function removeExercise(id) {
  try {
    await deleteExerciseRecord(id)
    exerciseRecords.value = exerciseRecords.value.filter(r => r.id !== id)
    const { data } = await getDailySummary(selectedDate.value)
    if (data.success) summary.value = data.data
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

watch(selectedDate, loadAll)
onMounted(loadAll)
</script>

<template>
  <!-- 标题 + 日期选择 -->
  <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; flex-wrap:wrap; gap:12px;">
    <h2 class="page-title" style="margin:0;">数据总览</h2>
    <div style="display:flex; align-items:center; gap:8px;">
      <span style="font-size:13px; color:#6b7280;">选择日期：</span>
      <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        :disabled-date="d => d > new Date()"
        style="width:160px;"
        size="small"
      />
      <el-button size="small" @click="selectedDate = today" :type="isToday() ? 'primary' : 'default'">
        今天
      </el-button>
    </div>
  </div>

  <el-empty v-if="noProfile" description="您还没有创建健康档案，请先完善档案信息">
    <el-button type="primary" @click="router.push('/profile')">去创建健康档案</el-button>
  </el-empty>

  <div v-else-if="loading" v-loading="true" style="height: 200px;" />

  <template v-else>
    <!-- 热量概览卡片 -->
    <div class="stats-grid" style="margin-bottom: 20px;">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-label">{{ dateLabel() }}摄入</div>
        <div class="stat-value">
          {{ summary?.intakeCalories ?? '-' }}<small>kcal</small>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-label">基础代谢</div>
        <div class="stat-value">
          {{ summary ? Number(summary.bmrCalories).toFixed(0) : '-' }}<small>kcal</small>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-label">{{ dateLabel() }}总消耗</div>
        <div class="stat-value">
          {{ summary?.totalBurnedCalories ?? '-' }}<small>kcal</small>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-label">热量差值</div>
        <div class="stat-value" :class="summary && summary.calorieBalance >= 0 ? 'text-success' : 'text-danger'">
          <template v-if="summary">
            {{ summary.calorieBalance >= 0 ? '+' : '' }}{{ summary.calorieBalance }}
            <small>kcal</small>
          </template>
          <template v-else>-</template>
        </div>
      </el-card>
    </div>

    <!-- 结果提示 -->
    <el-alert
      v-if="summary"
      :type="summary.calorieBalance > 0 ? 'success' : summary.calorieBalance === 0 ? 'info' : 'warning'"
      :title="summary.status"
      :description="summary.calorieBalance > 0
        ? `${dateLabel()}处于热量缺口状态，有助于减脂。`
        : summary.calorieBalance === 0
          ? '摄入与消耗基本平衡，保持继续。'
          : `${dateLabel()}摄入高于消耗，建议适当控制饮食或增加运动。`"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />
    <el-alert
      v-else-if="!loading"
      type="info"
      :title="`${dateLabel()}暂无打卡记录`"
      :description="isToday() ? '去「每日打卡」记录今天的饮食和运动吧！' : '该日期没有打卡数据。'"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    />

    <!-- 7天趋势图 -->
    <el-card shadow="hover" style="margin-bottom: 20px;">
      <template #header>
        <span style="font-weight:600;">近 7 天热量趋势</span>
      </template>
      <div ref="chartRef" style="width:100%; height:240px;" />
    </el-card>

    <!-- 饮食 + 运动记录 -->
    <div class="grid-2">
      <!-- 饮食记录 -->
      <el-card shadow="hover">
        <template #header>
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span style="font-weight:600;">{{ dateLabel() }}饮食</span>
            <el-button v-if="isToday()" type="primary" link @click="router.push('/checkin')">去打卡</el-button>
          </div>
        </template>
        <el-empty v-if="dietRecords.length === 0" :description="`${dateLabel()}还未记录饮食`" :image-size="60" />
        <el-table v-else :data="dietRecords" size="small">
          <el-table-column label="餐次" width="60">
            <template #default="{ row }">{{ MEAL_LABEL[row.mealType] || row.mealType }}</template>
          </el-table-column>
          <el-table-column prop="foodName" label="食物" show-overflow-tooltip />
          <el-table-column prop="amount" label="份量" width="70" />
          <el-table-column label="热量" width="85">
            <template #default="{ row }">{{ row.calories }} kcal</template>
          </el-table-column>
          <el-table-column v-if="isToday()" label="操作" width="55">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="removeDiet(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="dietRecords.length > 0" style="margin-top:10px; text-align:right; font-size:13px; color:#6b7280;">
          合计 {{ dietRecords.reduce((s, r) => s + Number(r.calories), 0).toFixed(1) }} kcal
        </div>
      </el-card>

      <!-- 运动记录 -->
      <el-card shadow="hover">
        <template #header>
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span style="font-weight:600;">{{ dateLabel() }}运动</span>
            <el-button v-if="isToday()" type="primary" link @click="router.push('/checkin')">去打卡</el-button>
          </div>
        </template>
        <el-empty v-if="exerciseRecords.length === 0" :description="`${dateLabel()}还未记录运动`" :image-size="60" />
        <el-table v-else :data="exerciseRecords" size="small">
          <el-table-column prop="exerciseType" label="类型" show-overflow-tooltip />
          <el-table-column label="时长" width="70">
            <template #default="{ row }">{{ row.durationMinutes }}min</template>
          </el-table-column>
          <el-table-column label="强度" width="70">
            <template #default="{ row }">{{ INTENSITY_LABEL[row.intensity] || row.intensity }}</template>
          </el-table-column>
          <el-table-column label="消耗" width="85">
            <template #default="{ row }">{{ row.caloriesBurned }} kcal</template>
          </el-table-column>
          <el-table-column v-if="isToday()" label="操作" width="55">
            <template #default="{ row }">
              <el-button type="danger" link size="small" @click="removeExercise(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="exerciseRecords.length > 0" style="margin-top:10px; text-align:right; font-size:13px; color:#6b7280;">
          {{ exerciseRecords.reduce((s, r) => s + Number(r.durationMinutes), 0) }} 分钟 ·
          消耗 {{ exerciseRecords.reduce((s, r) => s + Number(r.caloriesBurned), 0).toFixed(1) }} kcal
        </div>
      </el-card>
    </div>
  </template>
</template>

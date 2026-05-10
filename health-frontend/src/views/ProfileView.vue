<script setup>
import { onMounted, onUnmounted, reactive, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getProfile, saveProfile, getWeights, addWeight } from '../api/health'
import { ACTIVITY_LEVELS, HEALTH_GOALS } from '../constants/options'

const formRef = ref(null)
const saving = ref(false)
const form = reactive({
  gender: 'FEMALE',
  age: 20,
  heightCm: 165,
  weightKg: 60,
  targetWeightKg: 55,
  activityLevel: 'LIGHT',
  goal: 'FAT_LOSS',
  bmi: null,
  bmr: null,
  recommendedCalories: null,
})

const rules = {
  age: [{ required: true, type: 'number', min: 1, max: 120, message: '请输入 1-120 的年龄', trigger: 'blur' }],
  heightCm: [{ required: true, type: 'number', min: 50, max: 250, message: '请输入 50-250 cm', trigger: 'blur' }],
  weightKg: [{ required: true, type: 'number', min: 10, max: 300, message: '请输入 10-300 kg', trigger: 'blur' }],
}

// Weight records
const weightRecords = ref([])
const weightForm = reactive({ weightKg: 60, recordDate: new Date().toISOString().slice(0, 10), remark: '' })
const addingWeight = ref(false)
const weightChartRef = ref(null)
let weightChart = null

async function loadProfile() {
  try {
    const { data } = await getProfile()
    if (data.success && data.data) {
      Object.assign(form, data.data)
    }
  } catch { /* 未创建档案时静默 */ }
}

async function loadWeights() {
  try {
    const { data } = await getWeights()
    if (data.success) {
      weightRecords.value = data.data || []
      await nextTick()
      renderWeightChart()
    }
  } catch { /* ignore */ }
}

function renderWeightChart() {
  if (!weightChartRef.value || weightRecords.value.length === 0) return
  if (!weightChart) {
    weightChart = echarts.init(weightChartRef.value)
  }
  const sorted = [...weightRecords.value].sort((a, b) => a.recordDate.localeCompare(b.recordDate))
  const target = form.targetWeightKg != null ? Number(form.targetWeightKg) : null
  weightChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter(params) {
        const p = Array.isArray(params) ? params[0] : params
        const row = sorted[p.dataIndex]
        if (!row) return ''
        let html = `${row.recordDate}<br/>${p.marker}<span style="font-weight:600">${p.seriesName}</span> ${p.value} kg`
        if (row.remark) html += `<br/><span style="color:#909399;">备注：${row.remark}</span>`
        return html
      },
    },
    grid: { left: '3%', right: '4%', bottom: '3%', top: 28, containLabel: true },
    xAxis: {
      type: 'category',
      data: sorted.map(r => r.recordDate.slice(5)),
      axisLabel: { fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      name: 'kg',
      scale: true,
      axisLabel: { fontSize: 11 },
    },
    series: [{
      name: '体重',
      type: 'line',
      smooth: sorted.length > 2,
      symbol: 'circle',
      symbolSize: sorted.length === 1 ? 10 : 6,
      showSymbol: true,
      data: sorted.map(r => Number(r.weightKg)),
      itemStyle: { color: '#10b981' },
      lineStyle: { width: 2 },
      areaStyle: { opacity: 0.12 },
      markLine: target != null && !Number.isNaN(target)
        ? {
            silent: true,
            data: [{ yAxis: target, name: '目标', lineStyle: { color: '#f59e0b', type: 'dashed', width: 1.5 } }],
            label: { formatter: `目标 ${target} kg`, fontSize: 11, color: '#d97706' },
          }
        : { data: [] },
    }],
  })
}

function resizeWeightChart() {
  weightChart?.resize()
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const { data } = await saveProfile(form)
    if (data.success) {
      Object.assign(form, data.data)
      await nextTick()
      renderWeightChart()
      ElMessage.success('健康档案已保存')
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function recordDateKey(d) {
  return String(d ?? '').slice(0, 10)
}

async function handleAddWeight() {
  if (!weightForm.weightKg || weightForm.weightKg < 10) {
    ElMessage.warning('请输入有效体重')
    return
  }
  const dayKey = recordDateKey(weightForm.recordDate)
  if (!dayKey || dayKey.length < 10) {
    ElMessage.warning('请选择记录日期')
    return
  }
  const sameDay = weightRecords.value.some(r => recordDateKey(r.recordDate) === dayKey)
  addingWeight.value = true
  try {
    const payload = {
      weightKg: Number(weightForm.weightKg),
      recordDate: dayKey,
      remark: weightForm.remark ?? '',
    }
    const { data } = await addWeight(payload)
    if (data.success) {
      const row = data.data
      const key = recordDateKey(row.recordDate)
      const idx = weightRecords.value.findIndex(r => recordDateKey(r.recordDate) === key)
      if (idx >= 0) weightRecords.value[idx] = row
      else weightRecords.value.push(row)
      await nextTick()
      renderWeightChart()
      ElMessage.success(sameDay ? '已更新当日体重' : '体重已记录')
      weightForm.remark = ''
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '记录失败')
  } finally {
    addingWeight.value = false
  }
}

function bmiCategory(bmi) {
  if (!bmi) return ''
  const v = Number(bmi)
  if (v < 18.5) return '偏瘦'
  if (v < 24) return '正常'
  if (v < 28) return '超重'
  return '肥胖'
}

function bmiTagType(bmi) {
  if (!bmi) return 'info'
  const v = Number(bmi)
  if (v < 18.5) return 'warning'
  if (v < 24) return 'success'
  if (v < 28) return 'warning'
  return 'danger'
}

onMounted(async () => {
  await loadProfile()
  await loadWeights()
  window.addEventListener('resize', resizeWeightChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeWeightChart)
  weightChart?.dispose()
  weightChart = null
})
</script>

<template>
  <h2 class="page-title">健康档案</h2>

  <div class="grid-2">
    <!-- 档案表单 -->
    <el-card shadow="hover" class="form-card">
      <template #header><span style="font-weight:600;">基础信息</span></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio-button value="MALE">男</el-radio-button>
            <el-radio-button value="FEMALE">女</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄" prop="age">
          <el-input-number v-model="form.age" :min="1" :max="120" />
          <span class="muted" style="margin-left:8px;">岁</span>
        </el-form-item>
        <el-form-item label="身高" prop="heightCm">
          <el-input-number v-model="form.heightCm" :min="50" :max="250" :precision="1" />
          <span class="muted" style="margin-left:8px;">cm</span>
        </el-form-item>
        <el-form-item label="当前体重" prop="weightKg">
          <el-input-number v-model="form.weightKg" :min="10" :max="300" :precision="1" />
          <span class="muted" style="margin-left:8px;">kg</span>
        </el-form-item>
        <el-form-item label="目标体重">
          <el-input-number v-model="form.targetWeightKg" :min="10" :max="300" :precision="1" />
          <span class="muted" style="margin-left:8px;">kg</span>
        </el-form-item>
        <el-form-item label="活动水平">
          <el-select v-model="form.activityLevel">
            <el-option v-for="item in ACTIVITY_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康目标">
          <el-select v-model="form.goal">
            <el-option v-for="item in HEALTH_GOALS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存档案</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div style="display:flex; gap:20px; flex-wrap:wrap;">
        <div style="flex:1; min-width:120px; text-align:center;">
          <div class="muted" style="font-size:12px; margin-bottom:4px;">BMI</div>
          <div style="font-size:22px; font-weight:700;">{{ form.bmi ?? '-' }}</div>
          <el-tag v-if="form.bmi" :type="bmiTagType(form.bmi)" size="small" style="margin-top:4px;">
            {{ bmiCategory(form.bmi) }}
          </el-tag>
        </div>
        <div style="flex:1; min-width:120px; text-align:center;">
          <div class="muted" style="font-size:12px; margin-bottom:4px;">基础代谢</div>
          <div style="font-size:22px; font-weight:700;">{{ form.bmr ?? '-' }}</div>
          <div class="muted" style="font-size:11px;">kcal/天</div>
        </div>
        <div style="flex:1; min-width:120px; text-align:center;">
          <div class="muted" style="font-size:12px; margin-bottom:4px;">推荐每日摄入</div>
          <div style="font-size:22px; font-weight:700;">{{ form.recommendedCalories ?? '-' }}</div>
          <div class="muted" style="font-size:11px;">kcal/天</div>
        </div>
      </div>
    </el-card>

    <!-- 体重记录 -->
    <el-card shadow="hover">
      <template #header><span style="font-weight:600;">体重记录</span></template>

      <!-- 添加体重 -->
      <div style="display:flex; gap:10px; align-items:flex-end; flex-wrap:wrap; margin-bottom:16px;">
        <el-form-item label="体重" style="margin:0;">
          <el-input-number v-model="weightForm.weightKg" :min="10" :max="300" :precision="1" style="width:130px;" />
          <span class="muted" style="margin-left:6px;">kg</span>
        </el-form-item>
        <el-form-item label="日期" style="margin:0;">
          <el-date-picker v-model="weightForm.recordDate" value-format="YYYY-MM-DD" style="width:140px;" />
        </el-form-item>
        <el-button type="primary" :loading="addingWeight" @click="handleAddWeight">记录体重</el-button>
      </div>

      <!-- 体重趋势图 -->
      <template v-if="weightRecords.length > 0">
        <div class="muted" style="font-size:12px; font-weight:500; margin-bottom:6px;">体重趋势</div>
        <div ref="weightChartRef" style="width:100%; height:240px; margin-bottom:16px;" />
      </template>
      <el-empty v-else description="暂无体重记录" :image-size="50" style="margin-bottom:10px;" />

      <!-- 体重列表 -->
      <el-table
        v-if="weightRecords.length > 0"
        :data="[...weightRecords].sort((a,b) => b.recordDate.localeCompare(a.recordDate)).slice(0, 10)"
        size="small"
        max-height="200"
      >
        <el-table-column prop="recordDate" label="日期" width="110" />
        <el-table-column label="体重" width="90">
          <template #default="{ row }">{{ row.weightKg }} kg</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
      <p v-if="weightRecords.length > 10" class="muted" style="font-size:12px; margin-top:6px;">
        仅显示最近 10 条记录
      </p>
    </el-card>
  </div>
</template>

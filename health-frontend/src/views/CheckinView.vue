<script setup>
import { reactive, ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { createDietRecord, createExerciseRecord, searchFoodCalories, recognizeFoodFromImage } from '../api/health'
import { MEAL_TYPES, EXERCISE_INTENSITIES } from '../constants/options'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const today = new Date().toISOString().slice(0, 10)
const uploadHeaders = { Authorization: `Bearer ${userStore.token}` }

const dietRef = ref(null)
const exerciseRef = ref(null)
const savingDiet = ref(false)
const savingExercise = ref(false)
const foodCalorieHint = ref('')
const recognizingFood = ref(false)
const visionDialogVisible = ref(false)
const visionSuggestions = ref([])
let foodSearchTimer = null

const diet = reactive({
  recordDate: today,
  mealType: 'BREAKFAST',
  foodName: '',
  amount: '',
  calories: 0,
  imageUrl: '',
  remark: '',
})

const exercise = reactive({
  recordDate: today,
  exerciseType: '',
  durationMinutes: 30,
  intensity: 'MODERATE',
  caloriesBurned: 0,
  imageUrl: '',
  remark: '',
})

const dietRules = {
  foodName: [{ required: true, message: '请输入食物名称', trigger: 'blur' }],
  calories: [{ required: true, type: 'number', min: 0, message: '请输入热量', trigger: 'blur' }],
}

const exerciseRules = {
  exerciseType: [{ required: true, message: '请输入运动类型', trigger: 'blur' }],
  durationMinutes: [{ required: true, type: 'number', min: 1, message: '时长至少 1 分钟', trigger: 'blur' }],
  caloriesBurned: [{ required: true, type: 'number', min: 0, message: '请输入消耗热量', trigger: 'blur' }],
}

function fetchFoodSuggestions(queryString, callback) {
  const q = (queryString || '').trim()
  if (!q) {
    callback([])
    return
  }
  clearTimeout(foodSearchTimer)
  foodSearchTimer = setTimeout(async () => {
    try {
      const { data } = await searchFoodCalories({ keyword: q, limit: 20 })
      if (data.success && Array.isArray(data.data)) {
        callback(
          data.data.map((r) => ({
            value: r.name,
            caloriesPerUnit: Number(r.caloriesPerUnit),
            unitLabel: r.unitLabel,
            category: r.category,
          })),
        )
      } else {
        callback([])
      }
    } catch {
      callback([])
    }
  }, 280)
}

function onDietFoodFocus() {
  foodCalorieHint.value = ''
}

function onDietFoodSelect(item) {
  diet.foodName = item.value
  diet.calories = item.caloriesPerUnit
  foodCalorieHint.value = `参考热量：约 ${item.caloriesPerUnit} kcal / ${item.unitLabel}（估算值，可按实际修改）`
  nextTick(() => dietRef.value?.clearValidate(['foodName', 'calories']))
}

function handleDietUpload(response) {
  if (response.success && response.data?.url) {
    diet.imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

async function runFoodVisionRecognize() {
  if (!diet.imageUrl) {
    ElMessage.warning('请先上传餐食图片')
    return
  }
  recognizingFood.value = true
  visionSuggestions.value = []
  try {
    const { data } = await recognizeFoodFromImage(diet.imageUrl)
    if (data.success && Array.isArray(data.data)) {
      visionSuggestions.value = data.data
      visionDialogVisible.value = true
      if (data.data.length === 0) {
        ElMessage.info('未返回识别条目，请手填热量')
      }
    } else {
      ElMessage.error(data.message || '识别失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '识别失败')
  } finally {
    recognizingFood.value = false
  }
}

function applyVisionSuggestion(row) {
  if (!row) return
  diet.foodName = row.name || ''
  const kcal = Number(row.estimatedTotalKcal)
  diet.calories = Number.isFinite(kcal) && kcal >= 0 ? kcal : 0
  const basis = row.basis ? `依据：${row.basis}` : ''
  const note = row.note ? `说明：${row.note}` : ''
  foodCalorieHint.value = ['云 API 估算（请核对后再保存）', basis, note].filter(Boolean).join('；')
  visionDialogVisible.value = false
  nextTick(() => dietRef.value?.clearValidate(['foodName', 'calories']))
}

function handleExerciseUpload(response) {
  if (response.success && response.data?.url) {
    exercise.imageUrl = response.data.url
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

function handleUploadError() {
  ElMessage.error('上传失败，请确认已登录且后端服务正在运行')
}

async function saveDiet() {
  await dietRef.value.validate()
  savingDiet.value = true
  try {
    const { data } = await createDietRecord(diet)
    if (data.success) {
      ElMessage.success('饮食记录已保存')
      diet.foodName = ''
      diet.amount = ''
      diet.calories = 0
      diet.imageUrl = ''
      diet.remark = ''
      foodCalorieHint.value = ''
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingDiet.value = false
  }
}

async function saveExercise() {
  await exerciseRef.value.validate()
  savingExercise.value = true
  try {
    const { data } = await createExerciseRecord(exercise)
    if (data.success) {
      ElMessage.success('运动记录已保存')
      exercise.exerciseType = ''
      exercise.durationMinutes = 30
      exercise.caloriesBurned = 0
      exercise.imageUrl = ''
      exercise.remark = ''
    } else {
      ElMessage.error(data.message)
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    savingExercise.value = false
  }
}
</script>

<template>
  <h2 class="page-title">每日打卡</h2>
  <div class="grid">
    <el-card shadow="hover">
      <h3>饮食记录</h3>
      <el-form ref="dietRef" :model="diet" :rules="dietRules" label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="diet.recordDate" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="餐次">
          <el-select v-model="diet.mealType">
            <el-option v-for="item in MEAL_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="食物" prop="foodName">
          <el-autocomplete
            v-model="diet.foodName"
            class="food-autocomplete"
            :fetch-suggestions="fetchFoodSuggestions"
            clearable
            placeholder="输入关键词从热量库搜索，或直接手填"
            value-key="value"
            @select="onDietFoodSelect"
            @focus="onDietFoodFocus"
          >
            <template #default="{ item }">
              <div class="food-suggest-row">
                <span class="food-suggest-name">{{ item.value }}</span>
                <span v-if="item.category" class="muted food-suggest-meta">{{ item.category }}</span>
                <span class="muted food-suggest-kcal">{{ item.caloriesPerUnit }} kcal / {{ item.unitLabel }}</span>
              </div>
            </template>
          </el-autocomplete>
        </el-form-item>
        <el-form-item label="份量">
          <el-input v-model="diet.amount" placeholder="如与库中单位一致可填 100g；实际份量用于自己区分" clearable />
        </el-form-item>
        <el-form-item label="热量" prop="calories">
          <el-input-number v-model="diet.calories" :min="0" :precision="1" />
          <span class="muted" style="margin-left:8px;">kcal</span>
          <p v-if="foodCalorieHint" class="muted food-calorie-hint">{{ foodCalorieHint }}</p>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="diet.remark" clearable />
        </el-form-item>
        <el-form-item label="图片">
          <div style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
            <el-upload
              action="/api/uploads/images"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleDietUpload"
              :on-error="handleUploadError"
              accept="image/jpeg,image/png,image/webp"
            >
              <el-button>{{ diet.imageUrl ? '重新上传' : '上传饮食图片' }}</el-button>
            </el-upload>
            <el-button
              v-if="diet.imageUrl"
              type="primary"
              plain
              :loading="recognizingFood"
              @click="runFoodVisionRecognize"
            >
              云 API 识别热量
            </el-button>
            <el-image
              v-if="diet.imageUrl"
              :src="diet.imageUrl"
              style="width:80px; height:80px; border-radius:6px;"
              fit="cover"
              :preview-src-list="[diet.imageUrl]"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingDiet" @click="saveDiet">保存饮食</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="visionDialogVisible" title="识别结果（估算，请核对）" width="min(520px, 92vw)">
      <p class="muted" style="font-size:13px; margin:0 0 12px;">以下为云侧模型返回的参考值，保存前请按需修改食物名称与热量。</p>
      <el-empty v-if="!visionSuggestions.length" description="无候选结果" :image-size="60" />
      <ul v-else class="vision-list">
        <li v-for="(row, idx) in visionSuggestions" :key="idx" class="vision-item">
          <div>
            <strong>{{ row.name || '未命名' }}</strong>
            <span class="muted" style="margin-left:8px;">≈ {{ row.estimatedTotalKcal }} kcal</span>
          </div>
          <p v-if="row.basis" class="muted vision-meta">{{ row.basis }}</p>
          <p v-if="row.note" class="muted vision-meta">{{ row.note }}</p>
          <el-button type="primary" link @click="applyVisionSuggestion(row)">填入表单</el-button>
        </li>
      </ul>
    </el-dialog>

    <el-card shadow="hover">
      <h3>运动记录</h3>
      <el-form ref="exerciseRef" :model="exercise" :rules="exerciseRules" label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="exercise.recordDate" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="类型" prop="exerciseType">
          <el-input v-model="exercise.exerciseType" placeholder="跑步、游泳、力量训练" clearable />
        </el-form-item>
        <el-form-item label="时长" prop="durationMinutes">
          <el-input-number v-model="exercise.durationMinutes" :min="1" />
          <span class="muted" style="margin-left:8px;">分钟</span>
        </el-form-item>
        <el-form-item label="强度">
          <el-select v-model="exercise.intensity">
            <el-option v-for="item in EXERCISE_INTENSITIES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="消耗" prop="caloriesBurned">
          <el-input-number v-model="exercise.caloriesBurned" :min="0" :precision="1" />
          <span class="muted" style="margin-left:8px;">kcal</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="exercise.remark" clearable />
        </el-form-item>
        <el-form-item label="截图">
          <div style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
            <el-upload
              action="/api/uploads/images"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleExerciseUpload"
              :on-error="handleUploadError"
              accept="image/jpeg,image/png,image/webp"
            >
              <el-button>{{ exercise.imageUrl ? '重新上传' : '上传运动截图' }}</el-button>
            </el-upload>
            <el-image
              v-if="exercise.imageUrl"
              :src="exercise.imageUrl"
              style="width:80px; height:80px; border-radius:6px;"
              fit="cover"
              :preview-src-list="[exercise.imageUrl]"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingExercise" @click="saveExercise">保存运动</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.food-autocomplete {
  width: 100%;
  max-width: 420px;
}
.food-suggest-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  line-height: 1.35;
}
.food-suggest-name {
  font-weight: 500;
}
.food-suggest-meta {
  font-size: 12px;
}
.food-suggest-kcal {
  font-size: 12px;
  margin-left: auto;
}
.food-calorie-hint {
  font-size: 12px;
  margin: 6px 0 0;
  line-height: 1.4;
  max-width: 420px;
}
.vision-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.vision-item {
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.vision-item:last-child {
  border-bottom: none;
}
.vision-meta {
  font-size: 12px;
  margin: 4px 0 6px;
  line-height: 1.35;
}
</style>

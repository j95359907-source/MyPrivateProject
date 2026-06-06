<template>
  <div class="strategy-manager">
    <h2>策略管理</h2>

    <!-- 策略模板 -->
    <el-card style="margin-bottom:16px;">
      <template #header>内置策略模板</template>
      <el-row :gutter="16">
        <el-col :span="8" v-for="t in templates" :key="t.type">
          <el-card shadow="hover" :body-style="{ padding: '16px' }" @click="selectTemplate(t)" style="cursor:pointer;">
            <h4>{{ t.name }}</h4>
            <p style="color:#909399; font-size:13px; margin-top:8px;">{{ t.description }}</p>
            <el-tag size="small">{{ t.type }}</el-tag>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 策略参数预览 -->
    <el-card v-if="selected" style="margin-bottom:16px;">
      <template #header>{{ selected.name }} — 默认参数</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item v-for="(val, key) in selected.defaultParams" :key="key" :label="key">
          {{ val }}
        </el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:12px;">
        <el-button type="primary" @click="goBacktest">用此策略回测</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStrategyTemplates, type StrategyTemplate } from '../api/backtests'

const router = useRouter()
const templates = ref<StrategyTemplate[]>([])
const selected = ref<StrategyTemplate | null>(null)

onMounted(async () => {
  try {
    const { data } = await getStrategyTemplates()
    templates.value = data
  } catch (_) { /* */ }
})

function selectTemplate(t: StrategyTemplate) {
  selected.value = t
}

function goBacktest() {
  router.push('/backtest')
}
</script>

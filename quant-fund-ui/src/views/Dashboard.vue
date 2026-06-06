<template>
  <div class="dashboard">
    <h2>量化基金分析平台</h2>

    <!-- 数据概览卡片 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="6">
        <el-card shadow="hover" @click="router.push('/funds')" style="cursor:pointer;">
          <el-statistic title="活跃基金" :value="stats?.activeFunds || 0" />
          <template #footer><span style="font-size:12px;color:#409eff;">进入筛选 →</span></template>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" @click="router.push('/funds')" style="cursor:pointer;">
          <el-statistic title="ETF数量" :value="stats?.etfCount || 0" />
          <template #footer><span style="font-size:12px;color:#409eff;">浏览ETF →</span></template>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" @click="router.push('/backtest')" style="cursor:pointer;">
          <el-statistic title="回测次数" :value="backtestCount" />
          <template #footer><span style="font-size:12px;color:#409eff;">创建回测 →</span></template>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" @click="router.push('/portfolio')" style="cursor:pointer;">
          <el-statistic title="组合数量" :value="portfolioCount" />
          <template #footer><span style="font-size:12px;color:#409eff;">管理组合 →</span></template>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速入口 -->
    <el-row :gutter="16" style="margin-top: 16px;">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>🔍 基金筛选</template>
          <p style="font-size:13px;color:#666;">多维度量化指标筛选优质基金</p>
          <el-link type="primary" @click="router.push('/funds')">立即使用</el-link>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>📈 策略回测</template>
          <p style="font-size:13px;color:#666;">动量/均值回归/双均线，验证历史表现</p>
          <el-link type="primary" @click="router.push('/backtest')">开始回测</el-link>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>📊 组合优化</template>
          <p style="font-size:13px;color:#666;">均值方差/风险平价，构建最优配置</p>
          <el-link type="primary" @click="router.push('/portfolio')">构建组合</el-link>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近回测 -->
    <el-card style="margin-top: 16px;" v-if="recentBacktests.length">
      <template #header>最近回测</template>
      <el-table :data="recentBacktests" size="small" @row-click="(r: any) => router.push(`/backtest/result/${r.id}`)" style="cursor:pointer;">
        <el-table-column prop="runName" label="名称" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="startDate" label="区间" width="200">
          <template #default="{row}">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 平台能力概览 -->
    <el-card style="margin-top: 16px;">
      <template #header>平台能力</template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="cap in capabilities" :key="cap.title">
          <div style="text-align:center;padding:12px;">
            <div style="font-size:28px;">{{ cap.icon }}</div>
            <div style="font-weight:bold;margin-top:8px;">{{ cap.title }}</div>
            <div style="font-size:12px;color:#999;">{{ cap.desc }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFundStats } from '../api/funds'
import { listBacktests } from '../api/backtests'
import { listPortfolios } from '../api/portfolios'

const router = useRouter()

const stats = ref<any>({})
const backtestCount = ref(0)
const portfolioCount = ref(0)
const recentBacktests = ref<any[]>([])

const capabilities = [
  { icon: '📡', title: '数据采集', desc: '东方财富实时同步' },
  { icon: '🧮', title: '量化指标', desc: '15+风险评估指标' },
  { icon: '⚡', title: '策略回测', desc: 'A股规则费率模型' },
  { icon: '🎯', title: '组合优化', desc: '均值方差/风险平价' },
]

onMounted(async () => {
  try {
    const [statsRes, btRes, pfRes] = await Promise.allSettled([
      getFundStats(), listBacktests(), listPortfolios()
    ])
    if (statsRes.status === 'fulfilled') stats.value = statsRes.value.data
    if (btRes.status === 'fulfilled') {
      recentBacktests.value = (btRes.value.data || []).slice(0, 5)
      backtestCount.value = (btRes.value.data || []).length
    }
    if (pfRes.status === 'fulfilled') {
      portfolioCount.value = (pfRes.value.data || []).length
    }
  } catch (_) { /* offline */ }
})
</script>

<template>
  <div class="backtest-result">
    <el-button @click="$router.push('/backtest')" style="margin-bottom:16px;">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>

    <div v-if="result" v-loading="loading">
      <!-- 核心指标卡片 -->
      <el-row :gutter="12" style="margin-bottom:16px;">
        <el-col :span="4"><metric-card title="总收益" :value="fmtPct(result.totalReturn)" color="#e6a23c" /></el-col>
        <el-col :span="4"><metric-card title="年化收益" :value="fmtPct(result.annualReturn)" color="#67c23a" /></el-col>
        <el-col :span="4"><metric-card title="夏普比率" :value="fmtNum(result.sharpeRatio)" color="#409eff" /></el-col>
        <el-col :span="4"><metric-card title="最大回撤" :value="fmtPct(result.maxDrawdown)" color="#f56c6c" negative /></el-col>
        <el-col :span="4"><metric-card title="Sortino" :value="fmtNum(result.sortinoRatio)" color="#409eff" /></el-col>
        <el-col :span="4"><metric-card title="总交易" :value="String(result.totalTrades)" color="#909399" /></el-col>
      </el-row>

      <!-- 净值曲线 -->
      <el-card style="margin-bottom:16px;">
        <template #header>净值走势 vs 回撤</template>
        <v-chart :option="navChartOption" style="height:400px;" autoresize />
      </el-card>

      <!-- 交易记录 -->
      <el-card>
        <template #header>交易记录</template>
        <el-table :data="trades" stripe max-height="400" size="small">
          <el-table-column prop="date" label="日期" width="110" />
          <el-table-column prop="fundCode" label="基金代码" width="90" />
          <el-table-column prop="type" label="方向" width="60">
            <template #default="{row}">
              <el-tag :type="row.type==='BUY'?'success':'danger'" size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="shares" label="份额" width="100" align="right" />
          <el-table-column prop="price" label="价格" width="90" align="right" />
          <el-table-column prop="amount" label="金额" width="110" align="right" />
          <el-table-column prop="fee" label="手续费" width="80" align="right" />
          <el-table-column prop="reason" label="理由" min-width="150" show-overflow-tooltip />
        </el-table>
      </el-card>
    </div>

    <el-empty v-else-if="!loading" description="回测结果不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getBacktestResult, getBacktestStatus } from '../api/backtests'
import VChart from 'vue-echarts'
import MetricCard from '../components/common/MetricCard.vue'

const route = useRoute()
const id = Number(route.params.id)
const loading = ref(false)
const result = ref<any>(null)
const status = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const [res, st] = await Promise.all([
      getBacktestResult(id),
      getBacktestStatus(id)
    ])
    result.value = res.data
    status.value = st.data
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})

const trades = computed(() => {
  if (!result.value?.tradeLog) return []
  try { return JSON.parse(result.value.tradeLog) } catch { return [] }
})

const equityCurve = computed(() => {
  if (!result.value?.equityCurve) return { dates: [], values: [] }
  try {
    const data = JSON.parse(result.value.equityCurve)
    return { dates: data.map((d: any) => d[0]), values: data.map((d: any) => d[1]) }
  } catch { return { dates: [], values: [] } }
})

const navChartOption = computed(() => {
  const ec = equityCurve.value
  const dd: number[] = []
  let peak = ec.values[0] || 0
  for (const v of ec.values) {
    if (v > peak) peak = v
    dd.push(peak > 0 ? Number(((v - peak) / peak * 100).toFixed(2)) : 0)
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['净值', '回撤%'] },
    grid: { left: 70, right: 60, top: 40, bottom: 50 },
    xAxis: { type: 'category', data: ec.dates },
    yAxis: [
      { type: 'value', name: '净值', splitLine: { lineStyle: { type: 'dashed' } } },
      { type: 'value', name: '回撤%', axisLabel: { formatter: '{value}%' } }
    ],
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0 }],
    series: [
      {
        name: '净值', type: 'line', data: ec.values, yAxisIndex: 0,
        smooth: true, symbol: 'none', lineStyle: { color: '#409eff', width: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] }}
      },
      {
        name: '回撤%', type: 'line', data: dd, yAxisIndex: 1,
        symbol: 'none', lineStyle: { color: '#f56c6c', width: 1 },
        areaStyle: { color: 'rgba(245,108,108,0.1)' }
      }
    ]
  }
})

function fmtPct(v: number | null | undefined) {
  if (v == null) return '-'
  return (v * 100).toFixed(2) + '%'
}
function fmtNum(v: number | null | undefined) {
  if (v == null) return '-'
  return v.toFixed(3)
}
</script>

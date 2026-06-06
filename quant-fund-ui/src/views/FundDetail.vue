<template>
  <div class="fund-detail">
    <el-button @click="router.back()" style="margin-bottom: 16px;">
      <el-icon><ArrowLeft /></el-icon> 返回
    </el-button>

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card v-if="fund" style="margin-bottom: 16px;">
        <h2>{{ fund.fundName }} <el-tag>{{ fund.fundCode }}</el-tag></h2>
        <el-descriptions :column="3" border style="margin-top: 16px;">
          <el-descriptions-item label="类型">{{ fund.fundType }}</el-descriptions-item>
          <el-descriptions-item label="基金公司">{{ fund.managementCompany }}</el-descriptions-item>
          <el-descriptions-item label="成立日期">{{ fund.inceptionDate }}</el-descriptions-item>
          <el-descriptions-item label="管理费率">{{ fund.managementFee }}%</el-descriptions-item>
          <el-descriptions-item label="托管银行">{{ fund.custodianBank }}</el-descriptions-item>
          <el-descriptions-item label="是否ETF">{{ fund.isEtf ? '是' : '否' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 量化指标卡片 -->
      <el-row :gutter="12" style="margin-bottom: 16px;" v-if="metrics">
        <el-col :span="4">
          <metric-card title="年化收益" :value="fmtPct(metrics.ratios?.sharpe ? null : null)" color="#e6a23c">
            <template #content>{{ fmtPct(metrics.returns?.['1y']) }}</template>
          </metric-card>
        </el-col>
        <el-col :span="4">
          <metric-card title="夏普比率" :value="fmtNum(metrics.ratios?.sharpe)" color="#67c23a" />
        </el-col>
        <el-col :span="4">
          <metric-card title="Sortino" :value="fmtNum(metrics.ratios?.sortino)" color="#409eff" />
        </el-col>
        <el-col :span="4">
          <metric-card title="年化波动率" :value="fmtPct(metrics.risk?.volatility1y)" color="#909399" />
        </el-col>
        <el-col :span="4">
          <metric-card title="最大回撤" :value="fmtPct(metrics.risk?.maxDrawdown1y)" color="#f56c6c" negative />
        </el-col>
        <el-col :span="4">
          <metric-card title="Calmar" :value="fmtNum(metrics.ratios?.calmar)" color="#e6a23c" />
        </el-col>
      </el-row>

      <!-- 净值曲线 -->
      <el-card style="margin-bottom: 16px;">
        <template #header>净值走势</template>
        <v-chart :option="navChartOption" style="height: 350px;" autoresize />
      </el-card>

      <!-- 回撤曲线 + 月度收益热力图 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-card>
            <template #header>回撤曲线</template>
            <v-chart :option="drawdownOption" style="height: 300px;" autoresize />
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>月度收益热力图</template>
            <v-chart :option="heatmapOption" style="height: 300px;" autoresize />
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-empty v-if="!loading && !fund" description="基金不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getFundDetail, getNavHistory } from '../api/funds'
import { use } from 'echarts/core'
import { LineChart, HeatmapChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, VisualMapComponent, DataZoomComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import MetricCard from '../components/common/MetricCard.vue'

use([LineChart, HeatmapChart, TitleComponent, TooltipComponent, GridComponent, VisualMapComponent, DataZoomComponent, CanvasRenderer])

const router = useRouter()
const route = useRoute()
const fund = ref<any>(null)
const navList = ref<any[]>([])
const metrics = ref<any>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  const code = route.params.fundCode as string
  try {
    const [detailRes, navRes] = await Promise.all([
      getFundDetail(code),
      getNavHistory(code, '2018-01-01', new Date().toISOString().slice(0,10))
    ])
    fund.value = detailRes.data
    navList.value = navRes.data

    // 加载指标
    try {
      const metricsRes = await fetch(`http://localhost:8080/api/v1/screening/metrics/${code}`)
      if (metricsRes.ok) metrics.value = await metricsRes.json()
    } catch (_) { /* 指标暂不可用 */ }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})

// ---- ECharts Options ----

const navDates = computed(() => navList.value.map(n => n.navDate))
const navValues = computed(() => navList.value.map(n => n.unitNav))

const navChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: navDates.value, show: false },
  yAxis: { type: 'value', scale: true, splitLine: { lineStyle: { type: 'dashed' } } },
  dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0 }],
  series: [{
    type: 'line', data: navValues.value,
    smooth: true, symbol: 'none',
    lineStyle: { color: '#409eff', width: 2 },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
      colorStops: [{ offset: 0, color: 'rgba(64,158,255,0.3)' }, { offset: 1, color: 'rgba(64,158,255,0.02)' }] } }
  }]
}))

// 回撤曲线
const drawdownOption = computed(() => {
  const values = navValues.value
  if (!values.length) return {}
  let peak = values[0], maxDD = 0
  const ddData: number[] = []
  for (const v of values) {
    if (v > peak) peak = v
    const dd = (v - peak) / peak * 100
    ddData.push(Number(dd.toFixed(2)))
    if (dd < maxDD) maxDD = dd
  }
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => v.toFixed(2) + '%' },
    grid: { left: 50, right: 20, top: 20, bottom: 20 },
    xAxis: { type: 'category', data: navDates.value, show: false },
    yAxis: { type: 'value', axisLabel: { formatter: '{value}%' }, splitLine: { lineStyle: { type: 'dashed' } } },
    dataZoom: [{ type: 'inside' }],
    series: [{
      type: 'line', data: ddData,
      symbol: 'none', smooth: true,
      lineStyle: { color: '#f56c6c', width: 1.5 },
      areaStyle: { color: 'rgba(245,108,108,0.15)' }
    }]
  }
})

// 月度收益热力图
const heatmapOption = computed(() => {
  const data: any[] = []
  const monthSet = new Set<string>()
  const yearSet = new Set<number>()

  // 从navList中计算月度收益
  let lastMonth = '', monthNavStart = 0
  const monthData: Map<string, number> = new Map()

  for (let i = 0; i < navList.value.length; i++) {
    const d = navList.value[i]
    const ym = d.navDate.slice(0, 7)
    if (ym !== lastMonth) {
      if (lastMonth && monthNavStart > 0) {
        const prev = navList.value.find(n => n.navDate.startsWith(lastMonth))
        if (prev) {
          const ret = (d.unitNav - prev.unitNav) / prev.unitNav * 100
          monthData.set(lastMonth, ret)
        }
      }
      lastMonth = ym
      monthNavStart = d.unitNav
      monthSet.add(ym)
      yearSet.add(Number(d.navDate.slice(0, 4)))
    }
  }

  const years = [...yearSet].sort()
  const monthNames = ['01','02','03','04','05','06','07','08','09','10','11','12']

  for (const y of years) {
    for (const m of monthNames) {
      const key = `${y}-${m}`
      const ret = monthData.get(key)
      if (ret !== undefined) {
        data.push([m, String(y), Number(ret.toFixed(2))])
      }
    }
  }

  return {
    tooltip: { formatter: (p: any) => `${p.value[1]}-${p.value[0]}: ${p.value[2].toFixed(2)}%` },
    grid: { left: 60, right: 40, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: monthNames, axisLabel: { rotate: 0 } },
    yAxis: { type: 'category', data: years.map(String).reverse() },
    visualMap: {
      min: -10, max: 10, calculable: true,
      orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#f56c6c', '#f0f0f0', '#67c23a'] }
    },
    series: [{
      type: 'heatmap', data,
      label: { show: false },
      emphasis: { itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
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

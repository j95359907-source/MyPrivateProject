<template>
  <div class="backtest-runner">
    <h2>策略回测</h2>

    <el-row :gutter="16">
      <!-- 左侧：配置 -->
      <el-col :span="10">
        <el-card header="回测参数">
          <el-form label-width="100px">
            <!-- 策略类型 -->
            <el-form-item label="策略类型">
              <el-select v-model="config.strategyType" @change="onStrategyChange" style="width:100%;">
                <el-option v-for="t in templates" :key="t.type" :label="t.name + ' - ' + t.description"
                           :value="t.type" />
              </el-select>
            </el-form-item>

            <!-- 策略参数 -->
            <template v-if="currentTemplate">
              <el-form-item v-for="(_val, key) in currentTemplate.defaultParams" :key="key"
                            :label="paramLabel(key)">
                <el-input-number v-model="config.parameters[key]" :step="paramStep(key)" :min="0" />
              </el-form-item>
            </template>

            <!-- 基金池 -->
            <el-form-item label="回测基金">
              <el-select v-model="config.fundCodes" multiple filterable remote
                         :remote-method="searchFunds" placeholder="输入基金代码或名称搜索" style="width:100%"
                         :loading="fundSearchLoading">
                <el-option v-for="f in fundOptions" :key="f.fundCode"
                           :label="f.fundCode + ' ' + f.fundName" :value="f.fundCode" />
              </el-select>
            </el-form-item>

            <el-form-item label="回测区间">
              <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
                              start-placeholder="起始日" end-placeholder="结束日"
                              format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
            </el-form-item>

            <el-form-item label="初始资金(元)">
              <el-input-number v-model="config.initialCapital" :min="10000" :step="10000" style="width:100%" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="runBacktest" :loading="running" :disabled="!canRun">
                运行回测
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：结果 -->
      <el-col :span="14">
        <el-card header="回测结果">
          <div v-if="currentRun" style="text-align:center; padding:20px;">
            <el-progress v-if="currentRun.status === 'RUNNING' || currentRun.status === 'PENDING'"
                         :percentage="100" :indeterminate="true" :duration="2" />
            <el-result v-if="currentRun.status === 'COMPLETED'" icon="success" title="回测完成">
              <template #extra>
                <el-button type="primary" @click="viewResult(currentRun.id)">查看详细结果</el-button>
              </template>
            </el-result>
            <el-result v-if="currentRun.status === 'FAILED'" icon="error" title="回测失败"
                       :sub-title="currentRun.errorMessage" />
          </div>
          <el-empty v-else description="配置参数后点击 运行回测" />
        </el-card>

        <!-- 历史回测 -->
        <el-card header="历史回测" style="margin-top:16px;">
          <el-table :data="history" stripe @row-click="viewResult" style="cursor:pointer;">
            <el-table-column prop="runName" label="名称" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{row}">
                <el-tag :type="row.status==='COMPLETED'?'success':row.status==='RUNNING'?'warning':'danger'" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="起始" width="100" />
            <el-table-column prop="endDate" label="结束" width="100" />
            <el-table-column label="操作" width="80">
              <template #default="{row}">
                <el-button size="small" type="danger" @click.stop="delBacktest(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStrategyTemplates, createBacktest, getBacktestStatus, listBacktests, deleteBacktest, type StrategyTemplate } from '../api/backtests'
import { getFundList } from '../api/funds'

const router = useRouter()
const templates = ref<StrategyTemplate[]>([])
const currentTemplate = ref<StrategyTemplate | null>(null)
const running = ref(false)
const currentRun = ref<any>(null)
const history = ref<any[]>([])
const fundOptions = ref<any[]>([])
const fundSearchLoading = ref(false)

const config = reactive({
  strategyName: '',
  strategyType: '',
  parameters: {} as Record<string, any>,
  fundCodes: [] as string[],
  initialCapital: 100000
})

const dateRange = ref<string[]>(['2019-01-01', new Date().toISOString().slice(0, 10)])

const canRun = computed(() =>
  config.strategyType && config.fundCodes.length >= 1 && dateRange.value.length === 2
)

onMounted(async () => {
  try {
    const { data } = await getStrategyTemplates()
    templates.value = data
  } catch (_) { /* */ }
  refreshHistory()
})

function onStrategyChange(type: string) {
  const t = templates.value.find(t => t.type === type)
  if (t) {
    currentTemplate.value = t
    config.parameters = { ...t.defaultParams }
  }
}

async function searchFunds(query: string) {
  if (!query || query.length < 1) return
  fundSearchLoading.value = true
  try {
    const { data } = await getFundList({ keyword: query, size: 10 })
    fundOptions.value = data.content || []
  } catch (_) { /* */ }
  finally { fundSearchLoading.value = false }
}

async function runBacktest() {
  running.value = true
  try {
    const { data } = await createBacktest({
      strategyName: (currentTemplate.value?.name || '') + '-' + Date.now(),
      strategyType: config.strategyType,
      parameters: { ...config.parameters },
      fundCodes: [...config.fundCodes],
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      initialCapital: config.initialCapital
    })
    currentRun.value = { id: data.id, status: 'PENDING' }

    // 轮询状态
    const poll = setInterval(async () => {
      try {
        const { data: status } = await getBacktestStatus(data.id)
        currentRun.value = status
        if (status.status === 'COMPLETED' || status.status === 'FAILED') {
          clearInterval(poll)
          running.value = false
          refreshHistory()
        }
      } catch (_) { clearInterval(poll); running.value = false }
    }, 2000)
  } catch (e) {
    console.error(e)
    running.value = false
  }
}

async function refreshHistory() {
  try {
    const { data } = await listBacktests()
    history.value = data.slice(0, 10)
  } catch (_) { /* */ }
}

function viewResult(row: any) {
  router.push(`/backtest/result/${row.id || row}`)
}

async function delBacktest(id: number) {
  await deleteBacktest(id)
  refreshHistory()
}

function paramLabel(key: string) {
  const labels: Record<string, string> = {
    lookbackMonths: '回顾期(月)', topN: '持仓数', rebalanceMonths: '调仓周期(月)',
    minReturn: '最低收益阈值', maDays: '均线周期(天)', buyThreshold: '买入偏离阈值',
    sellThreshold: '卖出偏离阈值', fastDays: '快线周期(天)', slowDays: '慢线周期(天)',
    confirmDays: '确认天数', maxHoldings: '最大持仓'
  }
  return labels[key] || key
}

function paramStep(key: string) {
  const steps: Record<string, number> = {
    buyThreshold: 0.01, sellThreshold: 0.01, minReturn: 0.01
  }
  return steps[key] || 1
}
</script>

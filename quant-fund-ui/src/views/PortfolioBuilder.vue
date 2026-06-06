<template>
  <div class="portfolio-builder">
    <h2>组合管理</h2>

    <!-- 组合列表 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header>我的组合</template>
          <el-button type="primary" size="small" @click="showCreate = true" style="margin-bottom:8px;">
            + 新建组合
          </el-button>
          <el-table :data="portfolios" stripe @row-click="selectPortfolio" highlight-current-row style="cursor:pointer;">
            <el-table-column prop="name" label="名称" />
            <el-table-column label="现值" width="100">
              <template #default="{row}">{{ fmtMoney(row.currentValue) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 创建弹窗 -->
        <el-dialog v-model="showCreate" title="新建组合" width="400px">
          <el-input v-model="newName" placeholder="组合名称" style="margin-bottom:8px;" />
          <el-input-number v-model="newCapital" :min="10000" :step="10000" placeholder="初始资金" style="width:100%;" />
          <template #footer>
            <el-button @click="showCreate = false">取消</el-button>
            <el-button type="primary" @click="doCreate">创建</el-button>
          </template>
        </el-dialog>
      </el-col>

      <!-- 选中组合操作 -->
      <el-col :span="16" v-if="selected">
        <el-card>
          <template #header>{{ selected.name }}</template>

          <el-tabs>
            <!-- Tab 1: 添加基金 -->
            <el-tab-pane label="持仓管理">
              <el-select v-model="addFundCode" filterable remote :remote-method="searchFunds"
                         placeholder="搜索基金代码" :loading="searching" style="width:200px;" />
              <el-input-number v-model="addWeight" :min="1" :max="100" style="width:100px;margin-left:8px;" /> %
              <el-button type="primary" size="small" @click="addFund" style="margin-left:8px;">添加</el-button>

              <el-table :data="holdings" style="margin-top:8px;">
                <el-table-column prop="fundId" label="基金ID" width="80" />
                <el-table-column label="目标权重" width="100">
                  <template #default="{row}">{{ (row.targetWeight * 100).toFixed(1) }}%</template>
                </el-table-column>
                <el-table-column label="当前权重" width="100">
                  <template #default="{row}">{{ row.currentWeight ? (row.currentWeight*100).toFixed(1)+'%' : '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Tab 2: 优化 -->
            <el-tab-pane label="组合优化">
              <el-radio-group v-model="objective">
                <el-radio value="max_sharpe">最大夏普</el-radio>
                <el-radio value="min_volatility">最小波动</el-radio>
                <el-radio value="risk_parity">风险平价</el-radio>
              </el-radio-group>
              <el-button type="success" @click="runOptimize" :loading="optimizing" style="margin-left:16px;">
                运行优化
              </el-button>

              <!-- 优化结果 -->
              <div v-if="optResult" style="margin-top:16px;">
                <el-row :gutter="12">
                  <el-col :span="8"><metric-card title="预期年化" :value="fmtPct(optResult.expectedReturn)" color="#67c23a" /></el-col>
                  <el-col :span="8"><metric-card title="预期波动" :value="fmtPct(optResult.volatility)" color="#909399" /></el-col>
                  <el-col :span="8"><metric-card title="夏普比率" :value="fmtNum(optResult.sharpeRatio)" color="#409eff" /></el-col>
                </el-row>

                <!-- 有效前沿 -->
                <div v-if="optResult.frontier" style="margin-top:16px;">
                  <h4>有效前沿</h4>
                  <v-chart :option="frontierOption" style="height:300px;" autoresize />
                </div>

                <!-- 权重分配 -->
                <div v-if="optResult.weights" style="margin-top:16px;">
                  <h4>最优权重</h4>
                  <v-chart :option="weightPieOption" style="height:250px;" autoresize />
                </div>
              </div>
            </el-tab-pane>

            <!-- Tab 3: 再平衡 -->
            <el-tab-pane label="再平衡">
              <el-button type="warning" @click="runRebalance" :loading="rebalancing">生成再平衡方案</el-button>
              <div v-if="rebalancePlan" style="margin-top:12px;">
                <el-descriptions :column="3" border>
                  <el-descriptions-item label="需再平衡">{{ rebalancePlan.needsRebalance ? '是' : '否' }}</el-descriptions-item>
                  <el-descriptions-item label="总买入">{{ fmtMoney(rebalancePlan.totalBuy) }}</el-descriptions-item>
                  <el-descriptions-item label="总卖出">{{ fmtMoney(rebalancePlan.totalSell) }}</el-descriptions-item>
                </el-descriptions>
                <el-table :data="rebalancePlan.instructions" style="margin-top:12px;" size="small">
                  <el-table-column prop="fundCode" label="代码" width="80" />
                  <el-table-column prop="type" label="方向" width="60">
                    <template #default="{row}">
                      <el-tag :type="row.type==='BUY'?'success':'danger'" size="small">{{ row.type }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="金额" width="110">
                    <template #default="{row}">{{ fmtMoney(row.amount) }}</template>
                  </el-table-column>
                  <el-table-column prop="reason" label="理由" />
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listPortfolios, getPortfolio, createPortfolio, addFundToPortfolio, optimizePortfolio, rebalancePortfolio }
  from '../api/portfolios'
import VChart from 'vue-echarts'
import MetricCard from '../components/common/MetricCard.vue'

const portfolios = ref<any[]>([])
const selected = ref<any>(null)
const holdings = ref<any[]>([])
const showCreate = ref(false)
const newName = ref('')
const newCapital = ref(100000)
const addFundCode = ref('')
const addWeight = ref(10)
const searching = ref(false)
const objective = ref('max_sharpe')
const optimizing = ref(false)
const optResult = ref<any>(null)
const rebalancing = ref(false)
const rebalancePlan = ref<any>(null)

onMounted(async () => { refreshList() })

async function refreshList() {
  try { const { data } = await listPortfolios(); portfolios.value = data } catch (_) { /* */ }
}

async function selectPortfolio(row: any) {
  try {
    const { data } = await getPortfolio(row.id)
    selected.value = data
    holdings.value = data.holdings || []
    optResult.value = null
    rebalancePlan.value = null
  } catch (_) { /* */ }
}

async function doCreate() {
  await createPortfolio({ name: newName.value, initialCapital: newCapital.value })
  showCreate.value = false; refreshList()
}

async function searchFunds(_query: string) { /* simplified */ }

async function addFund() {
  if (!addFundCode.value || !selected.value) return
  await addFundToPortfolio(selected.value.id, addFundCode.value, addWeight.value / 100)
  selectPortfolio(selected.value)
}

async function runOptimize() {
  if (!selected.value) return
  optimizing.value = true
  try {
    const { data } = await optimizePortfolio(selected.value.id, objective.value)
    optResult.value = data
  } catch (_) { /* */ }
  finally { optimizing.value = false }
}

async function runRebalance() {
  if (!selected.value) return
  rebalancing.value = true
  try {
    const { data } = await rebalancePortfolio(selected.value.id)
    rebalancePlan.value = data
  } catch (_) { /* */ }
  finally { rebalancing.value = false }
}

const frontierOption = computed(() => {
  if (!optResult.value?.frontier) return {}
  const points = optResult.value.frontier
  return {
    tooltip: { trigger: 'item', formatter: (p: any) =>
      `收益: ${(p.value[0]*100).toFixed(2)}% 波动: ${(p.value[1]*100).toFixed(2)}%` },
    xAxis: { type: 'value', name: '波动率', axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'value', name: '收益率', axisLabel: { formatter: '{value}%' } },
    series: [{
      type: 'scatter', symbolSize: 8,
      data: points.map((p: any) => [p.volatility*100, p.return*100]),
      markPoint: { data: [{ name: '最优', coord: [
        (optResult.value.volatility*100).toFixed(2),
        (optResult.value.expectedReturn*100).toFixed(2) ] }] }
    }]
  }
})

const weightPieOption = computed(() => {
  if (!optResult.value?.weights) return {}
  const data = Object.entries(optResult.value.weights)
    .filter(([_, w]: any) => w > 0.5)
    .map(([code, w]: any) => ({ name: code, value: w }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    series: [{ type: 'pie', data, radius: ['40%', '70%'], label: { formatter: '{b}\n{c}%' } }]
  }
})

function fmtPct(v: any) { return v != null ? (v*100).toFixed(2)+'%' : '-' }
function fmtNum(v: any) { return v != null ? v.toFixed(3) : '-' }
function fmtMoney(v: any) { return v != null ? '¥'+Number(v).toLocaleString('zh-CN') : '-' }
</script>

<template>
  <div class="fund-screener">
    <h2 style="margin-bottom: 16px;">基金筛选</h2>

    <!-- 筛选面板 -->
    <el-card style="margin-bottom: 16px;">
      <el-form :model="filters" inline>
        <el-form-item label="基金类型">
          <el-select v-model="filters.fundType" placeholder="全部" clearable style="width: 140px;">
            <el-option v-for="t in fundTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低年收益(%)">
          <el-input-number v-model="filters.minReturn1y" :min="0" :max="200" :step="5" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="最大回撤(%)">
          <el-input-number v-model="filters.maxDrawdown1y" :min="0" :max="100" :step="5" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="最低夏普比率">
          <el-input-number v-model="filters.minSharpe" :min="0" :max="5" :step="0.5" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="filters.sortBy" style="width: 140px;">
            <el-option label="夏普比率" value="sharpe" />
            <el-option label="年化收益" value="return_1y" />
            <el-option label="最大回撤" value="max_drawdown" />
            <el-option label="波动率" value="volatility" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doScreen" :loading="loading">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 结果表格 -->
    <el-card>
      <template #header>
        <span>筛选结果（{{ results.length }}只）</span>
      </template>
      <el-table :data="results" stripe highlight-current-row
                @row-click="goDetail" style="cursor: pointer;" v-loading="loading">
        <el-table-column prop="fundCode" label="代码" width="90" />
        <el-table-column prop="fundName" label="基金名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="fundType" label="类型" width="80" />
        <el-table-column label="1年收益" width="100" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.return1y >= 0 ? '#e6a23c' : '#f56c6c' }">
              {{ (row.return1y * 100).toFixed(2) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="1年波动率" width="100" align="right">
          <template #default="{ row }">{{ (row.volatility1y * 100).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="最大回撤" width="100" align="right">
          <template #default="{ row }">{{ (row.maxDrawdown1y * 100).toFixed(2) }}%</template>
        </el-table-column>
        <el-table-column label="夏普比率" width="100" align="right">
          <template #default="{ row }">{{ row.sharpe1y?.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column label="Sortino" width="90" align="right">
          <template #default="{ row }">{{ row.sortino1y?.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column label="Calmar" width="90" align="right">
          <template #default="{ row }">{{ row.calmarRatio?.toFixed(3) }}</template>
        </el-table-column>
        <el-table-column label="同类排名" width="80" align="center">
          <template #default="{ row }">{{ row.rank1y || '-' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && results.length === 0" description="暂无数据，请调整筛选条件" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { screenFunds, getFundTypes, type FundScreenerDto } from '../api/funds'

const router = useRouter()
const loading = ref(false)
const results = ref<FundScreenerDto[]>([])
const fundTypes = ref<string[]>([])

const filters = reactive({
  fundType: '',
  minReturn1y: null as number | null,
  maxDrawdown1y: null as number | null,
  minSharpe: null as number | null,
  sortBy: 'sharpe'
})

onMounted(async () => {
  try {
    const { data } = await getFundTypes()
    fundTypes.value = data
  } catch (e) {
    console.warn('获取基金类型失败', e)
  }
  doScreen()
})

async function doScreen() {
  loading.value = true
  try {
    const params: any = {
      topN: 50,
      sortBy: filters.sortBy
    }
    if (filters.fundType) params.fundType = filters.fundType
    if (filters.minReturn1y) params.minReturn1y = filters.minReturn1y / 100
    if (filters.maxDrawdown1y) params.maxDrawdown1y = filters.maxDrawdown1y / 100
    if (filters.minSharpe) params.minSharpe = filters.minSharpe

    const { data } = await screenFunds(params)
    results.value = data
  } catch (e) {
    console.error('筛选失败', e)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.fundType = ''
  filters.minReturn1y = null
  filters.maxDrawdown1y = null
  filters.minSharpe = null
  filters.sortBy = 'sharpe'
  doScreen()
}

function goDetail(row: FundScreenerDto) {
  router.push(`/funds/${row.fundCode}`)
}
</script>

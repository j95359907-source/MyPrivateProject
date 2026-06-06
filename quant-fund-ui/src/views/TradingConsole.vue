<template>
  <div class="trading-console">
    <h2 style="margin-bottom:16px;">交易控制台
      <el-tag :type="liveMode ? 'danger' : 'success'" style="margin-left:8px;">
        {{ liveMode ? '实盘' : '模拟' }}
      </el-tag>
    </h2>

    <el-row :gutter="16">
      <el-col :span="6">
        <el-card>
          <template #header>账户摘要</template>
          <div v-if="account">
            <el-statistic title="总资产" :value="account.totalAssets" :formatter="fmtMoney" />
            <el-statistic title="可用资金" :value="account.availableCash" :formatter="fmtMoney" />
            <el-statistic title="持仓市值" :value="account.marketValue" :formatter="fmtMoney" />
            <el-statistic title="总盈亏" :value="account.totalPnl" :formatter="fmtMoney"
                          :value-style="{ color: account.totalPnl >= 0 ? '#67c23a' : '#f56c6c' }" />
          </div>
          <el-button type="primary" @click="refreshAccount" size="small" style="margin-top:8px;">刷新</el-button>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card>
          <template #header>持仓</template>
          <el-table :data="positions" size="small" max-height="200">
            <el-table-column prop="fundCode" label="代码" width="80" />
            <el-table-column prop="shares" label="份额" width="70" />
            <el-table-column label="市值" width="90">
              <template #default="{row}">{{ fmtMoney(row.marketValue) }}</template>
            </el-table-column>
            <el-table-column label="盈亏" width="80">
              <template #default="{row}">
                <span :style="{ color: row.unrealizedPnl >= 0 ? '#67c23a' : '#f56c6c' }">
                  {{ fmtMoney(row.unrealizedPnl) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!positions.length" description="无持仓" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <!-- 交易模式切换 -->
        <el-card style="margin-bottom:16px;">
          <el-radio-group v-model="tradeMode" @change="switchTradeMode">
            <el-radio-button value="paper">模拟交易</el-radio-button>
            <el-radio-button value="live">实盘 (QMT)</el-radio-button>
          </el-radio-group>
          <el-button @click="testConnection" size="small" style="margin-left:12px;" v-if="tradeMode==='live'">
            测试连接
          </el-button>
          <span v-if="qmtStatus" style="margin-left:8px;font-size:13px;color:#909399;">
            {{ qmtStatus }}
          </span>
        </el-card>

        <!-- 下单 -->
        <el-card>
          <template #header>快速下单</template>
          <el-form :model="orderForm" inline size="small">
            <el-form-item label="基金代码">
              <el-input v-model="orderForm.fundCode" style="width:110px;" />
            </el-form-item>
            <el-form-item label="方向">
              <el-select v-model="orderForm.tradeType" style="width:80px;">
                <el-option value="BUY" label="买入" />
                <el-option value="SELL" label="卖出" />
              </el-select>
            </el-form-item>
            <el-form-item label="金额(元)">
              <el-input-number v-model="orderForm.amount" :min="100" :step="1000" style="width:130px;" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitOrder" :loading="submitting">
                {{ liveMode ? '提交审批' : '即刻下单' }}
              </el-button>
            </el-form-item>
            <!-- 实盘确认 -->
            <el-form-item v-if="liveMode" label="确认实盘">
              <el-switch v-model="orderForm.confirmed" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 订单历史 -->
    <el-card style="margin-top:16px;">
      <template #header>订单历史</template>
      <el-table :data="orders" size="small">
        <el-table-column prop="orderId" label="订单ID" width="140" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="row.status==='EXECUTED'?'success':row.status==='SUBMITTED'?'warning':'info'" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{row}">
            <el-button size="small" type="danger" @click="doCancel(row.orderId)"
                       :disabled="row.status==='EXECUTED' || row.status==='CANCELLED'">撤单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getAccount, getPositions, getOrders, placeOrder, cancelOrder,
         switchMode, testQmt } from '../api/trading'

const liveMode = ref(false)
const tradeMode = ref('paper')
const qmtStatus = ref('')
const account = ref<any>(null)
const positions = ref<any[]>([])
const orders = ref<any[]>([])
const submitting = ref(false)

const orderForm = reactive({
  fundCode: '', tradeType: 'BUY', amount: 10000, confirmed: false
})

onMounted(async () => {
  await Promise.all([refreshAccount(), refreshOrders()])
})

async function refreshAccount() {
  try { const [acc, pos] = await Promise.all([getAccount(), getPositions()])
    account.value = acc.data; positions.value = pos.data
  } catch (_) { /* API未就绪 */ }
}

async function refreshOrders() {
  try { const { data } = await getOrders(); orders.value = data
  } catch (_) { /* */ }
}

async function switchTradeMode(mode: string) {
  try {
    const { data } = await switchMode(mode)
    liveMode.value = data.mode === 'live'
  } catch (e: any) {
    tradeMode.value = 'paper'
    qmtStatus.value = e.response?.data?.error || '切换失败'
  }
}

async function testConnection() {
  try {
    const { data } = await testQmt()
    qmtStatus.value = data.connected ? '✅ QMT已连接' : '❌ 连接失败'
  } catch (_) { qmtStatus.value = '❌ 连接超时' }
}

async function submitOrder() {
  submitting.value = true
  try {
    await placeOrder({ ...orderForm })
    orderForm.fundCode = ''
    refreshAccount(); refreshOrders()
  } catch (_) { /* */ }
  finally { submitting.value = false }
}

async function doCancel(orderId: string) {
  await cancelOrder(orderId); refreshOrders()
}

function fmtMoney(v: any) {
  if (v == null) return '-'
  return '¥' + Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}
</script>

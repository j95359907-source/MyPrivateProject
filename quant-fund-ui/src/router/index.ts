import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('../views/Dashboard.vue'),
      meta: { title: '仪表盘' }
    },
    {
      path: '/funds',
      name: 'fund-screener',
      component: () => import('../views/FundScreener.vue'),
      meta: { title: '基金筛选' }
    },
    {
      path: '/funds/:fundCode',
      name: 'fund-detail',
      component: () => import('../views/FundDetail.vue'),
      meta: { title: '基金详情' }
    },
    {
      path: '/strategies',
      name: 'strategies',
      component: () => import('../views/StrategyManager.vue'),
      meta: { title: '策略管理' }
    },
    {
      path: '/backtest',
      name: 'backtest',
      component: () => import('../views/BacktestRunner.vue'),
      meta: { title: '策略回测' }
    },
    {
      path: '/backtest/result/:id',
      name: 'backtest-result',
      component: () => import('../views/BacktestResult.vue'),
      meta: { title: '回测结果' }
    },
    {
      path: '/portfolio',
      name: 'portfolio',
      component: () => import('../views/PortfolioBuilder.vue'),
      meta: { title: '组合管理' }
    },
    {
      path: '/trading',
      name: 'trading',
      component: () => import('../views/TradingConsole.vue'),
      meta: { title: '交易控制台' }
    }
  ]
})

export default router

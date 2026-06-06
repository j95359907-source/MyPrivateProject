<template>
  <el-container class="app-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-text">量化基金</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/funds">
          <el-icon><Search /></el-icon>
          <span>基金筛选</span>
        </el-menu-item>
        <el-menu-item index="/strategies">
          <el-icon><Setting /></el-icon>
          <span>策略管理</span>
        </el-menu-item>
        <el-menu-item index="/backtest">
          <el-icon><TrendCharts /></el-icon>
          <span>策略回测</span>
        </el-menu-item>
        <el-menu-item index="/portfolio">
          <el-icon><PieChart /></el-icon>
          <span>组合管理</span>
        </el-menu-item>
        <el-menu-item index="/trading">
          <el-icon><CreditCard /></el-icon>
          <span>交易控制台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
.app-container { height: 100vh; }
.sidebar { background-color: #304156; overflow-y: auto; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.logo-text { color: #fff; font-size: 18px; font-weight: bold; }
.main-content { background: #f0f2f5; min-height: 100vh; padding: 20px; }
</style>

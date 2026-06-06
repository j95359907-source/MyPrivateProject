<template>
  <el-card shadow="hover" :body-style="{ padding: '16px', textAlign: 'center' }">
    <div style="font-size: 12px; color: #909399; margin-bottom: 8px;">{{ title }}</div>
    <div :style="{ fontSize: '24px', fontWeight: 'bold', color: negative && value !== '-' ? '#f56c6c' : color }">
      <slot name="content">{{ value }}</slot>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  value: string
  color?: string
  negative?: boolean
}>()
</script>

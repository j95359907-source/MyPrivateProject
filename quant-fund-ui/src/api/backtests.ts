import apiClient from './client'

export interface BacktestRunDto {
  id: number
  strategyId: number
  runName: string
  status: string
  startDate: string
  endDate: string
  initialCapital: number
  completedAt?: string
  durationSeconds?: number
  errorMessage?: string
}

export interface BacktestResultDto {
  backtestId: number
  totalReturn: number
  annualReturn: number
  volatility: number
  sharpeRatio: number
  sortinoRatio: number
  calmarRatio: number
  maxDrawdown: number
  winRate: number
  totalTrades: number
  alpha: number
  beta: number
  equityCurve: string  // JSON array of [date, value]
  tradeLog: string     // JSON array of trade objects
}

export interface StrategyTemplate {
  type: string
  name: string
  description: string
  defaultParams: Record<string, any>
}

// --- API ---

export function getStrategyTemplates() {
  return apiClient.get<StrategyTemplate[]>('/strategies/templates')
}

export function createBacktest(params: {
  strategyName: string
  strategyType: string
  parameters: Record<string, any>
  fundCodes: string[]
  startDate: string
  endDate: string
  initialCapital: number
}) {
  return apiClient.post<any>('/backtests', params)
}

export function getBacktestStatus(id: number) {
  return apiClient.get<any>(`/backtests/${id}`)
}

export function getBacktestResult(id: number) {
  return apiClient.get<BacktestResultDto>(`/backtests/${id}/results`)
}

export function listBacktests() {
  return apiClient.get<any[]>('/backtests')
}

export function deleteBacktest(id: number) {
  return apiClient.delete(`/backtests/${id}`)
}

import apiClient from './client'

export interface FundDto {
  id: number
  fundCode: string
  fundName: string
  fundShortName: string
  fundType: string
  fundSubtype: string
  managementCompany: string
  fundSize: number
  managementFee: number
  isEtf: boolean
  isIndex: boolean
  isActive: boolean
}

export interface NavHistoryDto {
  navDate: string
  unitNav: number
  accNav: number
  dailyReturn: number
  adjNav: number
}

export interface FundScreenerDto {
  fundCode: string
  fundName: string
  fundShortName: string
  fundType: string
  managementCompany: string
  return1m: number
  return3m: number
  return1y: number
  return3y: number
  volatility1y: number
  maxDrawdown1y: number
  sharpe1y: number
  sortino1y: number
  calmarRatio: number
  alpha: number
  beta: number
  rank1y: number
  percentile1y: number
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// --- API calls ---

export function getFundList(params: {
  fundType?: string
  keyword?: string
  page?: number
  size?: number
}) {
  return apiClient.get<PageResponse<FundDto>>('/funds', { params })
}

export function getEtfList() {
  return apiClient.get<FundDto[]>('/funds/etf')
}

export function getFundDetail(fundCode: string) {
  return apiClient.get<any>(`/funds/${fundCode}`)
}

export function getNavHistory(fundCode: string, start?: string, end?: string) {
  return apiClient.get<NavHistoryDto[]>(`/funds/${fundCode}/nav`, {
    params: { start, end }
  })
}

export function screenFunds(params: {
  fundType?: string
  minReturn1y?: number
  maxDrawdown1y?: number
  minSharpe?: number
  sortBy?: string
  topN?: number
}) {
  return apiClient.get<FundScreenerDto[]>('/funds/screener', { params })
}

export function getCompanies() {
  return apiClient.get<string[]>('/funds/companies')
}

export function getFundTypes() {
  return apiClient.get<string[]>('/funds/types')
}

export function getFundStats() {
  return apiClient.get<any>('/funds/stats')
}

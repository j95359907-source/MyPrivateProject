import apiClient from './client'

export function listPortfolios() {
  return apiClient.get<any[]>('/portfolios')
}

export function getPortfolio(id: number) {
  return apiClient.get<any>(`/portfolios/${id}`)
}

export function createPortfolio(params: { name: string; description?: string; initialCapital: number }) {
  return apiClient.post<any>('/portfolios', params)
}

export function addFundToPortfolio(id: number, fundCode: string, targetWeight: number) {
  return apiClient.post(`/portfolios/${id}/funds`, { fundCode, targetWeight })
}

export function optimizePortfolio(id: number, objective: string = 'max_sharpe') {
  return apiClient.post<any>(`/portfolios/${id}/optimize`, { objective })
}

export function rebalancePortfolio(id: number) {
  return apiClient.post<any>(`/portfolios/${id}/rebalance`)
}

export function deletePortfolio(id: number) {
  return apiClient.delete(`/portfolios/${id}`)
}

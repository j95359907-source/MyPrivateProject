import apiClient from './client'

export function getBrokers() { return apiClient.get('/trading/brokers') }
export function testQmt() { return apiClient.post('/trading/brokers/qmt/test') }
export function switchMode(mode: string) { return apiClient.post('/trading/mode', { mode }) }
export function placeOrder(params: any) { return apiClient.post('/trading/orders', params) }
export function cancelOrder(id: string) { return apiClient.delete(`/trading/orders/${id}`) }
export function getOrders() { return apiClient.get('/trading/orders') }
export function getPositions() { return apiClient.get('/trading/positions') }
export function getAccount() { return apiClient.get('/trading/account') }
export function generateSignals(params: any) { return apiClient.post('/trading/signals/generate', params) }
export function executeSignals(signals: any[]) { return apiClient.post('/trading/signals/execute', signals) }

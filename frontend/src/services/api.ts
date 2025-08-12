import axios from 'axios';
import { CalculationResult, MarinationRequest, SalesData, SalesTotals, MarinationLog } from '../types';

// Use relative URL in production, or environment variable for development
const API_BASE_URL = process.env.REACT_APP_API_URL || '/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const calculatorApi = {
  calculate: async (request: MarinationRequest): Promise<CalculationResult> => {
    const response = await api.post('/calculator/calculate', request);
    return response.data;
  },
  
  hasSalesData: async (): Promise<boolean> => {
    const response = await api.get('/calculator/has-sales-data');
    return response.data.hasSalesData;
  },
};

export const salesDataApi = {
  getAll: async (): Promise<SalesData[]> => {
    const response = await api.get('/sales-data');
    return response.data;
  },
  
  getTotals: async (): Promise<SalesTotals> => {
    const response = await api.get('/sales-data/totals');
    return response.data;
  },
  
  add: async (salesData: SalesData): Promise<SalesData> => {
    const response = await api.post('/sales-data', salesData);
    return response.data;
  },
  
  delete: async (id: number): Promise<void> => {
    await api.delete(`/sales-data/${id}`);
  },
  
  deleteAll: async (): Promise<void> => {
    await api.delete('/sales-data');
  },
};

export const marinationLogApi = {
  getAll: async (): Promise<MarinationLog[]> => {
    const response = await api.get('/marination-log');
    return response.data;
  },
  
  getTodaysLogs: async (): Promise<MarinationLog[]> => {
    const response = await api.get('/marination-log/today');
    return response.data;
  },
  
  add: async (log: MarinationLog): Promise<MarinationLog> => {
    const response = await api.post('/marination-log', log);
    return response.data;
  },
  
  delete: async (id: number): Promise<void> => {
    await api.delete(`/marination-log/${id}`);
  },
};
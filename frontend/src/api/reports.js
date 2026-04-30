import api from './client.js';

export async function submitReport({ type, targetID, reason }) {
  const { data } = await api.post('/reports', { type, targetID, reason });
  return data;
}

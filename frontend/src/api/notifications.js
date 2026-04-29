import api from './client.js';

export async function listNotifications() {
  const { data } = await api.get('/notifications');
  return data;
}

export async function getUnreadCount() {
  const { data } = await api.get('/notifications/unread-count');
  return data?.count ?? 0;
}

export async function markRead(notificationID) {
  await api.put(`/notifications/${notificationID}/read`);
}

export async function clearAll() {
  await api.delete('/notifications');
}

export async function getPreferences() {
  const { data } = await api.get('/notifications/preferences');
  return data;
}

export async function updatePreferences(prefs) {
  const { data } = await api.put('/notifications/preferences', prefs);
  return data;
}

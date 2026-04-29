import api from './client.js';

export async function getOrCreateSession(postID) {
  const { data } = await api.post('/chat/sessions', { postID });
  return data;
}

export async function getMySessions() {
  const { data } = await api.get('/chat/sessions');
  return data;
}

export async function getHistory(sessionID) {
  const { data } = await api.get(`/chat/sessions/${sessionID}/messages`);
  return data;
}

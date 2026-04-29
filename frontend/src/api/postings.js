import api from './client.js';

export async function createPosting(payload) {
  const { data } = await api.post('/postings', payload);
  return data;
}

export async function editPosting(postID, payload) {
  const { data } = await api.put(`/postings/${postID}`, payload);
  return data;
}

export async function softDeletePosting(postID) {
  await api.delete(`/postings/${postID}`);
}

export async function getMyPostings() {
  const { data } = await api.get('/postings/mine');
  return data;
}

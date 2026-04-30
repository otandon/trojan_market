import api from './client.js';

export async function getPastPurchases() {
  const { data } = await api.get('/stats/past-purchases');
  return data;
}

export async function getSoldItems() {
  const { data } = await api.get('/stats/sold-items');
  return data;
}

export async function getSavedPostings() {
  const { data } = await api.get('/stats/saved-postings');
  return data;
}

export async function getChatForPost(postID) {
  const { data } = await api.get(`/stats/chat/${postID}`);
  return data;
}

export async function removeSavedPosting(postID) {
  await api.delete(`/stats/saved-postings/${postID}`);
}

export async function saveListing(postID) {
  await api.post('/stats/saved', { postID });
}

import api from './client.js';

export async function search({ q, category, minPrice, maxPrice, sortBy } = {}) {
  const params = {};
  if (q) params.q = q;
  if (category) params.category = category;
  if (minPrice !== undefined && minPrice !== null && minPrice !== '') params.minPrice = minPrice;
  if (maxPrice !== undefined && maxPrice !== null && maxPrice !== '') params.maxPrice = maxPrice;
  if (sortBy) params.sortBy = sortBy;
  const { data } = await api.get('/search', { params });
  return data;
}

export async function getCategories() {
  const { data } = await api.get('/search/categories');
  return data;
}

export async function getPostingDetail(postID) {
  const { data } = await api.get(`/search/postings/${postID}`);
  return data;
}

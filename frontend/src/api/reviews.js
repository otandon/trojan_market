import api from './client.js';

export async function getSellerReviews(sellerID) {
  const { data } = await api.get(`/reviews/seller/${sellerID}`);
  return data;
}

export async function createReview({ transactionID, rating, comment }) {
  const { data } = await api.post('/reviews', { transactionID, rating, comment });
  return data;
}

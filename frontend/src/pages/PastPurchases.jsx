import { useEffect, useState } from 'react';
import { getChatForPost, getPastPurchases } from '../api/stats.js';
import { createReview } from '../api/reviews.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function PastPurchases() {
  const { isGuest, user } = useAuth();
  const toast = useToast();

  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openTx, setOpenTx] = useState(null);
  const [chat, setChat] = useState([]);

  const [reviewTx, setReviewTx] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitting, setReviewSubmitting] = useState(false);

  useEffect(() => {
    if (isGuest) {
      setLoading(false);
      return;
    }
    getPastPurchases()
      .then(setPurchases)
      .catch(() => setPurchases([]))
      .finally(() => setLoading(false));
  }, [isGuest]);

  const openDetails = async (tx) => {
    setOpenTx(tx);
    setChat([]);
    try {
      const messages = await getChatForPost(tx.postID);
      setChat(messages);
    } catch {
      setChat([]);
    }
  };

  const openReview = (tx, e) => {
    e.stopPropagation();
    setReviewTx(tx);
    setReviewRating(5);
    setReviewComment('');
  };

  const submitReview = async (e) => {
    e.preventDefault();
    if (!reviewTx) return;
    setReviewSubmitting(true);
    try {
      const created = await createReview({
        transactionID: reviewTx.transactionID,
        rating: Number(reviewRating),
        comment: reviewComment.trim() || null,
      });
      // Mark this transaction as reviewed in local state so the button flips.
      setPurchases((list) =>
        list.map((t) =>
          t.transactionID === reviewTx.transactionID ? { ...t, reviewID: created.reviewID } : t,
        ),
      );
      toast.success('Review submitted — thanks!');
      setReviewTx(null);
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not submit review'));
    } finally {
      setReviewSubmitting(false);
    }
  };

  if (isGuest) {
    return <div className="p-6 text-gray-600">Sign in to view your purchase history.</div>;
  }
  if (loading) return <div className="p-6 text-gray-500">Loading...</div>;

  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold">Past Purchases</h1>
      {purchases.length === 0 ? (
        <p className="mt-3 text-sm text-gray-500">No purchases yet.</p>
      ) : (
        <ul className="mt-4 space-y-2">
          {purchases.map((tx) => (
            <li key={tx.transactionID}>
              <button
                type="button"
                onClick={() => openDetails(tx)}
                className="w-full rounded-md border border-gray-200 bg-white p-3 text-left hover:border-usc-cardinal"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-900">{tx.postTitle}</div>
                    <div className="text-xs text-gray-500">Seller: {tx.sellerUsername}</div>
                  </div>
                  <div className="flex items-center gap-3">
                    <div className="text-right">
                      <div className="font-semibold">${Number(tx.salePrice).toFixed(2)}</div>
                      <div className="text-xs text-gray-500">
                        {tx.transactionTime ? new Date(tx.transactionTime).toLocaleDateString() : ''}
                      </div>
                    </div>
                    {tx.reviewID ? (
                      <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-800">
                        ✓ Reviewed
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={(e) => openReview(tx, e)}
                        className="rounded-md border border-usc-cardinal px-3 py-1 text-xs font-semibold text-usc-cardinal hover:bg-usc-cardinal hover:text-white"
                      >
                        Leave a review
                      </button>
                    )}
                  </div>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      {openTx && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setOpenTx(null)}
        >
          <div
            className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">{openTx.postTitle}</h2>
              <button type="button" onClick={() => setOpenTx(null)} className="text-gray-500">
                ✕
              </button>
            </div>
            <div className="mt-2 text-sm text-gray-600">
              ${Number(openTx.salePrice).toFixed(2)} · Seller: {openTx.sellerUsername}
            </div>
            <h3 className="mt-4 text-xs font-bold uppercase text-gray-500">Chat history</h3>
            <div className="mt-2 space-y-2">
              {chat.length === 0 && <div className="text-xs text-gray-400">No messages.</div>}
              {chat.map((m) => {
                const mine = m.senderID === user?.userID;
                return (
                  <div key={m.messageID} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[80%] rounded-2xl px-3 py-1.5 text-sm ${mine ? 'bg-usc-cardinal text-white' : 'bg-gray-100 text-gray-900'}`}>
                      {m.messageText}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {reviewTx && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => !reviewSubmitting && setReviewTx(null)}
        >
          <form
            onSubmit={submitReview}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-xl bg-white p-5"
          >
            <div className="flex items-start justify-between">
              <h2 className="text-lg font-semibold">Review your purchase</h2>
              <button
                type="button"
                onClick={() => setReviewTx(null)}
                disabled={reviewSubmitting}
                className="text-gray-500"
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            <div className="mt-1 text-xs text-gray-500">
              {reviewTx.postTitle} · Seller {reviewTx.sellerUsername}
            </div>

            <fieldset className="mt-4">
              <legend className="text-sm font-semibold">Rating</legend>
              <div className="mt-1 flex gap-1">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button
                    key={n}
                    type="button"
                    onClick={() => setReviewRating(n)}
                    aria-label={`${n} star${n === 1 ? '' : 's'}`}
                    className={`text-2xl ${n <= reviewRating ? 'text-usc-gold' : 'text-gray-300'} hover:text-usc-gold`}
                  >
                    ★
                  </button>
                ))}
              </div>
            </fieldset>

            <label className="mt-4 block text-sm">
              <span className="font-semibold">Comment (optional)</span>
              <textarea
                rows={4}
                value={reviewComment}
                onChange={(e) => setReviewComment(e.target.value)}
                maxLength={2000}
                placeholder="How was your transaction with this seller?"
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              />
            </label>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setReviewTx(null)}
                disabled={reviewSubmitting}
                className="rounded border border-gray-300 px-3 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={reviewSubmitting}
                className="rounded bg-usc-cardinal px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {reviewSubmitting ? 'Submitting...' : 'Submit review'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

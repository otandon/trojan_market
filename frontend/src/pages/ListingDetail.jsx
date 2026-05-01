import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getPostingDetail } from '../api/search.js';
import { getOrCreateSession, getSessionsForPosting } from '../api/chat.js';
import { editPosting, softDeletePosting } from '../api/postings.js';
import { saveListing } from '../api/stats.js';
import { submitReport } from '../api/reports.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function ListingDetail() {
  const { postID } = useParams();
  const { isGuest, user } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [posting, setPosting] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);
  const [savePending, setSavePending] = useState(false);
  const [activePhoto, setActivePhoto] = useState(0);
  const [reportOpen, setReportOpen] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reportSubmitting, setReportSubmitting] = useState(false);

  // Seller-side state
  const [statusBusy, setStatusBusy] = useState(false);
  const [soldOpen, setSoldOpen] = useState(false);
  const [soldBuyers, setSoldBuyers] = useState([]);
  const [soldBuyerID, setSoldBuyerID] = useState('');
  const [soldPrice, setSoldPrice] = useState('');
  const [soldSubmitting, setSoldSubmitting] = useState(false);
  const [soldLoading, setSoldLoading] = useState(false);

  const refresh = async () => {
    try {
      const data = await getPostingDetail(postID);
      setPosting(data);
      setSaved(Boolean(data?.isSaved));
    } catch (e) {
      setError(apiErrorMessage(e, 'Listing not found'));
    }
  };

  useEffect(() => {
    setError(null);
    setActivePhoto(0);
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postID]);

  const isMine = !isGuest && posting && user?.userID === posting.sellerID;

  const onMessageSeller = async () => {
    setBusy(true);
    try {
      const session = await getOrCreateSession(Number(postID));
      navigate(`/messages/${session.sessionID}`);
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not open chat'));
    } finally {
      setBusy(false);
    }
  };

  const onSave = async () => {
    if (savePending) return;
    setSavePending(true);
    try {
      await saveListing(Number(postID));
      setSaved(true);
      toast.success('Saved to your list');
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not save listing'));
    } finally {
      setSavePending(false);
    }
  };

  const onChangeStatus = async (newStatus) => {
    if (statusBusy) return;
    setStatusBusy(true);
    try {
      await editPosting(Number(postID), { status: newStatus });
      toast.success(newStatus === 'PENDING' ? 'Listing paused' : 'Listing reactivated');
      await refresh();
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not update listing'));
    } finally {
      setStatusBusy(false);
    }
  };

  const onDelete = async () => {
    if (statusBusy) return;
    setStatusBusy(true);
    try {
      await softDeletePosting(Number(postID));
      toast.success('Listing deleted');
      navigate('/');
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not delete listing'));
      setStatusBusy(false);
    }
  };

  const openMarkAsSold = async () => {
    setSoldOpen(true);
    setSoldBuyerID('');
    setSoldPrice(posting?.price ? String(posting.price) : '');
    setSoldLoading(true);
    try {
      const sessions = await getSessionsForPosting(Number(postID));
      setSoldBuyers(sessions);
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not load buyers'));
      setSoldBuyers([]);
    } finally {
      setSoldLoading(false);
    }
  };

  const submitMarkAsSold = async (e) => {
    e.preventDefault();
    if (!soldBuyerID) {
      toast.error('Pick a buyer');
      return;
    }
    setSoldSubmitting(true);
    try {
      await editPosting(Number(postID), {
        status: 'SOLD',
        buyerID: Number(soldBuyerID),
        salePrice: soldPrice === '' ? null : Number(soldPrice),
      });
      toast.success('Listing marked as sold');
      setSoldOpen(false);
      await refresh();
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not mark as sold'));
    } finally {
      setSoldSubmitting(false);
    }
  };

  const onReportSubmit = async (e) => {
    e.preventDefault();
    setReportSubmitting(true);
    try {
      await submitReport({
        type: 'POSTING',
        targetID: Number(postID),
        reason: reportReason.trim() || null,
      });
      toast.success('Report submitted. Thank you.');
      setReportOpen(false);
      setReportReason('');
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not submit report'));
    } finally {
      setReportSubmitting(false);
    }
  };

  if (error) return <div className="p-6 text-red-600">{error}</div>;
  if (!posting) return <div className="p-6 text-gray-500">Loading...</div>;

  const photos = posting.photos || [];
  const main = photos[activePhoto];

  return (
    <div className="grid gap-6 p-6 md:grid-cols-2">
      <div>
        <div className="flex aspect-square items-center justify-center overflow-hidden rounded-xl bg-gray-100 text-gray-400">
          {main ? (
            <img src={main} alt={posting.title} className="h-full w-full object-cover" />
          ) : (
            <span>📷 No photo</span>
          )}
        </div>
        {photos.length > 1 && (
          <div className="mt-3 grid grid-cols-4 gap-2">
            {photos.map((src, i) => (
              <button
                key={i}
                type="button"
                onClick={() => setActivePhoto(i)}
                className={`flex aspect-square items-center justify-center overflow-hidden rounded-md border ${
                  i === activePhoto ? 'border-usc-cardinal ring-2 ring-usc-cardinal' : 'border-gray-200'
                } bg-gray-50`}
                aria-label={`View photo ${i + 1}`}
              >
                <img src={src} alt={`${posting.title} thumbnail ${i + 1}`} className="h-full w-full object-cover" />
              </button>
            ))}
          </div>
        )}
      </div>

      <div>
        <h1 className="text-2xl font-semibold text-gray-900">{posting.title}</h1>
        <div className="mt-1 text-3xl font-bold text-usc-cardinal">
          ${Number(posting.price ?? 0).toFixed(2)}
        </div>

        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-green-100 px-2 py-0.5 text-green-800">{posting.status}</span>
          <span className="rounded-full bg-gray-100 px-2 py-0.5 text-gray-700">
            {posting.category?.replaceAll('_', ' ')}
          </span>
        </div>

        <div className="mt-5 rounded-md border border-gray-200 p-3">
          <div className="text-xs uppercase tracking-wide text-gray-500">Seller</div>
          <div className="mt-1 flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gray-200 text-sm font-semibold">
              {((posting.sellerFirstName?.[0] || '') + (posting.sellerLastName?.[0] || '')
                || (posting.sellerUsername || '?').slice(0, 2)).toUpperCase()}
            </div>
            <div>
              <div className="font-semibold text-gray-900">
                {[posting.sellerFirstName, posting.sellerLastName].filter(Boolean).join(' ') || posting.sellerUsername}
              </div>
              <div className="text-xs text-gray-600">
                <span className="text-usc-gold">★</span>{' '}
                {Number(posting.sellerRating ?? 0).toFixed(1)}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-5">
          <div className="text-xs uppercase tracking-wide text-gray-500">Description</div>
          <p className="mt-1 whitespace-pre-line text-gray-800">{posting.description || '—'}</p>
        </div>

        <div className="mt-6 space-y-2">
          {isMine ? (
            <>
              <div className="rounded-md bg-usc-gold/20 px-3 py-2 text-xs text-gray-800">
                This is your listing — only you see these controls.
              </div>
              {posting.status !== 'SOLD' && (
                <button
                  type="button"
                  onClick={openMarkAsSold}
                  disabled={statusBusy}
                  className="w-full rounded-md bg-usc-cardinal py-2 font-semibold text-white disabled:opacity-50"
                >
                  Mark as sold
                </button>
              )}
              <div className="grid grid-cols-2 gap-2">
                {posting.status === 'AVAILABLE' && (
                  <button
                    type="button"
                    onClick={() => onChangeStatus('PENDING')}
                    disabled={statusBusy}
                    className="rounded-md border border-gray-300 py-2 text-sm font-semibold text-gray-700 disabled:opacity-50"
                  >
                    Pause listing
                  </button>
                )}
                {posting.status === 'PENDING' && (
                  <button
                    type="button"
                    onClick={() => onChangeStatus('AVAILABLE')}
                    disabled={statusBusy}
                    className="rounded-md border border-gray-300 py-2 text-sm font-semibold text-gray-700 disabled:opacity-50"
                  >
                    Reactivate
                  </button>
                )}
                <button
                  type="button"
                  onClick={onDelete}
                  disabled={statusBusy}
                  className={`rounded-md border border-red-300 py-2 text-sm font-semibold text-red-600 hover:bg-red-50 disabled:opacity-50 ${
                    posting.status === 'SOLD' ? 'col-span-2' : ''
                  }`}
                >
                  Delete listing
                </button>
              </div>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={onMessageSeller}
                disabled={isGuest || busy}
                className="w-full rounded-md bg-usc-cardinal py-2 font-semibold text-white disabled:cursor-not-allowed disabled:bg-gray-300"
              >
                {isGuest ? 'Sign In to Interact' : busy ? 'Opening...' : 'Message Seller'}
              </button>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={onSave}
                  disabled={isGuest || savePending || saved}
                  className="rounded-md border border-gray-300 py-2 text-sm font-semibold text-gray-700 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                >
                  {saved ? '✓ Saved' : savePending ? 'Saving...' : '♡ Save Listing'}
                </button>
                <button
                  type="button"
                  onClick={() => setReportOpen(true)}
                  disabled={isGuest}
                  className="rounded-md border border-gray-300 py-2 text-sm font-semibold text-gray-700 disabled:cursor-not-allowed disabled:bg-gray-100 disabled:text-gray-400"
                >
                  ⚑ Report
                </button>
              </div>
              {isGuest && (
                <p className="pt-1 text-center text-xs text-gray-500">Sign in to interact with this listing</p>
              )}
            </>
          )}
        </div>
      </div>

      {soldOpen && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
          onClick={() => !soldSubmitting && setSoldOpen(false)}
        >
          <form
            onSubmit={submitMarkAsSold}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-xl bg-white p-5"
          >
            <div className="flex items-start justify-between">
              <h2 className="text-lg font-semibold">Mark as sold</h2>
              <button
                type="button"
                onClick={() => setSoldOpen(false)}
                disabled={soldSubmitting}
                className="text-gray-500"
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">
              Pick the buyer from the people who messaged you about this listing. The buyer will be
              able to leave a review afterward.
            </p>

            <label className="mt-4 block text-sm">
              <span className="font-semibold">Buyer</span>
              {soldLoading ? (
                <div className="mt-1 text-xs text-gray-500">Loading buyers...</div>
              ) : soldBuyers.length === 0 ? (
                <div className="mt-1 rounded border border-yellow-300 bg-yellow-50 px-3 py-2 text-xs text-yellow-900">
                  No one has messaged you about this listing yet. The buyer must start a chat with
                  you before you can mark it as sold to them.
                </div>
              ) : (
                <select
                  value={soldBuyerID}
                  onChange={(e) => setSoldBuyerID(e.target.value)}
                  className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
                  required
                >
                  <option value="">Pick a buyer…</option>
                  {soldBuyers.map((s) => (
                    <option key={s.sessionID} value={s.buyerID}>
                      {[s.buyerFirstName, s.buyerLastName].filter(Boolean).join(' ') || `Buyer #${s.buyerID}`}
                    </option>
                  ))}
                </select>
              )}
            </label>

            <label className="mt-4 block text-sm">
              <span className="font-semibold">Sale price</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={soldPrice}
                onChange={(e) => setSoldPrice(e.target.value)}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              />
              <span className="mt-1 block text-xs text-gray-500">
                Defaults to the listing price. Edit if you negotiated a different amount.
              </span>
            </label>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setSoldOpen(false)}
                disabled={soldSubmitting}
                className="rounded border border-gray-300 px-3 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={soldSubmitting || soldLoading || soldBuyers.length === 0}
                className="rounded bg-usc-cardinal px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {soldSubmitting ? 'Saving...' : 'Confirm sale'}
              </button>
            </div>
          </form>
        </div>
      )}

      {reportOpen && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
          onClick={() => !reportSubmitting && setReportOpen(false)}
        >
          <form
            onSubmit={onReportSubmit}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-xl bg-white p-5"
          >
            <div className="flex items-start justify-between">
              <h2 className="text-lg font-semibold">Report this listing</h2>
              <button
                type="button"
                onClick={() => setReportOpen(false)}
                disabled={reportSubmitting}
                className="text-gray-500"
                aria-label="Close"
              >
                ✕
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">
              Tell us what's wrong with this listing. Reports help keep Trojan Market safe.
            </p>
            <label className="mt-3 block text-sm">
              <span className="font-semibold">Reason (optional)</span>
              <textarea
                rows={4}
                value={reportReason}
                onChange={(e) => setReportReason(e.target.value)}
                maxLength={1000}
                placeholder="e.g. prohibited item, suspected scam, wrong category..."
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2 text-sm"
              />
            </label>
            <div className="mt-4 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setReportOpen(false)}
                disabled={reportSubmitting}
                className="rounded border border-gray-300 px-3 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={reportSubmitting}
                className="rounded bg-usc-cardinal px-3 py-2 text-sm font-semibold text-white disabled:opacity-50"
              >
                {reportSubmitting ? 'Submitting...' : 'Submit report'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

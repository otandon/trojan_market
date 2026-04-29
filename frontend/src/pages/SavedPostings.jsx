import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getSavedPostings, removeSavedPosting } from '../api/stats.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function SavedPostings() {
  const { isGuest } = useAuth();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (isGuest) {
      setLoading(false);
      return;
    }
    getSavedPostings()
      .then(setItems)
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [isGuest]);

  const onRemove = async (postID) => {
    await removeSavedPosting(postID);
    setItems((prev) => prev.filter((it) => it.postID !== postID));
  };

  if (isGuest) {
    return <div className="p-6 text-gray-600">Sign in to view your saved listings.</div>;
  }
  if (loading) return <div className="p-6 text-gray-500">Loading...</div>;

  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold">Saved Postings</h1>
      {items.length === 0 ? (
        <p className="mt-3 text-sm text-gray-500">You haven't saved any listings yet.</p>
      ) : (
        <ul className="mt-4 space-y-2">
          {items.map((it) => (
            <li
              key={it.savedID}
              className="flex items-center gap-4 rounded-md border border-gray-200 bg-white p-3"
            >
              <div className="flex h-16 w-16 items-center justify-center rounded bg-gray-100 text-xs text-gray-400">
                📷
              </div>
              <div className="flex-1">
                <Link
                  to={`/listings/${it.postID}`}
                  className="block font-semibold text-gray-900 hover:text-usc-cardinal"
                >
                  {it.title}
                </Link>
                <div className="text-sm text-gray-600">${Number(it.price).toFixed(2)} · {it.status}</div>
                <div className="text-xs text-gray-500">Seller: {it.sellerUsername}</div>
              </div>
              <button
                type="button"
                onClick={() => onRemove(it.postID)}
                className="text-xs text-gray-500 hover:text-red-600"
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

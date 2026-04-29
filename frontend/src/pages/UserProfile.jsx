import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import PostingCard from '../components/PostingCard.jsx';
import { getMyPostings } from '../api/postings.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function UserProfile() {
  const { userID } = useParams();
  const { user } = useAuth();
  const isMe = String(user?.userID) === String(userID);

  const [tab, setTab] = useState('listings');
  const [postings, setPostings] = useState([]);

  useEffect(() => {
    if (!isMe) {
      // TODO: GET /users/{userID}/postings is not implemented backend-side yet.
      setPostings([]);
      return;
    }
    getMyPostings().then(setPostings).catch(() => setPostings([]));
  }, [isMe, userID]);

  return (
    <div className="p-6">
      <header className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gray-200 text-lg font-bold text-gray-700">
            {(user?.username || 'U').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <h1 className="text-xl font-semibold">{isMe ? user?.username : `User #${userID}`}</h1>
            <div className="text-xs text-gray-500">Member of Trojan Market</div>
            <div className="mt-1 text-sm">
              <span className="text-usc-gold">★★★★☆</span>{' '}
              <span className="text-gray-600">— ratings coming soon</span>
              {isMe && user?.isVerified && (
                <span className="ml-2 rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-800">
                  ✓ Verified USC
                </span>
              )}
            </div>
          </div>
        </div>
        <button
          type="button"
          disabled={isMe}
          className="rounded border border-gray-300 px-3 py-1.5 text-sm disabled:opacity-50"
        >
          ⚑ Report User
        </button>
      </header>

      <nav className="mt-6 flex gap-6 border-b border-gray-200 text-sm">
        <button
          type="button"
          onClick={() => setTab('listings')}
          className={`pb-2 ${tab === 'listings' ? 'border-b-2 border-usc-cardinal font-semibold text-usc-cardinal' : 'text-gray-500'}`}
        >
          Active Listings
        </button>
        <button
          type="button"
          onClick={() => setTab('reviews')}
          className={`pb-2 ${tab === 'reviews' ? 'border-b-2 border-usc-cardinal font-semibold text-usc-cardinal' : 'text-gray-500'}`}
        >
          Reviews
        </button>
      </nav>

      <div className="mt-4">
        {tab === 'listings' ? (
          postings.length === 0 ? (
            <div className="text-sm text-gray-500">No active listings.</div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {postings.map((p) => (
                <PostingCard key={p.postID} posting={p} />
              ))}
            </div>
          )
        ) : (
          <div className="text-sm text-gray-500">Reviews list — TODO once /reviews endpoint lands.</div>
        )}
      </div>
    </div>
  );
}

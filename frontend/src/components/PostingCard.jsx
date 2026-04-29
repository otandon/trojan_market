import { Link } from 'react-router-dom';

const STATUS_STYLES = {
  AVAILABLE: 'bg-green-100 text-green-800',
  PENDING: 'bg-yellow-100 text-yellow-800',
  SOLD: 'bg-gray-200 text-gray-700',
};

function StarRating({ value }) {
  const v = typeof value === 'number' ? value : 0;
  return (
    <span className="text-xs text-gray-600">
      <span className="text-usc-gold">{'★'.repeat(Math.round(v))}</span>
      {' '}
      {v.toFixed(1)}
    </span>
  );
}

export default function PostingCard({ posting }) {
  const status = posting.status || 'AVAILABLE';
  return (
    <Link
      to={`/listings/${posting.postID}`}
      className="block overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm transition hover:shadow-md"
    >
      <div className="flex aspect-square items-center justify-center bg-gray-100 text-gray-400 text-xs">
        {posting.photo ? (
          <img src={posting.photo} alt={posting.title} className="h-full w-full object-cover" />
        ) : (
          <span>📷 Photo</span>
        )}
      </div>
      <div className="p-3">
        <div className="truncate text-sm font-semibold text-gray-900">{posting.title}</div>
        <div className="mt-1 text-base font-bold text-usc-cardinal">
          ${Number(posting.price ?? 0).toFixed(2)}
        </div>
        <div className="mt-2 flex items-center justify-between">
          <StarRating value={posting.sellerRating} />
          <span className={`rounded-full px-2 py-0.5 text-xs ${STATUS_STYLES[status] || STATUS_STYLES.AVAILABLE}`}>
            {status[0] + status.slice(1).toLowerCase()}
          </span>
        </div>
      </div>
    </Link>
  );
}

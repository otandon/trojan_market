import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import Sidebar from '../components/Sidebar.jsx';
import PostingCard from '../components/PostingCard.jsx';
import { getCategories, search } from '../api/search.js';

export default function HomeBrowse() {
  const [searchParams] = useSearchParams();
  const q = searchParams.get('q') || '';

  const [filters, setFilters] = useState({
    category: '',
    minPrice: '',
    maxPrice: '',
    sortBy: 'relevant',
  });
  const [categories, setCategories] = useState([]);
  const [postings, setPostings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  const queryKey = useMemo(
    () => JSON.stringify({ q, ...filters }),
    [q, filters],
  );

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    search({ q, ...filters })
      .then((data) => {
        if (!cancelled) setPostings(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err?.response?.data?.error || 'Search failed');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryKey]);

  return (
    <div className="flex">
      <Sidebar filters={filters} setFilters={setFilters} categories={categories} />
      <div className="flex-1 p-4">
        {q && (
          <div className="mb-3 text-sm text-gray-600">
            Results for <span className="font-semibold">"{q}"</span>
          </div>
        )}
        {loading && <div className="text-gray-500">Loading...</div>}
        {error && <div className="text-red-600">{error}</div>}
        {!loading && !error && postings.length === 0 && (
          <div className="text-gray-500">No listings found.</div>
        )}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {postings.map((p) => (
            <PostingCard key={p.postID} posting={p} />
          ))}
        </div>
      </div>
    </div>
  );
}

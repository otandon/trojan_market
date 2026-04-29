import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategories } from '../api/search.js';
import { createPosting } from '../api/postings.js';
import { useAuth } from '../auth/AuthContext.jsx';

const STEPS = ['Details', 'Photos', 'Review'];

export default function CreatePosting() {
  const { isGuest } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState(0);
  const [categories, setCategories] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    title: '',
    category: '',
    price: '',
    description: '',
    pickupLocation: '',
    photos: [],
  });

  useEffect(() => {
    getCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  if (isGuest) {
    return <div className="p-6 text-gray-600">Sign in to create a listing.</div>;
  }

  const update = (patch) => setForm((f) => ({ ...f, ...patch }));

  const onPhotos = (e) => {
    const files = Array.from(e.target.files || []);
    update({ photos: files });
  };

  const submit = async () => {
    setSubmitting(true);
    try {
      const res = await createPosting({
        title: form.title,
        description: form.description || null,
        category: form.category,
        price: Number(form.price),
      });
      navigate(`/listings/${res.postID}`);
    } catch (e) {
      alert(e?.response?.data?.error || 'Could not publish listing');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-semibold">Create a New Listing</h1>

      <ol className="mt-3 flex gap-6 border-b border-gray-200 pb-3 text-sm">
        {STEPS.map((label, i) => (
          <li key={label} className={`flex items-center gap-2 ${i === step ? 'text-usc-cardinal font-semibold' : 'text-gray-500'}`}>
            <span className={`flex h-5 w-5 items-center justify-center rounded-full text-xs ${i === step ? 'bg-usc-cardinal text-white' : 'bg-gray-200 text-gray-600'}`}>
              {i + 1}
            </span>
            {label}
          </li>
        ))}
      </ol>

      <div className="mt-6 max-w-3xl rounded-xl border border-gray-200 bg-white p-6">
        {step === 0 && (
          <div className="grid grid-cols-2 gap-4">
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Title *</span>
              <input
                value={form.title}
                onChange={(e) => update({ title: e.target.value })}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
                placeholder="e.g. Intro to CS Textbook"
              />
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Category *</span>
              <select
                value={form.category}
                onChange={(e) => update({ category: e.target.value })}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
              >
                <option value="">Select a category</option>
                {categories.map((c) => (
                  <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                ))}
              </select>
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Price *</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.price}
                onChange={(e) => update({ price: e.target.value })}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
                placeholder="$ 0.00"
              />
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Pickup Location</span>
              <input
                value={form.pickupLocation}
                onChange={(e) => update({ pickupLocation: e.target.value })}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
                placeholder="e.g. Leavey Library"
              />
            </label>
            <label className="col-span-2 block text-sm">
              <span className="font-semibold">Description</span>
              <textarea
                rows={4}
                value={form.description}
                onChange={(e) => update({ description: e.target.value })}
                className="mt-1 w-full rounded border border-gray-300 px-3 py-2"
                placeholder="Add more details..."
              />
            </label>
          </div>
        )}

        {step === 1 && (
          <div>
            <label className="block text-sm font-semibold">Photos (minimum 1)</label>
            <input type="file" accept="image/*" multiple onChange={onPhotos} className="mt-2" />
            <div className="mt-3 grid grid-cols-4 gap-2">
              {form.photos.map((f, i) => (
                <div key={i} className="rounded border border-gray-200 p-2 text-xs">
                  {f.name}
                </div>
              ))}
            </div>
            <p className="mt-3 text-xs text-gray-500">
              Photo upload backend storage is a TODO — files will not persist yet.
            </p>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-2 text-sm">
            <div><span className="font-semibold">Title:</span> {form.title}</div>
            <div><span className="font-semibold">Category:</span> {form.category || '—'}</div>
            <div><span className="font-semibold">Price:</span> ${form.price || '0.00'}</div>
            <div><span className="font-semibold">Pickup:</span> {form.pickupLocation || '—'}</div>
            <div><span className="font-semibold">Description:</span> {form.description || '—'}</div>
            <div><span className="font-semibold">Photos:</span> {form.photos.length}</div>
          </div>
        )}

        <div className="mt-6 flex justify-between">
          <button
            type="button"
            onClick={() => setStep((s) => Math.max(0, s - 1))}
            disabled={step === 0}
            className="rounded border border-gray-300 px-4 py-2 text-sm disabled:opacity-50"
          >
            Back
          </button>
          {step < STEPS.length - 1 ? (
            <button
              type="button"
              onClick={() => setStep((s) => s + 1)}
              className="rounded bg-usc-cardinal px-4 py-2 text-sm font-semibold text-white"
            >
              Next: {STEPS[step + 1]} →
            </button>
          ) : (
            <button
              type="button"
              onClick={submit}
              disabled={submitting || !form.title || !form.category || !form.price}
              className="rounded bg-usc-cardinal px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            >
              {submitting ? 'Publishing...' : 'Publish'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

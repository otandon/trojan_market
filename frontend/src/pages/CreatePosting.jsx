import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCategories } from '../api/search.js';
import { createPosting } from '../api/postings.js';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

const STEPS = ['Details', 'Photos', 'Review'];
const CONDITIONS = ['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'POOR'];
const MAX_PHOTOS = 8;
const MAX_PHOTO_BYTES = 1_000_000; // 1 MB

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

export default function CreatePosting() {
  const { isGuest } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [step, setStep] = useState(0);
  const [categories, setCategories] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState({});
  const [form, setForm] = useState({
    title: '',
    category: '',
    price: '',
    condition: '',
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

  const update = (patch) => {
    setForm((f) => ({ ...f, ...patch }));
    // Clear field-level errors as the user fixes them.
    setErrors((prev) => {
      const next = { ...prev };
      Object.keys(patch).forEach((k) => delete next[k]);
      return next;
    });
  };

  const onPhotos = async (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    const oversize = files.find((f) => f.size > MAX_PHOTO_BYTES);
    if (oversize) {
      setErrors((p) => ({ ...p, photos: `"${oversize.name}" exceeds 1 MB. Resize before uploading.` }));
      return;
    }
    if (files.length > MAX_PHOTOS) {
      setErrors((p) => ({ ...p, photos: `You can upload up to ${MAX_PHOTOS} photos.` }));
      return;
    }

    try {
      const items = await Promise.all(
        files.map(async (f) => ({ name: f.name, dataUrl: await readFileAsDataUrl(f) })),
      );
      update({ photos: items });
    } catch {
      setErrors((p) => ({ ...p, photos: 'Could not read one of the files. Try again.' }));
    }
  };

  const validateDetails = () => {
    const e = {};
    if (!form.title.trim()) e.title = 'Title is required';
    if (!form.category) e.category = 'Category is required';
    if (form.price === '' || form.price === null || Number.isNaN(Number(form.price))) {
      e.price = 'Price is required';
    } else if (Number(form.price) < 0) {
      e.price = 'Price must be 0 or greater';
    }
    if (!form.condition) e.condition = 'Condition is required';
    return e;
  };

  const validatePhotos = () => {
    const e = {};
    if (!form.photos || form.photos.length === 0) {
      e.photos = 'Please upload at least one photo';
    }
    return e;
  };

  const onNext = () => {
    const stepErrors = step === 0 ? validateDetails() : step === 1 ? validatePhotos() : {};
    setErrors(stepErrors);
    if (Object.keys(stepErrors).length === 0) {
      setStep((s) => s + 1);
    }
  };

  const submit = async () => {
    const detailErrors = validateDetails();
    const photoErrors = validatePhotos();
    const all = { ...detailErrors, ...photoErrors };
    if (Object.keys(all).length > 0) {
      setErrors(all);
      toast.error('Please fix the errors before publishing');
      return;
    }
    setSubmitting(true);
    try {
      const res = await createPosting({
        title: form.title,
        description: form.description || null,
        category: form.category,
        price: Number(form.price),
        // condition is client-side only for now (backend schema migration TODO).
        condition: form.condition,
        photos: form.photos.map((p) => p.dataUrl),
      });
      toast.success('Listing published');
      navigate(`/listings/${res.postID}`);
    } catch (e) {
      toast.error(apiErrorMessage(e, 'Could not publish listing'));
    } finally {
      setSubmitting(false);
    }
  };

  const fieldClass = (field) =>
    `mt-1 w-full rounded border px-3 py-2 ${errors[field] ? 'border-red-500' : 'border-gray-300'}`;

  const FieldError = ({ field }) =>
    errors[field] ? (
      <span className="mt-1 block text-xs text-red-600">{errors[field]}</span>
    ) : null;

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
                className={fieldClass('title')}
                placeholder="e.g. Intro to CS Textbook"
              />
              <FieldError field="title" />
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Category *</span>
              <select
                value={form.category}
                onChange={(e) => update({ category: e.target.value })}
                className={fieldClass('category')}
              >
                <option value="">Select a category</option>
                {categories.map((c) => (
                  <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                ))}
              </select>
              <FieldError field="category" />
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Price *</span>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.price}
                onChange={(e) => update({ price: e.target.value })}
                className={fieldClass('price')}
                placeholder="$ 0.00"
              />
              <FieldError field="price" />
            </label>
            <label className="col-span-1 block text-sm">
              <span className="font-semibold">Condition *</span>
              <select
                value={form.condition}
                onChange={(e) => update({ condition: e.target.value })}
                className={fieldClass('condition')}
              >
                <option value="">Select a condition</option>
                {CONDITIONS.map((c) => (
                  <option key={c} value={c}>{c.replaceAll('_', ' ')}</option>
                ))}
              </select>
              <FieldError field="condition" />
            </label>
            <label className="col-span-2 block text-sm">
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
            <label className="block text-sm font-semibold">
              Photos * (at least 1 required, up to {MAX_PHOTOS}, 1 MB each)
            </label>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={onPhotos}
              className={`mt-2 block ${errors.photos ? 'rounded border border-red-500 p-1' : ''}`}
            />
            {errors.photos && (
              <span className="mt-1 block text-xs text-red-600">{errors.photos}</span>
            )}
            <div className="mt-3 grid grid-cols-4 gap-2">
              {form.photos.map((p, i) => (
                <div key={i} className="overflow-hidden rounded border border-gray-200">
                  <img src={p.dataUrl} alt={p.name} className="aspect-square w-full object-cover" />
                  <div className="truncate px-2 py-1 text-xs text-gray-600">{p.name}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="space-y-2 text-sm">
            <div><span className="font-semibold">Title:</span> {form.title}</div>
            <div><span className="font-semibold">Category:</span> {form.category || '—'}</div>
            <div><span className="font-semibold">Price:</span> ${form.price || '0.00'}</div>
            <div><span className="font-semibold">Condition:</span> {form.condition || '—'}</div>
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
              onClick={onNext}
              className="rounded bg-usc-cardinal px-4 py-2 text-sm font-semibold text-white"
            >
              Next: {STEPS[step + 1]} →
            </button>
          ) : (
            <button
              type="button"
              onClick={submit}
              disabled={submitting}
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

import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function Signup() {
  const { isGuest, ready, signup } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();

  const [form, setForm] = useState({ firstName: '', lastName: '', email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (ready && !isGuest) {
      navigate('/', { replace: true });
    }
  }, [ready, isGuest, navigate]);

  const update = (patch) => {
    setForm((f) => ({ ...f, ...patch }));
    setErrors((prev) => {
      const next = { ...prev };
      Object.keys(patch).forEach((k) => delete next[k]);
      return next;
    });
  };

  const validate = () => {
    const e = {};
    if (!form.firstName.trim()) e.firstName = 'First name is required';
    if (!form.lastName.trim()) e.lastName = 'Last name is required';

    const email = form.email.trim().toLowerCase();
    if (!email) {
      e.email = 'Email is required';
    } else if (!email.endsWith('@usc.edu')) {
      e.email = 'Only @usc.edu emails are accepted';
    }

    if (!form.password) {
      e.password = 'Password is required';
    } else if (form.password.length < 8) {
      e.password = 'Password must be at least 8 characters';
    }
    return e;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;

    const email = form.email.trim().toLowerCase();
    setSubmitting(true);
    try {
      await signup({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email,
        password: form.password,
      });
      toast.success('Account created — check your inbox for a verification code.');
      navigate('/verify', { state: { email } });
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not create account'));
    } finally {
      setSubmitting(false);
    }
  };

  const fieldClass = (k) =>
    `mt-1 w-full rounded border px-3 py-2 ${errors[k] ? 'border-red-500' : 'border-gray-300'}`;

  return (
    <div className="flex min-h-[calc(100vh-72px)] items-center justify-center px-4 py-10">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
        <div className="text-center">
          <div className="text-3xl font-bold text-usc-cardinal">Create account</div>
          <div className="mt-1 text-sm text-gray-600">USC students only — we'll verify your @usc.edu email</div>
        </div>

        <div className="mt-6 grid grid-cols-2 gap-3">
          <label className="block text-sm">
            <span className="font-semibold">First name</span>
            <input
              value={form.firstName}
              onChange={(e) => update({ firstName: e.target.value })}
              autoComplete="given-name"
              className={fieldClass('firstName')}
            />
            {errors.firstName && <span className="mt-1 block text-xs text-red-600">{errors.firstName}</span>}
          </label>
          <label className="block text-sm">
            <span className="font-semibold">Last name</span>
            <input
              value={form.lastName}
              onChange={(e) => update({ lastName: e.target.value })}
              autoComplete="family-name"
              className={fieldClass('lastName')}
            />
            {errors.lastName && <span className="mt-1 block text-xs text-red-600">{errors.lastName}</span>}
          </label>
        </div>

        <label className="mt-4 block text-sm">
          <span className="font-semibold">USC email</span>
          <input
            type="email"
            value={form.email}
            onChange={(e) => update({ email: e.target.value })}
            placeholder="ttrojan@usc.edu"
            autoComplete="email"
            className={fieldClass('email')}
          />
          {errors.email && <span className="mt-1 block text-xs text-red-600">{errors.email}</span>}
        </label>

        <label className="mt-4 block text-sm">
          <span className="font-semibold">Password</span>
          <input
            type="password"
            value={form.password}
            onChange={(e) => update({ password: e.target.value })}
            placeholder="At least 8 characters"
            autoComplete="new-password"
            className={fieldClass('password')}
          />
          {errors.password && <span className="mt-1 block text-xs text-red-600">{errors.password}</span>}
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="mt-6 w-full rounded-md bg-usc-cardinal py-2.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          {submitting ? 'Creating account...' : 'Create account'}
        </button>

        <div className="mt-4 text-center text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="font-semibold text-usc-cardinal hover:underline">
            Sign in
          </Link>
        </div>
      </form>
    </div>
  );
}

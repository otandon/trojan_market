import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function Login() {
  const { isGuest, ready, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = location.state?.from || '/';

  useEffect(() => {
    if (ready && !isGuest) {
      navigate(redirectTo, { replace: true });
    }
  }, [ready, isGuest, navigate, redirectTo]);

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
    if (!form.email.trim()) e.email = 'Email is required';
    if (!form.password) e.password = 'Password is required';
    return e;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;

    setSubmitting(true);
    try {
      await login({ email: form.email.trim().toLowerCase(), password: form.password });
      toast.success('Welcome back!');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      const msg = apiErrorMessage(err, 'Sign-in failed');
      // If the backend says "please verify", offer a route to /verify.
      if (msg.toLowerCase().includes('verify')) {
        toast.error(msg);
        navigate('/verify', { state: { email: form.email.trim().toLowerCase() } });
      } else {
        toast.error(msg);
      }
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
          <div className="text-3xl font-bold text-usc-cardinal">Trojan Market</div>
          <div className="mt-1 text-sm text-gray-600">Sign in to your account</div>
        </div>

        <label className="mt-6 block text-sm">
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
            autoComplete="current-password"
            className={fieldClass('password')}
          />
          {errors.password && <span className="mt-1 block text-xs text-red-600">{errors.password}</span>}
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="mt-6 w-full rounded-md bg-usc-cardinal py-2.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          {submitting ? 'Signing in...' : 'Sign in'}
        </button>

        <div className="mt-4 text-center text-sm text-gray-600">
          Don't have an account?{' '}
          <Link to="/signup" className="font-semibold text-usc-cardinal hover:underline">
            Sign up
          </Link>
        </div>
      </form>
    </div>
  );
}

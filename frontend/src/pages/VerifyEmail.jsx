import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function VerifyEmail() {
  const { verifyEmail, resendVerification } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const initialEmail = (location.state?.email || '').trim().toLowerCase();
  const [email, setEmail] = useState(initialEmail);
  const [code, setCode] = useState('');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);

  useEffect(() => {
    if (!initialEmail) {
      // No email in state — user navigated here directly. Allow them to enter it manually.
      return;
    }
  }, [initialEmail]);

  const validate = () => {
    const e = {};
    if (!email) e.email = 'Email is required';
    else if (!email.toLowerCase().endsWith('@usc.edu')) e.email = 'Only @usc.edu emails are accepted';
    if (!code.trim()) e.code = 'Verification code is required';
    return e;
  };

  const onSubmit = async (ev) => {
    ev.preventDefault();
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length) return;

    setSubmitting(true);
    try {
      await verifyEmail({ email: email.trim().toLowerCase(), code: code.trim() });
      toast.success('Email verified — welcome to Trojan Market!');
      navigate('/', { replace: true });
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Verification failed'));
    } finally {
      setSubmitting(false);
    }
  };

  const onResend = async () => {
    if (!email) {
      setErrors((e) => ({ ...e, email: 'Enter your email first' }));
      return;
    }
    setResending(true);
    try {
      await resendVerification(email.trim().toLowerCase());
      toast.success('A new verification code has been sent.');
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Could not resend code'));
    } finally {
      setResending(false);
    }
  };

  const fieldClass = (k) =>
    `mt-1 w-full rounded border px-3 py-2 ${errors[k] ? 'border-red-500' : 'border-gray-300'}`;

  return (
    <div className="flex min-h-[calc(100vh-72px)] items-center justify-center px-4 py-10">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
        <div className="text-center">
          <div className="text-3xl font-bold text-usc-cardinal">Verify your email</div>
          <div className="mt-1 text-sm text-gray-600">
            We sent a 6-digit code to {initialEmail || 'your USC email'}.
          </div>
        </div>

        {!initialEmail && (
          <label className="mt-6 block text-sm">
            <span className="font-semibold">USC email</span>
            <input
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setErrors((p) => ({ ...p, email: undefined })); }}
              placeholder="ttrojan@usc.edu"
              className={fieldClass('email')}
            />
            {errors.email && <span className="mt-1 block text-xs text-red-600">{errors.email}</span>}
          </label>
        )}

        <label className="mt-6 block text-sm">
          <span className="font-semibold">Verification code</span>
          <input
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => { setCode(e.target.value.replace(/\D/g, '')); setErrors((p) => ({ ...p, code: undefined })); }}
            placeholder="123456"
            className={`${fieldClass('code')} text-center tracking-[0.5em] text-lg font-mono`}
            autoFocus
          />
          {errors.code && <span className="mt-1 block text-xs text-red-600">{errors.code}</span>}
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="mt-6 w-full rounded-md bg-usc-cardinal py-2.5 text-sm font-semibold text-white disabled:opacity-50"
        >
          {submitting ? 'Verifying...' : 'Verify and sign in'}
        </button>

        <div className="mt-4 flex items-center justify-between text-sm">
          <button
            type="button"
            onClick={onResend}
            disabled={resending}
            className="text-usc-cardinal hover:underline disabled:opacity-50"
          >
            {resending ? 'Sending...' : 'Resend code'}
          </button>
          <Link to="/login" className="text-gray-600 hover:underline">
            Back to sign in
          </Link>
        </div>
      </form>
    </div>
  );
}

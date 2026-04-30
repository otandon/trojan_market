import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext.jsx';
import { apiErrorMessage, useToast } from '../components/Toast.jsx';

export default function Login() {
  const { isGuest, ready, loginWithDevEmail } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [showDevForm, setShowDevForm] = useState(false);
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const redirectTo = location.state?.from || '/';

  useEffect(() => {
    if (ready && !isGuest) {
      navigate(redirectTo, { replace: true });
    }
  }, [ready, isGuest, navigate, redirectTo]);

  const onSignInWithUSC = () => {
    // TODO(USC SSO): Replace with a redirect to the backend SSO start endpoint, e.g.
    //   window.location.href = `${import.meta.env.VITE_API_BASE_URL}/auth/sso/start`;
    // which will 302 to https://shibboleth.usc.edu/idp/profile/SAML2/Redirect/SSO,
    // run Duo MFA, and POST a SAMLResponse back to /auth/sso/callback. For now,
    // open the dev-only mock SSO form below.
    setShowDevForm(true);
  };

  const onDevSubmit = async (e) => {
    e.preventDefault();
    const trimmed = email.trim().toLowerCase();
    if (!trimmed) {
      setEmailError('Email is required');
      return;
    }
    if (!trimmed.endsWith('@usc.edu')) {
      setEmailError('Only @usc.edu emails are accepted');
      return;
    }
    setEmailError('');
    setSubmitting(true);
    try {
      await loginWithDevEmail(trimmed);
      toast.success('Signed in');
      navigate(redirectTo, { replace: true });
    } catch (err) {
      toast.error(apiErrorMessage(err, 'Sign-in failed'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-72px)] items-center justify-center px-4 py-10">
      <div className="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
        <div className="text-center">
          <div className="text-3xl font-bold text-usc-cardinal">Trojan Market</div>
          <div className="mt-1 text-sm text-gray-600">Sign in with your USC account to continue</div>
        </div>

        <button
          type="button"
          onClick={onSignInWithUSC}
          className="mt-8 flex w-full items-center justify-center gap-2 rounded-md bg-usc-cardinal px-4 py-3 text-sm font-semibold text-white hover:bg-usc-cardinal/90"
        >
          <span aria-hidden="true">✦</span> Sign in with USC
        </button>

        <p className="mt-3 text-center text-xs text-gray-500">
          You'll be redirected to USC's single sign-on (Shibboleth + Duo MFA).
        </p>

        {showDevForm && (
          <form onSubmit={onDevSubmit} className="mt-8 rounded-md border border-dashed border-gray-300 bg-gray-50 p-4">
            <div className="text-xs font-semibold uppercase tracking-wide text-gray-500">
              Mock USC SSO (dev only)
            </div>
            <p className="mt-1 text-xs text-gray-500">
              Real Shibboleth integration is not yet wired up. Enter a USC email to issue a dev JWT.
            </p>
            <label className="mt-3 block text-sm">
              <span className="font-semibold">USC email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="ttrojan@usc.edu"
                autoFocus
                className={`mt-1 w-full rounded border px-3 py-2 ${emailError ? 'border-red-500' : 'border-gray-300'}`}
              />
              {emailError && <span className="mt-1 block text-xs text-red-600">{emailError}</span>}
            </label>
            <button
              type="submit"
              disabled={submitting}
              className="mt-3 w-full rounded-md bg-usc-cardinal px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            >
              {submitting ? 'Signing in...' : 'Continue'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

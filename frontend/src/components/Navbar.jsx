import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Navbar() {
  const { user, isGuest, logout } = useAuth();
  const [search, setSearch] = useState('');
  const navigate = useNavigate();

  const onSearch = (e) => {
    e.preventDefault();
    const q = search.trim();
    navigate(q ? `/?q=${encodeURIComponent(q)}` : '/');
  };

  return (
    <header className="sticky top-0 z-30 border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3">
        <Link to="/" className="text-xl font-bold text-usc-cardinal whitespace-nowrap">
          Trojan Market
        </Link>

        <form onSubmit={onSearch} className="flex-1">
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search listings..."
            className="w-full rounded-full border border-gray-300 px-4 py-2 text-sm focus:border-usc-cardinal focus:outline-none focus:ring-1 focus:ring-usc-cardinal"
          />
        </form>

        {!isGuest && (
          <>
            <NavLink
              to="/messages"
              className={({ isActive }) =>
                `text-sm font-semibold ${isActive ? 'text-usc-cardinal' : 'text-gray-700 hover:text-usc-cardinal'}`
              }
            >
              Messages
            </NavLink>
            <NavLink
              to="/saved"
              className={({ isActive }) =>
                `text-sm font-semibold ${isActive ? 'text-usc-cardinal' : 'text-gray-700 hover:text-usc-cardinal'}`
              }
            >
              Saved
            </NavLink>
            <NavLink
              to="/purchases"
              className={({ isActive }) =>
                `text-sm font-semibold ${isActive ? 'text-usc-cardinal' : 'text-gray-700 hover:text-usc-cardinal'}`
              }
            >
              Purchases
            </NavLink>
          </>
        )}

        {isGuest ? (
          <>
            <Link
              to="/login"
              className="rounded-full border border-usc-cardinal px-4 py-1.5 text-sm font-semibold text-usc-cardinal hover:bg-usc-cardinal hover:text-white"
            >
              Sign In
            </Link>
            <Link
              to="/signup"
              className="rounded-full bg-usc-cardinal px-4 py-1.5 text-sm font-semibold text-white hover:bg-usc-cardinal/90"
            >
              Sign Up
            </Link>
          </>
        ) : (
          <Link
            to={`/users/${user.userID}`}
            aria-label="Profile"
            className="flex h-9 w-9 items-center justify-center rounded-full bg-gray-200 text-sm font-semibold text-gray-700 hover:bg-gray-300"
          >
            {((user.firstName?.[0] || '') + (user.lastName?.[0] || '') || (user.username || '?').slice(0, 2)).toUpperCase()}
          </Link>
        )}

        {!isGuest && (
          <Link
            to="/sell"
            className="rounded-full bg-usc-cardinal px-4 py-1.5 text-sm font-semibold text-white hover:bg-usc-cardinal/90"
          >
            + Sell
          </Link>
        )}

        {!isGuest && (
          <button
            type="button"
            onClick={logout}
            className="text-xs text-gray-500 hover:text-gray-800"
          >
            Logout
          </button>
        )}
      </div>
    </header>
  );
}

import { Route, Routes } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import { ToastProvider } from './components/Toast.jsx';
import HomeBrowse from './pages/HomeBrowse.jsx';
import ListingDetail from './pages/ListingDetail.jsx';
import Login from './pages/Login.jsx';
import Messages from './pages/Messages.jsx';
import CreatePosting from './pages/CreatePosting.jsx';
import UserProfile from './pages/UserProfile.jsx';
import SavedPostings from './pages/SavedPostings.jsx';
import PastPurchases from './pages/PastPurchases.jsx';

export default function App() {
  return (
    <ToastProvider>
      <div className="min-h-full bg-gray-50">
        <Navbar />
        <main className="mx-auto max-w-7xl">
          <Routes>
            <Route path="/" element={<HomeBrowse />} />
            <Route path="/login" element={<Login />} />
            <Route path="/listings/:postID" element={<ListingDetail />} />
            <Route path="/messages" element={<Messages />} />
            <Route path="/messages/:sessionID" element={<Messages />} />
            <Route path="/sell" element={<CreatePosting />} />
            <Route path="/users/:userID" element={<UserProfile />} />
            <Route path="/saved" element={<SavedPostings />} />
            <Route path="/purchases" element={<PastPurchases />} />
          </Routes>
        </main>
      </div>
    </ToastProvider>
  );
}

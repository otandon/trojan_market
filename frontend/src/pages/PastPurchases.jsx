import { useEffect, useState } from 'react';
import { getChatForPost, getPastPurchases } from '../api/stats.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function PastPurchases() {
  const { isGuest, user } = useAuth();
  const [purchases, setPurchases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openTx, setOpenTx] = useState(null);
  const [chat, setChat] = useState([]);

  useEffect(() => {
    if (isGuest) {
      setLoading(false);
      return;
    }
    getPastPurchases()
      .then(setPurchases)
      .catch(() => setPurchases([]))
      .finally(() => setLoading(false));
  }, [isGuest]);

  const openDetails = async (tx) => {
    setOpenTx(tx);
    setChat([]);
    try {
      const messages = await getChatForPost(tx.postID);
      setChat(messages);
    } catch {
      setChat([]);
    }
  };

  if (isGuest) {
    return <div className="p-6 text-gray-600">Sign in to view your purchase history.</div>;
  }
  if (loading) return <div className="p-6 text-gray-500">Loading...</div>;

  return (
    <div className="p-6">
      <h1 className="text-xl font-semibold">Past Purchases</h1>
      {purchases.length === 0 ? (
        <p className="mt-3 text-sm text-gray-500">No purchases yet.</p>
      ) : (
        <ul className="mt-4 space-y-2">
          {purchases.map((tx) => (
            <li key={tx.transactionID}>
              <button
                type="button"
                onClick={() => openDetails(tx)}
                className="w-full rounded-md border border-gray-200 bg-white p-3 text-left hover:border-usc-cardinal"
              >
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-semibold text-gray-900">{tx.postTitle}</div>
                    <div className="text-xs text-gray-500">Seller: {tx.sellerUsername}</div>
                  </div>
                  <div className="text-right">
                    <div className="font-semibold">${Number(tx.salePrice).toFixed(2)}</div>
                    <div className="text-xs text-gray-500">
                      {tx.transactionTime ? new Date(tx.transactionTime).toLocaleDateString() : ''}
                    </div>
                  </div>
                </div>
              </button>
            </li>
          ))}
        </ul>
      )}

      {openTx && (
        <div
          className="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setOpenTx(null)}
        >
          <div
            className="max-h-[80vh] w-full max-w-lg overflow-y-auto rounded-xl bg-white p-5"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">{openTx.postTitle}</h2>
              <button type="button" onClick={() => setOpenTx(null)} className="text-gray-500">
                ✕
              </button>
            </div>
            <div className="mt-2 text-sm text-gray-600">
              ${Number(openTx.salePrice).toFixed(2)} · Seller: {openTx.sellerUsername}
            </div>
            <h3 className="mt-4 text-xs font-bold uppercase text-gray-500">Chat history</h3>
            <div className="mt-2 space-y-2">
              {chat.length === 0 && <div className="text-xs text-gray-400">No messages.</div>}
              {chat.map((m) => {
                const mine = m.senderID === user?.userID;
                return (
                  <div key={m.messageID} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div className={`max-w-[80%] rounded-2xl px-3 py-1.5 text-sm ${mine ? 'bg-usc-cardinal text-white' : 'bg-gray-100 text-gray-900'}`}>
                      {m.messageText}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

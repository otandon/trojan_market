import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getHistory, getMySessions } from '../api/chat.js';
import { createStompClient } from '../ws/stompClient.js';
import { useAuth } from '../auth/AuthContext.jsx';

export default function Messages() {
  const { user, isGuest } = useAuth();
  const { sessionID: routeSessionID } = useParams();
  const navigate = useNavigate();

  const [sessions, setSessions] = useState([]);
  const [activeID, setActiveID] = useState(routeSessionID ? Number(routeSessionID) : null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState(null);

  const clientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const scrollerRef = useRef(null);

  useEffect(() => {
    if (isGuest) return;
    getMySessions().then(setSessions).catch(() => setSessions([]));
  }, [isGuest]);

  useEffect(() => {
    if (!activeID) return;
    setMessages([]);
    getHistory(activeID).then(setMessages).catch(() => setMessages([]));
  }, [activeID]);

  useEffect(() => {
    if (isGuest) return;
    const client = createStompClient({
      onConnect: (c) => {
        clientRef.current = c;
        if (activeID) subscribe(c, activeID);
      },
      onError: (frame) => setError(frame?.headers?.message || 'WebSocket error'),
    });
    return () => {
      try {
        subscriptionRef.current?.unsubscribe();
      } catch {
        // noop
      }
      client.deactivate();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isGuest]);

  const subscribe = useCallback((client, sid) => {
    if (subscriptionRef.current) {
      try {
        subscriptionRef.current.unsubscribe();
      } catch {
        // noop
      }
    }
    subscriptionRef.current = client.subscribe(`/topic/chat/${sid}`, (frame) => {
      const m = JSON.parse(frame.body);
      setMessages((prev) => [...prev, m]);
    });
  }, []);

  useEffect(() => {
    const c = clientRef.current;
    if (c?.connected && activeID) subscribe(c, activeID);
  }, [activeID, subscribe]);

  useEffect(() => {
    scrollerRef.current?.scrollTo({ top: scrollerRef.current.scrollHeight });
  }, [messages]);

  const onSelect = (sid) => {
    setActiveID(sid);
    navigate(`/messages/${sid}`, { replace: true });
  };

  const onSend = (e) => {
    e.preventDefault();
    const text = draft.trim();
    if (!text || !activeID || !clientRef.current?.connected) return;
    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ sessionID: activeID, messageText: text }),
    });
    setDraft('');
  };

  const activeSession = useMemo(
    () => sessions.find((s) => s.sessionID === activeID),
    [sessions, activeID],
  );

  if (isGuest) {
    return <div className="p-6 text-gray-600">Sign in to view your messages.</div>;
  }

  return (
    <div className="grid h-[calc(100vh-72px)] grid-cols-12 border-t border-gray-200">
      <aside className="col-span-4 overflow-y-auto border-r border-gray-200 bg-white">
        <div className="border-b border-gray-200 px-4 py-3 text-sm font-semibold">Messages</div>
        <ul>
          {sessions.length === 0 && (
            <li className="p-4 text-sm text-gray-500">No conversations yet.</li>
          )}
          {sessions.map((s) => {
            const sellerName = [s.sellerFirstName, s.sellerLastName].filter(Boolean).join(' ') || `Seller #${s.sellerID}`;
            return (
              <li key={s.sessionID}>
                <button
                  type="button"
                  onClick={() => onSelect(s.sessionID)}
                  className={`w-full px-4 py-3 text-left text-sm hover:bg-gray-50 ${
                    s.sessionID === activeID ? 'bg-usc-cardinal/5' : ''
                  }`}
                >
                  <div className="font-semibold text-gray-900 truncate">{s.postTitle || `Listing #${s.postID}`}</div>
                  <div className="truncate text-xs text-gray-500">Seller: {sellerName}</div>
                </button>
              </li>
            );
          })}
        </ul>
      </aside>

      <section className="col-span-8 flex flex-col">
        {activeID ? (
          <>
            <div className="border-b border-gray-200 px-4 py-3 text-sm">
              <div className="font-semibold">
                {activeSession?.postTitle || (activeSession?.postID ? `Listing #${activeSession.postID}` : '—')}
              </div>
              {activeSession && (
                <div className="text-xs text-gray-500">
                  Seller: {[activeSession.sellerFirstName, activeSession.sellerLastName].filter(Boolean).join(' ') || `Seller #${activeSession.sellerID}`}
                </div>
              )}
            </div>
            <div ref={scrollerRef} className="flex-1 overflow-y-auto bg-gray-50 p-4 space-y-2">
              {messages.map((m) => {
                const mine = m.senderID === user?.userID;
                return (
                  <div key={m.messageID || `${m.senderID}-${m.messageTime}`} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    <div
                      className={`max-w-[75%] rounded-2xl px-3 py-2 text-sm ${
                        mine ? 'bg-usc-cardinal text-white' : 'bg-white text-gray-900 border border-gray-200'
                      }`}
                    >
                      {m.messageText}
                    </div>
                  </div>
                );
              })}
            </div>
            <form onSubmit={onSend} className="flex gap-2 border-t border-gray-200 bg-white p-3">
              <input
                value={draft}
                onChange={(e) => setDraft(e.target.value)}
                placeholder="Type a message..."
                className="flex-1 rounded-full border border-gray-300 px-4 py-2 text-sm focus:border-usc-cardinal focus:outline-none"
              />
              <button
                type="submit"
                className="rounded-full bg-usc-cardinal px-4 py-2 text-sm font-semibold text-white"
              >
                Send
              </button>
            </form>
            {error && <div className="bg-red-50 px-4 py-2 text-xs text-red-700">{error}</div>}
          </>
        ) : (
          <div className="m-auto text-gray-500">Pick a conversation</div>
        )}
      </section>
    </div>
  );
}

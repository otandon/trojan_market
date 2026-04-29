# Trojan Market — Claude Code Context

## Project
USC-exclusive second-hand marketplace. CSCI 201L, Prof. Marco Papa.
Waterfall process. Academic submission.

## Stack
- Frontend: React 18 + Vite, hosted on Vercel
- Backend: Spring Boot 3.x (Java 17), hosted on Railway
- Database: MySQL 8.0+
- Real-time: Spring WebSocket + STOMP over SockJS
- Auth: USC SSO + Duo MFA → JWT
- Migrations: Flyway
- Containerization: Docker + docker-compose
- CI/CD: GitHub Actions

## Hard Constraints
- NO JSP. NO Java UX frameworks (no Thymeleaf, no Vaadin, nothing like that).
- Multi-threading REQUIRED: WebSocket broker must run on a dedicated thread pool separate from HTTP threads.
- Networking REQUIRED: Both WebSocket and REST API must be used.
- Only @usc.edu emails accepted at the application layer.
- Users with 10+ reports = "high-risk": blocked from messaging and purchasing features.
- senderID in messages MUST be extracted from the JWT server-side. Never trust the client payload for this.
- Return HTTP 403 (not 401) for all access control violations.
- Spring Security enforces all access rules. Frontend button disabling is UX only.

## Project Structure
- /frontend — React app (Vite scaffold)
- /backend — Spring Boot Maven project (NOT Gradle)

## Database: 9 Tables
Users, Postings, ChatSessions, Messages, Transactions,
SavedPostings, Notifications, NotificationPreferences, Reviews

## Exact Column Names — match these precisely in all entity classes and queries

Users: userID, username, password, email, isVerified, review, reviewCount, is_active
Postings: postID, sellerID, title, description, category, status, price, postTime, is_active
ChatSessions: sessionID, postID, buyerID, sellerID, created_at
Messages: messageID, sessionID, senderID, messageText, messageTime, is_read
Transactions: transactionID, postID, buyerID, sellerID, sale_price, transactionTime
SavedPostings: savedID, userID, postID, savedTime
Notifications: notificationID, receiverID, type, relatedPostID, relatedSessionID, isRead, createdAt
NotificationPreferences: userID, new_message, new_offer, offer_response, item_sold
Reviews: reviewID, reviewerID, sellerID, transactionID, rating, comment, reviewTime

## ENUM Values — exact spelling

Category: BOOKS, CLOTHING_AND_ACCESSORIES, ELECTRONICS, FURNITURE, SCHOOL_SUPPLIES, TICKETS, OTHER
Posting status: AVAILABLE, PENDING, SOLD
Notification type: NEW_MESSAGE, NEW_OFFER, OFFER_ACCEPTED, OFFER_REJECTED, ITEM_SOLD, ITEM_PURCHASED
Item condition: NEW, LIKE_NEW, GOOD, FAIR, POOR

## Backend Package Structure
Root package: com.trojanmarket
Subpackages: controller, service, manager, dto, entity, repository, security, config

## Key Classes

Controllers: AuthController, ChatController, PostingController,
             SearchController, StatsController, NotificationController

Services: AuthService, ChatService, PostingService, NotificationService

Managers (JDBC direct — do NOT use JPA for these):
  SearchManager, StatsManager

DTOs: ChatMessageDTO, PostingSummaryDTO, PostingDetailDTO, TransactionDTO

Security: JwtFilter, SecurityConfig

## API Base Path
/api/v1/

## Auth Flow
1. Frontend redirects user to USC SSO login page
2. User authenticates and completes Duo MFA
3. USC redirects back to backend with an SSO token
4. AuthController.handleSSOCallback(token) receives it
5. AuthService.handleSSOLogin() validates @usc.edu domain, creates user if new, issues JWT
6. JWT returned to client, stored, sent as Bearer token on all subsequent requests
7. JwtFilter validates token on every protected request

## AuthService Methods
- handleSSOLogin(User)
- createUserIfNotExists(User)
- validateUSCEmail(email)
- isBannedUser(userID) — returns true if report count >= 10

## WebSocket Architecture
Broker runs on dedicated thread pool (NOT the HTTP thread pool).

Channels:
- /app/chat.send           → client sends a message
- /topic/chat/{sessionID}  → server broadcasts to both participants
- /user/{userID}/queue/notifications → private notification delivery
- /user/{userID}/queue/unread        → unread badge count update

## ChatService Methods
- getOrCreateSession(postID, buyerID) — throws 403 if high-risk or guest
- sendMessage(sessionID, senderID, text) — persists, broadcasts, triggers notification
- getHistory(sessionID, callerID) — throws 403 if caller not a participant
- validateParticipant(sessionID, userID) — helper, throws ForbiddenException if outsider

## Search Algorithm (implement in SearchManager using JDBC)
1. Clean input: trim → lowercase → strip special chars → split by whitespace into keyword array
2. Phase 1 — SQL: pull all AVAILABLE postings where any keyword appears in title OR description
3. Phase 2 — Relevance scoring per posting:
   +3 = whole word match in title
   +2 = substring match in title
   +1 = substring match in description
   Total = sum across all keywords
4. Sort options: relevant (default, score desc), price_asc, price_dec, newest (postTime desc)
5. Return List<PostingSummaryDTO>: postID, title, price, photo, sellerID, sellerRating, status

## StatsManager Methods (JDBC direct)
- getPastPurchases(userID) — all transactions where user is buyer
- getSoldItems(userID) — all transactions where user is seller
- getSavedPostings(userID) — registered users only, throws AccessDeniedException for guests
- getChat(postID, userID) — returns message history for that post
- removeSavedPosting(userID, postID) — idempotent DELETE

## NotificationService Methods
- createNotification(receiverID, type, relatedPostID, relatedSessionID) — checks preferences first
- getNotifications(userID) — returns all, most recent first
- getUnreadCount(userID)
- markRead(notificationID)
- clearAll(userID)
- updatePreferences(userID, preferencesMap)

## Posting Lifecycle
AVAILABLE → PENDING → SOLD
When status changes to SOLD: automatically create a Transactions record.
Only registered, non-high-risk users can create postings.
Sellers can pause (PENDING) or reactivate (AVAILABLE) their own listings.
Use is_active = FALSE for soft-deletion (never hard delete).

## Frontend Pages
- Home/Browse — listing grid with sidebar filters
- ListingDetail — image carousel, seller info, Message Seller button
- Messages — two-panel chat layout
- CreatePosting — 3-step form (Details → Photos → Review)
- UserProfile — avatar, rating, active listings, reviews tabs
- SavedPostings — bookmarked listings
- PastPurchases — transaction history with chat history access

## Frontend Design
Primary color: USC Cardinal Red #9D2235
Accent color: USC Gold #FFC72C
Persistent navbar: logo, search bar, notification bell, profile avatar, Sell button
Guest users see listings but all interaction buttons show "Sign In to Interact"
High-risk users see same restriction as guests for messaging/purchasing

## Code Style Rules
- Constructor injection only — no @Autowired on fields
- No hard-coded credentials anywhere — use application.properties placeholders
- Flyway handles all DDL — no schema.sql or hibernate auto-ddl
- All JDBC queries in manager classes, not scattered across controllers

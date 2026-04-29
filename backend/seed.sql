-- Test data for local dev. Re-runnable: wipes existing rows first.
-- Run with: mysql -uroot -p trojan_market < seed.sql
--
-- All seeded users sign in via the SSO stub by typing their email (e.g. "alice@usc.edu")
-- in the navbar's Sign In prompt.

USE trojan_market;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE Reviews;
TRUNCATE TABLE Notifications;
TRUNCATE TABLE NotificationPreferences;
TRUNCATE TABLE SavedPostings;
TRUNCATE TABLE Transactions;
TRUNCATE TABLE Messages;
TRUNCATE TABLE ChatSessions;
TRUNCATE TABLE Postings;
TRUNCATE TABLE Users;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- Users — all @usc.edu, all verified, varying ratings
-- ----------------------------------------------------------------------------
INSERT INTO Users (userID, username, password, email, isVerified, review, reviewCount, is_active) VALUES
  (1, 'alice_usc', '', 'alice@usc.edu', TRUE, 42, 10, TRUE),  -- 4.2
  (2, 'bob_usc',   '', 'bob@usc.edu',   TRUE, 25,  5, TRUE),  -- 5.0
  (3, 'carol_usc', '', 'carol@usc.edu', TRUE, 18,  6, TRUE),  -- 3.0
  (4, 'dan_usc',   '', 'dan@usc.edu',   TRUE,  0,  0, TRUE),  -- no rating
  (5, 'eve_usc',   '', 'eve@usc.edu',   TRUE, 12,  3, TRUE);  -- 4.0

-- ----------------------------------------------------------------------------
-- Postings — mix of categories and statuses
-- ----------------------------------------------------------------------------
INSERT INTO Postings (postID, sellerID, title, description, category, status, price, is_active) VALUES
  ( 1, 1, 'Intro to CS Textbook',           'CSCI 103 textbook, no highlights.',                  'BOOKS',                    'AVAILABLE',  25.00, TRUE),
  ( 2, 1, 'Calculus Textbook',              'Stewart Calculus 8th ed. Some pencil notes.',        'BOOKS',                    'AVAILABLE',  40.00, TRUE),
  ( 3, 1, 'Bike Lock',                       'Kryptonite U-lock, lightly used.',                   'OTHER',                    'AVAILABLE',  10.00, TRUE),
  ( 4, 2, 'Desk Lamp',                      'Adjustable arm. Warm light.',                        'FURNITURE',                'AVAILABLE',  15.00, TRUE),
  ( 5, 2, 'Mini Fridge',                    '2.7 cu ft, perfect for dorm.',                       'ELECTRONICS',              'AVAILABLE',  80.00, TRUE),
  ( 6, 2, 'USC Hoodie (M)',                 'Heather grey, worn a few times.',                    'CLOTHING_AND_ACCESSORIES', 'PENDING',    35.00, TRUE),
  ( 7, 3, 'Backpack',                       'Herschel Little America, charcoal.',                 'CLOTHING_AND_ACCESSORIES', 'AVAILABLE',  45.00, TRUE),
  ( 8, 3, 'Coffee Maker',                   'Mr. Coffee 12-cup. Cleaned.',                        'ELECTRONICS',              'SOLD',       20.00, TRUE),
  ( 9, 3, 'USC Football Tix x2',            'Section 113. Saturday game.',                        'TICKETS',                  'AVAILABLE', 120.00, TRUE),
  (10, 4, 'Engineering Notes Pack',          'CSCI 270 + EE 109 + MATH 225 notes.',                'SCHOOL_SUPPLIES',          'AVAILABLE',   5.00, TRUE),
  (11, 4, 'IKEA Desk',                      'White Linnmon, 47 inch. Easy disassembly.',          'FURNITURE',                'AVAILABLE',  50.00, TRUE),
  (12, 5, 'Microwave',                      '700W, works great.',                                 'ELECTRONICS',              'AVAILABLE',  30.00, TRUE),
  (13, 5, 'Calculus Reference Manual',       'Schaum''s Outline.',                                 'BOOKS',                    'AVAILABLE',  15.00, TRUE),
  (14, 5, 'Spring Concert Tickets',          'Two GA tickets to Springfest.',                      'TICKETS',                  'SOLD',       25.00, TRUE),
  (15, 1, 'Portable Speaker',                'JBL Flip 6, blue. Comes with charger.',             'ELECTRONICS',              'AVAILABLE',  55.00, TRUE);

-- ----------------------------------------------------------------------------
-- Chat sessions — buyer + seller talking about a posting
-- ----------------------------------------------------------------------------
INSERT INTO ChatSessions (sessionID, postID, buyerID, sellerID) VALUES
  (1, 1, 2, 1),  -- bob asking alice about CS textbook
  (2, 4, 3, 2),  -- carol asking bob about desk lamp
  (3, 5, 4, 2),  -- dan asking bob about mini fridge
  (4, 7, 1, 3),  -- alice asking carol about backpack
  (5, 13, 1, 5); -- alice asking eve about calc reference

-- ----------------------------------------------------------------------------
-- Messages — chronological per session
-- ----------------------------------------------------------------------------
INSERT INTO Messages (sessionID, senderID, messageText, is_read) VALUES
  (1, 2, 'Hi! Is this textbook still available?',         TRUE),
  (1, 1, 'Yes, still have it!',                           TRUE),
  (1, 2, 'Great, condition still good?',                  TRUE),
  (1, 1, 'Like new — barely used.',                       TRUE),
  (1, 2, 'Can I pick up tomorrow near Leavey?',           FALSE),

  (2, 3, 'Does the desk lamp work?',                      TRUE),
  (2, 2, 'Yep, perfect condition.',                       TRUE),
  (2, 3, 'Awesome, I''ll take it.',                       FALSE),

  (3, 4, 'Is the mini fridge still available?',           TRUE),
  (3, 2, 'Yes! Are you on campus?',                       TRUE),
  (3, 4, 'I''m at Parkside. Can do today.',               FALSE),

  (4, 1, 'Hey, what color is the backpack?',              TRUE),
  (4, 3, 'Charcoal grey.',                                TRUE),
  (4, 1, 'Perfect, can I see more pictures?',             FALSE),

  (5, 1, 'Is the calc reference manual marked up?',       TRUE),
  (5, 5, 'Just a little highlighting in chapter 3.',      TRUE);

-- ----------------------------------------------------------------------------
-- Transactions — completed sales
-- ----------------------------------------------------------------------------
INSERT INTO Transactions (postID, buyerID, sellerID, sale_price) VALUES
  ( 8, 1, 3, 20.00),  -- alice bought carol's coffee maker
  (14, 4, 5, 25.00);  -- dan bought eve's concert tickets

-- ----------------------------------------------------------------------------
-- SavedPostings (bookmarks)
-- ----------------------------------------------------------------------------
INSERT INTO SavedPostings (userID, postID) VALUES
  (1, 5),  -- alice saved bob's mini fridge
  (1, 11), -- alice saved dan's IKEA desk
  (2, 7),  -- bob saved carol's backpack
  (3, 3),  -- carol saved alice's bike lock
  (4, 12); -- dan saved eve's microwave

-- ----------------------------------------------------------------------------
-- Notifications — populate the bell icon
-- ----------------------------------------------------------------------------
INSERT INTO Notifications (receiverID, type, relatedPostID, relatedSessionID, isRead) VALUES
  (1, 'NEW_MESSAGE',     7,  4,  FALSE), -- alice has unread msg from carol
  (2, 'NEW_MESSAGE',     5,  3,  FALSE), -- bob has unread msg from dan
  (3, 'ITEM_SOLD',       8,  NULL, TRUE),  -- carol's coffee maker sold
  (1, 'ITEM_PURCHASED',  8,  NULL, TRUE),  -- alice's purchase confirmation
  (5, 'ITEM_SOLD',       14, NULL, FALSE), -- eve's tickets sold (unread)
  (4, 'ITEM_PURCHASED',  14, NULL, TRUE);  -- dan's purchase confirmation

SELECT
  (SELECT COUNT(*) FROM Users)         AS users,
  (SELECT COUNT(*) FROM Postings)      AS postings,
  (SELECT COUNT(*) FROM ChatSessions)  AS sessions,
  (SELECT COUNT(*) FROM Messages)      AS messages,
  (SELECT COUNT(*) FROM Transactions)  AS transactions,
  (SELECT COUNT(*) FROM SavedPostings) AS saved,
  (SELECT COUNT(*) FROM Notifications) AS notifications;

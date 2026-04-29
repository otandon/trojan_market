-- Trojan Market — initial schema
-- 9 tables. Column names match CLAUDE.md exactly.

CREATE TABLE Users (
    userID       INT          NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    password     VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    isVerified   BOOLEAN      NOT NULL DEFAULT FALSE,
    review       INT          NOT NULL DEFAULT 0,
    reviewCount  INT          NOT NULL DEFAULT 0,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (userID),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE Postings (
    postID      INT            NOT NULL AUTO_INCREMENT,
    sellerID    INT            NOT NULL,
    title       VARCHAR(200)   NOT NULL,
    description TEXT,
    category    ENUM('BOOKS','CLOTHING_AND_ACCESSORIES','ELECTRONICS','FURNITURE','SCHOOL_SUPPLIES','TICKETS','OTHER') NOT NULL,
    status      ENUM('AVAILABLE','PENDING','SOLD') NOT NULL DEFAULT 'AVAILABLE',
    price       DECIMAL(10,2)  NOT NULL,
    postTime    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active   BOOLEAN        NOT NULL DEFAULT TRUE,
    PRIMARY KEY (postID),
    KEY idx_postings_seller   (sellerID),
    KEY idx_postings_status   (status),
    KEY idx_postings_category (category),
    CONSTRAINT fk_postings_seller
        FOREIGN KEY (sellerID) REFERENCES Users(userID)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE ChatSessions (
    sessionID  INT      NOT NULL AUTO_INCREMENT,
    postID     INT      NOT NULL,
    buyerID    INT      NOT NULL,
    sellerID   INT      NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sessionID),
    UNIQUE KEY uk_chat_post_buyer (postID, buyerID),
    KEY idx_chat_buyer  (buyerID),
    KEY idx_chat_seller (sellerID),
    CONSTRAINT fk_chat_post
        FOREIGN KEY (postID)   REFERENCES Postings(postID) ON DELETE RESTRICT,
    CONSTRAINT fk_chat_buyer
        FOREIGN KEY (buyerID)  REFERENCES Users(userID)    ON DELETE RESTRICT,
    CONSTRAINT fk_chat_seller
        FOREIGN KEY (sellerID) REFERENCES Users(userID)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE Messages (
    messageID   INT      NOT NULL AUTO_INCREMENT,
    sessionID   INT      NOT NULL,
    senderID    INT      NOT NULL,
    messageText TEXT     NOT NULL,
    messageTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read     BOOLEAN  NOT NULL DEFAULT FALSE,
    PRIMARY KEY (messageID),
    KEY idx_messages_session (sessionID),
    KEY idx_messages_sender  (senderID),
    CONSTRAINT fk_messages_session
        FOREIGN KEY (sessionID) REFERENCES ChatSessions(sessionID) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender
        FOREIGN KEY (senderID)  REFERENCES Users(userID)           ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE Transactions (
    transactionID   INT            NOT NULL AUTO_INCREMENT,
    postID          INT            NOT NULL,
    buyerID         INT            NOT NULL,
    sellerID        INT            NOT NULL,
    sale_price      DECIMAL(10,2)  NOT NULL,
    transactionTime DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transactionID),
    KEY idx_tx_buyer  (buyerID),
    KEY idx_tx_seller (sellerID),
    KEY idx_tx_post   (postID),
    CONSTRAINT fk_tx_post
        FOREIGN KEY (postID)   REFERENCES Postings(postID) ON DELETE RESTRICT,
    CONSTRAINT fk_tx_buyer
        FOREIGN KEY (buyerID)  REFERENCES Users(userID)    ON DELETE RESTRICT,
    CONSTRAINT fk_tx_seller
        FOREIGN KEY (sellerID) REFERENCES Users(userID)    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE SavedPostings (
    savedID   INT      NOT NULL AUTO_INCREMENT,
    userID    INT      NOT NULL,
    postID    INT      NOT NULL,
    savedTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (savedID),
    UNIQUE KEY uk_saved_user_post (userID, postID),
    KEY idx_saved_user (userID),
    CONSTRAINT fk_saved_user
        FOREIGN KEY (userID) REFERENCES Users(userID)    ON DELETE CASCADE,
    CONSTRAINT fk_saved_post
        FOREIGN KEY (postID) REFERENCES Postings(postID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE Notifications (
    notificationID   INT      NOT NULL AUTO_INCREMENT,
    receiverID       INT      NOT NULL,
    type             ENUM('NEW_MESSAGE','NEW_OFFER','OFFER_ACCEPTED','OFFER_REJECTED','ITEM_SOLD','ITEM_PURCHASED') NOT NULL,
    relatedPostID    INT      NULL,
    relatedSessionID INT      NULL,
    isRead           BOOLEAN  NOT NULL DEFAULT FALSE,
    createdAt        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notificationID),
    KEY idx_notif_receiver (receiverID),
    KEY idx_notif_unread   (receiverID, isRead),
    CONSTRAINT fk_notif_receiver
        FOREIGN KEY (receiverID)       REFERENCES Users(userID)            ON DELETE CASCADE,
    CONSTRAINT fk_notif_post
        FOREIGN KEY (relatedPostID)    REFERENCES Postings(postID)         ON DELETE SET NULL,
    CONSTRAINT fk_notif_session
        FOREIGN KEY (relatedSessionID) REFERENCES ChatSessions(sessionID)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE NotificationPreferences (
    userID         INT     NOT NULL,
    new_message    BOOLEAN NOT NULL DEFAULT TRUE,
    new_offer      BOOLEAN NOT NULL DEFAULT TRUE,
    offer_response BOOLEAN NOT NULL DEFAULT TRUE,
    item_sold      BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (userID),
    CONSTRAINT fk_notifprefs_user
        FOREIGN KEY (userID) REFERENCES Users(userID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE Reviews (
    reviewID      INT      NOT NULL AUTO_INCREMENT,
    reviewerID    INT      NOT NULL,
    sellerID      INT      NOT NULL,
    transactionID INT      NOT NULL,
    rating        TINYINT  NOT NULL,
    comment       TEXT,
    reviewTime    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reviewID),
    UNIQUE KEY uk_reviews_transaction (transactionID),
    KEY idx_reviews_seller (sellerID),
    CONSTRAINT fk_reviews_reviewer
        FOREIGN KEY (reviewerID)    REFERENCES Users(userID)               ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_seller
        FOREIGN KEY (sellerID)      REFERENCES Users(userID)               ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_tx
        FOREIGN KEY (transactionID) REFERENCES Transactions(transactionID) ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

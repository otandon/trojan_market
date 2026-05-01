-- V3: store posting photos.
-- Base64 data URLs in LONGTEXT for now. TODO: migrate to object storage
-- (S3 / Cloudinary) in production — DB blob is fine for class-project scale
-- but bloats the DB and slows queries at any real size.

CREATE TABLE PostingPhotos (
    photoID    INT       NOT NULL AUTO_INCREMENT,
    postID     INT       NOT NULL,
    photoData  LONGTEXT  NOT NULL,
    sortOrder  INT       NOT NULL DEFAULT 0,
    uploadedAt DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (photoID),
    KEY idx_photos_post (postID, sortOrder),
    CONSTRAINT fk_photos_post
        FOREIGN KEY (postID) REFERENCES Postings(postID) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

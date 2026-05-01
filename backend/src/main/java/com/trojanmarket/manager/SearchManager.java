package com.trojanmarket.manager;

import com.trojanmarket.dto.PostingDetailDTO;
import com.trojanmarket.dto.PostingSummaryDTO;
import com.trojanmarket.entity.Category;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Direct-JDBC search manager. Implements the two-phase search algorithm from CLAUDE.md:
 *   Phase 1 — SQL pull of AVAILABLE postings whose title or description matches any keyword.
 *   Phase 2 — In-memory relevance scoring (+3 whole-word title, +2 substring title, +1 substring description).
 */
@Component
public class SearchManager {

    private final DataSource dataSource;

    public SearchManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PostingSummaryDTO> searchPostings(String query,
                                                  String category,
                                                  BigDecimal minPrice,
                                                  BigDecimal maxPrice,
                                                  String sortBy,
                                                  Integer excludeSellerID) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot exceed maxPrice");
        }

        List<String> keywords = cleanKeywords(query);
        List<RawPost> raw = sqlRetrieve(keywords, category, minPrice, maxPrice, excludeSellerID);

        List<Scored> scored = new ArrayList<>(raw.size());
        for (RawPost rp : raw) {
            scored.add(new Scored(rp, score(rp, keywords)));
        }

        scored.sort(comparator(sortBy));

        List<PostingSummaryDTO> out = new ArrayList<>(scored.size());
        for (Scored s : scored) {
            out.add(PostingSummaryDTO.builder()
                    .postID(s.post.postID)
                    .title(s.post.title)
                    .price(s.post.price)
                    .photo(s.post.photo)
                    .sellerID(s.post.sellerID)
                    .sellerRating(s.post.sellerRating)
                    .status(s.post.status)
                    .build());
        }
        return out;
    }

    /**
     * Returns the seller's own listings (all statuses, only active=TRUE) as
     * summaries with thumbnails, used by the seller's profile page.
     */
    public List<PostingSummaryDTO> getPostingsForSeller(Integer sellerID) {
        String sql = """
                SELECT p.postID, p.title, p.price, p.status, p.postTime, p.sellerID,
                       u.review AS sellerReview, u.reviewCount AS sellerReviewCount,
                       (SELECT ph.photoData FROM PostingPhotos ph
                          WHERE ph.postID = p.postID
                          ORDER BY ph.sortOrder ASC, ph.photoID ASC
                          LIMIT 1) AS thumbnail
                FROM Postings p
                JOIN Users u ON u.userID = p.sellerID
                WHERE p.sellerID = ? AND p.is_active = TRUE
                ORDER BY p.postTime DESC
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<PostingSummaryDTO> out = new ArrayList<>();
                while (rs.next()) {
                    int reviewSum = rs.getInt("sellerReview");
                    int reviewCount = rs.getInt("sellerReviewCount");
                    out.add(PostingSummaryDTO.builder()
                            .postID(rs.getInt("postID"))
                            .title(rs.getString("title"))
                            .price(rs.getBigDecimal("price"))
                            .photo(rs.getString("thumbnail"))
                            .sellerID(rs.getInt("sellerID"))
                            .sellerRating(reviewCount == 0 ? 0.0 : (double) reviewSum / reviewCount)
                            .status(rs.getString("status"))
                            .build());
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch postings for seller", e);
        }
    }

    public List<String> getCategories() {
        return Arrays.stream(Category.values()).map(Enum::name).toList();
    }

    public PostingDetailDTO getPostingDetail(Integer postID, Integer viewerID) {
        String sql = """
                SELECT p.postID, p.title, p.description, p.category, p.status, p.price, p.postTime,
                       p.sellerID, u.username AS sellerUsername,
                       u.firstName AS sellerFirstName, u.lastName AS sellerLastName,
                       u.review AS sellerReview, u.reviewCount AS sellerReviewCount
                FROM Postings p
                JOIN Users u ON u.userID = p.sellerID
                WHERE p.postID = ? AND p.is_active = TRUE
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new EntityNotFoundException("Posting not found: " + postID);
                }
                int reviewSum = rs.getInt("sellerReview");
                int reviewCount = rs.getInt("sellerReviewCount");
                double rating = reviewCount == 0 ? 0.0 : (double) reviewSum / reviewCount;

                Timestamp ts = rs.getTimestamp("postTime");

                List<String> photos = fetchPhotos(conn, postID);
                Boolean isSaved = viewerID == null ? null : isPostingSavedBy(conn, postID, viewerID);

                return PostingDetailDTO.builder()
                        .postID(rs.getInt("postID"))
                        .title(rs.getString("title"))
                        .description(rs.getString("description"))
                        .category(rs.getString("category"))
                        .status(rs.getString("status"))
                        .price(rs.getBigDecimal("price"))
                        .postTime(ts == null ? null : ts.toLocalDateTime())
                        .sellerID(rs.getInt("sellerID"))
                        .sellerUsername(rs.getString("sellerUsername"))
                        .sellerFirstName(rs.getString("sellerFirstName"))
                        .sellerLastName(rs.getString("sellerLastName"))
                        .sellerRating(rating)
                        .photos(photos)
                        .isSaved(isSaved)
                        .build();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch posting detail", e);
        }
    }

    // --- internals ----------------------------------------------------------

    private List<String> cleanKeywords(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleaned = query.trim().toLowerCase().replaceAll("[^a-z0-9 ]", " ");
        return Arrays.stream(cleaned.split("\\s+"))
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<RawPost> sqlRetrieve(List<String> keywords,
                                      String category,
                                      BigDecimal minPrice,
                                      BigDecimal maxPrice,
                                      Integer excludeSellerID) {
        // Subquery selects the lowest-sortOrder photo per posting so the list view
        // gets a single thumbnail (not the entire photo array).
        StringBuilder sql = new StringBuilder("""
                SELECT p.postID, p.title, p.description, p.price, p.status, p.postTime, p.sellerID,
                       u.username AS sellerUsername,
                       u.review AS sellerReview, u.reviewCount AS sellerReviewCount,
                       (SELECT ph.photoData FROM PostingPhotos ph
                          WHERE ph.postID = p.postID
                          ORDER BY ph.sortOrder ASC, ph.photoID ASC
                          LIMIT 1) AS thumbnail
                FROM Postings p
                JOIN Users u ON u.userID = p.sellerID
                WHERE p.is_active = TRUE AND p.status = 'AVAILABLE'
                """);

        List<Object> params = new ArrayList<>();

        if (!keywords.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < keywords.size(); i++) {
                if (i > 0) sql.append(" OR ");
                sql.append("LOWER(p.title) LIKE ? OR LOWER(p.description) LIKE ?");
                String like = "%" + keywords.get(i) + "%";
                params.add(like);
                params.add(like);
            }
            sql.append(")");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND p.category = ?");
            params.add(category);
        }
        if (minPrice != null) {
            sql.append(" AND p.price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND p.price <= ?");
            params.add(maxPrice);
        }
        if (excludeSellerID != null) {
            sql.append(" AND p.sellerID != ?");
            params.add(excludeSellerID);
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RawPost> out = new ArrayList<>();
                while (rs.next()) {
                    RawPost rp = new RawPost();
                    rp.postID = rs.getInt("postID");
                    rp.title = rs.getString("title");
                    rp.description = rs.getString("description");
                    rp.price = rs.getBigDecimal("price");
                    rp.status = rs.getString("status");
                    Timestamp ts = rs.getTimestamp("postTime");
                    rp.postTime = ts == null ? null : ts.toLocalDateTime();
                    rp.sellerID = rs.getInt("sellerID");
                    int reviewSum = rs.getInt("sellerReview");
                    int reviewCount = rs.getInt("sellerReviewCount");
                    rp.sellerRating = reviewCount == 0 ? 0.0 : (double) reviewSum / reviewCount;
                    rp.photo = rs.getString("thumbnail");
                    out.add(rp);
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Search query failed", e);
        }
    }

    private List<String> fetchPhotos(Connection conn, Integer postID) throws SQLException {
        String sql = "SELECT photoData FROM PostingPhotos WHERE postID = ? ORDER BY sortOrder ASC, photoID ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postID);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getString("photoData"));
                }
                return out;
            }
        }
    }

    private boolean isPostingSavedBy(Connection conn, Integer postID, Integer userID) throws SQLException {
        String sql = "SELECT 1 FROM SavedPostings WHERE postID = ? AND userID = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postID);
            ps.setInt(2, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int score(RawPost p, List<String> keywords) {
        if (keywords.isEmpty()) {
            return 0;
        }
        String titleLower = p.title == null ? "" : p.title.toLowerCase();
        String descLower = p.description == null ? "" : p.description.toLowerCase();
        Set<String> titleWords = new HashSet<>(Arrays.asList(titleLower.split("\\W+")));

        int total = 0;
        for (String kw : keywords) {
            if (titleWords.contains(kw)) {
                total += 3;
            } else if (titleLower.contains(kw)) {
                total += 2;
            }
            if (descLower.contains(kw)) {
                total += 1;
            }
        }
        return total;
    }

    private Comparator<Scored> comparator(String sortBy) {
        if (sortBy == null) {
            return relevanceDesc();
        }
        return switch (sortBy.toLowerCase()) {
            case "price_asc" -> Comparator.comparing((Scored s) -> s.post.price);
            case "price_dec", "price_desc" -> Comparator.comparing((Scored s) -> s.post.price).reversed();
            case "newest" -> Comparator.comparing((Scored s) -> s.post.postTime,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed();
            default -> relevanceDesc();
        };
    }

    private Comparator<Scored> relevanceDesc() {
        return Comparator.comparingInt((Scored s) -> s.score).reversed();
    }

    private static final class RawPost {
        Integer postID;
        String title;
        String description;
        BigDecimal price;
        String status;
        LocalDateTime postTime;
        Integer sellerID;
        Double sellerRating;
        String photo;
    }

    private static final class Scored {
        final RawPost post;
        final int score;

        Scored(RawPost post, int score) {
            this.post = post;
            this.score = score;
        }
    }
}

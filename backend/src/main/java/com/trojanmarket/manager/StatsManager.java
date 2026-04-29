package com.trojanmarket.manager;

import com.trojanmarket.dto.MessageDTO;
import com.trojanmarket.dto.SavedPostingDTO;
import com.trojanmarket.dto.TransactionDTO;
import com.trojanmarket.security.ForbiddenException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Direct-JDBC stats manager. Reads Transactions, Messages, Postings, SavedPostings, Users.
 */
@Component
public class StatsManager {

    private final DataSource dataSource;

    public StatsManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<TransactionDTO> getPastPurchases(Integer userID) {
        return queryTransactions("WHERE t.buyerID = ?", userID);
    }

    public List<TransactionDTO> getSoldItems(Integer userID) {
        return queryTransactions("WHERE t.sellerID = ?", userID);
    }

    public List<SavedPostingDTO> getSavedPostings(Integer userID) {
        if (userID == null) {
            // CLAUDE.md: "registered users only, throws AccessDeniedException for guests"
            throw new ForbiddenException("Saved postings are only available to registered users");
        }
        String sql = """
                SELECT s.savedID, s.postID, s.savedTime,
                       p.title, p.price, p.status, p.sellerID,
                       u.username AS sellerUsername,
                       u.review AS sellerReview, u.reviewCount AS sellerReviewCount
                FROM SavedPostings s
                JOIN Postings p ON p.postID = s.postID
                JOIN Users u    ON u.userID = p.sellerID
                WHERE s.userID = ? AND p.is_active = TRUE
                ORDER BY s.savedTime DESC
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                List<SavedPostingDTO> out = new ArrayList<>();
                while (rs.next()) {
                    int reviewSum = rs.getInt("sellerReview");
                    int reviewCount = rs.getInt("sellerReviewCount");
                    Timestamp ts = rs.getTimestamp("savedTime");
                    out.add(SavedPostingDTO.builder()
                            .savedID(rs.getInt("savedID"))
                            .postID(rs.getInt("postID"))
                            .title(rs.getString("title"))
                            .price(rs.getBigDecimal("price"))
                            .status(rs.getString("status"))
                            .sellerID(rs.getInt("sellerID"))
                            .sellerUsername(rs.getString("sellerUsername"))
                            .sellerRating(reviewCount == 0 ? 0.0 : (double) reviewSum / reviewCount)
                            .savedTime(ts == null ? null : ts.toLocalDateTime())
                            .build());
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch saved postings", e);
        }
    }

    /**
     * Returns the chat history for the given postID belonging to userID (as buyer or seller).
     */
    public List<MessageDTO> getChat(Integer postID, Integer userID) {
        if (userID == null) {
            throw new ForbiddenException("Authentication required");
        }
        String sql = """
                SELECT m.messageID, m.sessionID, m.senderID, m.messageText, m.messageTime, m.is_read
                FROM Messages m
                JOIN ChatSessions cs ON cs.sessionID = m.sessionID
                WHERE cs.postID = ? AND (cs.buyerID = ? OR cs.sellerID = ?)
                ORDER BY m.messageTime ASC
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postID);
            ps.setInt(2, userID);
            ps.setInt(3, userID);
            try (ResultSet rs = ps.executeQuery()) {
                List<MessageDTO> out = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("messageTime");
                    out.add(MessageDTO.builder()
                            .messageID(rs.getInt("messageID"))
                            .sessionID(rs.getInt("sessionID"))
                            .senderID(rs.getInt("senderID"))
                            .messageText(rs.getString("messageText"))
                            .messageTime(ts == null ? null : ts.toLocalDateTime())
                            .isRead(rs.getBoolean("is_read"))
                            .build());
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch chat history", e);
        }
    }

    /**
     * Idempotent — returns silently whether or not a row was deleted.
     */
    public void removeSavedPosting(Integer userID, Integer postID) {
        if (userID == null) {
            throw new ForbiddenException("Authentication required");
        }
        String sql = "DELETE FROM SavedPostings WHERE userID = ? AND postID = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setInt(2, postID);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove saved posting", e);
        }
    }

    // --- internals ----------------------------------------------------------

    private List<TransactionDTO> queryTransactions(String whereClause, Integer userID) {
        String sql = """
                SELECT t.transactionID, t.postID, t.buyerID, t.sellerID, t.sale_price, t.transactionTime,
                       p.title AS postTitle,
                       buyer.username  AS buyerUsername,
                       seller.username AS sellerUsername
                FROM Transactions t
                JOIN Postings p   ON p.postID  = t.postID
                JOIN Users buyer  ON buyer.userID  = t.buyerID
                JOIN Users seller ON seller.userID = t.sellerID
                """ + whereClause + " ORDER BY t.transactionTime DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                List<TransactionDTO> out = new ArrayList<>();
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("transactionTime");
                    out.add(TransactionDTO.builder()
                            .transactionID(rs.getInt("transactionID"))
                            .postID(rs.getInt("postID"))
                            .postTitle(rs.getString("postTitle"))
                            .buyerID(rs.getInt("buyerID"))
                            .buyerUsername(rs.getString("buyerUsername"))
                            .sellerID(rs.getInt("sellerID"))
                            .sellerUsername(rs.getString("sellerUsername"))
                            .salePrice(rs.getBigDecimal("sale_price"))
                            .transactionTime(ts == null ? null : ts.toLocalDateTime())
                            .build());
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query transactions", e);
        }
    }
}

package com.example.csms.dao.impl;

import com.example.csms.dao.UserDAO;
import com.example.csms.exception.DataAccessException;
import com.example.csms.model.User;
import com.example.csms.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDAOImpl implements UserDAO {

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la recherche de l'utilisateur: " + e.getMessage(), e);
        }
        return Optional.empty(); // Utilisateur non trouvé
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password") // Récupère le mot de passe (non sécurisé)
        );
    }
}
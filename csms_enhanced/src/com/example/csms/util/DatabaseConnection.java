package com.example.csms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(DatabaseConfig.DB_DRIVER);
                connection = DriverManager.getConnection(
                        DatabaseConfig.DB_URL,
                        DatabaseConfig.DB_USER,
                        DatabaseConfig.DB_PASSWORD
                );
                // System.out.println("DEBUG: Connexion BDD réussie."); // Décommenter pour debug
            } catch (ClassNotFoundException e) {
                throw new SQLException("Erreur: Driver MySQL introuvable. Vérifiez le classpath.", e);
            } catch (SQLException e) {
                throw new SQLException("Erreur: Connexion BDD échouée: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    // System.out.println("DEBUG: Connexion BDD fermée."); // Décommenter pour debug
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion BDD: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
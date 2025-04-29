package com.example.csms.util;

// Centralise la configuration de la base de données
public class DatabaseConfig {
    // ** ADAPTEZ CES VALEURS **
    public static final String DB_URL = "jdbc:mysql://localhost:3306/csms_db_enhanced"; // Nom de la nouvelle BDD
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = ""; // Mot de passe XAMPP par défaut (vide)
    public static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
}
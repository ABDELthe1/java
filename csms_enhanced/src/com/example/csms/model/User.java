package com.example.csms.model;

public class User {
    private long id;
    private String username;
    private String password; // ATTENTION: Mot de passe en clair !

    public User() {}

    public User(long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; } // Utiliser pour la vérification (non sécurisé)
    public void setPassword(String password) { this.password = password; } // Utiliser pour la création/MAJ

    @Override
    public String toString() {
        return "User [id=" + id + ", username=" + username + "]"; // Ne pas afficher le mot de passe
    }
}
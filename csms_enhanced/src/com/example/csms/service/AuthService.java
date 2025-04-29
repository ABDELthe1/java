package com.example.csms.service;

import com.example.csms.dao.UserDAO;
import com.example.csms.dao.impl.UserDAOImpl;
import com.example.csms.exception.DataAccessException;
import com.example.csms.model.User;

import java.util.Optional;

public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAOImpl();
    }

    // Constructeur pour injection (tests)
    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authentifie un utilisateur.
     * ATTENTION: Comparaison de mot de passe en clair - NON SÉCURISÉ !
     * @param username Le nom d'utilisateur.
     * @param password Le mot de passe en clair.
     * @return true si l'authentification réussit, false sinon.
     * @throws DataAccessException Si une erreur BDD survient.
     */
    public boolean authenticate(String username, String password) throws DataAccessException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false; // Identifiants invalides
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Comparaison directe (NON SÉCURISÉE)
            return user.getPassword().equals(password);
            // En production, utiliser une bibliothèque de hachage :
            // return BCrypt.checkpw(password, user.getPasswordHash());
        }

        return false; // Utilisateur non trouvé
    }
}
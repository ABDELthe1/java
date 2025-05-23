package com.example.csms;

import com.example.csms.util.DatabaseConnection;
import com.example.csms.view.LoginDialog;
import com.example.csms.view.MainAppFrame;

import javax.swing.*;

public class MainApplication {

    public static void main(String[] args) {
        // Appliquer un Look and Feel plus agréable
        try {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(new javax.swing.plaf.nimbus.NimbusLookAndFeel()); // Nimbus est souvent joli
        } catch (Exception e) {
            System.err.println("Impossible d'appliquer le Look and Feel Nimbus : " + e);
        }

        // Afficher d'abord la boîte de dialogue de connexion
        LoginDialog loginDialog = new LoginDialog(null); // null car pas de parent initial
        loginDialog.setVisible(true);

        // Si l'authentification réussit OU si l'utilisateur choisit le mode invité, lancer la fenêtre principale
        if (loginDialog.isAccessGranted()) {
            boolean isAuthenticated = loginDialog.isAuthenticated();
            boolean isGuestMode = loginDialog.isGuestMode();

            // Afficher le mode d'accès dans la console pour debug
            if (isAuthenticated) {
                System.out.println("Application lancée en mode Administrateur (accès complet)");
            } else if (isGuestMode) {
                System.out.println("Application lancée en mode Invité (lecture seule)");
            }

            // Lancer l'interface principale dans l'EDT en passant le mode d'accès
            SwingUtilities.invokeLater(() -> {
                MainAppFrame mainFrame = new MainAppFrame(isAuthenticated);
                mainFrame.setVisible(true);
            });
        } else {
            // L'utilisateur a fermé la boîte de dialogue ou annulé, on quitte
            System.out.println("Accès refusé ou annulé. Fermeture de l'application.");
            System.exit(0);
        }

        // Hook pour fermer la connexion BDD à la fin
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arrêt de l'application, fermeture de la connexion BDD...");
            DatabaseConnection.closeConnection();
        }));
    }
}
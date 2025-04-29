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

        // Si l'authentification réussit, lancer la fenêtre principale
        if (loginDialog.isAuthenticated()) {
            // Lancer l'interface principale dans l'EDT
            SwingUtilities.invokeLater(() -> {
                MainAppFrame mainFrame = new MainAppFrame();
                mainFrame.setVisible(true);
            });
        } else {
            // L'utilisateur a fermé la boîte de dialogue ou annulé, on quitte
            System.out.println("Authentification échouée ou annulée. Fermeture de l'application.");
            System.exit(0);
        }


        // Hook pour fermer la connexion BDD à la fin
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arrêt de l'application, fermeture de la connexion BDD...");
            DatabaseConnection.closeConnection();
        }));
    }
}
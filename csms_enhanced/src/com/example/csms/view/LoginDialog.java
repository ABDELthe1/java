package com.example.csms.view;

import com.example.csms.service.AuthService;
import com.example.csms.exception.DataAccessException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton guestButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private boolean authenticated = false;
    private boolean guestMode = false;
    private final AuthService authService;

    public LoginDialog(Frame parent) {
        super(parent, "Connexion", true); // true = modal
        this.authService = new AuthService();

        setLayout(new BorderLayout(10, 10)); // Ajoute des marges
        setSize(380, 240);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Gérer la fermeture manuellement

        // Panel pour les champs de saisie
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Marges internes
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Utilisateur:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        fieldsPanel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Mot de passe:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        fieldsPanel.add(passwordField, gbc);

        // Label pour les messages d'état/erreur
        statusLabel = new JLabel(" "); // Espace pour la hauteur
        statusLabel.setForeground(Color.RED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        fieldsPanel.add(statusLabel, gbc);

        // Panel pour les boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Connexion");
        guestButton = new JButton("Continuer en tant qu'invité");
        guestButton.setToolTipText("Accès en lecture seule - Voir les stations sans pouvoir les modifier");
        cancelButton = new JButton("Annuler");

        buttonPanel.add(loginButton);
        buttonPanel.add(guestButton);
        buttonPanel.add(cancelButton);

        // Ajout des panels à la JDialog
        add(fieldsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Actions ---
        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin()); // Permet de valider avec Entrée
        guestButton.addActionListener(e -> continueAsGuest());
        cancelButton.addActionListener(e -> System.exit(0)); // Quitte l'application

        // Gérer la fermeture de la fenêtre (clic sur la croix)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0); // Quitte l'application
            }
        });

        pack(); // Ajuste la taille aux composants
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        statusLabel.setText(" "); // Efface le message précédent

        try {
            if (authService.authenticate(username, password)) {
                authenticated = true;
                guestMode = false;
                dispose(); // Ferme la boîte de dialogue si succès
            } else {
                statusLabel.setText("Utilisateur ou mot de passe incorrect.");
                passwordField.setText(""); // Efface le champ mot de passe
                usernameField.requestFocusInWindow(); // Remet le focus sur l'utilisateur
            }
        } catch (DataAccessException ex) {
            statusLabel.setText("Erreur de connexion à la base de données.");
            System.err.println("Erreur BDD pendant l'authentification: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Impossible de vérifier les informations d'identification.\nErreur de base de données: " + ex.getMessage(),
                    "Erreur d'authentification", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            statusLabel.setText("Erreur inattendue.");
            System.err.println("Erreur inattendue pendant l'authentification: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Une erreur inattendue est survenue: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void continueAsGuest() {
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Mode Invité: Vous pourrez consulter les stations et exporter en PDF,\n" +
                        "mais vous ne pourrez pas ajouter, modifier ou supprimer des stations.\n\n" +
                        "Continuer en mode invité?",
                "Confirmation Mode Invité",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            authenticated = false;
            guestMode = true;
            dispose(); // Ferme la boîte de dialogue
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isGuestMode() {
        return guestMode;
    }

    public boolean isAccessGranted() {
        return authenticated || guestMode;
    }
}
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
    private JButton cancelButton;
    private JLabel statusLabel;
    private boolean authenticated = false;
    private final AuthService authService;

    public LoginDialog(Frame parent) {
        super(parent, "Connexion", true); // true = modal
        this.authService = new AuthService();

        setLayout(new BorderLayout(10, 10)); // Ajoute des marges
        setSize(350, 200);
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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loginButton = new JButton("Connexion");
        cancelButton = new JButton("Annuler");
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        // Ajout des panels à la JDialog
        add(fieldsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- Actions ---
        loginButton.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin()); // Permet de valider avec Entrée
        cancelButton.addActionListener(e -> System.exit(0)); // Quitte l'application

        // Gérer la fermeture de la fenêtre (clic sur la croix)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0); // Quitte l'application
            }
        });

        // Affiche un avertissement crucial
//        JOptionPane.showMessageDialog(this,
//                "ATTENTION : Ce système utilise des mots de passe en clair pour la connexion.\n" +
//                        "Ceci est extrêmement non sécurisé et ne doit JAMAIS être utilisé en production.\n" +
//                        "Utilisateur de test : admin / password",
//                "Avertissement de Sécurité Majeur", JOptionPane.WARNING_MESSAGE);

        pack(); // Ajuste la taille aux composants
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        statusLabel.setText(" "); // Efface le message précédent

        try {
            if (authService.authenticate(username, password)) {
                authenticated = true;
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
            // On pourrait afficher un JOptionPane plus détaillé ici
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

    public boolean isAuthenticated() {
        return authenticated;
    }
}
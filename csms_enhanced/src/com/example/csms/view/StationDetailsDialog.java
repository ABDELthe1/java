package com.example.csms.view;

import com.example.csms.model.Station;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat; // Pour formater la date

public class StationDetailsDialog extends JDialog {

    // Formateur de date pour l'affichage
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public StationDetailsDialog(Frame parent, Station station) {
        super(parent, "Détails de la Station", true); // Modal
        setSize(450, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        if (station == null) {
            // Cas improbable mais sécurité
            add(new JLabel("Erreur : Aucune station fournie."), BorderLayout.CENTER);
            JButton closeButton = new JButton("Fermer");
            closeButton.addActionListener(e -> dispose());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(closeButton);
            add(buttonPanel, BorderLayout.SOUTH);
            return;
        }

        // Panel principal pour les détails avec GridBagLayout pour un bon alignement
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4); // Espacement
        gbc.anchor = GridBagConstraints.WEST; // Alignement à gauche

        // Helper pour ajouter une ligne label + valeur
        int gridY = 0;
        gbc.gridx = 0; // Colonne des labels
        gbc.weightx = 0.1; // Donne un peu de poids aux labels
        gbc.fill = GridBagConstraints.NONE;

        JLabel idLabelDesc = new JLabel("ID:");
        idLabelDesc.setFont(idLabelDesc.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(idLabelDesc, gbc);

        gbc.gridx = 1; // Colonne des valeurs
        gbc.weightx = 0.9; // Donne plus de poids aux valeurs
        gbc.fill = GridBagConstraints.HORIZONTAL;
        detailsPanel.add(new JLabel(String.valueOf(station.getId())), gbc);

        gridY++; gbc.gridy = gridY;
        gbc.gridx = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        JLabel nomLabelDesc = new JLabel("Nom:");
        nomLabelDesc.setFont(nomLabelDesc.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(nomLabelDesc, gbc);
        gbc.gridx = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        detailsPanel.add(new JLabel(station.getNom()), gbc);

        gridY++; gbc.gridy = gridY;
        gbc.gridx = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        JLabel locLabelDesc = new JLabel("Localisation:");
        locLabelDesc.setFont(locLabelDesc.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(locLabelDesc, gbc);
        gbc.gridx = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        detailsPanel.add(new JLabel(station.getLocalisation() != null ? station.getLocalisation() : "-"), gbc);

        gridY++; gbc.gridy = gridY;
        gbc.gridx = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        JLabel statutLabelDesc = new JLabel("Statut:");
        statutLabelDesc.setFont(statutLabelDesc.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(statutLabelDesc, gbc);
        gbc.gridx = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel statutValueLabel = new JLabel(station.getStatut() != null ? station.getStatut().getDescription() : "N/D");
        // Appliquer la couleur du statut aussi ici
        if (station.getStatut() != null) {
            switch (station.getStatut()) {
                case DISPONIBLE: statutValueLabel.setForeground(new Color(0, 128, 0)); break; // Vert foncé
                case EN_CHARGE: statutValueLabel.setForeground(new Color(255, 140, 0)); break; // Orange foncé
                case HORS_SERVICE: statutValueLabel.setForeground(Color.RED); break;
            }
        }
        detailsPanel.add(statutValueLabel, gbc);

        gridY++; gbc.gridy = gridY;
        gbc.gridx = 0; gbc.weightx = 0.1; gbc.fill = GridBagConstraints.NONE;
        JLabel dateLabelDesc = new JLabel("Dernière MàJ:");
        dateLabelDesc.setFont(dateLabelDesc.getFont().deriveFont(Font.BOLD));
        detailsPanel.add(dateLabelDesc, gbc);
        gbc.gridx = 1; gbc.weightx = 0.9; gbc.fill = GridBagConstraints.HORIZONTAL;
        String dateStr = (station.getDerniereMiseAJour() != null) ? DATE_FORMAT.format(station.getDerniereMiseAJour()) : "-";
        detailsPanel.add(new JLabel(dateStr), gbc);


        // Bouton Fermer
        JButton closeButton = new JButton("Fermer");
        closeButton.addActionListener(e -> dispose()); // Ferme simplement le dialogue
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // Marge en bas
        buttonPanel.add(closeButton);

        // Ajout des composants au dialogue
        add(detailsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // Ajuste la taille
    }
}
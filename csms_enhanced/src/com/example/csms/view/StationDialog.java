package com.example.csms.view;

import com.example.csms.exception.DataAccessException;
import com.example.csms.model.Statut;
import com.example.csms.model.Station;
import com.example.csms.service.StationService;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class StationDialog extends JDialog {

    private final StationService stationService;
    private final Station stationCourante; // null si ajout, existante si modification
    private boolean succes = false; // Indique si l'opération a réussi

    private JTextField nomField;
    private JTextField localisationField;
    private JComboBox<String> statutComboBox;
    private JButton saveButton;
    private JButton cancelButton;

    public StationDialog(Frame parent, StationService service, Station station) {
        super(parent, (station == null ? "Ajouter" : "Modifier") + " une Station", true); // Titre dynamique
        this.stationService = service;
        this.stationCourante = station;

        setSize(400, 250);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        // Création des composants
        nomField = new JTextField(25);
        localisationField = new JTextField(25);
        statutComboBox = new JComboBox<>(Statut.getAllDescriptions().toArray(new String[0]));
        saveButton = new JButton(station == null ? "Ajouter" : "Enregistrer");
        cancelButton = new JButton("Annuler");

        // Panel pour les champs
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Nom*:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fieldsPanel.add(nomField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Localisation:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fieldsPanel.add(localisationField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        fieldsPanel.add(new JLabel("Statut:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fieldsPanel.add(statutComboBox, gbc);

        // Panel pour les boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // Remplir les champs si c'est une modification
        if (stationCourante != null) {
            nomField.setText(stationCourante.getNom());
            localisationField.setText(stationCourante.getLocalisation());
            statutComboBox.setSelectedItem(stationCourante.getStatut().getDescription());
        } else {
            statutComboBox.setSelectedItem(Statut.DISPONIBLE.getDescription()); // Défaut pour ajout
        }

        // Ajout des panels au dialogue
        add(fieldsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Actions
        saveButton.addActionListener(e -> enregistrerStation());
        cancelButton.addActionListener(e -> dispose()); // Ferme simplement le dialogue

        pack(); // Ajuste la taille
    }

    private void enregistrerStation() {
        String nom = nomField.getText().trim();
        String localisation = localisationField.getText().trim();
        Statut statut = Statut.fromString((String) Objects.requireNonNull(statutComboBox.getSelectedItem()));

        if (nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom est obligatoire.", "Erreur de validation", JOptionPane.WARNING_MESSAGE);
            nomField.requestFocusInWindow();
            return;
        }

        try {
            if (stationCourante == null) { // Mode Ajout
                Station nouvelleStation = new Station(nom, localisation, statut);
                stationService.ajouterStation(nouvelleStation);
                JOptionPane.showMessageDialog(this, "Station ajoutée avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
            } else { // Mode Modification
                stationCourante.setNom(nom);
                stationCourante.setLocalisation(localisation);
                stationCourante.setStatut(statut);
                stationService.modifierStation(stationCourante);
                JOptionPane.showMessageDialog(this, "Station modifiée avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
            succes = true; // Indique que l'opération a réussi
            dispose(); // Ferme le dialogue

        } catch (IllegalArgumentException | DataAccessException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement:\n" + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur inattendue:\n" + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // Permet à la fenêtre appelante de savoir si l'opération a réussi
    public boolean isSucces() {
        return succes;
    }
}
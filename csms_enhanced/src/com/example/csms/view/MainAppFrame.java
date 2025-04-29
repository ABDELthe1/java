package com.example.csms.view;

import com.example.csms.exception.DataAccessException;
import com.example.csms.model.Statut;
import com.example.csms.model.Station;
import com.example.csms.service.StationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel; // Import pour l'export CSV
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter; // Pour sauvegarder la taille/position
import java.awt.event.WindowEvent; // Pour sauvegarder la taille/position
import java.io.BufferedWriter; // Pour Export CSV
import java.io.File; // Pour Export CSV
import java.io.FileWriter; // Pour Export CSV
import java.io.IOException; // Pour Export CSV
import java.util.ArrayList;
import java.util.Arrays; // Pour la suppression multiple
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.prefs.Preferences; // Pour sauvegarder la taille/position
import java.util.stream.Collectors;

public class MainAppFrame extends JFrame {

    private final StationService stationService;
    private JTable stationTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton viewDetailsButton;
    private JButton addButton; // Rendre accessible pour les tooltips etc.
    private JButton editButton;
    private JButton deleteButton;
    private TableRowSorter<DefaultTableModel> sorter;

    private JComboBox<String> statusFilterComboBox;
    private JLabel statusBar;
    private JPopupMenu tablePopupMenu;
    private JMenuItem changeStatusMenuItem;
    private JMenuItem copyInfoMenuItem;

    private List<Station> allLoadedStations = new ArrayList<>();

    private JLabel totalStationsLabel;
    private JLabel availableStationsLabel;
    private JLabel chargingStationsLabel;
    private JLabel outOfServiceStationsLabel;

    // Clés pour sauvegarder les préférences de la fenêtre
    private static final String PREF_KEY_X = "main_window_x";
    private static final String PREF_KEY_Y = "main_window_y";
    private static final String PREF_KEY_WIDTH = "main_window_width";
    private static final String PREF_KEY_HEIGHT = "main_window_height";
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 750;


    public MainAppFrame() {
        this.stationService = new StationService();

        setTitle("Gestionnaire de Stations de Charge v0.4"); // Version
        // Ne pas définir la taille ici, on le fera après avoir lu les préférences
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Gérer la fermeture manuellement pour sauvegarder
        // loadPreferences(); // Appeler pour charger taille/position AVANT setLocationRelativeTo

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem exportCsvItem = new JMenuItem("Exporter Vue Actuelle (CSV)..."); // Nouvelle option
        exportCsvItem.addActionListener(e -> exportTableToCsv()); // Action pour l'export
        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(e -> closeApplication()); // Action pour quitter proprement
        fileMenu.add(exportCsvItem); // Ajout de l'export
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Aide");
        JMenuItem aboutItem = new JMenuItem("À Propos...");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // --- Table Model et JTable ---
        tableModel = new DefaultTableModel(new Object[]{"ID", "Nom", "Localisation", "Statut", "Dernière MàJ"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4) return java.sql.Timestamp.class;
                if (columnIndex == 3) return String.class;
                return super.getColumnClass(columnIndex);
            }
        };
        stationTable = new JTable(tableModel);
        // Permettre la sélection multiple pour la suppression groupée
        stationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        stationTable.setAutoCreateRowSorter(true);
        stationTable.setFillsViewportHeight(true);
        stationTable.setRowHeight(25);

        TableColumn statusColumn = stationTable.getColumnModel().getColumn(3);
        statusColumn.setCellRenderer(new StatusCellRenderer());

        sorter = new TableRowSorter<>(tableModel);
        stationTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(stationTable);

        // --- Menu Contextuel ---
        createTablePopupMenu();
        stationTable.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { maybeShowPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShowPopup(e); }
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int rowAtPoint = stationTable.rowAtPoint(e.getPoint());
                    // Si le clic est sur une ligne non sélectionnée, on sélectionne UNIQUEMENT cette ligne
                    if (rowAtPoint >= 0 && !stationTable.isRowSelected(rowAtPoint)) {
                        stationTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                    }
                    updatePopupMenuState(); // Met à jour l'état activé/désactivé
                    tablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // --- Panneau de Contrôle (Haut) ---
        JPanel controlPanel = new JPanel(new BorderLayout(10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Rechercher:"));
        searchField = new JTextField(20);
        searchButton = new JButton("🔍 Chercher"); // Ajout icone unicode simple
        searchButton.setToolTipText("Rechercher dans le nom ou la localisation");
        JButton clearSearchButton = new JButton("Effacer Filtres"); // Renommé
        clearSearchButton.setToolTipText("Effacer le critère de recherche et le filtre de statut");
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(clearSearchButton);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(new JLabel("Filtrer Statut:"));
        statusFilterComboBox = new JComboBox<>();
        statusFilterComboBox.addItem("Tous");
        for (Statut s : Statut.values()) { statusFilterComboBox.addItem(s.getDescription()); }
        statusFilterComboBox.setToolTipText("Filtrer la vue par statut");
        filterPanel.add(statusFilterComboBox);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("🔄 Rafraîchir"); // Ajout icone
        refreshButton.setToolTipText("Recharger les données depuis la base");
        viewDetailsButton = new JButton("ℹ️ Voir Détails"); // Ajout icone
        viewDetailsButton.setToolTipText("Afficher les détails de la station sélectionnée (une seule)");
        actionButtonPanel.add(refreshButton);
        actionButtonPanel.add(viewDetailsButton);

        controlPanel.add(filterPanel, BorderLayout.CENTER); // Filtres prennent plus de place
        controlPanel.add(actionButtonPanel, BorderLayout.EAST);

        // --- Panneau de Statistiques ---
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Statistiques Globales"));
        totalStationsLabel = new JLabel("Total: ?");
        availableStationsLabel = new JLabel("Disponibles: ?");
        chargingStationsLabel = new JLabel("En Charge: ?");
        outOfServiceStationsLabel = new JLabel("Hors Service: ?");
        statsPanel.add(totalStationsLabel);
        statsPanel.add(Box.createHorizontalStrut(15));
        statsPanel.add(availableStationsLabel);
        statsPanel.add(Box.createHorizontalStrut(15));
        statsPanel.add(chargingStationsLabel);
        statsPanel.add(Box.createHorizontalStrut(15));
        statsPanel.add(outOfServiceStationsLabel);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(controlPanel, BorderLayout.NORTH);
        northPanel.add(statsPanel, BorderLayout.SOUTH);

        // --- Panneau de Boutons CRUD ---
        JPanel crudButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        crudButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        addButton = new JButton("➕ Ajouter Station"); // Ajout icone
        addButton.setToolTipText("Ouvrir le formulaire pour ajouter une nouvelle station");
        editButton = new JButton("✏️ Modifier Sélection"); // Ajout icone
        editButton.setToolTipText("Ouvrir le formulaire pour modifier la station sélectionnée (une seule)");
        deleteButton = new JButton("❌ Supprimer Sélection"); // Ajout icone
        deleteButton.setToolTipText("Supprimer la ou les station(s) sélectionnée(s) (avec confirmation)");

        crudButtonPanel.add(addButton);
        crudButtonPanel.add(editButton);
        crudButtonPanel.add(deleteButton);

        // --- Barre de Statut ---
        statusBar = new JLabel("Prêt.");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        statusBar.setHorizontalAlignment(SwingConstants.LEFT);

        // --- Layout Principal ---
        setLayout(new BorderLayout(0, 5));
        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(crudButtonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Actions ---
        refreshButton.addActionListener(e -> chargerDonneesEtFiltrer());
        addButton.addActionListener(e -> ouvrirDialogueStation(null));
        editButton.addActionListener(e -> ouvrirDialogueModificationSelectionUnique()); // Action spécifique pour édition
        deleteButton.addActionListener(e -> supprimerStationsSelectionnees()); // Action pour suppression multiple
        viewDetailsButton.addActionListener(e -> ouvrirDialogueDetailsSelectionUnique()); // Action spécifique pour détails
        searchButton.addActionListener(e -> appliquerFiltres());
        searchField.addActionListener(e -> appliquerFiltres());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            statusFilterComboBox.setSelectedIndex(0);
            appliquerFiltres();
            searchField.requestFocusInWindow(); // Remet le focus sur la recherche
        });
        statusFilterComboBox.addActionListener(e -> appliquerFiltres());

        // --- Gestion Fermeture Fenêtre (Sauvegarde Préférences) ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApplication(); // Appelle notre méthode propre
            }
        });

        // Charge les préférences et les données initiales
        loadPreferences(); // Charger taille/position
        chargerDonneesEtFiltrer(); // Charger les données
    }

    // Création du menu contextuel
    private void createTablePopupMenu() {
        tablePopupMenu = new JPopupMenu();
        changeStatusMenuItem = new JMenuItem("Changer Statut Rapide...");
        changeStatusMenuItem.addActionListener(e -> changerStatutRapideSelection());
        tablePopupMenu.add(changeStatusMenuItem);
        tablePopupMenu.addSeparator();
        copyInfoMenuItem = new JMenuItem("Copier Infos (ID, Nom, Lieu)");
        copyInfoMenuItem.addActionListener(e -> copierInfosSelection());
        tablePopupMenu.add(copyInfoMenuItem);
    }

    // Mise à jour état du menu contextuel
    private void updatePopupMenuState() {
        int selectedRowCount = stationTable.getSelectedRowCount();
        // Activé seulement si EXACTEMENT une ligne est sélectionnée
        changeStatusMenuItem.setEnabled(selectedRowCount == 1);
        copyInfoMenuItem.setEnabled(selectedRowCount == 1);
    }

    // Méthode pour charger toutes les données puis appliquer les filtres locaux
    private void chargerDonneesEtFiltrer() {
        statusBar.setText("Chargement des données depuis la base...");
        SwingUtilities.invokeLater(() -> { // Exécuter le chargement hors de l'EDT si long, mais ici simple
            try {
                allLoadedStations = stationService.trouverToutesLesStations();
                appliquerFiltresLocaux(); // Applique les filtres sur la liste chargée
                mettreAJourStatistiques(); // Met à jour les stats globales
                statusBar.setText(tableModel.getRowCount() + " station(s) affichée(s). Total global: " + allLoadedStations.size() + ".");
            } catch (DataAccessException e) {
                handleDataAccessException("chargement des données", e);
                allLoadedStations.clear();
                tableModel.setRowCount(0);
                mettreAJourStatistiquesErreur();
            } catch (Exception e) {
                handleUnexpectedError("chargement des données", e);
                allLoadedStations.clear();
                tableModel.setRowCount(0);
                mettreAJourStatistiquesErreur();
            }
        });
    }

    // Méthode pour appliquer les filtres (recherche texte + statut)
    private void appliquerFiltres() {
        appliquerFiltresLocaux();
        statusBar.setText(tableModel.getRowCount() + " station(s) affichée(s) sur " + allLoadedStations.size() + " (Filtres appliqués).");
    }

    // Filtre la liste locale et met à jour la table
    private void appliquerFiltresLocaux() {
        String texteFiltre = searchField.getText().trim().toLowerCase();
        String statutFiltreDesc = (String) statusFilterComboBox.getSelectedItem();
        Statut statutFiltre = !"Tous".equals(statutFiltreDesc) ? Statut.fromString(statutFiltreDesc) : null;

        List<Station> stationsFiltrees = allLoadedStations.stream()
                .filter(station -> {
                    boolean matchTexte = texteFiltre.isEmpty() ||
                            (station.getNom() != null && station.getNom().toLowerCase().contains(texteFiltre)) ||
                            (station.getLocalisation() != null && station.getLocalisation().toLowerCase().contains(texteFiltre));
                    boolean matchStatut = statutFiltre == null || (station.getStatut() != null && station.getStatut() == statutFiltre);
                    return matchTexte && matchStatut;
                })
                .collect(Collectors.toList());

        tableModel.setRowCount(0); // Efface la table
        for (Station station : stationsFiltrees) {
            Vector<Object> row = new Vector<>();
            row.add(station.getId());
            row.add(station.getNom());
            row.add(station.getLocalisation());
            row.add(station.getStatut() != null ? station.getStatut().getDescription() : "N/D");
            row.add(station.getDerniereMiseAJour());
            tableModel.addRow(row);
        }
    }

    // Ouvre le dialogue d'ajout/modif
    private void ouvrirDialogueStation(Station stationAModifier) {
        StationDialog dialog = new StationDialog(this, stationService, stationAModifier);
        dialog.setVisible(true);
        if (dialog.isSucces()) {
            String action = (stationAModifier == null) ? "ajoutée" : "modifiée";
            chargerDonneesEtFiltrer();
            statusBar.setText("Station " + action + " avec succès. " + tableModel.getRowCount() + " station(s) affichée(s).");
        }
    }

    // Ouvre le dialogue pour MODIFIER (sélection unique requise)
    private void ouvrirDialogueModificationSelectionUnique() {
        if (stationTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner EXACTEMENT une station à modifier.", "Sélection Invalide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRowView = stationTable.getSelectedRow();
        int selectedRowModel = stationTable.convertRowIndexToModel(selectedRowView);
        long idStation = (long) tableModel.getValueAt(selectedRowModel, 0);

        statusBar.setText("Chargement des données pour modification ID " + idStation + "...");
        try {
            Optional<Station> stationOpt = stationService.trouverStationParId(idStation);
            if (stationOpt.isPresent()) {
                statusBar.setText("Prêt.");
                ouvrirDialogueStation(stationOpt.get()); // Ouvre le dialogue en mode édition
            } else {
                handleStationNotFoundError();
            }
        } catch(DataAccessException e) {
            handleDataAccessException("récupération pour modification", e);
        }
    }

    // Ouvre le dialogue pour VOIR DETAILS (sélection unique requise)
    private void ouvrirDialogueDetailsSelectionUnique() {
        if (stationTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner EXACTEMENT une station pour voir les détails.", "Sélection Invalide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int selectedRowView = stationTable.getSelectedRow();
        int selectedRowModel = stationTable.convertRowIndexToModel(selectedRowView);
        long idStation = (long) tableModel.getValueAt(selectedRowModel, 0);

        statusBar.setText("Chargement des détails pour ID " + idStation + "...");
        try {
            Optional<Station> stationOpt = stationService.trouverStationParId(idStation);
            if (stationOpt.isPresent()) {
                statusBar.setText("Prêt.");
                StationDetailsDialog detailsDialog = new StationDetailsDialog(this, stationOpt.get());
                detailsDialog.setVisible(true);
            } else {
                handleStationNotFoundError();
            }
        } catch(DataAccessException e) {
            handleDataAccessException("consultation des détails", e);
        }
    }

    // Supprime la ou les station(s) sélectionnée(s)
    private void supprimerStationsSelectionnees() {
        int[] selectedRowsView = stationTable.getSelectedRows();
        if (selectedRowsView.length == 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins une station à supprimer.", "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Conversion des indices de vue en indices de modèle
        int[] selectedRowsModel = Arrays.stream(selectedRowsView)
                .map(stationTable::convertRowIndexToModel)
                .toArray();

        // Récupération des IDs et noms pour confirmation
        StringBuilder confirmationMessage = new StringBuilder("Êtes-vous sûr de vouloir supprimer ");
        List<Long> idsToDelete = new ArrayList<>();
        if (selectedRowsModel.length == 1) {
            long id = (long) tableModel.getValueAt(selectedRowsModel[0], 0);
            String nom = (String) tableModel.getValueAt(selectedRowsModel[0], 1);
            confirmationMessage.append("la station '").append(nom).append("' (ID: ").append(id).append(")?");
            idsToDelete.add(id);
        } else {
            confirmationMessage.append("les ").append(selectedRowsModel.length).append(" stations sélectionnées ?\nIDs: ");
            for(int i = 0; i < selectedRowsModel.length; i++) {
                long id = (long) tableModel.getValueAt(selectedRowsModel[i], 0);
                idsToDelete.add(id);
                confirmationMessage.append(id).append(i < selectedRowsModel.length - 1 ? ", " : "");
            }
        }


        int confirmation = JOptionPane.showConfirmDialog(this,
                confirmationMessage.toString(),
                "Confirmation Suppression Multiple", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            statusBar.setText("Suppression de " + idsToDelete.size() + " station(s)...");
            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            // Boucle sur les IDs à supprimer
            for (long id : idsToDelete) {
                try {
                    boolean deleted = stationService.supprimerStation(id);
                    if (deleted) {
                        successCount++;
                    } else {
                        failCount++; // Normalement géré par exception mais sécurité
                        errors.add("Échec suppression ID " + id + " (non trouvée?)");
                    }
                } catch (DataAccessException e) {
                    failCount++;
                    errors.add("Erreur BDD suppression ID " + id + ": " + e.getMessage());
                    e.printStackTrace(); // Log l'erreur complète
                } catch (Exception e) {
                    failCount++;
                    errors.add("Erreur inattendue suppression ID " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Afficher un résumé
            StringBuilder resultMessage = new StringBuilder();
            if (successCount > 0) {
                resultMessage.append(successCount).append(" station(s) supprimée(s) avec succès.\n");
            }
            if (failCount > 0) {
                resultMessage.append(failCount).append(" suppression(s) échouée(s).\n");
                // Afficher les erreurs détaillées si nécessaire
                if (!errors.isEmpty()) {
                    resultMessage.append("Détails erreurs:\n");
                    errors.forEach(err -> resultMessage.append("- ").append(err).append("\n"));
                }
                JOptionPane.showMessageDialog(this, resultMessage.toString(), "Résultat Suppression", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, resultMessage.toString(), "Succès Suppression", JOptionPane.INFORMATION_MESSAGE);
            }

            chargerDonneesEtFiltrer(); // Rafraîchit la table
            statusBar.setText(successCount + "/" + idsToDelete.size() + " station(s) supprimée(s). " + tableModel.getRowCount() + " affichée(s).");
        } else {
            statusBar.setText("Suppression annulée.");
        }
    }

    // Action pour le changement de statut rapide
    private void changerStatutRapideSelection() {
        if (stationTable.getSelectedRowCount() != 1) return; // Vérification par sécurité

        int selectedRowView = stationTable.getSelectedRow();
        int selectedRowModel = stationTable.convertRowIndexToModel(selectedRowView);
        long idStation = (long) tableModel.getValueAt(selectedRowModel, 0);
        String nomStation = (String) tableModel.getValueAt(selectedRowModel, 1);
        Statut statutActuel = Statut.fromString((String) tableModel.getValueAt(selectedRowModel, 3));

        Object[] options = Statut.values();
        Object initialSelection = statutActuel;

        Object selection = JOptionPane.showInputDialog(this,
                "Choisir le nouveau statut pour '" + nomStation + "' (ID: " + idStation + ") :",
                "Changement Rapide de Statut", JOptionPane.QUESTION_MESSAGE, null, options, initialSelection);

        if (selection instanceof Statut) {
            Statut nouveauStatut = (Statut) selection;
            if (nouveauStatut != statutActuel) {
                statusBar.setText("Mise à jour statut ID " + idStation + "...");
                try {
                    boolean updated = stationService.updateStationStatus(idStation, nouveauStatut);
                    if (updated) {
                        // MAJ directe dans le modèle et la liste locale
                        tableModel.setValueAt(nouveauStatut.getDescription(), selectedRowModel, 3);
                        allLoadedStations.stream().filter(s -> s.getId() == idStation).findFirst().ifPresent(s -> s.setStatut(nouveauStatut));
                        mettreAJourStatistiques();
                        statusBar.setText("Statut ID " + idStation + " mis à jour. " + tableModel.getRowCount() + " station(s) affichée(s).");
                        ((DefaultTableModel)stationTable.getModel()).fireTableRowsUpdated(selectedRowModel, selectedRowModel);
                    } else {
                        handleOperationFailure("mise à jour du statut");
                    }
                } catch (DataAccessException e) {
                    handleDataAccessException("mise à jour du statut", e);
                } catch (Exception e) {
                    handleUnexpectedError("mise à jour du statut", e);
                }
            } else { statusBar.setText("Statut inchangé."); }
        } else { statusBar.setText("Changement de statut annulé."); }
    }

    // Action pour copier les infos
    private void copierInfosSelection() {
        if (stationTable.getSelectedRowCount() != 1) return; // Vérification

        int selectedRowView = stationTable.getSelectedRow();
        int selectedRowModel = stationTable.convertRowIndexToModel(selectedRowView);
        long id = (long) tableModel.getValueAt(selectedRowModel, 0);
        String nom = (String) tableModel.getValueAt(selectedRowModel, 1);
        Object locObj = tableModel.getValueAt(selectedRowModel, 2); // Peut être null
        String localisation = (locObj != null) ? locObj.toString() : "-";

        String textToCopy = String.format("ID: %d\nNom: %s\nLocalisation: %s", id, nom, localisation);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(textToCopy);
        clipboard.setContents(stringSelection, null);

        updateStatusBar("Infos ID " + id + " copiées.", 2500); // Message temporaire
    }

    // Met à jour les labels de statistiques
    private void mettreAJourStatistiques() {
        try {
            Map<String, Long> stats = stationService.getStationStatistics();
            totalStationsLabel.setText("Total: " + stats.getOrDefault("TOTAL", 0L));
            availableStationsLabel.setText("Disponibles: " + stats.getOrDefault(Statut.DISPONIBLE.name(), 0L));
            chargingStationsLabel.setText("En Charge: " + stats.getOrDefault(Statut.EN_CHARGE.name(), 0L));
            outOfServiceStationsLabel.setText("Hors Service: " + stats.getOrDefault(Statut.HORS_SERVICE.name(), 0L));
        } catch (DataAccessException e) {
            System.err.println("Erreur lors de la récupération des statistiques: " + e.getMessage());
            mettreAJourStatistiquesErreur();
            e.printStackTrace();
        }
    }

    private void mettreAJourStatistiquesErreur() {
        totalStationsLabel.setText("Total: Erreur");
        availableStationsLabel.setText("Disponibles: Erreur");
        chargingStationsLabel.setText("En Charge: Erreur");
        outOfServiceStationsLabel.setText("Hors Service: Erreur");
    }

    // Affiche la boîte de dialogue "À Propos"
    private void showAboutDialog() {
        // Version du dialogue avec plus de détails sur les fonctionnalités
        JOptionPane.showMessageDialog(this,
                "Gestionnaire de Stations de Charge v0.4\n\n" +
                        "Fonctionnalités :\n" +
                        "  - Authentification utilisateur\n" +
                        "  - CRUD Stations (Ajout, Modif., Suppr.)\n" +
                        "  - Consultation, Recherche, Filtrage par Statut\n" +
                        "  - Statistiques globales\n" +
                        "  - Vue détaillée, Copie d'infos\n" +
                        "  - Changement rapide de statut\n" +
                        "  - Export CSV, Suppression multiple\n" +
                        "  - Persistance taille/position fenêtre\n\n" +
                        "Technologies : Java, Swing, JDBC, MySQL\n\n" +
                        "ATTENTION : Sécurité des mots de passe basique (exemple).",
                "À Propos de CSMS Enhanced",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Méthodes utilitaires pour la gestion des erreurs et préférences ---

    private void handleDataAccessException(String operation, DataAccessException e) {
        JOptionPane.showMessageDialog(this, "Erreur Base de Données lors de: " + operation + "\n" + e.getMessage(), "Erreur BDD", JOptionPane.ERROR_MESSAGE);
        statusBar.setText("Erreur BDD pendant: " + operation);
        e.printStackTrace();
    }

    private void handleUnexpectedError(String operation, Exception e) {
        JOptionPane.showMessageDialog(this, "Erreur Inattendue lors de: " + operation + "\n" + e.getMessage(), "Erreur Inattendue", JOptionPane.ERROR_MESSAGE);
        statusBar.setText("Erreur inattendue pendant: " + operation);
        e.printStackTrace();
    }

    private void handleOperationFailure(String operation) {
        JOptionPane.showMessageDialog(this, "Échec de l'opération: " + operation, "Échec", JOptionPane.WARNING_MESSAGE);
        statusBar.setText("Échec: " + operation);
    }

    private void handleStationNotFoundError() {
        JOptionPane.showMessageDialog(this, "La station sélectionnée n'a pas été trouvée (peut-être supprimée?).", "Erreur", JOptionPane.ERROR_MESSAGE);
        chargerDonneesEtFiltrer(); // Rafraîchit
        statusBar.setText("Erreur: Station non trouvée.");
    }

    // Met à jour la barre de statut et efface après un délai
    private void updateStatusBar(String message, int delayMillis) {
        statusBar.setText(message);
        Timer timer = new Timer(delayMillis, e -> {
            // Vérifie si le message n'a pas été écrasé entre temps
            if (statusBar.getText().equals(message)) {
                statusBar.setText("Prêt.");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    // Sauvegarde la position et la taille de la fenêtre
    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainAppFrame.class);
        Rectangle bounds = getBounds();
        prefs.putInt(PREF_KEY_X, bounds.x);
        prefs.putInt(PREF_KEY_Y, bounds.y);
        prefs.putInt(PREF_KEY_WIDTH, bounds.width);
        prefs.putInt(PREF_KEY_HEIGHT, bounds.height);
        // System.out.println("DEBUG: Préférences sauvegardées: " + bounds); // Décommenter pour debug
    }

    // Charge la position et la taille de la fenêtre
    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainAppFrame.class);
        int x = prefs.getInt(PREF_KEY_X, -1); // -1 pour indiquer non défini
        int y = prefs.getInt(PREF_KEY_Y, -1);
        int width = prefs.getInt(PREF_KEY_WIDTH, DEFAULT_WIDTH);
        int height = prefs.getInt(PREF_KEY_HEIGHT, DEFAULT_HEIGHT);

        // System.out.println("DEBUG: Préférences chargées: x="+x+", y="+y+", w="+width+", h="+height); // Décommenter pour debug

        // Applique taille/position
        setSize(width, height);
        if (x != -1 && y != -1) {
            // Vérifie si les coordonnées sont valides pour l'écran actuel (simple vérification)
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            if (screenBounds.contains(x, y)) {
                setLocation(x, y);
            } else {
                setLocationRelativeTo(null); // Centre si hors écran
            }
        } else {
            setLocationRelativeTo(null); // Centre si pas de position sauvegardée
        }
    }

    // Méthode centralisée pour quitter l'application proprement
    private void closeApplication() {
        savePreferences(); // Sauvegarde la taille/position
        // Ferme la connexion BDD via le shutdown hook déjà en place dans MainApplication
        System.exit(0); // Termine la JVM
    }


    // --- Export CSV ---
    private void exportTableToCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exporter la vue actuelle en CSV");
        fileChooser.setSelectedFile(new File("export_stations_" + System.currentTimeMillis() + ".csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile(); // Variable locale initiale

            // S'assurer que le fichier a l'extension .csv
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                // Réassignation potentielle ici
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }

            // Confirmation si le fichier existe déjà
            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                        "Le fichier '" + fileToSave.getName() + "' existe déjà.\nVoulez-vous le remplacer?",
                        "Confirmer l'écrasement", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) {
                    statusBar.setText("Export CSV annulé.");
                    return;
                }
            }

            // --- DEBUT DE LA CORRECTION ---
            // Crée une nouvelle variable final (ou effectivement final)
            // qui contient la valeur de fileToSave APRES ses modifications potentielles.
            final File finalFileToSave = fileToSave;
            // --- FIN DE LA CORRECTION ---

            statusBar.setText("Exportation en CSV vers " + finalFileToSave.getName() + "..."); // Utilise la nouvelle variable

            // Utiliser un thread séparé pour l'export si la table est très grande
            SwingUtilities.invokeLater(() -> {
                // Utilise la variable final dans la lambda
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalFileToSave))) {
                    TableModel model = stationTable.getModel();
                    int columnCount = model.getColumnCount();

                    // Écrire l'en-tête
                    for (int i = 0; i < columnCount; i++) {
                        writer.write(escapeCsvValue(model.getColumnName(i)));
                        if (i < columnCount - 1) {
                            writer.write(",");
                        }
                    }
                    writer.newLine();

                    // Écrire les lignes de données
                    int rowCount = stationTable.getRowCount();
                    for (int i = 0; i < rowCount; i++) {
                        for (int j = 0; j < columnCount; j++) {
                            Object value = stationTable.getValueAt(i, j);
                            writer.write(escapeCsvValue(value != null ? value.toString() : ""));
                            if (j < columnCount - 1) {
                                writer.write(",");
                            }
                        }
                        writer.newLine();
                    }

                    // Utilise la variable final dans la lambda
                    JOptionPane.showMessageDialog(this,
                            "Données exportées avec succès vers :\n" + finalFileToSave.getAbsolutePath(),
                            "Export CSV Réussi", JOptionPane.INFORMATION_MESSAGE);
                    statusBar.setText("Export CSV terminé. " + tableModel.getRowCount() + " station(s) affichée(s).");

                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Erreur lors de l'exportation en CSV:\n" + ex.getMessage(),
                            "Erreur d'Exportation", JOptionPane.ERROR_MESSAGE);
                    statusBar.setText("Erreur lors de l'export CSV.");
                    ex.printStackTrace();
                }
            });
        } else {
            statusBar.setText("Export CSV annulé par l'utilisateur.");
        }
    }

    // Méthode simple pour échapper les virgules et guillemets pour CSV
    private String escapeCsvValue(String value) {
        if (value == null) return "";
        // Si la valeur contient une virgule, un guillemet ou un retour à la ligne, l'encadrer de guillemets
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Doubler les guillemets existants à l'intérieur
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }
}
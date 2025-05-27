package com.example.csms.view;

import com.example.csms.exception.DataAccessException;
import com.example.csms.model.Statut;
import com.example.csms.model.Station;
import com.example.csms.service.StationService;

// PDF imports (you'll need to add iText library to classpath)
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.Rectangle;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class MainAppFrame extends JFrame {

    private final StationService stationService;
    private final boolean isAuthenticated; // Mode utilisateur: true = admin, false = visiteur

    private JTable stationTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton viewDetailsButton;
    private JButton addButton;
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

    public MainAppFrame(boolean isAuthenticated) {
        this.stationService = new StationService();
        this.isAuthenticated = isAuthenticated;

        String modeText = isAuthenticated ? "Mode Administrateur" : "Mode visiteur";
        setTitle("Gestionnaire de Stations de Charge v0.5 - " + modeText);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem exportPdfItem = new JMenuItem("Exporter Vue Actuelle (PDF)...");
        exportPdfItem.addActionListener(e -> exportTableToPdf());
        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(e -> closeApplication());
        fileMenu.add(exportPdfItem);
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
        tableModel = new DefaultTableModel(new Object[] { "ID", "Nom", "Localisation", "Statut", "Dernière MàJ" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 4)
                    return java.sql.Timestamp.class;
                if (columnIndex == 3)
                    return String.class;
                return super.getColumnClass(columnIndex);
            }
        };
        stationTable = new JTable(tableModel);
        stationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        stationTable.setAutoCreateRowSorter(true);
        stationTable.setFillsViewportHeight(true);
        stationTable.setRowHeight(25);

        TableColumn statusColumn = stationTable.getColumnModel().getColumn(3);
        statusColumn.setCellRenderer(new StatusCellRenderer());

        sorter = new TableRowSorter<>(tableModel);
        stationTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(stationTable);

        // --- Menu Contextuel (seulement pour utilisateurs authentifiés) ---
        if (isAuthenticated) {
            createTablePopupMenu();
            stationTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShowPopup(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShowPopup(e);
                }

                private void maybeShowPopup(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int rowAtPoint = stationTable.rowAtPoint(e.getPoint());
                        if (rowAtPoint >= 0 && !stationTable.isRowSelected(rowAtPoint)) {
                            stationTable.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                        }
                        updatePopupMenuState();
                        tablePopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            });
        }

        // --- Panneau de Contrôle (Haut) ---
        JPanel controlPanel = new JPanel(new BorderLayout(10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Rechercher:"));
        searchField = new JTextField(20);
        searchButton = new JButton("🔍 Chercher");
        searchButton.setToolTipText("Rechercher dans le nom ou la localisation");
        JButton clearSearchButton = new JButton("Effacer Filtres");
        clearSearchButton.setToolTipText("Effacer le critère de recherche et le filtre de statut");
        filterPanel.add(searchField);
        filterPanel.add(searchButton);
        filterPanel.add(clearSearchButton);
        filterPanel.add(Box.createHorizontalStrut(15));
        filterPanel.add(new JLabel("Filtrer Statut:"));
        statusFilterComboBox = new JComboBox<>();
        statusFilterComboBox.addItem("Tous");
        for (Statut s : Statut.values()) {
            statusFilterComboBox.addItem(s.getDescription());
        }
        statusFilterComboBox.setToolTipText("Filtrer la vue par statut");
        filterPanel.add(statusFilterComboBox);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("🔄 Rafraîchir");
        refreshButton.setToolTipText("Recharger les données depuis la base");
        viewDetailsButton = new JButton("ℹ️ Voir Détails");
        viewDetailsButton.setToolTipText("Afficher les détails de la station sélectionnée (une seule)");
        actionButtonPanel.add(refreshButton);
        actionButtonPanel.add(viewDetailsButton);

        controlPanel.add(filterPanel, BorderLayout.CENTER);
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

        // --- Panneau de Boutons CRUD (seulement pour utilisateurs authentifiés) ---
        JPanel crudButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        crudButtonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        if (isAuthenticated) {
            addButton = new JButton("➕ Ajouter Station");
            addButton.setToolTipText("Ouvrir le formulaire pour ajouter une nouvelle station");
            editButton = new JButton("✏️ Modifier Sélection");
            editButton.setToolTipText("Ouvrir le formulaire pour modifier la station sélectionnée (une seule)");
            deleteButton = new JButton("❌ Supprimer Sélection");
            deleteButton.setToolTipText("Supprimer la ou les station(s) sélectionnée(s) (avec confirmation)");

            crudButtonPanel.add(addButton);
            crudButtonPanel.add(editButton);
            crudButtonPanel.add(deleteButton);
        } else {
            // Mode visiteur - Afficher un message informatif et bouton de connexion
            JLabel guestLabel = new JLabel("Mode visiteur ");
            guestLabel.setForeground(Color.BLUE);
            guestLabel.setFont(guestLabel.getFont().deriveFont(java.awt.Font.ITALIC));

            JButton loginButton = new JButton("🔐 Se Connecter");
            loginButton.setToolTipText("Se connecter pour accéder aux fonctions d'administration");
            loginButton.addActionListener(e -> switchToAuthenticatedMode());

            crudButtonPanel.add(guestLabel);
            crudButtonPanel.add(Box.createHorizontalStrut(20));
            crudButtonPanel.add(loginButton);
        }

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
        viewDetailsButton.addActionListener(e -> ouvrirDialogueDetailsSelectionUnique());
        searchButton.addActionListener(e -> appliquerFiltres());
        searchField.addActionListener(e -> appliquerFiltres());
        clearSearchButton.addActionListener(e -> {
            searchField.setText("");
            statusFilterComboBox.setSelectedIndex(0);
            appliquerFiltres();
            searchField.requestFocusInWindow();
        });
        statusFilterComboBox.addActionListener(e -> appliquerFiltres());

        // Actions CRUD (seulement pour utilisateurs authentifiés)
        if (isAuthenticated) {
            addButton.addActionListener(e -> ouvrirDialogueStation(null));
            editButton.addActionListener(e -> ouvrirDialogueModificationSelectionUnique());
            deleteButton.addActionListener(e -> supprimerStationsSelectionnees());
        }

        // --- Gestion Fermeture Fenêtre ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeApplication();
            }
        });

        loadPreferences();
        chargerDonneesEtFiltrer();
    }

    // Création du menu contextuel (seulement pour utilisateurs authentifiés)
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

    private void updatePopupMenuState() {
        int selectedRowCount = stationTable.getSelectedRowCount();
        changeStatusMenuItem.setEnabled(selectedRowCount == 1);
        copyInfoMenuItem.setEnabled(selectedRowCount == 1);
    }

    private void chargerDonneesEtFiltrer() {
        statusBar.setText("Chargement des données depuis la base...");
        SwingUtilities.invokeLater(() -> {
            try {
                allLoadedStations = stationService.trouverToutesLesStations();
                appliquerFiltresLocaux();
                mettreAJourStatistiques();
                statusBar.setText(tableModel.getRowCount() + " station(s) affichée(s). Total global: "
                        + allLoadedStations.size() + ".");
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

    private void appliquerFiltres() {
        appliquerFiltresLocaux();
        statusBar.setText(tableModel.getRowCount() + " station(s) affichée(s) sur " + allLoadedStations.size()
                + " (Filtres appliqués).");
    }

    private void appliquerFiltresLocaux() {
        String texteFiltre = searchField.getText().trim().toLowerCase();
        String statutFiltreDesc = (String) statusFilterComboBox.getSelectedItem();
        Statut statutFiltre = !"Tous".equals(statutFiltreDesc) ? Statut.fromString(statutFiltreDesc) : null;

        List<Station> stationsFiltrees = allLoadedStations.stream()
                .filter(station -> {
                    boolean matchTexte = texteFiltre.isEmpty() ||
                            (station.getNom() != null && station.getNom().toLowerCase().contains(texteFiltre)) ||
                            (station.getLocalisation() != null
                                    && station.getLocalisation().toLowerCase().contains(texteFiltre));
                    boolean matchStatut = statutFiltre == null
                            || (station.getStatut() != null && station.getStatut() == statutFiltre);
                    return matchTexte && matchStatut;
                })
                .collect(Collectors.toList());

        tableModel.setRowCount(0);
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

    // Méthodes CRUD (seulement disponibles pour utilisateurs authentifiés)
    private void ouvrirDialogueStation(Station stationAModifier) {
        if (!isAuthenticated)
            return;
        StationDialog dialog = new StationDialog(this, stationService, stationAModifier);
        dialog.setVisible(true);
        if (dialog.isSucces()) {
            String action = (stationAModifier == null) ? "ajoutée" : "modifiée";
            chargerDonneesEtFiltrer();
            statusBar.setText(
                    "Station " + action + " avec succès. " + tableModel.getRowCount() + " station(s) affichée(s).");
        }
    }

    private void ouvrirDialogueModificationSelectionUnique() {
        if (!isAuthenticated)
            return;
        if (stationTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner EXACTEMENT une station à modifier.",
                    "Sélection Invalide", JOptionPane.WARNING_MESSAGE);
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
                ouvrirDialogueStation(stationOpt.get());
            } else {
                handleStationNotFoundError();
            }
        } catch (DataAccessException e) {
            handleDataAccessException("récupération pour modification", e);
        }
    }

    private void ouvrirDialogueDetailsSelectionUnique() {
        if (stationTable.getSelectedRowCount() != 1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner EXACTEMENT une station pour voir les détails.",
                    "Sélection Invalide", JOptionPane.WARNING_MESSAGE);
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
        } catch (DataAccessException e) {
            handleDataAccessException("consultation des détails", e);
        }
    }

    private void supprimerStationsSelectionnees() {
        if (!isAuthenticated)
            return;
        int[] selectedRowsView = stationTable.getSelectedRows();
        if (selectedRowsView.length == 0) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner au moins une station à supprimer.",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int[] selectedRowsModel = Arrays.stream(selectedRowsView)
                .map(stationTable::convertRowIndexToModel)
                .toArray();

        StringBuilder confirmationMessage = new StringBuilder("Êtes-vous sûr de vouloir supprimer ");
        List<Long> idsToDelete = new ArrayList<>();
        if (selectedRowsModel.length == 1) {
            long id = (long) tableModel.getValueAt(selectedRowsModel[0], 0);
            String nom = (String) tableModel.getValueAt(selectedRowsModel[0], 1);
            confirmationMessage.append("la station '").append(nom).append("' (ID: ").append(id).append(")?");
            idsToDelete.add(id);
        } else {
            confirmationMessage.append("les ").append(selectedRowsModel.length)
                    .append(" stations sélectionnées ?\nIDs: ");
            for (int i = 0; i < selectedRowsModel.length; i++) {
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

            for (long id : idsToDelete) {
                try {
                    boolean deleted = stationService.supprimerStation(id);
                    if (deleted) {
                        successCount++;
                    } else {
                        failCount++;
                        errors.add("Échec suppression ID " + id + " (non trouvée?)");
                    }
                } catch (DataAccessException e) {
                    failCount++;
                    errors.add("Erreur BDD suppression ID " + id + ": " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    failCount++;
                    errors.add("Erreur inattendue suppression ID " + id + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            StringBuilder resultMessage = new StringBuilder();
            if (successCount > 0) {
                resultMessage.append(successCount).append(" station(s) supprimée(s) avec succès.\n");
            }
            if (failCount > 0) {
                resultMessage.append(failCount).append(" suppression(s) échouée(s).\n");
                if (!errors.isEmpty()) {
                    resultMessage.append("Détails erreurs:\n");
                    errors.forEach(err -> resultMessage.append("- ").append(err).append("\n"));
                }
                JOptionPane.showMessageDialog(this, resultMessage.toString(), "Résultat Suppression",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, resultMessage.toString(), "Succès Suppression",
                        JOptionPane.INFORMATION_MESSAGE);
            }

            chargerDonneesEtFiltrer();
            statusBar.setText(successCount + "/" + idsToDelete.size() + " station(s) supprimée(s). "
                    + tableModel.getRowCount() + " affichée(s).");
        } else {
            statusBar.setText("Suppression annulée.");
        }
    }

    private void changerStatutRapideSelection() {
        if (!isAuthenticated)
            return;
        if (stationTable.getSelectedRowCount() != 1)
            return;

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
                        tableModel.setValueAt(nouveauStatut.getDescription(), selectedRowModel, 3);
                        allLoadedStations.stream().filter(s -> s.getId() == idStation).findFirst()
                                .ifPresent(s -> s.setStatut(nouveauStatut));
                        mettreAJourStatistiques();
                        statusBar.setText("Statut ID " + idStation + " mis à jour. " + tableModel.getRowCount()
                                + " station(s) affichée(s).");
                        ((DefaultTableModel) stationTable.getModel()).fireTableRowsUpdated(selectedRowModel,
                                selectedRowModel);
                    } else {
                        handleOperationFailure("mise à jour du statut");
                    }
                } catch (DataAccessException e) {
                    handleDataAccessException("mise à jour du statut", e);
                } catch (Exception e) {
                    handleUnexpectedError("mise à jour du statut", e);
                }
            } else {
                statusBar.setText("Statut inchangé.");
            }
        } else {
            statusBar.setText("Changement de statut annulé.");
        }
    }

    private void copierInfosSelection() {
        if (stationTable.getSelectedRowCount() != 1)
            return;

        int selectedRowView = stationTable.getSelectedRow();
        int selectedRowModel = stationTable.convertRowIndexToModel(selectedRowView);
        long id = (long) tableModel.getValueAt(selectedRowModel, 0);
        String nom = (String) tableModel.getValueAt(selectedRowModel, 1);
        Object locObj = tableModel.getValueAt(selectedRowModel, 2);
        String localisation = (locObj != null) ? locObj.toString() : "-";

        String textToCopy = String.format("ID: %d\nNom: %s\nLocalisation: %s", id, nom, localisation);

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(textToCopy);
        clipboard.setContents(stringSelection, null);

        updateStatusBar("Infos ID " + id + " copiées.", 2500);
    }

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

    private void showAboutDialog() {
        String accessMode = isAuthenticated ? "Administrateur (accès complet)" : "visiteur (lecture seule)";
        JOptionPane.showMessageDialog(this,
                "Gestionnaire de Stations de Charge v0.5\n\n" +
                        "Mode d'accès actuel: " + accessMode + "\n\n" +
                        "Fonctionnalités :\n" +
                        "  - Authentification utilisateur + Mode visiteur\n" +
                        "  - CRUD Stations (Admin uniquement)\n" +
                        "  - Consultation, Recherche, Filtrage\n" +
                        "  - Statistiques globales\n" +
                        "  - Vue détaillée, Copie d'infos\n" +
                        "  - Export PDF\n" +
                        "  - Persistance taille/position fenêtre\n\n" +
                        "Technologies : Java, Swing, JDBC, MySQL, iText PDF\n\n" +
                        "ATTENTION : Sécurité des mots de passe basique (exemple).",
                "À Propos de CSMS Enhanced",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // --- Export PDF ---
    private void exportTableToPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Exporter la vue actuelle en PDF");
        fileChooser.setSelectedFile(new File("export_stations_" + System.currentTimeMillis() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".pdf");
            }

            if (fileToSave.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                        "Le fichier '" + fileToSave.getName() + "' existe déjà.\nVoulez-vous le remplacer?",
                        "Confirmer l'écrasement", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION) {
                    statusBar.setText("Export PDF annulé.");
                    return;
                }
            }

            final File finalFileToSave = fileToSave;

            statusBar.setText("Exportation en PDF vers " + finalFileToSave.getName() + "...");

            SwingUtilities.invokeLater(() -> {
                try {
                    Document document = new Document(PageSize.A4, 50, 50, 50, 50);
                    PdfWriter.getInstance(document, new FileOutputStream(finalFileToSave));
                    document.open();

                    // Titre du document
                    com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18,
                            BaseColor.BLACK);
                    Paragraph title = new Paragraph("Export des Stations de Charge", titleFont);
                    title.setAlignment(Element.ALIGN_CENTER);
                    title.setSpacingAfter(20);
                    document.add(title);

                    // Informations sur l'export
                    com.itextpdf.text.Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
                    String modeText = isAuthenticated ? "Mode Administrateur" : "Mode visiteur";
                    Paragraph info = new Paragraph("Généré le: " + new java.util.Date() + " | " + modeText, infoFont);
                    info.setAlignment(Element.ALIGN_CENTER);
                    info.setSpacingAfter(15);
                    document.add(info);

                    // Statistiques
                    Map<String, Long> stats = stationService.getStationStatistics();
                    com.itextpdf.text.Font statsFont = FontFactory.getFont(FontFactory.HELVETICA, 11,
                            BaseColor.DARK_GRAY);
                    Paragraph statsP = new Paragraph(
                            "Statistiques: Total: " + stats.getOrDefault("TOTAL", 0L) +
                                    " | Disponibles: " + stats.getOrDefault(Statut.DISPONIBLE.name(), 0L) +
                                    " | En Charge: " + stats.getOrDefault(Statut.EN_CHARGE.name(), 0L) +
                                    " | Hors Service: " + stats.getOrDefault(Statut.HORS_SERVICE.name(), 0L),
                            statsFont);
                    statsP.setAlignment(Element.ALIGN_CENTER);
                    statsP.setSpacingAfter(20);
                    document.add(statsP);

                    // Tableau des données
                    PdfPTable table = new PdfPTable(5); // 5 colonnes
                    table.setWidthPercentage(100);
                    table.setWidths(new float[] { 1f, 3f, 3f, 2f, 2.5f }); // Largeurs relatives

                    // En-têtes
                    com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10,
                            BaseColor.WHITE);
                    String[] headers = { "ID", "Nom", "Localisation", "Statut", "Dernière MàJ" };
                    for (String header : headers) {
                        PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                        cell.setBackgroundColor(BaseColor.DARK_GRAY);
                        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        cell.setPadding(8);
                        table.addCell(cell);
                    }

                    // Données
                    com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
                    int rowCount = stationTable.getRowCount();
                    for (int i = 0; i < rowCount; i++) {
                        for (int j = 0; j < 5; j++) {
                            Object value = stationTable.getValueAt(i, j);
                            String cellText = (value != null) ? value.toString() : "";

                            PdfPCell cell = new PdfPCell(new Phrase(cellText, dataFont));
                            cell.setPadding(5);

                            // Coloration selon le statut (colonne 3)
                            if (j == 3 && value != null) {
                                String statusText = value.toString();
                                if ("Disponible".equals(statusText)) {
                                    cell.setBackgroundColor(new BaseColor(200, 255, 200));
                                } else if ("En charge".equals(statusText)) {
                                    cell.setBackgroundColor(new BaseColor(255, 230, 180));
                                } else if ("Hors service".equals(statusText)) {
                                    cell.setBackgroundColor(new BaseColor(255, 200, 200));
                                }
                            }

                            if (j == 0) { // ID centré
                                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            }

                            table.addCell(cell);
                        }
                    }

                    document.add(table);

                    // Pied de page
                    Paragraph footer = new Paragraph("\nNombre de stations affichées: " + rowCount, infoFont);
                    footer.setAlignment(Element.ALIGN_RIGHT);
                    footer.setSpacingBefore(15);
                    document.add(footer);

                    document.close();

                    JOptionPane.showMessageDialog(this,
                            "Données exportées avec succès vers :\n" + finalFileToSave.getAbsolutePath(),
                            "Export PDF Réussi", JOptionPane.INFORMATION_MESSAGE);
                    statusBar.setText("Export PDF terminé. " + tableModel.getRowCount() + " station(s) affichée(s).");

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Erreur lors de l'exportation en PDF:\n" + ex.getMessage() +
                                    "\nVérifiez que la bibliothèque iText est dans le classpath.",
                            "Erreur d'Exportation", JOptionPane.ERROR_MESSAGE);
                    statusBar.setText("Erreur lors de l'export PDF.");
                    ex.printStackTrace();
                }
            });
        } else {
            statusBar.setText("Export PDF annulé par l'utilisateur.");
        }
    }

    // --- Méthodes utilitaires ---
    private void handleDataAccessException(String operation, DataAccessException e) {
        JOptionPane.showMessageDialog(this, "Erreur Base de Données lors de: " + operation + "\n" + e.getMessage(),
                "Erreur BDD", JOptionPane.ERROR_MESSAGE);
        statusBar.setText("Erreur BDD pendant: " + operation);
        e.printStackTrace();
    }

    private void handleUnexpectedError(String operation, Exception e) {
        JOptionPane.showMessageDialog(this, "Erreur Inattendue lors de: " + operation + "\n" + e.getMessage(),
                "Erreur Inattendue", JOptionPane.ERROR_MESSAGE);
        statusBar.setText("Erreur inattendue pendant: " + operation);
        e.printStackTrace();
    }

    private void handleOperationFailure(String operation) {
        JOptionPane.showMessageDialog(this, "Échec de l'opération: " + operation, "Échec", JOptionPane.WARNING_MESSAGE);
        statusBar.setText("Échec: " + operation);
    }

    private void handleStationNotFoundError() {
        JOptionPane.showMessageDialog(this, "La station sélectionnée n'a pas été trouvée (peut-être supprimée?).",
                "Erreur", JOptionPane.ERROR_MESSAGE);
        chargerDonneesEtFiltrer();
        statusBar.setText("Erreur: Station non trouvée.");
    }

    private void updateStatusBar(String message, int delayMillis) {
        statusBar.setText(message);
        Timer timer = new Timer(delayMillis, e -> {
            if (statusBar.getText().equals(message)) {
                statusBar.setText("Prêt.");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void savePreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainAppFrame.class);
        Rectangle bounds = getBounds();
        prefs.putInt(PREF_KEY_X, bounds.x);
        prefs.putInt(PREF_KEY_Y, bounds.y);
        prefs.putInt(PREF_KEY_WIDTH, bounds.width);
        prefs.putInt(PREF_KEY_HEIGHT, bounds.height);
    }

    private void loadPreferences() {
        Preferences prefs = Preferences.userNodeForPackage(MainAppFrame.class);
        int x = prefs.getInt(PREF_KEY_X, -1);
        int y = prefs.getInt(PREF_KEY_Y, -1);
        int width = prefs.getInt(PREF_KEY_WIDTH, DEFAULT_WIDTH);
        int height = prefs.getInt(PREF_KEY_HEIGHT, DEFAULT_HEIGHT);

        setSize(width, height);
        if (x != -1 && y != -1) {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            if (screenBounds.contains(x, y)) {
                setLocation(x, y);
            } else {
                setLocationRelativeTo(null);
            }
        } else {
            setLocationRelativeTo(null);
        }
    }

    private void closeApplication() {
        savePreferences();
        System.exit(0);
    }

    // Méthode pour passer du mode visiteur au mode authentifié
    private void switchToAuthenticatedMode() {
        LoginDialog loginDialog = new LoginDialog(this);
        loginDialog.setVisible(true);

        if (loginDialog.isAuthenticated()) {
            // L'utilisateur s'est connecté avec succès
            JOptionPane.showMessageDialog(this,
                    "Connexion réussie ! L'application va redémarrer en mode administrateur.",
                    "Connexion Réussie", JOptionPane.INFORMATION_MESSAGE);

            // Sauvegarder les préférences actuelles
            savePreferences();

            // Fermer la fenêtre actuelle et relancer en mode authentifié
            dispose();

            SwingUtilities.invokeLater(() -> {
                MainAppFrame newFrame = new MainAppFrame(true); // Mode authentifié
                newFrame.setVisible(true);
            });
        }
        // Si la connexion échoue ou est annulée, on reste en mode visiteur
    }
}
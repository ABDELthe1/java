package com.example.csms.view;

import com.example.csms.model.Statut; // Importe l'enum

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

// Renderer personnalisé pour afficher des couleurs selon le statut de la station
public class StatusCellRenderer extends DefaultTableCellRenderer {

    // Définition des couleurs (vous pouvez les personnaliser)
    private static final Color COLOR_DISPONIBLE_BG = new Color(200, 255, 200); // Vert clair
    private static final Color COLOR_EN_CHARGE_BG = new Color(255, 230, 180);  // Orange clair
    private static final Color COLOR_HORS_SERVICE_BG = new Color(255, 200, 200); // Rouge clair
    private static final Color COLOR_DEFAUT_BG = Color.WHITE;

    private static final Color COLOR_DISPONIBLE_FG = Color.BLACK;
    private static final Color COLOR_EN_CHARGE_FG = Color.BLACK;
    private static final Color COLOR_HORS_SERVICE_FG = Color.BLACK; // Ou Color.DARK_GRAY;
    private static final Color COLOR_DEFAUT_FG = Color.BLACK;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {

        // Appelle le renderer par défaut pour obtenir le composant de base (JLabel)
        Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Réinitialise les couleurs au cas où la cellule serait réutilisée
        Color bgColor = COLOR_DEFAUT_BG;
        Color fgColor = COLOR_DEFAUT_FG;

        if (value instanceof String) {
            String statusDescription = (String) value;
            // Trouve le statut correspondant à la description
            Statut statut = Statut.fromString(statusDescription);

            switch (statut) {
                case DISPONIBLE:
                    bgColor = COLOR_DISPONIBLE_BG;
                    fgColor = COLOR_DISPONIBLE_FG;
                    break;
                case EN_CHARGE:
                    bgColor = COLOR_EN_CHARGE_BG;
                    fgColor = COLOR_EN_CHARGE_FG;
                    break;
                case HORS_SERVICE:
                    bgColor = COLOR_HORS_SERVICE_BG;
                    fgColor = COLOR_HORS_SERVICE_FG;
                    break;
                default:
                    // Utilise les couleurs par défaut si statut inconnu
                    break;
            }
        }

        // Applique les couleurs seulement si la cellule n'est pas sélectionnée
        // (la sélection a sa propre couleur définie par le Look and Feel)
        if (!isSelected) {
            cellComponent.setBackground(bgColor);
            cellComponent.setForeground(fgColor);
        } else {
            // Garde les couleurs de sélection par défaut
            cellComponent.setBackground(table.getSelectionBackground());
            cellComponent.setForeground(table.getSelectionForeground());
        }

        // Centre le texte dans la cellule de statut (optionnel)
        setHorizontalAlignment(JLabel.CENTER);

        return cellComponent;
    }
}
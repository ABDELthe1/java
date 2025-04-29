package com.example.csms.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Statut {
    DISPONIBLE("Disponible"),
    EN_CHARGE("En charge"),
    HORS_SERVICE("Hors service");

    private final String description;

    Statut(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Retourne la valeur à stocker en BDD (le nom de l'enum)
    public String dbValue() {
        return this.name();
    }

    // Trouve un Statut depuis une chaîne (venant de BDD ou UI)
    public static Statut fromString(String text) {
        if (text == null) return HORS_SERVICE; // Valeur par défaut ou gestion d'erreur
        return Arrays.stream(Statut.values())
                .filter(s -> s.name().equalsIgnoreCase(text) || s.getDescription().equalsIgnoreCase(text))
                .findFirst()
                .orElse(HORS_SERVICE); // Valeur par défaut si non trouvé
    }

    // Retourne toutes les descriptions pour JComboBox
    public static List<String> getAllDescriptions() {
        return Arrays.stream(Statut.values())
                .map(Statut::getDescription)
                .collect(Collectors.toList());
    }
}
package com.example.csms.model;

import java.sql.Timestamp; // Pour la dernière mise à jour

public class Station {
    private long id;
    private String nom;
    private String localisation;
    private Statut statut;
    private Timestamp derniereMiseAJour; // Optionnel

    // Constructeurs
    public Station() {}

    public Station(String nom, String localisation, Statut statut) {
        this.nom = nom;
        this.localisation = localisation;
        this.statut = statut;
    }

    public Station(long id, String nom, String localisation, Statut statut, Timestamp derniereMiseAJour) {
        this.id = id;
        this.nom = nom;
        this.localisation = localisation;
        this.statut = statut;
        this.derniereMiseAJour = derniereMiseAJour;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
    public Timestamp getDerniereMiseAJour() { return derniereMiseAJour; }
    public void setDerniereMiseAJour(Timestamp derniereMiseAJour) { this.derniereMiseAJour = derniereMiseAJour; }

    @Override
    public String toString() {
        return String.format("Station [ID=%d, Nom='%s', Lieu='%s', Statut=%s]",
                id, nom, localisation, (statut != null ? statut.getDescription() : "N/A"));
    }
}
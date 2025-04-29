package com.example.csms.dao;

import com.example.csms.model.Station;
import java.util.List;
import java.util.Optional;

public interface StationDAO {
    Station ajouterStation(Station station); // Retourne la station avec ID
    boolean modifierStation(Station station); // Retourne true si succès
    boolean supprimerStation(long id); // Retourne true si succès
    Optional<Station> trouverStationParId(long id);
    List<Station> trouverToutesLesStations();
    List<Station> rechercherStations(String critere); // Nouvelle méthode
}
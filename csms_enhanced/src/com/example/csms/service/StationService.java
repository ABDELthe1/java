package com.example.csms.service;

import com.example.csms.dao.StationDAO;
import com.example.csms.dao.impl.StationDAOImpl;
import com.example.csms.exception.DataAccessException;
import com.example.csms.model.Statut;
import com.example.csms.model.Station;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StationService {

    private final StationDAO stationDAO;

    public StationService() {
        this.stationDAO = new StationDAOImpl();
    }

    public StationService(StationDAO stationDAO) {
        this.stationDAO = stationDAO;
    }

    // ... (méthodes ajouterStation, modifierStation, supprimerStation, trouverStationParId inchangées) ...
    public Station ajouterStation(Station station) throws DataAccessException {
        if (station == null || station.getNom() == null || station.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de la station ne peut pas être vide.");
        }
        return stationDAO.ajouterStation(station);
    }

    public boolean modifierStation(Station station) throws DataAccessException {
        if (station == null || station.getId() <= 0 || station.getNom() == null || station.getNom().trim().isEmpty()) {
            throw new IllegalArgumentException("Données de station invalides pour la modification.");
        }
        return stationDAO.modifierStation(station);
    }

    /**
     * Met à jour uniquement le statut d'une station existante.
     * @param stationId L'ID de la station à mettre à jour.
     * @param nouveauStatut Le nouveau statut à appliquer.
     * @return true si la mise à jour a réussi, false sinon.
     * @throws DataAccessException Si une erreur BDD survient ou si la station n'est pas trouvée.
     * @throws IllegalArgumentException Si l'ID ou le statut est invalide.
     */
    public boolean updateStationStatus(long stationId, Statut nouveauStatut) throws DataAccessException {
        if (stationId <= 0) {
            throw new IllegalArgumentException("ID de station invalide.");
        }
        if (nouveauStatut == null) {
            throw new IllegalArgumentException("Le nouveau statut ne peut pas être null.");
        }

        // 1. Récupérer la station existante pour avoir toutes ses infos
        Optional<Station> stationOpt = trouverStationParId(stationId);
        if (!stationOpt.isPresent()) {
            throw new DataAccessException("Station avec ID " + stationId + " non trouvée pour la mise à jour du statut.");
        }

        // 2. Mettre à jour uniquement le statut
        Station stationExistante = stationOpt.get();
        stationExistante.setStatut(nouveauStatut);

        // 3. Appeler la méthode de modification générale du DAO
        return stationDAO.modifierStation(stationExistante);
    }


    public boolean supprimerStation(long id) throws DataAccessException {
        if (id <= 0) {
            throw new IllegalArgumentException("ID de station invalide pour la suppression.");
        }
        return stationDAO.supprimerStation(id);
    }

    public Optional<Station> trouverStationParId(long id) throws DataAccessException {
        if (id <= 0) {
            return Optional.empty();
        }
        return stationDAO.trouverStationParId(id);
    }

    public List<Station> trouverToutesLesStations() throws DataAccessException {
        try {
            return stationDAO.trouverToutesLesStations();
        } catch (DataAccessException e) {
            System.err.println("Erreur service: impossible de récupérer les stations: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Station> rechercherStations(String critere) throws DataAccessException {
        if (critere == null || critere.trim().isEmpty()) {
            return trouverToutesLesStations();
        }
        try {
            return stationDAO.rechercherStations(critere.trim());
        } catch (DataAccessException e) {
            System.err.println("Erreur service: impossible de rechercher les stations: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Long> getStationStatistics() throws DataAccessException {
        List<Station> stations = trouverToutesLesStations();
        Map<String, Long> stats = new HashMap<>();

        for (Statut s : Statut.values()) {
            stats.put(s.name(), 0L);
        }

        Map<Statut, Long> countsByStatus = stations.stream()
                .filter(s -> s.getStatut() != null)
                .collect(Collectors.groupingBy(Station::getStatut, Collectors.counting()));

        stats.putAll(countsByStatus.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)));

        stats.put("TOTAL", (long) stations.size());
        return stats;
    }
}
package com.example.csms.dao.impl;

import com.example.csms.dao.StationDAO;
import com.example.csms.exception.DataAccessException;
import com.example.csms.model.Statut;
import com.example.csms.model.Station;
import com.example.csms.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StationDAOImpl implements StationDAO {

    @Override
    public Station ajouterStation(Station station) {
        String sql = "INSERT INTO stations (nom, localisation, statut) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, station.getNom());
            pstmt.setString(2, station.getLocalisation());
            pstmt.setString(3, station.getStatut().dbValue());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DataAccessException("Échec de l'ajout de la station, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    station.setId(generatedKeys.getLong(1));
                    return station; // Retourne la station avec l'ID généré
                } else {
                    throw new DataAccessException("Échec de l'ajout, impossible de récupérer l'ID généré.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de l'ajout de la station: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean modifierStation(Station station) {
        String sql = "UPDATE stations SET nom = ?, localisation = ?, statut = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, station.getNom());
            pstmt.setString(2, station.getLocalisation());
            pstmt.setString(3, station.getStatut().dbValue());
            pstmt.setLong(4, station.getId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // True si au moins une ligne a été modifiée
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la modification de la station: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supprimerStation(long id) {
        String sql = "DELETE FROM stations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0; // True si au moins une ligne a été supprimée
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la suppression de la station: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Station> trouverStationParId(long id) {
        String sql = "SELECT id, nom, localisation, statut, derniere_mise_a_jour FROM stations WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToStation(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la recherche par ID: " + e.getMessage(), e);
        }
        return Optional.empty(); // Non trouvé
    }

    @Override
    public List<Station> trouverToutesLesStations() {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT id, nom, localisation, statut, derniere_mise_a_jour FROM stations ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                stations.add(mapRowToStation(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la récupération de toutes les stations: " + e.getMessage(), e);
        }
        return stations;
    }

    @Override
    public List<Station> rechercherStations(String critere) {
        List<Station> stations = new ArrayList<>();
        // Recherche simple dans nom ou localisation (insensible à la casse)
        String sql = "SELECT id, nom, localisation, statut, derniere_mise_a_jour FROM stations " +
                "WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(localisation) LIKE LOWER(?) ORDER BY nom";
        String recherchePattern = "%" + critere + "%"; // Ajoute les wildcards SQL

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, recherchePattern);
            pstmt.setString(2, recherchePattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    stations.add(mapRowToStation(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur SQL lors de la recherche de stations: " + e.getMessage(), e);
        }
        return stations;
    }

    // Helper method pour mapper un ResultSet à un objet Station
    private Station mapRowToStation(ResultSet rs) throws SQLException {
        return new Station(
                rs.getLong("id"),
                rs.getString("nom"),
                rs.getString("localisation"),
                Statut.fromString(rs.getString("statut")),
                rs.getTimestamp("derniere_mise_a_jour")
        );
    }
}
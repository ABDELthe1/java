# CSMS Enhanced - Gestionnaire de Stations de Charge (Application de Bureau)

Ce projet est une application de bureau conçue en Java Swing pour la gestion d'un parc de stations de charge pour véhicules électriques. Elle permet à un opérateur de superviser et d'administrer les informations relatives aux stations.

## Table des Matières

- [Fonctionnalités](#fonctionnalités)
- [Technologies Utilisées](#technologies-utilisées)
- [Prérequis](#prérequis)
- [Configuration de la Base de Données](#configuration-de-la-base-de-données)
- [Structure du Projet](#structure-du-projet)
- [Compilation](#compilation)
- [Exécution](#exécution)
  - [Depuis IntelliJ IDEA (Recommandé)](#depuis-intellij-idea-recommandé)
  - [Depuis la Ligne de Commande (Windows)](#depuis-la-ligne-de-commande-windows)
  - [Depuis la Ligne de Commande (Linux/macOS)](#depuis-la-ligne-de-commande-linuxmacos)
- [Créer un Exécutable `.exe` (Windows)](#créer-un-exécutable-exe-windows)
- [Utilisateur de Test](#utilisateur-de-test)
- [Avertissement de Sécurité](#avertissement-de-sécurité)
- [Perspectives d'Évolution](#perspectives-dévolution)
- [Auteur](#auteur)

## Fonctionnalités

L'application "CSMS Enhanced" offre les fonctionnalités suivantes :

*   **Authentification Opérateur :** Connexion sécurisée (basique) pour accéder au système.
*   **Gestion des Stations (CRUD) :**
    *   Ajouter de nouvelles stations de charge.
    *   Modifier les informations des stations existantes (nom, localisation, statut).
    *   Supprimer des stations (une ou plusieurs à la fois, avec confirmation).
*   **Consultation et Visualisation :**
    *   Afficher la liste complète des stations avec leurs détails (ID, Nom, Localisation, Statut, Dernière MàJ).
    *   Trier les stations par colonne dans le tableau principal.
    *   Indicateur visuel de couleur pour le statut des stations dans la table.
*   **Recherche et Filtrage :**
    *   Rechercher des stations par nom ou localisation.
    *   Filtrer la liste des stations par statut (Tous, Disponible, En Charge, Hors Service).
*   **Statistiques :** Affichage du nombre total de stations et décompte par statut.
*   **Actions Rapides (Menu Contextuel sur la table) :**
    *   Changer rapidement le statut d'une station sélectionnée.
    *   Copier les informations de base (ID, Nom, Lieu) d'une station dans le presse-papiers.
*   **Détails :** Consulter les informations complètes d'une station sélectionnée dans une fenêtre dédiée (lecture seule).
*   **Export de Données :** Exporter la vue actuelle du tableau des stations vers un fichier CSV.
*   **Interface Utilisateur :**
    *   Fenêtre "À Propos".
    *   Sauvegarde et restauration de la position et de la taille de la fenêtre principale.
    *   Infobulles (tooltips) sur les boutons.
    *   Barre de statut pour le feedback utilisateur.

## Technologies Utilisées

*   **Langage :** Java (JDK 17+ recommandé)
*   **Interface Graphique (GUI) :** Java Swing
*   **Accès Base de Données :** JDBC (Java Database Connectivity)
*   **Base de Données :** MySQL (via XAMPP par exemple)
*   **Pilote JDBC :** MySQL Connector/J
*   **IDE (pour le développement) :** IntelliJ IDEA (ou Eclipse, NetBeans)

## Prérequis

1.  **Java Development Kit (JDK) :** Version 17 ou plus récente installée et configurée (variable d'environnement `JAVA_HOME` et `Path`).
2.  **MySQL Server :** Installé et en cours d'exécution (par exemple via XAMPP).
3.  **MySQL Connector/J :** Le fichier `.jar` du connecteur (ex: `mysql-connector-j-9.3.0.jar`) doit être téléchargé.
4.  **Base de Données Créée :** La base de données et les tables doivent être créées en utilisant le script SQL fourni (voir section suivante).

## Configuration de la Base de Données

1.  Assurez-vous que votre serveur MySQL est démarré.
2.  Créez une base de données nommée `csms_db_enhanced` (ou le nom configuré dans `DatabaseConfig.java`).
3.  Exécutez le script SQL fourni dans le projet (souvent nommé `database_script.sql` ou similaire) pour créer les tables `users` et `stations` et insérer les données de test. Le script est disponible dans les réponses précédentes de la conversation ou doit être inclus dans le projet.
4.  Vérifiez/Adaptez les informations de connexion dans `src/com/example/csms/util/DatabaseConfig.java` si votre configuration MySQL est différente (URL, utilisateur, mot de passe).

## Structure du Projet

Le projet suit une architecture en couches simplifiée :

*   `src/com/example/csms/model/`: Contient les classes de modèle (POJOs) comme `Station`, `User`, `Statut`.
*   `src/com/example/csms/dao/`: Contient les interfaces DAO (`StationDAO`, `UserDAO`).
*   `src/com/example/csms/dao/impl/`: Contient les implémentations JDBC des DAO (`StationDAOImpl`, `UserDAOImpl`).
*   `src/com/example/csms/service/`: Contient la logique métier (`StationService`, `AuthService`).
*   `src/com/example/csms/view/`: Contient les classes de l'interface graphique Swing (`MainAppFrame`, `LoginDialog`, etc.).
*   `src/com/example/csms/util/`: Contient les classes utilitaires (`DatabaseConfig`, `DatabaseConnection`).
*   `src/com/example/csms/exception/`: Contient les exceptions personnalisées (`DataAccessException`).
*   `src/com/example/csms/MainApplication.java`: Point d'entrée de l'application.

## Compilation

1.  Placez le fichier `.jar` du **MySQL Connector/J** (ex: `mysql-connector-j-9.3.0.jar`) à la racine du dossier du projet (à côté du dossier `src`).
2.  Ouvrez un terminal ou une invite de commandes à la racine du projet.
3.  Créez un dossier `classes` s'il n'existe pas : `mkdir classes` (ou `md classes` sous Windows).
4.  Compilez les fichiers Java :
    *   **Linux/macOS :**
        ```bash
        javac -cp ".:mysql-connector-j-VERSION.jar" -d classes $(find src -name "*.java")
        ```
    *   **Windows :**
        ```cmd
        dir /s /B src\*.java > sources.txt
        javac -cp ".;mysql-connector-j-VERSION.jar" -d classes @sources.txt
        del sources.txt
        ```
    Remplacez `mysql-connector-j-VERSION.jar` par le nom exact de votre fichier JAR.

## Exécution

Assurez-vous que XAMPP (MySQL) est démarré avant de lancer l'application.

### Depuis IntelliJ IDEA (Recommandé)

1.  Ouvrez le projet dans IntelliJ IDEA.
2.  Ajoutez le fichier `mysql-connector-j-VERSION.jar` comme bibliothèque au projet (`File` > `Project Structure...` > `Libraries` > `+` > `Java`).
3.  Ouvrez le fichier `src/com/example/csms/MainApplication.java`.
4.  Cliquez sur la flèche verte "Play" à côté de la méthode `main` ou de la déclaration de la classe et sélectionnez `Run 'MainApplication.main()'`.

### Depuis la Ligne de Commande (Windows)

1.  Ouvrez une invite de commandes (`cmd`) dans le dossier racine du projet.
2.  Exécutez :
    ```cmd
    java -cp ".;classes;mysql-connector-j-VERSION.jar" com.example.csms.MainApplication
    ```
    Remplacez `mysql-connector-j-VERSION.jar` par le nom exact de votre fichier JAR.

### Depuis la Ligne de Commande (Linux/macOS)

1.  Ouvrez un terminal dans le dossier racine du projet.
2.  Exécutez :
    ```bash
    java -cp ".:classes:mysql-connector-j-VERSION.jar" com.example.csms.MainApplication
    ```
    Remplacez `mysql-connector-j-VERSION.jar` par le nom exact de votre fichier JAR.

## Créer un Exécutable `.exe` (Windows)

Vous pouvez utiliser des outils comme `jpackage` (inclus dans JDK 14+) ou Launch4j pour créer un fichier `.exe` autonome pour Windows. Consultez les réponses précédentes de la conversation pour les instructions détaillées.

## Utilisateur de Test

Pour la connexion, vous pouvez utiliser les identifiants suivants (définis dans le script SQL de la base de données) :

*   **Nom d'utilisateur :** `admin`
*   **Mot de passe :** `password`

## Avertissement de Sécurité

**IMPORTANT :** La gestion des mots de passe dans cette application d'exemple est **NON SÉCURISÉE**. Les mots de passe sont stockés et comparés en clair. Cette approche est utilisée uniquement à des fins de démonstration et ne doit **JAMAIS** être utilisée dans un environnement de production. Pour une application réelle, implémentez un hachage de mot de passe robuste (ex: bcrypt, scrypt, Argon2).

## Perspectives d'Évolution

*   Implémentation du hachage sécurisé des mots de passe.
*   Gestion avancée des rôles et permissions utilisateurs.
*   Interface Web ou Mobile complémentaire.
*   Intégration du protocole OCPP pour la communication temps réel avec les bornes.
*   Fonctionnalités de facturation et de réservation.
*   Monitoring avancé et génération de rapports graphiques.
*   Migration vers des frameworks plus modernes (Spring Boot, JavaFX).


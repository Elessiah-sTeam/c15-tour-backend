# c15-tour-backend

> **Description :**  
Ce projet intègre [OSRM](http://project-osrm.org/) dans l’environnement Docker du backend, permettant le calcul d’itinéraires (distance, durée, géométrie) localement, sans dépendre d’API externes coûteuses ni de connexion internet. En développement, une carte régionale (Pays de la Loire) est utilisée pour limiter la consommation de ressources, mais la configuration supporte des cartes de plus grande taille en production.

---

## Lancement rapide avec Docker

### Prérequis

- [Docker](https://www.docker.com/) 
- Accès à un terminal Unix (Linux/macOS/WSL recommandé)
- Accès internet pour le téléchargement initial de la carte routière

### 1. Préparer les données cartographiques OSRM

1. **Créer le dossier de données** :

    ```bash
    mkdir -p c15-tour-backend/osrm-data
    ```

2. **Télécharger la carte OSM — Pays de la Loire**  
   (vous pouvez choisir une autre région ou un fichier plus large pour la production, mais attention à la consommation mémoire !)

    ```bash
    curl -L https://download.geofabrik.de/europe/france/pays-de-la-loire-latest.osm.pbf -o c15-tour-backend/osrm-data/pays-de-la-loire.osm.pbf
    ```

3. **Prétraiter la carte (extraction & contraction) :**  
   Depuis le dossier racine du projet :

    ```bash
    cd c15-tour-backend
    docker run -t -v "${PWD}/osrm-data:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/pays-de-la-loire.osm.pbf
    docker run -t -v "${PWD}/osrm-data:/data" osrm/osrm-backend osrm-contract /data/pays-de-la-loire.osrm
    ```

> **Note :** Ces opérations génèrent les fichiers nécessaires dans `osrm-data/`.  
> Le dossier `osrm-data/` est ignoré par git (_voir_ `.gitignore`).

---

### 2. Lancer tous les services avec Docker Compose

```bash
docker-compose up -d
```

- Le backend et le service OSRM seront lancés ensemble, interconnectés via le réseau Docker.

---

### 3. Vérifications

- Pour tester que OSRM fonctionne :

    [http://localhost:5001/route/v1/driving/-1.5536,47.2184;-0.5518,47.4711?overview=false](http://localhost:5001/route/v1/driving/-1.5536,47.2184;-0.5518,47.4711?overview=false)

    - Vous devez obtenir une réponse JSON contenant la distance et la durée.

---

## Détails Techniques

- **Ports utilisés :**  
  - OSRM exposé sur `localhost:5001` (5000 en interne)
    - Le port 5001 est choisi pour éviter les conflits, notamment avec AirPlay sur macOS.
- **Volumes/Données :**  
  - Les fichiers OSRM sont stockés dans `./osrm-data` (persistant, ignoré par git).
- **Variables d’environnement :**  
  - `OSRM_API_URL` est injecté au service backend pour communiquer avec OSRM.
- **Ressources nécessaires :**
  - Carte régionale (~400Mo de RAM).
  - Carte France entière (~6-8Go de RAM).

---

## Fichiers importants

- **`docker-compose.yml`**
    - Définit deux services :
        - **backend** (votre API principale)
        - **osrm-backend** (service de routage)
    - Monte `./osrm-data` comme volume partagé.
    - Expose OSRM sur le port 5001 (host) / 5000 (container).
    - Gère les variables d’environnement.

- **`.gitignore`**
    - `osrm-data/` y est listé pour éviter de committer les fichiers volumineux.

---

## Exemples de commandes utilisées

```bash
# Création du dossier de données
mkdir -p osrm-data

# Téléchargement de la carte
curl -L https://download.geofabrik.de/europe/france/pays-de-la-loire-latest.osm.pbf -o osrm-data/pays-de-la-loire.osm.pbf

# Extraction et contraction (depuis la racine du projet)
docker run -t -v "${PWD}/osrm-data:/data" osrm/osrm-backend osrm-extract -p /opt/car.lua /data/pays-de-la-loire.osm.pbf
docker run -t -v "${PWD}/osrm-data:/data" osrm/osrm-backend osrm-contract /data/pays-de-la-loire.osrm

# Démarrage des services
docker-compose up -d
```

---

## Conseils

- Pour passer à une carte plus grande (ex. France entière), téléchargez le fichier OSM adéquat depuis [Geofabrik](https://download.geofabrik.de/europe/france.html), placez-le dans `osrm-data/`, et relancez les étapes d’extraction/contraction.
- Attention : vérifiez la mémoire disponible sur votre machine !
- Arrêtez tous les services avec :

    ```bash
    docker-compose down
    ```


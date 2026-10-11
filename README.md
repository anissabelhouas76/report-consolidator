# Consolidation des rapports d’activité

Application autonome Java 21 / Spring Boot 3.5.15 / Apache POI 5.4.1.
Aucun accès à une base de données ni appel à un service d’IA.

## Démarrage

Avec Java 21 et Maven 3.6.3 ou plus :

    mvn package -Dmaven.test.skip=true
    mvn spring-boot:run

Dans cet espace Windows, Maven est également accessible avec :

    ..\nouveau-projet\maven.ps1 -MavenArgs @('-Dmaven.test.skip=true','package')
    ..\nouveau-projet\maven.ps1 -MavenArgs @('spring-boot:run')

Ouvrir http://localhost:8081. Le port est configurable avec SERVER_PORT.
Après compilation : java -jar target/report-consolidator.war

## Tableau type du rapport Word

Le document source doit contenir un tableau avec les colonnes suivantes :

| Application | Travaux effectués |
|-------------|-------------------|
|             |                   |
|             |                   |
|             |                   |

## Utilisation

Sélectionner ou déposer plusieurs fichiers .docx, puis cliquer sur « Consolider les rapports ».
Un unique Rapport_Consolide.docx est téléchargé. Les anciens .doc doivent être convertis dans Word.
Les erreurs bloquantes invalident le lot entier : aucun fichier n’est ignoré silencieusement.
La période manquante et les lignes contenant des travaux sans application sont signalées dans les avertissements du rapport.

## Règles

Le nom de l’application est la seule clé. Les noms des auteurs et des fichiers n’interviennent jamais dans le regroupement.
Les espaces et la casse des applications sont normalisés ; la première graphie propre est conservée à l’affichage.
Les accents sont conservés : aucun rapprochement approximatif.
Les paragraphes et sauts de ligne deviennent des travaux ; les puces simples sont retirées.
Les phrases d’un même paragraphe ne sont pas découpées automatiquement.
Seuls les travaux identiques après nettoyage des espaces sont dédupliqués (casse et ponctuation conservées).
Les applications et travaux gardent leur ordre de première apparition.
Tous les tableaux sont inspectés, y compris les tableaux imbriqués ; les colonnes peuvent être inversées.
Les cellules d’application vides ne prolongent la ligne précédente que si Word indique une fusion verticale.
Les périodes détectées sont présentées dans leur ordre d’import, sans déduire un trimestre civil :
août–octobre chevauche deux trimestres.

## Limites

10 Mo par fichier, 100 Mo par requête, 100 fichiers ; limites configurées dans application.properties.
Sur Tomcat externe, les limites de parties multipart doivent être réglées sur le serveur pour accepter les lots attendus.
Les protections ZIP de POI restent actives : au plus 1 000 entrées et 20 Mo par entrée décompressée.
Les documents sont traités en mémoire, sans stockage persistant ni exécution de contenu embarqué.
Pour un serveur public, prévoir des contrôles d’accès et des limites de concurrence adaptés.

## Organisation

controller : HTTP et erreurs JSON.
service : extraction, normalisation, consolidation, génération et orchestration.
model : objets métier immuables.
config : limites des archives Word.
resources : interface française autonome, sans CDN.
test : fixtures Word créées en mémoire, tests métier et scénario HTTP à 9 et 30 fichiers.

## Déploiement sur Tomcat externe

Le projet produit un WAR : target/report-consolidator.war.
Le serveur doit utiliser Java 21 et Tomcat 10.1 (API Servlet Jakarta).

Compilation sans compilation ni exécution des tests :

    mvn package -Dmaven.test.skip=true

Dans Tomcat Manager, ouvrir /manager/html, sélectionner report-consolidator.war
dans la zone de déploiement de fichier WAR, puis cliquer sur Déployer.
L’application sera accessible à http://ADRESSE_DU_SERVEUR:8484/report-consolidator/.
Le port est celui du serveur Tomcat externe ; server.port=8081 ne le modifie pas.

Les liens CSS, JavaScript et l’envoi des fichiers tiennent compte du chemin de déploiement.
Les paramètres server.tomcat.* concernent le Tomcat embarqué.
Pour Tomcat externe, configurer aussi les limites multipart de son connecteur
afin d’autoriser les lots attendus (jusqu’à 100 fichiers et 100 Mo par requête),
ainsi que les limites éventuelles du proxy placé devant le serveur.

Le WAR peut également être lancé directement :

    java -jar target/report-consolidator.war

## Version distincte Spring Boot 3.5 et Java 21

Cette copie est indépendante du dossier report-consolidator destiné à Tomcat 9 et Java 17.
Le package Java reste dz.ab.reportconsolidator.
Les commentaires JavaScript, le tableau vierge du README et les chemins adaptés au déploiement sont conservés.

Configuration de cette version :
- Spring Boot 3.5.15
- Java 21, compilation avec --release 21
- Apache POI 5.4.1
- WAR target/report-consolidator.war
- Déploiement externe : Tomcat 10.1 utilisant Java 21

Ce WAR ne doit pas être déployé sur le serveur Tomcat 9 / Java 17 existant.
Il peut aussi être démarré directement avec Java 21 :

    java -jar target/report-consolidator.war

La génération utilise -Dmaven.test.skip=true : les tests ne sont ni compilés ni exécutés.

// Récupérer les éléments HTML grâce à leur identifiant (#...).
// Ces variables permettent de lire ou modifier le formulaire et ses messages.
const form = document.querySelector('#upload-form');
const input = document.querySelector('#files');
const dropzone = document.querySelector('#dropzone');
const submit = document.querySelector('#submit');
const error = document.querySelector('#error');
const status = document.querySelector('#status');
const download = document.querySelector('#download');
const list = document.querySelector('#file-list');
// Liste des fichiers choisis par l’utilisateur, même après plusieurs sélections.
let selected = [];
// Vaut true pendant le traitement : cela empêche plusieurs envois simultanés.
let busy = false;
// Adresse temporaire du dernier Word reçu, utilisée pour son téléchargement.
let outputUrl;
// Afficher un message d’erreur dans la page.
function fail(message) { error.textContent = message; error.hidden = false; }
// Mettre à jour l’affichage à partir des fichiers sélectionnés et de l’état busy.
function render() {
    // Vider la liste affichée avant de la reconstruire, pour éviter les lignes en double.
    list.replaceChildren();
    // Créer une ligne pour chaque fichier : son nom, sa taille et un bouton Retirer.
    selected.forEach((file, index) => {
        const row = document.createElement('li');
        const name = document.createElement('span');
        // Convertir les octets en Mo et afficher deux chiffres après la virgule.
        // textContent affiche le nom comme du texte, sans l’interpréter comme du HTML.
        name.textContent = file.name + ' (' + (file.size / 1024 / 1024).toFixed(2) + ' Mo)';
        const remove = document.createElement('button');
        // Le bouton Retirer ne doit pas envoyer le formulaire : son type est button.
        remove.type = 'button'; remove.className = 'secondary'; remove.textContent = 'Retirer';
        // Donner au bouton un nom explicite pour les lecteurs d’écran.
        remove.setAttribute('aria-label', 'Retirer ' + file.name);
        remove.disabled = busy;
        // splice(index, 1) enlève ce fichier de la sélection ; render() actualise la page.
        remove.onclick = () => { selected.splice(index, 1); render(); };
        row.append(name, remove); list.append(row);
    });
    // Afficher le nombre de fichiers sélectionnés.
    document.querySelector('#file-count').textContent = selected.length ? selected.length + ' fichier(s) sélectionné(s)' : 'Aucun fichier sélectionné';
    // Bloquer l’envoi pendant le traitement ou lorsqu’aucun fichier n’est sélectionné.
    submit.disabled = busy || selected.length === 0;
    // Empêcher de modifier la sélection pendant le traitement.
    input.disabled = busy;
    // Désactiver Tout retirer pendant le traitement ou lorsque la liste est vide.
    document.querySelector('#clear').disabled = busy || !selected.length;
}
// Vérifier les nouveaux fichiers avant de les ajouter à ceux déjà sélectionnés.
// Le serveur Java effectue aussi ses propres contrôles : ces vérifications aident l’utilisateur.
function add(files) {
    // Ne pas ajouter de fichiers tant qu’un traitement est en cours.
    if (busy) return;
    // Transformer la liste de fichiers du navigateur en un tableau JavaScript.
    const incoming = Array.from(files);
    // Masquer le précédent message d’erreur.
    error.hidden = true;
    // Vérifier chaque nouveau fichier. En cas d’erreur, aucun de ces nouveaux fichiers n’est ajouté.
    for (const file of incoming) {
        // Vérifier que le nom se termine par .docx ; le i accepte aussi .DOCX.
        // Cela ne vérifie pas le contenu Word : cette vérification est faite en Java.
        if (!/\.docx$/i.test(file.name)) { fail('Le fichier ' + file.name + ' doit être converti au format .docx.'); return; }
        // Refuser les fichiers vides ou dont la taille dépasse 10 Mo.
        if (!file.size || file.size > 10 * 1024 * 1024) { fail('Le fichier ' + file.name + ' est vide ou dépasse 10 Mo.'); return; }
    }
    // Préparer la sélection complète : anciens fichiers + nouveaux fichiers.
    const candidate = [...selected, ...incoming];
    // Limiter la sélection à 100 fichiers et 100 Mo au total.
    // reduce() additionne les tailles en octets de tous les fichiers.
    if (candidate.length > 100 || candidate.reduce((size, file) => size + file.size, 0) > 100 * 1024 * 1024) {
        fail('Limite dépassée : 100 fichiers et 100 Mo au total.'); return;
    }
    // Enregistrer la sélection validée puis mettre à jour son affichage.
    selected = candidate; render();
}
// Quand l’utilisateur choisit des fichiers, les ajouter à la sélection.
// Vider ensuite le champ permet de sélectionner à nouveau le même fichier.
input.addEventListener('change', () => { add(input.files); input.value = ''; });
// Tout retirer vide la sélection et actualise l’affichage.
document.querySelector('#clear').onclick = () => { selected = []; render(); };
// Quand des fichiers survolent la zone, empêcher le comportement habituel du navigateur
// et ajouter la classe CSS drag pour mettre la zone en évidence.
['dragenter', 'dragover'].forEach(event => dropzone.addEventListener(event, e => { e.preventDefault(); if (!busy) dropzone.classList.add('drag'); }));
// Retirer la mise en évidence lorsque les fichiers quittent la zone ou sont déposés.
['dragleave', 'drop'].forEach(event => dropzone.addEventListener(event, e => { e.preventDefault(); dropzone.classList.remove('drag'); }));
// Récupérer les fichiers déposés avec la souris et leur appliquer les mêmes vérifications.
dropzone.addEventListener('drop', e => add(e.dataTransfer.files));
// Au clic sur Consolider les rapports, envoyer les fichiers au serveur Java.
// async permet d’utiliser await pour attendre la réponse sans bloquer l’interface.
form.addEventListener('submit', async e => {
    // Empêcher l’envoi classique du formulaire, qui rechargerait la page.
    e.preventDefault();
    // Ignorer l’envoi si un traitement est déjà en cours ou si la sélection est vide.
    if (busy || !selected.length) return;
    // Passer en mode traitement, bloquer les boutons et masquer les anciens résultats.
    busy = true; render(); error.hidden = true; download.hidden = true;
    // Libérer l’ancienne adresse temporaire du Word pour ne pas conserver cette ressource en mémoire.
    if (outputUrl) { URL.revokeObjectURL(outputUrl); outputUrl = undefined; }
    // Informer l’utilisateur que le traitement a commencé.
    status.textContent = 'Traitement en cours…'; submit.textContent = 'Consolidation en cours…';
    // Informer aussi les lecteurs d’écran que le formulaire est occupé.
    form.setAttribute('aria-busy', 'true');
    // Essayer l’envoi et le téléchargement. En cas d’échec, le bloc catch affiche l’erreur.
    try {
        // Construire un formulaire multipart contenant tous les fichiers sous le nom files.
        // Ce nom correspond au paramètre MultipartFile[] files du contrôleur Java.
        const data = new FormData(); selected.forEach(file => data.append('files', file));
        // Utiliser l’adresse du formulaire : elle inclut le chemin de l’application dans Tomcat.
        // Le Java lit les Word, regroupe les travaux par application et génère le document final.

        const response = await fetch(form.action, { method: 'POST', body: data });
        // Si le serveur retourne un statut HTTP d’erreur, récupérer son message.
        if (!response.ok) {
            // Lire l’erreur JSON. Si la réponse n’est pas du JSON, utiliser un objet vide.
            const body = await response.json().catch(() => ({}));
            // Transmettre le message au bloc catch ; utiliser un message général si nécessaire.
            throw new Error(body.message || 'La consolidation a échoué. Vérifiez la taille et le format des fichiers.');
        }
        // Lire le Word reçu sous forme de Blob (données du fichier), puis créer une adresse
        // temporaire pour permettre au navigateur de le télécharger.
        outputUrl = URL.createObjectURL(await response.blob());
        // Préparer le lien, donner un nom au fichier et déclencher le téléchargement.
        // Le lien reste visible pour permettre un nouveau téléchargement.
        download.href = outputUrl; download.download = 'Rapport_Consolide.docx'; download.hidden = false; download.click();
        // Afficher la confirmation de génération du document.
        status.textContent = 'Le rapport consolidé a été généré. Consultez également les éventuels avertissements à la fin du document.';
        // Afficher les erreurs du serveur ou de connexion dans la page.
    } catch (err) { status.textContent = ''; fail(err.message); }
    // Toujours quitter le mode traitement, que l’opération ait réussi ou échoué.
    finally { busy = false; render(); submit.textContent = 'Consolider les rapports'; form.removeAttribute('aria-busy'); }
});
// Au chargement de la page, afficher la sélection vide et désactiver le bouton d’envoi.
render();
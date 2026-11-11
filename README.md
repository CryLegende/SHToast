<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHToast

*/shtoast : envoie une notification de type progrès à un joueur ou à tout le serveur.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale%20%C2%B7%20Lobby-3aa655?style=for-the-badge" alt="SkyHand Opale · Lobby">

</div>

---

## Présentation

/shtoast : envoie une notification de type progrès à un joueur ou à tout le serveur.

Utilisé sur : **Opale · Lobby**.

Avec **SHNetwork** (facultatif), le toast d'un joueur connecté sur un autre serveur du réseau lui est envoyé là où il se trouve (commande exécutée par la console de son serveur, une seule fois), et la cible `global` atteint les joueurs de tous les serveurs. Sans SHNetwork, ou avec `reseau.actif: false`, seuls les joueurs du serveur reçoivent le toast.

## Dépendances

- Obligatoires : `UltimateAdvancementAPI`
- Facultatives : `SHNetwork`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/shtoast <item> <type> <joueur\|global> <message>` | — | Envoie un toast à un joueur ou à tous (`TASK`, `GOAL`, `CHALLENGE`). | `shtoast.use` |
| `/shtoast reload` | — | Relit `config.yml` et `messages.yml`. | `shtoast.admin.reload` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shtoast.use` | Envoyer un toast avec `/shtoast`. | `op` |
| `shtoast.admin` | Toutes les sous-commandes d'administration. | `op` |
| `shtoast.admin.reload` | `/shtoast reload`. | `op` |

## Configuration

Fichiers par défaut :

<pre>
plugins/SHToast/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

La clé `reseau.actif` (par défaut `true`) active l'envoi vers les autres serveurs quand SHNetwork est présent. Elle se lit au démarrage : un changement demande un redémarrage.

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>

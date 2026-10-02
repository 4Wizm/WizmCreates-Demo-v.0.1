# WizmCrates

Plugin crate leggero e potente per Paper 1.21.

## Features
- Crate fisiche o virtuali (key item o chiavi nel database)
- Animazioni di apertura con particelle e suoni
- Hologram privati per ogni giocatore (con PlaceholderAPI)
- GUI di preview e di edit
- Ricompense garantite ogni 25 aperture
- Timed rewards (chiavi automatiche ogni tot tempo)
- Supporto MySQL (HikariCP) per chiavi e statistiche
- Placeholder per chiavi, aperture, timer e leaderboard

## Comandi
| Comando | Descrizione |
|---|---|
| `/crate reload` | Ricarica config e crate |
| `/crate key <player\|all> <crate> <amount>` | Dai chiavi |
| `/crate setblock <crate>` | Imposta una crate sul blocco che guardi |
| `/crate removeblock` | Rimuovi la crate dal blocco che guardi |
| `/crate edit <crate>` | Apri la GUI di modifica |

Alias: `/crates`, `/wizmcrates`

## Permessi
- `wizmcrates.command.reload`
- `wizmcrates.command.key`
- `wizmcrates.command.setblock`
- `wizmcrates.command.removeblock`
- `wizmcrates.command.edit`

## Placeholder
- `%wizmcrates_keys_<crate>%` — chiavi possedute
- `%wizmcrates_opened_<crate>%` — aperture totali
- `%wizmcrates_timer_<id>%` — countdown timed reward
- `%wizmcrates_leaderboard_<crate>_<page>_<row>%` — classifica

## Come si usa
1. Metti il jar in `plugins/`
2. Configura `config.yml` (crate, animazioni, rewards)
3. Avvia il server
4. Usa `/crate setblock <crate>` guardando un blocco
5. Apri con tasto destro, preview con tasto sinistro

## Requisiti
- Paper 1.21+
- Java 21
- PlaceholderAPI
- MySQL (opzionale, per chiavi virtuali e statistiche)

## Autore
Wizm — versione `beta-0.0.1`

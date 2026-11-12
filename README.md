<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="assets/logo-dark.png">
    <img src="assets/logo-light.png" alt="Blood" width="420">
  </picture>
</p>

<p align="center">
  An AI-powered anti-cheat for Minecraft servers.<br>
  Neural networks detect aim assist, killaura and triggerbot.
</p>

<p align="center">
  <a href="https://bloodac.space">Website</a> ·
  <a href="https://t.me/bloodanticheat">Telegram</a>
</p>

<p align="center">
  <img alt="Minecraft" src="https://img.shields.io/badge/Minecraft-1.16.5%20--%201.21.x-red?style=flat">
  <img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-red?style=flat">
</p>

---

## What is BloodAC

BloodAC is a machine-learning anti-cheat for combat. The plugin watches how players fight (aim, rotations, hits) and sends compact data windows to the BloodAC server, where neural networks score them in real time. You get a cheat probability per player, and decide what happens next: alert staff, run any command, or both.

## Detections

| Check | What it catches |
| --- | --- |
| **Aim** | Aim assist, soft-aim and neural-rotation cheats |
| **Attack** | Killaura and triggerbot |

## Features

- **Live probabilities** for every player, shown only to staff
- **Suspects menu**: online players sorted by how suspicious they are, with one-click teleport
- **Player history**: last 1000 predictions per player, color-coded
- **Holograms** over suspects with recent scores, ping and hit/miss stats
- **Flexible punishments**: alerts or any command (ban, kick, anything you want)
- **Wide version support** (1.16.5 – 1.21.x), no restart needed to change the key or config

## Requirements

- Paper, Spigot or a fork of them (Purpur, Pufferfish, ...)
- Minecraft 1.16.5 – 1.21.x
- [PacketEvents](https://github.com/retrooper/packetevents) installed on the server
- A BloodAC license key from [bloodac.space](https://bloodac.space)

## Installation

1. Download PacketEvents and put it into `plugins/`.
2. Put the BloodAC jar into `plugins/` and start the server once.
3. Open `plugins/BloodAC/config.yml` and paste your license key.
4. Run `/bloodac reload`.

Set your punishment command and suspect rules in the same `config.yml`.

## Commands

All commands start with `/bloodac`.

| Command | Description |
| --- | --- |
| `monitor on/off` | Show all AI percentages for every player |
| `monitor <player> on/off` | Show them for one player |
| `prob on/off` | Show percentage and buffer above your hotbar |
| `predictHologram on/off` | Holograms over suspects (up to 20 blocks) |
| `suspectsMenu` | Menu of online suspects, sorted by suspicion. LMB: teleport, RMB: details |
| `suspectsMenu <player>` | Full info and prediction history for any player |
| `canPunish on/off` | Enable or disable automatic punishments |
| `acPunish <player>` | Punish a player right now, ignores `canPunish` |
| `reload` | Reload the config (no restart needed) |

## Punishments

When a player crosses your threshold, BloodAC runs the command you set in `config.yml`, so it works with any ban or punishment plugin. Use `canPunish` to switch punishments off while you test, and `/bloodac acPunish` to punish manually.

## Privacy

To score players, the plugin sends combat data (rotations and attack info) to the BloodAC server. Inference runs there, not on your server, so it does not load your CPU.

## Support

- Website: [bloodac.space](https://bloodac.space)
- Telegram: [t.me/bloodanticheat](https://t.me/bloodanticheat)

## License

BloodAC is distributed under the [GNU General Public License v3.0](LICENSE).

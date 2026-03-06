# Colosseum Bee Sounds

##### A plugin for [RuneLite](https://runelite.net/)

Plays bee buzzing sounds in the Colosseum when the **Bees** modifier is active. The sound intensity increases as the bee swarm gets closer to your player by layering additional bee sound instances on top of each other.

---

## How It Works

When a **Bee Swarm** NPC spawns in the Colosseum, the plugin begins playing looping bee buzz sounds. As the swarm moves closer to your character, more sound layers are added to create a more intense buzzing effect:

| Distance from Player | Sound Layers | Effect |
|---|---|---|
| 10+ tiles | 1 | Faint single buzz |
| 5–9 tiles | 2 | Moderate buzz |
| 1–4 tiles | 3 | Loud buzz |
| 0 tiles | 4 | Intense swarm |

Each layer uses a different `.wav` file (`Bee_r1.wav` through `Bee_r4.wav`) to create natural variation. All sounds stop automatically when the bee swarm despawns, or when you leave the Colosseum / log out.

---

## Configuration

| Setting | Description | Default |
|---|---|---|
| **Colosseum Bees** | Enable or disable bee swarm sounds | On |
| **Volume** | Adjust bee sound volume (0–200) | 100 |

---

## Sound Files

Sound files are automatically downloaded from this repository's `sounds` branch on plugin startup and stored locally at:

```
~/.runelite/colosseum-bees/bees/
```

The plugin only downloads files that are missing — it will not re-download files you already have.

### Customizing Sounds

You can replace the bee sound files with your own `.wav` files:

1. Navigate to `~/.runelite/colosseum-bees/bees/`
2. Replace any of `Bee_r1.wav`, `Bee_r2.wav`, `Bee_r3.wav`, or `Bee_r4.wav` with your custom `.wav` files (must be actual `.wav` format, not renamed `.mp3`)
3. Restart the plugin to load the new sounds

To reset to defaults, delete the `colosseum-bees` folder and restart the plugin — the default sounds will be re-downloaded.

---

## Known Issues

- **PulseAudio on Linux** can refuse to accept the audio formats used despite claiming to accept them.

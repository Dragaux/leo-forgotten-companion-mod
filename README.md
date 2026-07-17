# Leo: The Forgotten Companion

Fabric mod for Minecraft 1.20.1. Drop it in any survival world — nothing to configure, no command blocks. The timeline runs off the world's own tick loop and saves its progress into the world file automatically.

## Timeline

| Day | Event |
|---|---|
| 1-3 | Normal. Mod only watches for a wolf named "Leo." |
| 3+ | If no Leo exists yet, a mystery wolf named Leo appears — already tamed, already collared. |
| 4 | First sign: a distant wolf bark at night with no source. |
| 7 | The Stranger: a phantom "Leo?" wolf appears, invulnerable, vanishes if unobserved for ~2 seconds. |
| 10-14 | The Corruption: random signs, chest item swaps ("Leo's Favorite"), and animals vanishing with particle trails. |
| 15+ | The real Leo gains warning bark (hostiles nearby), shadow detection (Lost Dog nearby), and protection mode (buffs when owner is low HP). |
| 20 | The Last Howl: the Lost Dog returns, now fightable. Kill it or feed it a bone. |
| post-ending | Guardian mode (bark alerts), treasure hunt (rare bone drops), ambient patrol. |

## Build / run

I can't compile this here — my sandbox doesn't have network access to Fabric's Maven repo. To build it yourself:

1. Unzip the project.
2. Open the folder in IntelliJ IDEA with JDK 17 installed.
3. Let Gradle sync (pulls Loom, Yarn mappings, Fabric API automatically).
4. Run the `runClient` Gradle task to test.

If Gradle sync throws a version-resolution error, the pinned versions in `gradle.properties` (Yarn build, Loader, Fabric API) may have been superseded since I wrote this — check https://fabricmc.net/develop/ for current numbers and swap them in.

## Everything is embedded — no separate resource pack to install

All textures and sounds live inside the mod jar under `assets/leo_forgotten_companion/`, so they load automatically the moment the mod is active. Nothing to drag into a resource pack folder.

- **`textures/entity/lost_dog/lost_dog.png`** — a custom 64x32 corrupted-wolf texture (dark gray/purple with red eye pixels and static streaks). It's an original placeholder I generated procedurally, not extracted from any game asset. It'll apply correctly via the custom `LostDogEntityRenderer`, but it's not hand-painted pixel art — swap this file for real art whenever you (or someone on Jayvera Studios) want the polished version. Same UV layout as the vanilla wolf, so any properly-painted 64x32 wolf-skin replacement will drop right in.
- **`sounds/leo/distant_bark.ogg`, `sounds/leo/lost_dog_howl.ogg`, `sounds/ambient/corruption_drone.ogg`** — synthesized placeholder audio (procedural tones/noise via Python + ffmpeg, not recorded from any source), registered as real custom `SoundEvent`s in `ModSounds.java` and wired into Day 4's bark, the corruption phase's ambient hum, and the Day 20 howl. They're genuinely playable Ogg Vorbis files, just not professionally produced — same swap-in-place approach: replace the `.ogg` files with better ones and keep the filenames/`sounds.json` entries.

## Known simplifications (things to check/tune once it's running)

- **Day 15 abilities and endgame guardian mode scan every loaded wolf named "Leo" across a huge bounding box** — fine for a single-player world, but wasteful on a big multiplayer server. Worth narrowing to per-player search radius if you scale this up.
- **The sign text API (`SignBlockEntity.changeText`) is exact for 1.20.1 but sign/text handling has changed across versions before and after** — if it doesn't compile, check the method signature IntelliJ suggests and adjust.
- **`LostDogEntityRenderer.getTexture(WolfEntity)`** overrides the real superclass method — if Yarn mappings for your exact build number use a slightly different method name, IntelliJ's "implement/override" autocomplete will show you the correct one; swap it in.
- **Day 20 is a flat day count**, not gated on corruption events actually having fired — if you want it to only trigger after the corruption phase produced at least N events, that's a small change to `LeoEvents`.

## What I'd build next if you want it

- Hand-painted Lost Dog texture (I can guide you through pixel placement against the UV map, or you commission/paint one and drop it in)
- Better-produced audio (the current set is intentionally simple synthesis, easy to tell apart from a real recording)
- Actual advancement/objective tracking (currently just chat messages)
- Multiplayer-safe per-player state instead of one shared world timeline

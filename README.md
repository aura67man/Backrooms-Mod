# Backrooms: Thresholds

An original liminal-spaces mod for **Minecraft Java Edition 26.3** on **Fabric**.
Three procedurally generated dimensions, four ways in, a way back from each, and two things that live there.

- Jar: `dist/backrooms-mod-1.0.0.jar` (the playtested build, committed so it survives), or build your own with `./gradlew build` into `build/libs/`
- Needs: Minecraft 26.3, Fabric Loader 0.20.1+, Fabric API 0.166.2+26.3, Java 25
- Install: drop the jar and Fabric API into `mods/`. Works in singleplayer and on dedicated servers (install on both sides).

## Why 26.3 and Fabric

1. 26.3 is the current stable release in Mojang's launcher metadata (checked 2026-11-14), and Fabric had a stable Loader, Fabric API and Loom for it, building straight against Mojang's own names (26.x ships unobfuscated, so there are no third-party mappings to drift).
2. In 26.3 dimensions, fog, sky, ambient sound and mob spawns are vanilla data; the parts that need code (chunk generator, entities, renderers, biome injection) are each covered by one small Fabric API module, so the only dependency is Fabric API.
3. Fabric's client game test API can drive a real client and a real dedicated server headlessly, which is how every claim below was playtested. (NeoForge 26.3 also exists and would have worked; this was the deciding factor.)

## The levels

| Dimension | What it is | Light, fog, sky, sound |
|---|---|---|
| `backrooms:level_0` | Endless damp-carpet office rooms on an 8-block grid: yellow wallpaper, skirting boards, doorways, stray pillars, open halls that tighten into mazes. About half the cells have a fluorescent panel and some of those flicker on and off at random. | Dim yellow ambient light, thick yellow fog, no sky, a mains-hum loop with buzzing tubes. |
| `backrooms:poolrooms` | White-tiled halls joined by wide arches. The floor is a patchwork of dry walkway, ankle-deep water and three-deep pools with lights on the bottom; water runs under the arches. | Bright, pale-blue haze, square skylights open to a blank noon sky, lapping water and echoing drips. |
| `backrooms:carpark` (Sublevel P) | An underground car park with no cars and no ramp out: painted bays, hazard-striped pillars, concrete lift cores. Fewer than half the sodium lamps still work. | Nearly dark, orange lamp pools, close brown-black fog, no sky, a ventilation drone with distant metal ticks. |

Generation is a pure function of position (see `world/layout/`), so it is identical for every world seed and safe to generate in any order.

## Ways in (and the tell for each)

| Entrance | Goes to | Kind | The tell |
|---|---|---|---|
| **Noclip**: walk into any solid wall while looking at it and keep pushing. After 5 seconds there is a 10% chance (configurable) you fall through. | Level 0 | Random | After 1.5 s the wall hums and sheds sparks and ash where you are pushing. |
| **Falling out of the End**: drop into the void below the End islands. Instead of dying you land in a deep pool. | Poolrooms | Location | Below Y=0 the air fills with falling water and you hear the pools before you arrive. |
| **Key Fob (No Car)**: craftable. Right-click it. | Sublevel P | Reliable | The fob shimmers like an enchanted item and beeps like a car answering somewhere. |
| **The wrong stairwell**: a rare white-tiled stairwell generated on the surface of any Overworld biome (about 1 attempt per 250 chunks). Walk down it and through the dark doorway at the bottom. | Poolrooms | Structure | A lit green EXIT sign over a tiled doorway standing in the middle of nowhere. |

Key Fob recipe (shaped): iron nugget on top; iron ingot, redstone, iron ingot in the middle; ender pearl on the bottom.
To see a stairwell without hunting for one: `/place feature backrooms:wrong_stairwell` while standing on solid ground at least 10 blocks thick.

## Ways back

- **Exit doors.** Every level has free-standing door frames (lift cores in the car park) with a dark threshold and a lit EXIT sign: one per 48 to 64 blocks. Walk into the threshold and you return to the exact spot you left from. If you fell in from the End, you return to your respawn point instead.
- **The Key Fob** works in reverse from any level.
- Dying sends you to your normal respawn point. Beds and respawn anchors do not work inside.

## Things that live there

- **Fixture** (hostile, the stalker). Stands exactly like a floor lamp for as long as any player has it on screen: it cannot move, turn or be pushed. The moment nobody is looking it drags itself towards the nearest survival player at about half walking speed, and hits for 3 hearts on contact. 30 health. Spawns rarely in all three levels (never within 64 blocks of another Fixture, never on Peaceful).
- **Tube Moth** (ambient). Pale hand-sized moths that drift between ceiling lights in small groups. Scenery, almost always: swat one and it and its neighbours retaliate for half a heart a bite. 4 health.

Both are original designs with code-built models and their own sounds. Spawn eggs are in the Functional Blocks creative tab along with all the blocks.

## Config

`config/backrooms.json` is created on first run. Restart to apply changes.

| Key | Default | Meaning |
|---|---|---|
| `noclipChance` | `0.1` | 0 to 1 chance per attempt that pushing into a wall drops you into Level 0. `0` disables noclip. |
| `noclipHoldSeconds` | `5.0` | How long you must keep pushing before each roll. |
| `fixtureEnabled` | `true` | `false` stops Fixtures spawning and removes existing ones. |
| `fixtureSpawnRate` | `0.25` | 0 to 1 multiplier on natural Fixture spawn attempts. |
| `tubeMothEnabled` | `true` | `false` stops Tube Moths spawning and removes existing ones. |
| `tubeMothSpawnRate` | `1.0` | 0 to 1 multiplier on natural Tube Moth spawn attempts. |

## Building and testing

```bash
python3 tools/fetch_mojang_jdk.py   # only if you have no Java 25: fetches Mojang's own runtime into ./.jdk25
```

```bash
. ./env.sh                          # points JAVA_HOME at ./.jdk25 and keeps Gradle's cache inside the project
```

```bash
./gradlew build                     # -> build/libs/backrooms-mod-1.0.0.jar
```

```bash
tools/run_playtest.sh               # scripted playthrough in a real client: singleplayer, then a dedicated server
```

```bash
python3 tools/smoke_test_server.py  # runs the built jar on a stock production Fabric server
```

The playtest lives in `src/gametest` and is not included in the jar. It uses real key presses for the noclip, stairwell, exit-door and key-fob checks, and logs a `[BRTEST] PASS ...` line per check. `tools/run_playtest.sh` runs it under Xvfb with software OpenGL; `src/gametest/.../HeadlessGlBackendMixin` exists only so the game can open a window there. Results from the last full run are in [`docs/playtest-evidence.md`](docs/playtest-evidence.md).

## Assets and credits

Every asset in this mod is generated by the scripts in `tools/`. Nothing was drawn by hand, recorded, sampled or taken from another mod, game or pack.

- **Textures** (`tools/gen_textures.py`, Python + Pillow): all block, item and entity textures and the icon, drawn procedurally from seeded noise and simple shapes.
- **Sounds** (`tools/gen_sounds.py`, Python + numpy, encoded to Ogg Vorbis by ffmpeg): all 15 sounds synthesised from sine waves and shaped noise.
- **Models**: entity models are built in code (`client/FixtureModel.java`, `client/TubeMothModel.java`); block and item model JSON is written by `tools/gen_data.py`.
- **Designs**: the Fixture and the Tube Moth are original to this mod. "The Backrooms" as a setting is a community-made concept; no named creature, level text or artwork from any existing work is used.

Code and assets were written with Claude Code. No licence has been chosen yet (`All-Rights-Reserved` in `fabric.mod.json`); pick one before publishing.

## Known limits

- The Fixture's freeze check uses a 60 degree cone around where each player is looking, so something at the very edge of a wide field of view can count as unwatched.
- The End entrance needs the End, so it is late-game by design; the stairwell and noclip cover early game.
- Level layouts ignore the world seed on purpose: the Backrooms are the same place for everyone.

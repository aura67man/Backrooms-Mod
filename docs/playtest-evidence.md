# Playtest evidence

Produced by `tools/run_playtest.sh` (a real Minecraft 26.3 client under Xvfb, driven by Fabric's client game test API) on 2026-11-15.
The same scripted playthrough runs twice: once in a singleplayer world and once with the client connected to a real dedicated server.

**Result: 113 checks passed, 0 failed.**

## Sound

243 s of audio captured from the game's OpenAL output (written by OpenAL Soft's wave backend); 197 of 243 seconds are non-silent; peak level 0.64, loudest one-second RMS 0.170.

`SOUND` lines below are logged by a listener on the client's sound engine the first time each of the mod's sound events is actually played.

## Screenshots

- [`singleplayer_noclip_tell.png`](screenshots/singleplayer_noclip_tell.png)
- [`singleplayer_level_0_arrival.png`](screenshots/singleplayer_level_0_arrival.png)
- [`singleplayer_level_0_light_flickered_off.png`](screenshots/singleplayer_level_0_light_flickered_off.png)
- [`singleplayer_level_0_exit_door.png`](screenshots/singleplayer_level_0_exit_door.png)
- [`singleplayer_wrong_stairwell.png`](screenshots/singleplayer_wrong_stairwell.png)
- [`singleplayer_poolrooms_arrival.png`](screenshots/singleplayer_poolrooms_arrival.png)
- [`singleplayer_end_void_tell.png`](screenshots/singleplayer_end_void_tell.png)
- [`singleplayer_poolrooms_fell_into_pool.png`](screenshots/singleplayer_poolrooms_fell_into_pool.png)
- [`singleplayer_key_fob_in_hand.png`](screenshots/singleplayer_key_fob_in_hand.png)
- [`singleplayer_carpark_arrival.png`](screenshots/singleplayer_carpark_arrival.png)
- [`singleplayer_carpark_exit_door.png`](screenshots/singleplayer_carpark_exit_door.png)
- [`singleplayer_fixture_watched_far.png`](screenshots/singleplayer_fixture_watched_far.png)
- [`singleplayer_fixture_watched_closer.png`](screenshots/singleplayer_fixture_watched_closer.png)
- [`singleplayer_fixture_point_blank.png`](screenshots/singleplayer_fixture_point_blank.png)
- [`singleplayer_tube_moth.png`](screenshots/singleplayer_tube_moth.png)
- [`dedicated_level_0_arrival.png`](screenshots/dedicated_level_0_arrival.png)
- [`dedicated_poolrooms_arrival.png`](screenshots/dedicated_poolrooms_arrival.png)
- [`dedicated_carpark_arrival.png`](screenshots/dedicated_carpark_arrival.png)
- [`dedicated_fixture_watched_closer.png`](screenshots/dedicated_fixture_watched_closer.png)

## Log lines

`MOD` lines are the mod's own server-side log; everything else is the test's.

```
PASS       singleplayer world created
SOUND      client played backrooms:event.noclip_hum
MOD        Player0 slipped from minecraft:overworld to backrooms:level_0 at 0, 33, 1
SOUND      client played backrooms:event.transit
PASS       singleplayer: noclip entrance (push into wall ~5s) moved the player to backrooms:level_0
SOUND      client played backrooms:ambient.level_0
SOUND      client played backrooms:block.fluorescent_light.buzz
PASS       block at 0, 32, 1 in backrooms:level_0 is Damp Carpet (found Damp Carpet)
PASS       singleplayer: Level 0 generated rooms (244 wallpaper blocks, 34 light panels within 24 blocks)
PASS       singleplayer: found a flickering light panel at -28, 37, 11
PASS       singleplayer: flickering light toggled 4 times while watched
PASS       block at -44, 33, 45 in backrooms:level_0 is Threshold (found Threshold)
PASS       block at -44, 35, 45 in backrooms:level_0 is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:level_0 to minecraft:overworld at 0, -60, 1
PASS       singleplayer: walked through the exit door in backrooms:level_0 and came back to the overworld
PASS       singleplayer: exit door returned the player to where they noclipped (0, -60, 1)
PASS       client played sound backrooms:ambient.level_0
PASS       client played sound backrooms:event.noclip_hum
PASS       client played sound backrooms:event.transit
PASS       client played sound backrooms:block.fluorescent_light.buzz
PASS       block at 20, -46, 20 in minecraft:overworld is Exit Sign (found Exit Sign)
PASS       block at 20, -55, 10 in minecraft:overworld is Threshold (found Threshold)
MOD        Player0 slipped from minecraft:overworld to backrooms:poolrooms at 16, 33, 7
PASS       singleplayer: walking down the wrong stairwell moved the player to backrooms:poolrooms
SOUND      client played backrooms:ambient.poolrooms
PASS       singleplayer: Poolrooms generated (1521 water, 880 tile, 31 ceiling lights within 24 blocks)
PASS       block at 12, 33, 60 in backrooms:poolrooms is Threshold (found Threshold)
PASS       block at 12, 35, 60 in backrooms:poolrooms is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:poolrooms to minecraft:overworld at 20, -55, 11
PASS       singleplayer: walked through the exit door in backrooms:poolrooms and came back to the overworld
PASS       singleplayer: exit door returned the player to the bottom of the stairwell (20, -55, 11)
MOD        Player0 slipped from minecraft:the_end to backrooms:poolrooms at 292, 37, 308
PASS       singleplayer: falling out of the End moved the player to backrooms:poolrooms
PASS       singleplayer: the fall ended in a deep pool with no damage (health 20.0)
PASS       block at 300, 33, 300 in backrooms:poolrooms is Threshold (found Threshold)
PASS       block at 300, 35, 300 in backrooms:poolrooms is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:poolrooms to minecraft:overworld at 0, -60, 0
PASS       singleplayer: walked through the exit door in backrooms:poolrooms and came back to the overworld
PASS       client played sound backrooms:ambient.poolrooms
PASS       singleplayer: key fob crafting recipe is loaded
MOD        Player0 slipped from minecraft:overworld to backrooms:carpark at 40, 33, -40
SOUND      client played backrooms:item.key_fob.beep
PASS       singleplayer: pressing the key fob moved the player to backrooms:carpark
SOUND      client played backrooms:ambient.carpark
PASS       singleplayer: car park generated (508 bay-line blocks, 28 pillars, 8 working lamps within 24 blocks)
MOD        Player0 slipped from backrooms:carpark to minecraft:overworld at 40, -60, -40
PASS       singleplayer: pressing the fob again returned the player to where they started (40, -60, -40)
MOD        Player0 slipped from minecraft:overworld to backrooms:carpark at 40, 33, -40
PASS       block at 12, 33, -14 in backrooms:carpark is Threshold (found Threshold)
PASS       block at 12, 35, -14 in backrooms:carpark is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:carpark to minecraft:overworld at 40, -60, -40
PASS       singleplayer: walked through the exit door in backrooms:carpark and came back to the overworld
PASS       client played sound backrooms:item.key_fob.beep
PASS       client played sound backrooms:ambient.carpark
SOUND      client played backrooms:entity.fixture.ambient
PASS       singleplayer: Fixture stayed frozen for 3s while the player looked at it (moved 0.00 blocks)
SOUND      client played backrooms:entity.fixture.step
PASS       singleplayer: Fixture crept 4.21 blocks towards the player in 2.5s while they looked away
PASS       singleplayer: Fixture froze again the moment the player looked back (moved 0.00 blocks)
SOUND      client played backrooms:entity.fixture.attack
PASS       singleplayer: Fixture reached the player and attacked (health now 14.0)
SOUND      client played backrooms:entity.fixture.hurt
SOUND      client played backrooms:entity.fixture.death
PASS       client played sound backrooms:entity.fixture.step
PASS       client played sound backrooms:entity.fixture.attack
PASS       client played sound backrooms:entity.fixture.ambient
PASS       client played sound backrooms:entity.fixture.hurt
PASS       client played sound backrooms:entity.fixture.death
SOUND      client played backrooms:entity.tube_moth.ambient
PASS       singleplayer: Tube Moth flew 11.31 blocks in 6s, up to y=36.56
PASS       singleplayer: Tube Moth ignored the player while left alone (no target, player health 20.0)
SOUND      client played backrooms:entity.tube_moth.hurt
PASS       singleplayer: Tube Moth targeted the player after being hit
PASS       singleplayer: the angry Tube Moth bit the player (health now 19.0)
SOUND      client played backrooms:entity.tube_moth.death
PASS       client played sound backrooms:entity.tube_moth.ambient
PASS       client played sound backrooms:entity.tube_moth.hurt
PASS       client played sound backrooms:entity.tube_moth.death
PASS       singleplayer: natural spawning in Level 0 produced 5 Fixture(s) and 18 Tube Moth(s)
PASS       singleplayer: config toggles removed every Fixture and Tube Moth and stopped new spawns
PASS       SINGLEPLAYER PLAYTHROUGH COMPLETE
PASS       dedicated server started and client connected
SOUND      client played backrooms:event.noclip_hum
MOD        Player0 slipped from minecraft:overworld to backrooms:level_0 at 0, 33, 1
SOUND      client played backrooms:event.transit
PASS       dedicated: noclip entrance (push into wall ~5s) moved the player to backrooms:level_0
SOUND      client played backrooms:ambient.level_0
SOUND      client played backrooms:block.fluorescent_light.buzz
PASS       block at 0, 32, 1 in backrooms:level_0 is Damp Carpet (found Damp Carpet)
PASS       dedicated: Level 0 generated rooms (244 wallpaper blocks, 34 light panels within 24 blocks)
PASS       dedicated: found a flickering light panel at -28, 37, 11
PASS       dedicated: flickering light toggled 2 times while watched
PASS       block at -44, 33, 45 in backrooms:level_0 is Threshold (found Threshold)
PASS       block at -44, 35, 45 in backrooms:level_0 is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:level_0 to minecraft:overworld at 0, -60, 1
PASS       dedicated: walked through the exit door in backrooms:level_0 and came back to the overworld
PASS       dedicated: exit door returned the player to where they noclipped (0, -60, 1)
PASS       client played sound backrooms:ambient.level_0
PASS       client played sound backrooms:event.noclip_hum
PASS       client played sound backrooms:event.transit
PASS       client played sound backrooms:block.fluorescent_light.buzz
PASS       block at 20, -46, 20 in minecraft:overworld is Exit Sign (found Exit Sign)
PASS       block at 20, -55, 10 in minecraft:overworld is Threshold (found Threshold)
MOD        Player0 slipped from minecraft:overworld to backrooms:poolrooms at 16, 33, 7
PASS       dedicated: walking down the wrong stairwell moved the player to backrooms:poolrooms
SOUND      client played backrooms:ambient.poolrooms
PASS       dedicated: Poolrooms generated (1521 water, 880 tile, 31 ceiling lights within 24 blocks)
PASS       block at 12, 33, 60 in backrooms:poolrooms is Threshold (found Threshold)
PASS       block at 12, 35, 60 in backrooms:poolrooms is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:poolrooms to minecraft:overworld at 20, -55, 11
PASS       dedicated: walked through the exit door in backrooms:poolrooms and came back to the overworld
PASS       dedicated: exit door returned the player to the bottom of the stairwell (20, -55, 11)
MOD        Player0 slipped from minecraft:the_end to backrooms:poolrooms at 292, 37, 308
PASS       dedicated: falling out of the End moved the player to backrooms:poolrooms
PASS       dedicated: the fall ended in a deep pool with no damage (health 20.0)
PASS       block at 300, 33, 300 in backrooms:poolrooms is Threshold (found Threshold)
PASS       block at 300, 35, 300 in backrooms:poolrooms is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:poolrooms to minecraft:overworld at 0, -60, 0
PASS       dedicated: walked through the exit door in backrooms:poolrooms and came back to the overworld
PASS       client played sound backrooms:ambient.poolrooms
PASS       dedicated: key fob crafting recipe is loaded
MOD        Player0 slipped from minecraft:overworld to backrooms:carpark at 40, 33, -40
SOUND      client played backrooms:item.key_fob.beep
PASS       dedicated: pressing the key fob moved the player to backrooms:carpark
SOUND      client played backrooms:ambient.carpark
PASS       dedicated: car park generated (508 bay-line blocks, 28 pillars, 8 working lamps within 24 blocks)
MOD        Player0 slipped from backrooms:carpark to minecraft:overworld at 40, -60, -40
PASS       dedicated: pressing the fob again returned the player to where they started (40, -60, -40)
MOD        Player0 slipped from minecraft:overworld to backrooms:carpark at 40, 33, -40
PASS       block at 12, 33, -14 in backrooms:carpark is Threshold (found Threshold)
PASS       block at 12, 35, -14 in backrooms:carpark is Exit Sign (found Exit Sign)
MOD        Player0 slipped from backrooms:carpark to minecraft:overworld at 40, -60, -40
PASS       dedicated: walked through the exit door in backrooms:carpark and came back to the overworld
PASS       client played sound backrooms:item.key_fob.beep
PASS       client played sound backrooms:ambient.carpark
SOUND      client played backrooms:entity.fixture.ambient
PASS       dedicated: Fixture stayed frozen for 3s while the player looked at it (moved 0.00 blocks)
SOUND      client played backrooms:entity.fixture.step
PASS       dedicated: Fixture crept 4.30 blocks towards the player in 2.5s while they looked away
PASS       dedicated: Fixture froze again the moment the player looked back (moved 0.00 blocks)
SOUND      client played backrooms:entity.fixture.attack
PASS       dedicated: Fixture reached the player and attacked (health now 14.0)
SOUND      client played backrooms:entity.fixture.hurt
SOUND      client played backrooms:entity.fixture.death
PASS       client played sound backrooms:entity.fixture.step
PASS       client played sound backrooms:entity.fixture.attack
PASS       client played sound backrooms:entity.fixture.ambient
PASS       client played sound backrooms:entity.fixture.hurt
PASS       client played sound backrooms:entity.fixture.death
SOUND      client played backrooms:entity.tube_moth.ambient
PASS       dedicated: Tube Moth flew 8.31 blocks in 6s, up to y=34.90
PASS       dedicated: Tube Moth ignored the player while left alone (no target, player health 20.0)
SOUND      client played backrooms:entity.tube_moth.hurt
PASS       dedicated: Tube Moth targeted the player after being hit
PASS       dedicated: the angry Tube Moth bit the player (health now 19.0)
SOUND      client played backrooms:entity.tube_moth.death
PASS       client played sound backrooms:entity.tube_moth.ambient
PASS       client played sound backrooms:entity.tube_moth.hurt
PASS       client played sound backrooms:entity.tube_moth.death
PASS       dedicated: natural spawning in Level 0 produced 8 Fixture(s) and 17 Tube Moth(s)
PASS       dedicated: config toggles removed every Fixture and Tube Moth and stopped new spawns
PASS       DEDICATED SERVER PLAYTHROUGH COMPLETE
PASS       ALL CHECKS COMPLETE
```

## ERROR lines in the client's latest.log

None come from the mod. They are vanilla failing to reach Mojang's online services from the offline sandbox, plus one graphics-option message from the software renderer:

```
[Download-2/ERROR] (Minecraft) Couldn't connect to realms
[Download-2/ERROR] (Minecraft) Failed to fetch Realms feature flags
[Download-2/ERROR] (Minecraft) Failed to fetch user properties
[Download-3/ERROR] (Minecraft) Couldn't connect to realms
[Render thread/ERROR] (Minecraft) Illegal option value 0 for Anisotropic Filtering
```
## Production server smoke test

`tools/smoke_test_server.py` ran the built jar (`build/libs/backrooms-mod-1.0.0.jar`, 389153 bytes, sha256 `d16a10412e9c6c72...`) on a stock Fabric 0.20.1 dedicated server for 26.3 with only Fabric API alongside it.

**156 log lines, 0 ERROR/FATAL lines.** The only warnings are vanilla's offline-mode notices and failed calls to Mojang's online services:

```
[Minecraft Services Discovery/WARN]: Failed to connect
[Minecraft Services Discovery/WARN]: Services are unavailable, unable to fetch uri for AUTHENTICATION/getPublicKeys
[Server thread/WARN]: **** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!
[Server thread/WARN]: The server will make no attempt to authenticate usernames. Beware.
[Server thread/WARN]: To change this, set "online-mode" to "true" in the server.properties file.
[Server thread/WARN]: While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any
```

Relevant lines:

```
[main/INFO]: Backrooms: Thresholds initialised
[Server thread/INFO]: Done (2.520s)! For help, type "help"
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:poolrooms
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:level_0
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:carpark
[Server thread/INFO]: System chat: Marked 16 chunks in backrooms:level_0 from [-2, -2] to [1, 1] to be force loaded
[Server thread/INFO]: System chat: Test passed
[Server thread/INFO]: System chat: Summoned new Fixture
[Server thread/INFO]: System chat: Summoned new Tube Moth
[Server thread/INFO]: System chat: Marked 16 chunks in backrooms:poolrooms from [-2, -2] to [1, 1] to be force loaded
[Server thread/INFO]: System chat: Test passed
[Server thread/INFO]: System chat: Summoned new Fixture
[Server thread/INFO]: System chat: Summoned new Tube Moth
[Server thread/INFO]: System chat: Marked 16 chunks in backrooms:carpark from [-2, -2] to [1, 1] to be force loaded
[Server thread/INFO]: System chat: Test passed
[Server thread/INFO]: System chat: Summoned new Fixture
[Server thread/INFO]: System chat: Summoned new Tube Moth
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:poolrooms
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:level_0
[Server thread/INFO]: Saving chunks for level 'ServerLevel[world]'/backrooms:carpark
```

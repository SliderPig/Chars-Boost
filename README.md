# {C} Char's Boost

Makes the Powers server's main thread do less work. Server side only: players don't need it.
Works on Arclight (Forge + Bukkit) 1.20.1, and does no harm in singleplayer.

## What it does

Timed on the real power mods with `/charboost scripts`, a lot of this server's lag turned out to be needless work: a few scripts going through every entity or every ability every tick, and Palladium redoing the same searches. Nearly all the gain measured below comes from stopping that work, which changes nothing about how the game plays.

**Always on. These change nothing about how the game plays.**

| Name | What it does |
|---|---|
| `selectorIndex` | `@e[type=x]` looks only at that type. It used to look at every entity in every dimension. FSang's tick function alone has over 100 lines like that. |
| `selectorTags` | `@e[tag=x]` finds nothing straight away if nobody has that tag. `@e[type=item,nbt={Item:{id:"x"}}]` asks the dropped items directly first, instead of saving them all to NBT. |
| `powerProviders` | Every tick, for every mob and player, Palladium goes through every armour slot, Curios slot and every suit set in every mod (FSang alone adds 280). Now it only does that when what's worn or the superpower list changed, with a quick re-check to catch anything else. |
| `palladiumAbilities` | Each ability built a new little map up to twice a tick just to check its conditions, and now keeps one. Checks like "has this entity got an intangibility ability on?", which run for every block a player touches, go straight to the answer. Scripted abilities that do nothing while off aren't called while off (234 of 267, the list comes from reading the scripts). Read-only scripted conditions aren't run twice for one entity in one tick, and two that loop a lot (`fsang:block_search`, `fsang:inventory_item_check`) run in Java. |
| `palladiumProperties` | `palladium.getProperty` and `setProperty` went through all of an entity's hundreds of properties one by one for each lookup. Now it's a direct lookup. Every new entity (even a dropped item or an arrow) ran every script's `registerProperties`. Now the first of each type does and the others copy the answer. Property packets are held to the end of the tick, so "set to 0, count back up to 3" sends one packet, or none if nothing changed. |
| `nativeScripts` | The worst script handlers run in Java with the same result: Echoes' `property.js` (every tick, sets 7 properties on every living entity on the server), FSang's `elytra_flight.js` (every tick, for each player, goes through all their abilities in script), FSang's `properties.js:1307` (every 4 ticks, asks every entity about a property only players have). |
| `asyncSaves` | Player files, advancements, stats and world data are compressed and written on another core, so autosaves don't freeze the game. |

Every shortcut that depends on a mod's script only goes in if that script file is byte for byte the one it was written against. If a mod updates, its own script just runs like normal.

**Only while the server is struggling.** These change timing a little, so they stay asleep until ticks go over the target
(45 ms, `targetMspt`) for 5 seconds. Then they step up one level at a time (1 to 3), and step back down once ticks stay under
35 ms for 20 seconds. A healthy server plays exactly like normal.

| Name | What it does |
|---|---|
| `mobAi` | Mobs far from every player run their goals, brains and item search less often (up to one tick in 16). They keep walking every tick, and brain memories keep counting down in real time, so villager farms keep working. |
| `mobPowers` | Far mobs' Palladium powers tick less often. Echoes puts two powers on every mob there is. |
| `mobPush` | Far mobs look for things to push less often. |
| `pathLimits` | Far mobs search shorter paths. A mob that found no way somewhere waits a moment before searching for the same spot. |
| `hoppers` | An idle hopper rests a few ticks between looks, 8 at the very most. That's a hopper's own pause between items, so working hoppers keep full speed. (spigot.yml's hopper settings do nothing on Arclight for this.) |
| `despawnChecks` | Mobs ask "should I despawn?" less often, with the chance raised to match. |
| `afkPlayers` | A player who hasn't moved, looked round, clicked or used a power for 60 seconds has their tick scripts and powers run less often. |
| `slowScripts` | KubeJS tick scripts on the slow list run less often, picked by reading each one: bookkeeping that catches up by itself. The worst side effect is skill XP from playtime coming in a bit slower while the server is overloaded. |
| `slowFunctions` | Datapack tick functions on the slow list run less often. Empty by default. |

**Left alone by the mob throttle:** players, mobs with a target or just hit, bosses (100+ max health or in the Forge boss tag),
tamed or named mobs, raiders, mobs being ridden or on a lead, mobs in water or lava, villagers trading, anything holding a Palladium power
(other than Echoes' `eop:effects/*`, which every mob has), Char's Powers clones and pets (tags `charpowers.clone`, `charpowers.held`, and the whole `charpowers` mod).
Add your own with the tag `charboost.exempt` or in the config. Furnaces and other block entities are never slowed.

## Commands (ops)

- `/charboost status` shows every part, whether it's on, what it skipped and what it saved, and current tick time and TPS. The "saved" figures are rough estimates: on the test below they added up to about twice the real saving. Trust the ms a tick at the top.
- `/charboost off` / `on` is the master switch. `/charboost off mobAi` / `on mobAi` switches one part. These stick after a restart.
- `/charboost level 0|1|2|3|auto` holds the pressure level by hand, or goes back to automatic.
- `/charboost scripts [count]` shows what each KubeJS script, scripted ability and scripted condition costs, by file and line. spark can't split these up.
- `/charboost functions [count]` shows what each datapack function costs.
- `/charboost mobs` shows how often mobs are thinking, why any are left alone, and which powers they hold.
- `/charboost scripts reset`, `/charboost functions reset`, `/charboost reset` clear the numbers.

## Config

`config/charboost.toml` appears after the first start. Every part has a switch. `enabled = false` is the emergency switch:
with it, no hook is put into the game on the next start at all. If it ever stops the server starting, create or edit that file
with the line `enabled = false`, or just the name of the bad part, like `mobAi = false`.

## Installing

Upload `build/libs/charboost-1.0.jar` to the server's `/mods` folder and restart. A reload isn't enough, because mods can only hook the game at start.
Nothing else is needed. Palladium and KubeJS are optional, it just skips those parts without them.
To remove it, delete the jar and restart. It saves nothing in the world.

## What runs on other cores, and what can't

Most of what lags this server has to stay on one core: scripts, commands and powers all change the world, and two threads doing that at once corrupts it.

- Moved off the main thread by this mod: saving player files, advancements, stats and world data.
- Already off the main thread without this mod: packet compression and encryption (Netty's threads), chunk file writes, chunk generation.
- Not done, on purpose: pathfinding on another thread. Pufferfish does it, but path searches read live chunks and call into each mod's mobs and blocks, which isn't safe with this many mods, and mob AI isn't the main cost here.

## Hooks and Arclight

Canary crashed this server because it patches a class Arclight patches too. Before hooking anything this mod was checked against
the Arclight jar's own mixin lists. Every hook is optional (`required: false`, `defaultRequire: 0`), so a clash turns off that one hook instead of crashing.
The log says which went in, in lines starting with `[CharBoost]`.

## Measured

All on the real Arclight 1.20.1 jar on an 8 GB PC (`-Xmx3G`), which was also doing other work. Tick time is the "Mean tick time" from `forge tps`. Lower is better, and 50 ms is the limit for a full 20 TPS. The first 3 to 4 minutes after a start are much slower and very noisy in every run, with or without the mod, so only settled readings count.

**Power mods:** Palladium, Echoes, Arrzee's, NBC Heroes, GabeUp, FSang, Powerborne, KubeJS, Power Compat and Arrzee Unlocks. 3 standing bots (Curtain fake players) with 5 big powers each, 300 mobs:

| | ms a tick (settled) |
|---|---|
| Without the mod at all | 28 to 30 |
| Mod in, switched off with `/charboost off` | 30 to 37 |
| Mod on, pressure 0 (the always-on parts) | 16 to 17 |
| Pressure 1 | 15 to 17 |
| Pressure 3 | 13 to 14 |
| Pressure 3, and all 3 bots counted as AFK | 8 |

That's about 40% less work from the always-on parts. An earlier run of the same code gave 38 to 52 ms switched off and 20 to 26 ms on. Readings bounce: later on, the run without the mod also read 34, 44 and 70 ms with nothing changing.

**3000 mobs, no power mods, 1 bot:** no measurable difference. 9 ms a tick with the mod at pressure 3, 9 ms without it. Spigot's activation range, which Arclight already has, is already doing that job. So `mobAi`, `mobPush` and `despawnChecks` barely showed up here. They're kept for crowds of mobs close to players, but don't expect much from them.

**Not measured:** the real server, real players pressing keys and using powers, or lag from real play. The bots just stand there, so this says how much idle overhead went away, not what TPS the real server will reach.

## Building

`gradlew build`. Needs these in `libs/` to compile (none are needed to run): Palladium 4.5.9, Palladium Core 1.0.1, KubeJS 2001.6.5-build.26, Rhino 2001.2.3-build.10, all from the Powers profile.

- `gradlew test`: pure rules (tuner, activation tiers, selector reading, tag counting, throttle fairness).
- `gradlew runGameTestServer -Psmoke`: real headless server checks at full pressure, in `src/smoke`. Rebuild without `-Psmoke` before installing.
- `python tools/run_arclight.py`: the real Arclight jar in a throwaway folder, fed console commands from `tools/scenarios/`.
- `python tools/make_idle_abilities.py`: re-reads the mods' scripts for the idle ability list after a power mod updates.

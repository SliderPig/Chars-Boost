# {C} Char's Boost

Makes the Powers server's main thread do less work. Server side only: players don't need it.
Works on Arclight (Forge + Bukkit) 1.20.1, and does no harm in singleplayer.

## What it does

Two kinds of optimisation.

**Always on.** These stop wasted work and change nothing about how the game plays.

| Name | What it does |
|---|---|
| `selectorIndex` | `@e[type=x]` looks only at that type. It used to look at every entity in every dimension. FSang's tick function alone has over 100 lines like that. |
| `selectorTags` | `@e[tag=x]` finds nothing straight away if nobody has that tag. |
| `powerProviders` | Every tick, for every mob and player, Palladium goes through every armour slot, Curios slot and every suit set in every mod (FSang alone adds 280). Now it only does that when what's worn or the superpower list changed, with a quick re-check to catch anything else. |
| `palladiumAbilities` | Each Palladium ability built a new little map up to twice a tick just to check its conditions. Now it keeps one. Checks like "has this entity got an intangibility ability on?", which run for every block a player touches, go straight to the answer instead of going through every ability of every power. |
| `palladiumProperties` | `palladium.getProperty` and `setProperty` used to go through all of an entity's hundreds of properties one by one for each lookup. Now it's a direct lookup. Every new entity (even a dropped item or an arrow) ran every script's `registerProperties` to get its list. Now the first of each type does and the others copy the answer. |
| `nativeScripts` | The few worst script handlers, timed with `/charboost scripts`, run in Java with the same result: Echoes' `property.js` (every tick, sets 7 properties on every living entity on the server), FSang's `elytra_flight.js` (every tick, for each player, goes through all their abilities in script), FSang's `properties.js:1307` (every 4 ticks, asks every entity about a property only players have). A replacement only goes in if the script file is byte for byte the one it was written against, so a mod update just falls back to the script. |
| `asyncSaves` | Player files, advancements, stats and world data are compressed and written on another core, so autosaves don't freeze the game. |

**Only while the server is struggling.** These change timing a little, so they stay asleep until ticks go over the target
(45 ms, `targetMspt`) for 5 seconds. Then they step up one level at a time (1 to 3), and step back down once ticks stay under
35 ms for 20 seconds. A healthy server plays exactly like normal.

| Name | What it does |
|---|---|
| `mobAi` | Mobs far from every player run their goals, brains and item search less often (up to one tick in 16). They keep walking every tick. |
| `mobPush` | Far mobs look for things to push less often. |
| `pathLimits` | Far mobs search shorter paths. A mob that found no way somewhere waits a moment before searching for the same spot. |
| `hoppers` | An idle hopper rests a few ticks between looks, 8 at the very most. That's a hopper's own pause between items, so working hoppers keep full speed, and spigot.yml's hopper settings do nothing on Arclight anyway. |
| `despawnChecks` | Mobs ask "should I despawn?" less often, with the chance raised to match. |
| `afkPlayers` | A player who hasn't moved, looked round, clicked or used a power for 60 seconds has their tick scripts and powers run less often. |
| `slowScripts` | KubeJS tick scripts on the slow list run less often. |
| `slowFunctions` | Datapack tick functions on the slow list run less often. Empty by default. |

**Left alone by the mob throttle:** players, mobs with a target or just hit, bosses (100+ max health or in the Forge boss tag),
tamed or named mobs, raiders, mobs being ridden or on a lead, mobs in water or lava, villagers trading, anything holding a Palladium power,
Char's Powers clones and pets (tags `charpowers.clone`, `charpowers.held`, and the whole `charpowers` mod).
Add your own with the tag `charboost.exempt` or in the config. Furnaces and other block entities are never slowed.

## Commands (ops)

- `/charboost status` shows every part, whether it's on, what it skipped and what it saved, and current tick time and TPS.
- `/charboost off` / `on` is the master switch. `/charboost off mobAi` / `on mobAi` switches one part. These stick after a restart.
- `/charboost level 0|1|2|3|auto` holds the pressure level by hand, or goes back to automatic.
- `/charboost scripts [count]` shows what each KubeJS script costs, by file and line. spark can't split these up.
- `/charboost functions [count]` shows what each datapack function costs.
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

## Building

`gradlew build`. Needs these in `libs/` to compile (none are needed to run): Palladium 4.5.9, Palladium Core 1.0.1, KubeJS 2001.6.5-build.26, Rhino 2001.2.3-build.10, all from the Powers profile.

- `gradlew test`: pure rules (tuner, activation tiers, selector reading, tag counting, throttle fairness).
- `gradlew runGameTestServer -Psmoke`: real headless server checks at full pressure, in `src/smoke`. Rebuild without `-Psmoke` before installing.
- `python tools/run_arclight.py`: the real Arclight jar in a throwaway folder, fed console commands from `tools/scenarios/`.

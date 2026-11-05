#!/usr/bin/env bash
# Real Arclight, no power mods. Only the mod, then with Curtain for a bot player: does it start clean, does everything still work?
# Run through heavy.sh, about 3 minutes.
cd "$(dirname "$0")/.."
CURTAIN=libs/curtain-mc1.20.1-1.3.2.jar
JAR=build/libs/charboost-1.0.jar
python -I tools/run_arclight.py --dir run-arclight/alone --mods $JAR --script tools/scenarios/alone.txt --limit 170
python -I tools/run_arclight.py --dir run-arclight/alone --mods $JAR --mods $CURTAIN --script tools/scenarios/behaviour.txt --limit 230
python -I tools/check_saves.py run-arclight/alone

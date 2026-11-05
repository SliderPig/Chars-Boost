#!/usr/bin/env bash
# Real Arclight, no power mods, one bot and 3000 mobs: how much does the mod save? The same again without it to compare.
# Run through heavy.sh, about 7 minutes.
cd "$(dirname "$0")/.."
CURTAIN=libs/curtain-mc1.20.1-1.3.2.jar
JAR=build/libs/charboost-1.0.jar
python -I tools/run_arclight.py --dir run-arclight/alone --mods $JAR --mods $CURTAIN --script tools/scenarios/mobs_measure.txt --limit 290
python -I tools/run_arclight.py --dir run-arclight/alone --mods $CURTAIN --script tools/scenarios/mobs_measure.txt --limit 290

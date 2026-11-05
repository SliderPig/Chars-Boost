#!/usr/bin/env bash
# One queue turn: GameTests, unit tests and clean build, then the exact finished jar on real Arclight with the real mods.
# Usage (through heavy.sh): final_check.sh <log prefix> <overlay folder>
cd "$(dirname "$0")/.."
LOG="$1"
OVER="$2"
bash tools/gametests.sh "$LOG-gt.log"
sha1sum build/libs/charboost-1.0.jar
python -I tools/run_arclight.py --dir run-arclight/powers --mods-list tools/powers_mods.txt --mods build/libs/charboost-1.0.jar --overlay "$OVER" --script tools/scenarios/final_start.txt --limit 300

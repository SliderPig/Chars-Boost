#!/usr/bin/env python3
"""After a test run: did the saves really reach the disk in good shape? Usage: check_saves.py <run folder>"""
import glob
import gzip
import json
import os
import sys

world = os.path.join(sys.argv[1], "world")
bad = 0
for f in glob.glob(os.path.join(world, "playerdata", "*.dat")):
    size = len(gzip.open(f).read())
    print("player file", os.path.basename(f), size, "bytes")
    if size < 100:
        bad += 1
for folder in ("advancements", "stats"):
    for f in glob.glob(os.path.join(world, folder, "*.json")):
        d = json.load(open(f, encoding="utf-8"))
        print(folder, os.path.basename(f), len(d), "entries")
for f in glob.glob(os.path.join(world, "data", "*.dat")):
    gzip.open(f).read()
    print("world data", os.path.basename(f), os.path.getsize(f), "bytes")
left = glob.glob(os.path.join(world, "**", "*.charboost"), recursive=True)
print("temp files left behind:", left)
if left:
    bad += 1
print("BAD" if bad else "saves OK")
sys.exit(1 if bad else 0)

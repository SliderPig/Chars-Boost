#!/usr/bin/env python3
"""Runs the real Arclight server jar in a throwaway folder and feeds it console commands.

Usage (always through heavy.sh, this PC has 8 GB):
  python tools/run_arclight.py --dir run-arclight/alone --mods build/libs/charboost-1.0.jar --script tools/scenarios/start.txt --limit 300

--mods takes jars or folders (repeatable). Mods are copied into <dir>/mods fresh on each run.
A script is a list of lines:
    # comment                (## comments get printed)
    wait 10                  seconds
    waitfor text             wait until a console line contains text (limit 120 s)
    tps                      runs "forge tps" and prints the Overall line
    expect regex             waits up to 20 s for a console line matching regex, prints OK or FAILED
    sample N out.txt         N thread dumps of the server thread, prints a summary, keeps the stacks in out.txt
    @file.txt                feeds a whole file of commands, one per line
    anything else            sent to the console as a command
The server is stopped with "stop" at the end and killed if it does not close.
"""
import argparse
import atexit
import os
import queue
import re
import shutil
import subprocess
import sys
import threading
import time

JAR = r"C:\Users\black\Downloads\arclight-forge-1.20.1-1.0.6-SNAPSHOT-5dc8683.jar"
JAVA = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe"

JCMD = JAVA.replace("java.exe", "jcmd.exe")
PROFILE = os.path.expandvars(r"%APPDATA%\ModrinthApp\profiles\Powers\mods")

PROPS = """online-mode=false
spawn-protection=0
view-distance=7
simulation-distance=7
level-type=flat
level-seed=12345
motd=charboost test
server-port=25588
sync-chunk-writes=false
enable-command-block=true
spawn-animals=true
spawn-monsters=true
max-players=8
network-compression-threshold=256
difficulty=hard
"""

DEFAULT_SHOW = r"\[CHARBOOST\]|\[CharBoost\] \d+ hooks|Overall|Mixin apply failed|Done \(|Caused by|FATAL|Exception in|ms/t |ms a tick|hooks in|has the following|No entity was found|Unknown or incomplete|Mixin.*(error|fail)"


BUCKETS = [
    ("kubejs script", ("dev.latvian.mods.kubejs.event", "dev.latvian.mods.rhino")),
    ("command functions", ("ServerFunctionManager", "CommandFunction")),
    ("palladium powers", ("net.threetag.palladium.power.PowerHandler",)),
    ("palladium other", ("net.threetag.palladium",)),
    ("mob brain+goals", ("GoalSelector", "ai.Brain.", "ai.sensing", "PathNavigation", "PathFinder")),
    ("entity tick", ("ServerLevel.m_184063_", "ServerLevel.m_8647_", "LivingEntity", "net.minecraft.world.entity.Entity.")),
    ("block entities", ("BoundTickingBlockEntity", "RebindableTickingBlockEntityWrapper")),
    ("chunks", ("ServerChunkCache", "ChunkMap")),
    ("network", ("ServerGamePacketListenerImpl", "ServerConnectionListener")),
]


def sample(pid, count, out):
    """Takes thread dumps of the server thread and prints where the time goes (rough, biased to safepoints)."""
    stacks = []
    for _ in range(count):
        try:
            r = subprocess.run([JCMD, str(pid), "Thread.print"], capture_output=True, text=True, timeout=20,
                               encoding="utf-8", errors="replace")
        except subprocess.TimeoutExpired:
            continue
        cur = None
        for line in r.stdout.splitlines():
            if line.startswith('"'):
                if cur is not None:
                    stacks.append(cur)
                    cur = None
                if line.startswith('"Server thread"'):
                    cur = []
            elif cur is not None and line.strip().startswith("at "):
                cur.append(line.strip()[3:])
        if cur is not None:
            stacks.append(cur)
    with open(out, "w", encoding="utf-8") as f:
        for s in stacks:
            f.write(chr(10).join(s) + chr(10) + chr(10))
    counts = {}
    frames = {}
    for s in stacks:
        for name, keys in BUCKETS:
            if any(k in fr for fr in s for k in keys):
                counts[name] = counts.get(name, 0) + 1
                break
        else:
            counts["other"] = counts.get("other", 0) + 1
        seen = set()
        for fr in s:
            key = fr.split("(")[0]
            if key not in seen:
                seen.add(key)
                frames[key] = frames.get(key, 0) + 1
    n = max(len(stacks), 1)
    print("samples", n, " ".join("%s=%d%%" % (k, 100 * v / n) for k, v in sorted(counts.items(), key=lambda kv: -kv[1])),
          flush=True)
    return counts


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", required=True)
    ap.add_argument("--mods", action="append", default=[])
    ap.add_argument("--script", required=True)
    ap.add_argument("--limit", type=int, default=540)
    ap.add_argument("--xmx", default="3G")
    ap.add_argument("--keep-world", action="store_true")
    ap.add_argument("--mods-list", help="text file of jar names to copy from the Powers profile (read only)")
    ap.add_argument("--overlay", action="append", default=[], help="folder copied over the run folder (datapacks, kubejs)")
    ap.add_argument("--show", default=DEFAULT_SHOW, help="regex of console lines to print")
    a = ap.parse_args()

    run = os.path.abspath(a.dir)
    os.makedirs(run, exist_ok=True)
    with open(os.path.join(run, "eula.txt"), "w") as f:
        f.write("eula=true\n")
    with open(os.path.join(run, "server.properties"), "w") as f:
        f.write(PROPS)
    mods = os.path.join(run, "mods")
    shutil.rmtree(mods, ignore_errors=True)
    os.makedirs(mods, exist_ok=True)
    for m in a.mods:
        if os.path.isdir(m):
            for f in os.listdir(m):
                if f.endswith(".jar"):
                    shutil.copy(os.path.join(m, f), mods)
        else:
            shutil.copy(m, mods)
    if a.mods_list:
        for name in open(a.mods_list, encoding="utf-8"):
            name = name.strip()
            if not name or name.startswith("#"):
                continue
            src = name if os.path.isabs(name) else os.path.join(PROFILE, name)
            shutil.copy(src, mods)
    for o in a.overlay:
        shutil.copytree(o, run, dirs_exist_ok=True)
    if not a.keep_world:
        for w in ("world", "world_nether", "world_the_end"):
            shutil.rmtree(os.path.join(run, w), ignore_errors=True)

    cmd = [JAVA, "-Xmx" + a.xmx, "-Xms1G", "-XX:+UseG1GC", "-jar", JAR, "nogui"]
    print("starting:", " ".join(cmd), flush=True)
    start = time.time()
    proc = subprocess.Popen(cmd, cwd=run, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, encoding="utf-8", errors="replace", bufsize=1)
    lines = queue.Queue()
    all_log = open(os.path.join(run, "console.txt"), "w", encoding="utf-8")
    show = re.compile(a.show)

    def reader():
        for line in proc.stdout:
            all_log.write(line)
            all_log.flush()
            lines.put(line)
            if show.search(line):
                print(line.rstrip()[:260], flush=True)
        lines.put(None)

    threading.Thread(target=reader, daemon=True).start()

    def send(c):
        try:
            proc.stdin.write(c + "\n")
            proc.stdin.flush()
        except OSError:
            pass

    def left():
        return a.limit - (time.time() - start)

    def wait_for(text, secs):
        end = time.time() + min(secs, max(left(), 1))
        while time.time() < end:
            try:
                line = lines.get(timeout=1)
            except queue.Empty:
                if proc.poll() is not None:
                    return False
                continue
            if line is None:
                return False
            if text in line:
                return line
        return False

    def drain():
        while True:
            try:
                lines.get_nowait()
            except queue.Empty:
                return

    def stop_server():
        send("stop")
        try:
            proc.wait(timeout=max(min(60, left()), 10))
        except subprocess.TimeoutExpired:
            print("server would not stop, killing", flush=True)
            subprocess.run(["taskkill", "/F", "/T", "/PID", str(proc.pid)], capture_output=True)
            proc.wait(timeout=20)
        print("server exit code", proc.returncode, "after %.0f s" % (time.time() - start), flush=True)

    atexit.register(lambda: proc.poll() is None and stop_server())

    ok = wait_for("Done (", a.limit - 60)
    if not ok:
        print("SERVER DID NOT FINISH STARTING", flush=True)
    else:
        print("server up after %.0f s" % (time.time() - start), flush=True)
        base = os.path.dirname(os.path.abspath(a.script))
        for raw in open(a.script, encoding="utf-8"):
            line = raw.strip()
            if not line or line.startswith("#"):
                if line.startswith("##"):
                    print(line, flush=True)
                continue
            if left() < 25:
                print("out of time, stopping early", flush=True)
                break
            if proc.poll() is not None:
                print("server died", flush=True)
                break
            if line.startswith("wait "):
                time.sleep(min(float(line.split()[1]), left()))
            elif line.startswith("waitfor "):
                drain()
                r = wait_for(line[8:], 120)
                print("waitfor", line[8:], "->", bool(r), flush=True)
            elif line.startswith("sample "):
                parts = line.split()
                sample(proc.pid, int(parts[1]), os.path.join(run, parts[2]))
            elif line == "tps":
                drain()
                send("forge tps")
                r = wait_for("Overall", 30)
                print("TPS", r.rstrip()[-90:] if r else "no answer", "at %.0f s" % (time.time() - start), flush=True)
            elif line.startswith("@"):
                with open(os.path.join(base, line[1:]), encoding="utf-8") as f:
                    n = 0
                    for c in f:
                        c = c.strip()
                        if c and not c.startswith("#"):
                            send(c)
                            n += 1
                    print("fed", n, "commands from", line[1:], flush=True)
            elif line.startswith("expect "):
                pat = re.compile(line[7:])
                end = time.time() + 20
                hit = None
                while time.time() < end and not hit:
                    try:
                        one = lines.get(timeout=1)
                    except queue.Empty:
                        continue
                    if one is None:
                        break
                    if pat.search(one):
                        hit = one
                print(("EXPECT OK: " + hit.rstrip()[-160:]) if hit else ("EXPECT FAILED: " + line[7:]), flush=True)
            else:
                send(line)
    stop_server()
    all_log.close()


if __name__ == "__main__":
    sys.exit(main())

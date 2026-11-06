#!/usr/bin/env python3
"""Reads every scripted Palladium ability in the power mods and lists the ones whose tick does nothing while the
ability is off. Those don't need a trip through the script engine every tick. Written to
src/main/resources/charboost_idle_abilities.txt as: ability id, mod id, script file, sha1 of that file.

Only a tick that plainly starts with "if (!enabled) return" or is wrapped whole in "if (enabled) {...}" counts.
A few harmless lines in front are allowed (reading a setting, "if (!entity.isPlayer()) return"). Anything else stays out.
Run with python -I tools/make_idle_abilities.py after a power mod updates. The game only trusts a line if the script
file is byte for byte the same, so an updated mod just runs its scripts like normal until this is re-run.
"""
import hashlib
import os
import re
import sys
import zipfile

PROFILE = os.path.expandvars(r"%APPDATA%\ModrinthApp\profiles\Powers\mods")
DOWNLOADS = os.path.join(os.path.expanduser('~'), 'Downloads')
# The server runs patched copies of these two.
JARS = [
    os.path.join(DOWNLOADS, "FSang18's Heroes v12.5.7 (server fix).jar"),
    os.path.join(DOWNLOADS, "Echoes-of-Power-3.3.0 (server fix).jar"),
    os.path.join(PROFILE, "powerborne-heroes-1.20.1-0.5.1.jar"),
    os.path.join(PROFILE, "Heroes_Beta_Forge_Fabric_V.1.1.83.jar"),
    os.path.join(PROFILE, "gabeupsp-2.0.0-forge.jar"),
    os.path.join(PROFILE, "dampened-v1.2.0.jar"),
    os.path.join(PROFILE, "arrzenhanced-3.0.1.jar"),
    os.path.join(PROFILE, "pantheonsent-1.1.1+1.20.1-forge.jar"),
]

HARMLESS_LINE = re.compile(
    r"^(?:const|let|var)\s+\w+\s*=\s*(?:entity|player|entry\.getPropertyByName\(\s*(?:'[^']*'|\"[^\"]*\"|\w+)\s*\)|\w+\.persistentData)\s*;?$"
    r"|^if\s*\(\s*!?\s*(?:\w+\.isPlayer\(\)|\w+\.isAlive\(\)|!?\w+)(?:\s*\|\|\s*!?\s*(?:\w+\.isPlayer\(\)|\w+\.isAlive\(\)|\w+\.isDeadOrDying\(\)|\w+))*\s*\)\s*\{?\s*return\s*;?\s*\}?\s*;?$"
)


def strip_comments(s):
    """Takes out comments, leaving strings alone."""
    out = []
    i = 0
    q = None
    while i < len(s):
        c = s[i]
        if q:
            out.append(c)
            if c == "\\":
                out.append(s[i + 1])
                i += 2
                continue
            if c == q:
                q = None
        elif c in "'\"`":
            q = c
            out.append(c)
        elif s.startswith("//", i):
            while i < len(s) and s[i] != "\n":
                i += 1
            continue
        elif s.startswith("/*", i):
            i = s.index("*/", i) + 2
            continue
        else:
            out.append(c)
        i += 1
    return "".join(out)


def match_close(s, start):
    """s[start] opens a bracket. Returns where it closes, minding strings."""
    pairs = {"(": ")", "{": "}", "[": "]"}
    stack = [pairs[s[start]]]
    i = start + 1
    q = None
    while i < len(s):
        c = s[i]
        if q:
            if c == "\\":
                i += 2
                continue
            if c == q:
                q = None
        elif c in "'\"`":
            q = c
        elif c in pairs:
            stack.append(pairs[c])
        elif c in ")}]":
            if not stack or stack[-1] != c:
                return -1
            stack.pop()
            if not stack:
                return i
        i += 1
    return -1


def statements_before_if(body):
    """Splits off plain lines in front of the part that tests enabled. Returns (front lines, rest)."""
    front = []
    rest = body.strip()
    while rest:
        m = re.match(r"if\s*\(", rest)
        if m:
            close = match_close(rest, m.end() - 1)
            if close < 0:
                return None, None
            cond = rest[m.end():close]
            if re.search(r"\benabled\b", cond):
                return front, rest
            after = rest[close + 1:].lstrip()
            r = re.match(r"\{?\s*return\s*;?\s*\}?\s*;?", after)
            if not r or not r.group(0):
                return None, None
            line = rest[:close + 1] + " " + r.group(0)
            if not HARMLESS_LINE.match(line.strip()):
                return None, None
            front.append(line)
            rest = after[r.end():].lstrip()
            continue
        end = rest.find("\n")
        semi = rest.find(";")
        cut = min(x for x in (end, semi, len(rest)) if x >= 0)
        line = rest[:cut + 1].strip()
        if not HARMLESS_LINE.match(line):
            return None, None
        front.append(line)
        rest = rest[cut + 1:].lstrip()
    return None, None


def idle_when_off(body, flag):
    """True only if this tick plainly does nothing when the flag parameter is false."""
    body = body.replace(flag, "enabled") if flag != "enabled" else body
    front, rest = statements_before_if(strip_comments(body))
    if rest is None:
        return False
    m = re.match(r"if\s*\(", rest)
    close = match_close(rest, m.end() - 1)
    cond = rest[m.end():close].strip()
    after = rest[close + 1:].strip()
    # if (!enabled) return / if (x || !enabled || y) return, every term only reads something
    terms = [t.strip() for t in re.split(r"\|\|", cond)]
    if "!enabled" in terms and re.match(r"\{?\s*return\b", after):
        return all(t == "!enabled" or re.match(r"^!?[\w.]+(\(\))?$", t) for t in terms)
    # if (!(enabled && a && b)) return
    if re.match(r"^!\(\s*enabled\s*(&&.*)?\)$", cond, re.S) and re.match(r"\{?\s*return\b", after):
        return True
    # if (enabled && ...) { everything }
    if re.match(r"^enabled\s*(&&.*)?$", cond, re.S) and after.startswith("{"):
        end = match_close(after, 0)
        return end == len(after) - 1 or after[end + 1:].strip() in ("", ";")
    return False


def main():
    out = []
    kept = 0
    total = 0
    for path in JARS:
        z = zipfile.ZipFile(path)
        toml = z.read("META-INF/mods.toml").decode("utf-8", "replace")
        mod = re.findall(r'modId\s*=\s*"([^"]+)"', toml)[0]
        for name in z.namelist():
            if not (name.endswith(".js") and name.startswith("addon/") and "kubejs_scripts" in name):
                continue
            raw = z.read(name)
            sha1 = hashlib.sha1(raw).hexdigest()
            s = raw.decode("utf-8", "replace")
            if "palladium:abilities" not in s and "palladium:ability" not in s:
                continue
            for m in re.finditer(r"\.create\(\s*['\"]([a-z0-9_:\-./]+)['\"]\s*\)", s):
                ability = m.group(1)
                nxt = s.find(".create(", m.end())
                seg = s[m.end():nxt if nxt > 0 else len(s)]
                t = re.search(r"\.tick\(\s*(?:function\s*)?\(([^)]*)\)\s*(?:=>\s*)?\{", seg)
                if not t:
                    continue
                params = [p.strip() for p in t.group(1).split(",")]
                if len(params) != 4:
                    continue
                total += 1
                open_at = t.end() - 1
                close = match_close(seg, open_at)
                if close < 0:
                    continue
                if idle_when_off(seg[open_at + 1:close], params[3]):
                    out.append("%s %s %s %s" % (ability, mod, name, sha1))
                    kept += 1
    target = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "charboost_idle_abilities.txt")
    with open(target, "w", newline="\n") as f:
        f.write("\n".join(sorted(set(out))) + "\n")
    print("%d of %d scripted abilities do nothing while off" % (kept, total))
    return 0


if __name__ == "__main__":
    sys.exit(main())

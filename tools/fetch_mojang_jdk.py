#!/usr/bin/env python3
"""Download Mojang's own Java 25 runtime (the one the launcher uses for Minecraft 26.x)
from the official launcher metadata into ./.jdk25 so the build does not need a
system-wide JDK 25. Usage: python3 tools/fetch_mojang_jdk.py"""
import hashlib, json, os, sys, urllib.request
from concurrent.futures import ThreadPoolExecutor

ALL = "https://piston-meta.mojang.com/v1/products/java-runtime/2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json"
COMPONENT = "java-runtime-epsilon"
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".jdk25")


def get(url):
    for attempt in range(5):
        try:
            with urllib.request.urlopen(url, timeout=120) as r:
                return r.read()
        except Exception as e:  # retry transient proxy errors
            err = e
    raise err


def main():
    plat = {"linux": "linux", "darwin": "mac-os", "win32": "windows-x64"}[sys.platform]
    entry = json.loads(get(ALL))[plat][COMPONENT][0]
    files = json.loads(get(entry["manifest"]["url"]))["files"]
    print("Mojang", COMPONENT, entry["version"]["name"], "-", len(files), "entries")

    def fetch(item):
        path, info = item
        dest = os.path.join(OUT, path)
        if info["type"] == "directory":
            os.makedirs(dest, exist_ok=True)
        elif info["type"] == "file":
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            raw = info["downloads"]["raw"]
            if not (os.path.exists(dest) and os.path.getsize(dest) == raw["size"]):
                data = get(raw["url"])
                assert hashlib.sha1(data).hexdigest() == raw["sha1"], path
                with open(dest, "wb") as fh:
                    fh.write(data)
            if info.get("executable"):
                os.chmod(dest, 0o755)

    with ThreadPoolExecutor(6) as ex:
        list(ex.map(fetch, files.items()))
    for path, info in files.items():
        if info["type"] == "link":
            dest = os.path.join(OUT, path)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            if not os.path.lexists(dest):
                os.symlink(info["target"], dest)
    print("done ->", OUT)


if __name__ == "__main__":
    main()

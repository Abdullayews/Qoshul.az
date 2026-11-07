#!/usr/bin/env python3
"""CI artifact-indən GitHub Release yaratma (v0.1x.0-beta pattern).

İstifadə: python3 make_release.py <version>   (məs. 0.17.0)
Token git remote-undan oxunur — heç vaxt çap olunmur.
Addımlar:
  1. API-dən son uğurlu run-in artifact-idəni tap (qoshul-native-apk)
  2. Artifact zip-i endir (APK + sha256)
  3. Tag v0.<x>-beta (artıq varsa skip)
  4. Release yarat + APK asset yüklə (Qoshul.az-v0.<x>-demo.apk)
"""
import hashlib
import io
import json
import re
import subprocess
import sys
import time
import urllib.request
import zipfile

REPO = "Abdullayews/Qoshul.az"


def token() -> str:
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"],
                         capture_output=True, text=True).stdout.strip()
    return re.sub(r"https://([^@]+)@.*", r"\1", url)


def api(method: str, path: str, data=None, tok=None, raw=False):
    req = urllib.request.Request(f"https://api.github.com{path}", method=method)
    req.add_header("Authorization", f"token {tok}")
    req.add_header("Accept", "application/vnd.github+json")
    body = None
    if data is not None:
        body = json.dumps(data).encode()
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req, body) as r:
        payload = r.read()
        return payload if raw else json.loads(payload or b"{}")


def main() -> int:
    version = sys.argv[1]
    tag = f"v{version}-beta"
    tok = token()

    # 1) son uğurlu run (main)
    runs = api("GET", f"/repos/{REPO}/actions/runs?branch=main&per_page=10", tok=tok)
    ok_run = next((r for r in runs["workflow_runs"]
                   if r["conclusion"] == "success"), None)
    if not ok_run:
        print("UĞURLU RUN YOXDUR")
        return 1
    artifacts = api("GET", f"/repos/{REPO}/actions/runs/{ok_run['id']}/artifacts", tok=tok)
    art = next((a for a in artifacts["artifacts"] if a["name"] == "qoshul-native-apk"), None)
    if not art:
        print("qoshul-native-apk artifact YOXDUR")
        return 1

    # 2) artifact endir (zip) → APK + sha256
    #    S3 redirect-ə Authorization DAŞINMAMALIdır (403) — curl istifadə edirik
    dl = subprocess.run(
        ["curl", "-sL", "--max-time", "300",
         "-H", f"Authorization: token {tok}",
         f"https://api.github.com/repos/{REPO}/actions/artifacts/{art['id']}/zip"],
        capture_output=True).stdout
    zf = zipfile.ZipFile(io.BytesIO(dl))
    apk_name = [n for n in zf.namelist() if n.endswith(".apk")][0]
    apk_bytes = zf.read(apk_name)
    sha = hashlib.sha256(apk_bytes).hexdigest()
    print(f"APK: {len(apk_bytes)} bytes, sha256={sha[:12]}…")

    # 3) tag
    tags = api("GET", f"/repos/{REPO}/tags?per_page=100", tok=tok)
    if any(t["name"] == tag for t in tags):
        print(f"tag {tag} artıq VAR")
    else:
        head = api("GET", f"/repos/{REPO}/git/ref/heads/main", tok=tok)
        api("POST", f"/repos/{REPO}/git/refs",
            {"ref": f"refs/tags/{tag}", "sha": head["object"]["sha"]}, tok=tok)
        print(f"tag {tag} yaradıldı")

    # 4) release + asset
    rels = api("GET", f"/repos/{REPO}/releases?per_page=100", tok=tok)
    rel = next((r for r in rels if r["tag_name"] == tag), None)
    if not rel:
        rel = api("POST", f"/repos/{REPO}/releases", {
            "tag_name": tag,
            "name": f"Qoshul.az v{version} (Beta)",
            "body": (f"Qoshul.az v{version}-beta\n\n"
                     f"APK: {len(apk_bytes):,} bayt · SHA-256 `{sha}`\n\n"
                     "Quraşdırma: https://github.com/Abdullayews/Qoshul.az#readme"),
            "prerelease": True,
        }, tok=tok)
        print(f"release {tag} yaradıldı (id={rel['id']})")
    else:
        print(f"release {tag} artıq VAR (id={rel['id']})")

    asset_name = f"Qoshul.az-v{version}-demo.apk"
    have = [a["name"] for a in api("GET", f"/repos/{REPO}/releases/{rel['id']}/assets", tok=tok)]
    if asset_name in have:
        print("asset artıq VAR")
        return 0
    up = urllib.request.Request(
        f"https://uploads.github.com/repos/{REPO}/releases/{rel['id']}/assets?name={asset_name}",
        method="POST", data=apk_bytes)
    up.add_header("Authorization", f"token {tok}")
    up.add_header("Content-Type", "application/vnd.android.package-archive")
    with urllib.request.urlopen(up) as r:
        json.loads(r.read())
    print(f"asset {asset_name} yükləndi")
    return 0


if __name__ == "__main__":
    sys.exit(main())

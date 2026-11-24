#!/usr/bin/env python3
"""GitHub Actions secret yükləyici (PyNaCl libsodium sealed box — GitHub-un öz metodu).

Repo-nun public key-ini alır, secret-i şifrələyib PUT edir.
İstifadə: gh_secret.py AD SECRET_DƏYƏRİ  (dəyər arqumentdə və ya stdin-də)
Token remote URL-dən oxunur — heç vaxt çap olunmur.
"""
import base64
import json
import re
import subprocess
import sys
import urllib.request

from nacl import encoding, public

REPO = "Abdullayews/Qoshul.az"


def token() -> str:
    url = subprocess.run(["git", "config", "--get", "remote.origin.url"],
                         capture_output=True, text=True).stdout.strip()
    # URL formatı: https://x-access-token:TOKEN@github.com/...
    m = re.search(r"https://[^:]+:([^@]+)@", url)
    return m.group(1) if m else ""


def main() -> int:
    name = sys.argv[1]
    value = sys.argv[2] if len(sys.argv) > 2 else sys.stdin.read().strip()
    tok = token()

    req = urllib.request.Request(
        f"https://api.github.com/repos/{REPO}/actions/secrets/public-key",
        headers={"Authorization": f"token {tok}",
                 "Accept": "application/vnd.github+json"})
    with urllib.request.urlopen(req) as r:
        pk = json.load(r)

    sealed = public.SealedBox(
        public.PublicKey(pk["key"].encode(), encoding.Base64Encoder()))
    enc = base64.b64encode(sealed.encrypt(value.encode())).decode()

    data = json.dumps({"encrypted_value": enc, "key_id": pk["key_id"]}).encode()
    req = urllib.request.Request(
        f"https://api.github.com/repos/{REPO}/actions/secrets/{name}",
        data=data, method="PUT",
        headers={"Authorization": f"token {tok}",
                 "Accept": "application/vnd.github+json",
                 "Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        print(f"OK: {name} → HTTP {r.status}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

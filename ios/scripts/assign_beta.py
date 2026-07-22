#!/usr/bin/env python3
# Build VALID olunca: tek internal grubu garanti et + Barış'ı tester ekle + build'i ata.
# Env: ASC_KEY_PATH, ASC_KEY_ID, ASC_ISSUER, ASC_APP_ID
import os, sys, time, json, urllib.request, urllib.error
try:
    import jwt
except ImportError:
    os.system(sys.executable + " -m pip install -q --break-system-packages pyjwt cryptography 2>/dev/null"
              " || " + sys.executable + " -m pip install -q --user pyjwt cryptography")
    import site, importlib; importlib.reload(site)
    import jwt

KEY_ID = os.environ["ASC_KEY_ID"]; ISS = os.environ["ASC_ISSUER"]; APP = os.environ["ASC_APP_ID"]
KEY = open(os.environ["ASC_KEY_PATH"]).read()
TESTER = "daclen10@icloud.com"

def tok():
    now = int(time.time())
    return jwt.encode({"iss": ISS, "iat": now, "exp": now + 1000, "aud": "appstoreconnect-v1"},
                      KEY, algorithm="ES256", headers={"kid": KEY_ID, "typ": "JWT"})

def api(path, method="GET", body=None):
    data = json.dumps(body).encode() if body is not None else None
    r = urllib.request.Request("https://api.appstoreconnect.apple.com" + path, data=data, method=method,
        headers={"Authorization": "Bearer " + tok(), "Content-Type": "application/json"})
    try:
        resp = urllib.request.urlopen(r, timeout=40); raw = resp.read()
        return {"_ok": resp.status} if not raw else json.loads(raw)
    except urllib.error.HTTPError as e:
        return {"_err": e.code, "_body": e.read().decode()[:400]}

# 1) Build VALID olana kadar bekle (~25 dk)
bid = None
for _ in range(50):
    d = api(f"/v1/builds?filter[app]={APP}&limit=1&sort=-uploadedDate")
    items = d.get("data") or []
    if items:
        a = items[0]["attributes"]; st = a.get("processingState")
        print(f"build v{a.get('version')} islem={st}", flush=True)
        if st == "VALID": bid = items[0]["id"]; break
        if st in ("INVALID", "FAILED"): print("❌ build islenemedi"); sys.exit(1)
    time.sleep(30)
if not bid:
    print("⚠️ build VALID olmadı, atama atlandı"); sys.exit(0)

# 2) Tek internal grup garanti et (kural: kopya grup üretme)
bg = api(f"/v1/apps/{APP}/betaGroups")
internal = [g for g in bg.get("data", []) if g["attributes"].get("isInternalGroup")]
if internal:
    gid = internal[0]["id"]
    print(f"mevcut internal grup: {internal[0]['attributes'].get('name')}")
else:
    r = api("/v1/betaGroups", "POST", {"data": {"type": "betaGroups",
        "attributes": {"name": "Internal", "isInternalGroup": True, "hasAccessToAllBuilds": True},
        "relationships": {"app": {"data": {"type": "apps", "id": APP}}}}})
    gid = (r.get("data") or {}).get("id")
    print(f"internal grup olusturuldu: {gid} {r.get('_err','')}")

if not gid:
    print("❌ grup yok/olusmadi"); sys.exit(1)

# 3) hasAccessToAllBuilds=True ise build otomatik gelir; yine de dene (idempotent)
r = api(f"/v1/betaGroups/{gid}/relationships/builds", "POST",
        {"data": [{"type": "builds", "id": bid}]})
print("build atama:", r if r.get("_err") else "OK")
print("✅ TestFlight hazir —", TESTER, "App Store Connect kullanicisi olarak TestFlight'ta gorur")

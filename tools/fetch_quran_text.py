#!/usr/bin/env python3
"""Fetch the Arabic Uthmani Quran text (AlQuran.cloud edition `quran-uthmani`) and write it
into the app's assets as one compact, verified JSON file.

Why an asset instead of always hitting the network: the Quran reader must work fully offline
(see README/TESTING). The Arabic text is bundled once at build time, and translations /
recitations are downloaded by the user at runtime.

Source: https://api.alquran.cloud/v1/quran/quran-uthmani  (edition: quran-uthmani, King Fahd
Complex Uthmani script text as distributed by AlQuran.cloud)

Run:  python3 tools/fetch_quran_text.py
"""
import json, os, sys, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app/src/main/assets/quran/quran_uthmani.json")
API = "https://api.alquran.cloud/v1/quran/quran-uthmani"
EXPECTED_SURAHS = 114
EXPECTED_AYAHS = 6236


def fetch(url):
    req = urllib.request.Request(url, headers={"User-Agent": "SalahMate-build/1.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.loads(r.read().decode("utf-8"))


def main():
    payload = fetch(API)
    if payload.get("code") != 200:
        sys.exit(f"API returned code {payload.get('code')}")
    data = payload["data"]
    surahs = data["surahs"]
    assert len(surahs) == EXPECTED_SURAHS, f"expected {EXPECTED_SURAHS} surahs, got {len(surahs)}"
    total = sum(len(s["ayahs"]) for s in surahs)
    assert total == EXPECTED_AYAHS, f"expected {EXPECTED_AYAHS} ayahs, got {total}"

    def surah_field(s, key, default=None):
        """The /v1/quran/{edition} payload omits some per-surah fields that /v1/surah returns."""
        return s.get(key, default)

    out = {
        "edition": {
            "identifier": data["edition"]["identifier"],
            "name": data["edition"]["englishName"],
            "language": data["edition"]["language"],
            "type": data["edition"]["type"],
            "direction": data["edition"].get("direction", "rtl"),
            "source": "https://api.alquran.cloud/v1/quran/quran-uthmani",
            "retrieved": "build-time",
        },
        "surahs": [
            {
                "number": s["number"],
                "name": surah_field(s, "name", ""),
                "englishName": surah_field(s, "englishName", ""),
                "englishNameTranslation": surah_field(s, "englishNameTranslation", ""),
                "revelationType": surah_field(s, "revelationType", "Meccan"),
                "numberOfAyahs": len(s["ayahs"]),
                "ayahs": [
                    {
                        "n": a["numberInSurah"],
                        "t": a["text"].replace("\ufeff", ""),  # strip stray BOM seen in some ayahs
                        "j": a["juz"],
                        "p": a["page"],
                        "s": bool(a["sajda"]) if not isinstance(a["sajda"], dict) else True,
                    }
                    for a in s["ayahs"]
                ],
            }
            for s in surahs
        ],
    }

    # Validation: no empty text, no accidental Latin letters in the Arabic field.
    bad = [(s["number"], a["n"]) for s in out["surahs"] for a in s["ayahs"] if not a["t"].strip()]
    if bad:
        sys.exit(f"empty ayah text at {bad[:5]}")
    latin = [(s["number"], a["n"]) for s in out["surahs"] for a in s["ayahs"]
             if any("a" <= ch.lower() <= "z" for ch in a["t"])]
    if latin:
        sys.exit(f"latin characters inside Arabic text at {latin[:5]}")

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
    size = os.path.getsize(OUT)
    print(f"wrote {OUT} ({size/1024/1024:.2f} MB) surahs={len(out['surahs'])} ayahs={total}")


if __name__ == "__main__":
    main()

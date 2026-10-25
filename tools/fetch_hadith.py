#!/usr/bin/env python3
"""Build the bundled Hadith datasets from the public-domain hadith-api project.

Source:  https://github.com/fawazahmed0/hadith-api  (The Unlicense — public domain)
CDN:     https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions/{edition}.min.json

Only *small* collections are bundled so the APK stays reasonable:
  * Forty Hadith of an-Nawawi (42 hadith, Arabic + Bengali + English)
  * Forty Hadith Qudsi       (40 hadith, Arabic + English; no Bengali edition exists upstream)

The large collections (Bukhari, Muslim, Abu Dawud, Tirmidhi, Nasa'i, Ibn Majah, Muwatta) are
offered as *optional user-initiated downloads* inside the app rather than being bundled; see
CONTENT_LICENSES.md for the reasoning and the exact upstream identifiers used.

Grading data: the upstream `grades` field is preserved verbatim. Nothing is invented or inferred
here — if upstream gives no grade, the app shows no grade.

Run: python3 tools/fetch_hadith.py
"""
import json
import os
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "app/src/main/assets/hadith")
BASE = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions"

BUNDLED = [
    {"id": "nawawi", "name": "Forty Hadith of an-Nawawi", "languages": ["ara", "ben", "eng"]},
    {"id": "qudsi", "name": "Forty Hadith Qudsi", "languages": ["ara", "eng"]},
]

# Collections offered as opt-in downloads (not bundled).
DOWNLOADABLE = [
    {"id": "bukhari", "name": "Sahih al-Bukhari", "languages": ["ara", "ben", "eng"]},
    {"id": "muslim", "name": "Sahih Muslim", "languages": ["ara", "ben", "eng"]},
    {"id": "abudawud", "name": "Sunan Abu Dawud", "languages": ["ara", "ben", "eng"]},
    {"id": "tirmidhi", "name": "Jami' at-Tirmidhi", "languages": ["ara", "ben", "eng"]},
    {"id": "nasai", "name": "Sunan an-Nasa'i", "languages": ["ara", "ben", "eng"]},
    {"id": "ibnmajah", "name": "Sunan Ibn Majah", "languages": ["ara", "ben", "eng"]},
    {"id": "malik", "name": "Muwatta Malik", "languages": ["ara", "ben", "eng"]},
]


def fetch(edition):
    url = f"{BASE}/{edition}.min.json"
    req = urllib.request.Request(url, headers={"User-Agent": "SalahMate-build/1.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.loads(r.read().decode("utf-8"))


def parse_grades(raw):
    """Upstream stores grades as a JSON-encoded string; normalise to a list of dicts."""
    if isinstance(raw, list):
        return raw
    if not raw:
        return []
    try:
        value = json.loads(raw)
        return value if isinstance(value, list) else []
    except (json.JSONDecodeError, TypeError):
        return []


def build(collection):
    langs = {}
    for lang in collection["languages"]:
        langs[lang] = fetch(f"{lang}-{collection['id']}")

    primary = langs["ara"]
    meta = primary["metadata"]
    sections = meta.get("sections", {})
    out_hadiths = []
    for entry in primary["hadiths"]:
        number = entry["hadithnumber"]
        item = {
            "number": number,
            "arabic": entry.get("text", ""),
            "grades": parse_grades(entry.get("grades")),
            "reference": entry.get("reference", {}),
            "book": entry.get("reference", {}).get("book", 0),
            "bookName": sections.get(str(entry.get("reference", {}).get("book", 0)), ""),
        }
        if "ben" in langs:
            match = next((h for h in langs["ben"]["hadiths"] if h["hadithnumber"] == number), None)
            item["bengali"] = match.get("text", "") if match else ""
        if "eng" in langs:
            match = next((h for h in langs["eng"]["hadiths"] if h["hadithnumber"] == number), None)
            item["english"] = match.get("text", "") if match else ""
            if not item["grades"] and match:
                item["grades"] = parse_grades(match.get("grades"))
        out_hadiths.append(item)

    payload = {
        "collection": {
            "id": collection["id"],
            "name": collection["name"],
            "source": "https://github.com/fawazahmed0/hadith-api",
            "license": "The Unlicense (public domain)",
            "retrieved": "build-time",
            "languages": collection["languages"],
            "hadithCount": len(out_hadiths),
        },
        "sections": sections,
        "hadiths": out_hadiths,
    }

    # Validation: every entry must keep its Arabic text and a reference.
    for h in out_hadiths:
        if not h["arabic"].strip():
            sys.exit(f"{collection['id']}: hadith {h['number']} has no Arabic text")
        if not h["reference"]:
            sys.exit(f"{collection['id']}: hadith {h['number']} has no reference")
    return payload


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    manifest = {"bundled": [], "downloadable": DOWNLOADABLE}
    for collection in BUNDLED:
        payload = build(collection)
        path = os.path.join(OUT_DIR, f"{collection['id']}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
        size = os.path.getsize(path)
        manifest["bundled"].append({
            "id": collection["id"],
            "name": collection["name"],
            "languages": collection["languages"],
            "hadithCount": payload["collection"]["hadithCount"],
            "asset": f"hadith/{collection['id']}.json",
            "bytes": size,
        })
        print(f"wrote {path} ({size/1024:.1f} KB, {payload['collection']['hadithCount']} hadith)")

    with open(os.path.join(OUT_DIR, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print("wrote manifest.json")


if __name__ == "__main__":
    main()

import json
import base64
import os

json_path = r"d:\github\DAM\lottie-studio\major-jackpot.json"
out_dir = r"d:\github\DAM\lottie-studio\extracted_assets"
os.makedirs(out_dir, exist_ok=True)

with open(json_path, "r", encoding="utf-8") as f:
    d = json.load(f)

for a in d.get("assets", []):
    p = a.get("p", "")
    if "," in p:
        b64 = p.split(",")[1]
        raw = base64.b64decode(b64)
        out_file = os.path.join(out_dir, f"{a['id']}.png")
        with open(out_file, "wb") as img_f:
            img_f.write(raw)
        print(f"Extraído: {a['id']}.png ({a.get('w')}x{a.get('h')})")

print("Listo!")

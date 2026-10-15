import re
import json

path = r"C:\Users\avila\.gemini\antigravity\brain\c72dfebd-542b-420a-8fb1-d31115180926\.system_generated\steps\50\content.md"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

urls = re.findall(r'https://[^\s"\'<>]+', text)
for u in set(urls):
    if any(k in u for k in ["lottie", "WWzdPccMWp", "animation", "json"]):
        print(u)

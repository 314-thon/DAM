import json
import copy

# Rutas
input_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

with open(input_path, "r", encoding="utf-8") as f:
    d = json.load(f)

# ESTRUCTURA DE LA ANIMACIÓN (Loop completo y fluido con Salida elegante):
# 0 - 60: Entrada de impacto (caída de letras, confeti, diamante).
# 60 - 100: Estabilización y exhibición del logo completo.
# 100 - 150: Mantener en reposo (Pausa de lectura para el usuario).
# 150 - 190: SALIDA ELEGANTE (Fade out + pop/dispersión hacia arriba y disolución suave).
# 190 - 200: Pausa limpia en vacío (0% opacidad).
# Al frame 200 reinicia al frame 0 orgánicamente como una nueva ronda.

total_frames = 200
hold_end = 150
exit_end = 190

d["op"] = total_frames

for l in d["layers"]:
    l["op"] = total_frames + 1

# Cortar keyframes viejos de posición/escala al frame 100 para aplicar la salida limpia
for l in d["layers"]:
    # 1. Posición
    p_prop = l["ks"]["p"]
    if isinstance(p_prop.get("k"), list) and isinstance(p_prop["k"][0], dict):
        # Conservar solo hasta frame 100
        p_prop["k"] = [kf for kf in p_prop["k"] if kf["t"] <= 100]
        last_p = copy.deepcopy(p_prop["k"][-1]["s"])
        # Del frame 100 al 150: Se queda quieto
        p_prop["k"].append({
            "t": hold_end,
            "s": copy.deepcopy(last_p),
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })
        # Del frame 150 al 190: Salida hacia arriba o flotación leve
        exit_p = [last_p[0], last_p[1] - 35, last_p[2]]
        p_prop["k"].append({
            "t": exit_end,
            "s": exit_p,
            "i": {"x": [0.4], "y": [1]},
            "o": {"x": [0.2], "y": [1]}
        })
        p_prop["k"].append({
            "t": total_frames,
            "s": exit_p,
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })

    # 2. Escala
    s_prop = l["ks"]["s"]
    if isinstance(s_prop.get("k"), list) and isinstance(s_prop["k"][0], dict):
        s_prop["k"] = [kf for kf in s_prop["k"] if kf["t"] <= 100]
        last_s = copy.deepcopy(s_prop["k"][-1]["s"])
        # Mantener quieto hasta hold_end
        s_prop["k"].append({
            "t": hold_end,
            "s": copy.deepcopy(last_s),
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })
        # En la salida hace una leve reducción
        exit_s = [last_s[0] * 0.85, last_s[1] * 0.85, last_s[2]]
        s_prop["k"].append({
            "t": exit_end,
            "s": exit_s,
            "i": {"x": [0.4], "y": [1]},
            "o": {"x": [0.2], "y": [1]}
        })
        s_prop["k"].append({
            "t": total_frames,
            "s": exit_s,
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })

    # 3. Opacidad (Fade Out impecable en todas las capas)
    # De 0 a hold_end (150): 100% visible
    # De hold_end a exit_end (150 -> 190): Desvanece de 100% a 0%
    # De exit_end a total_frames (190 -> 200): Permanece en 0%
    l["ks"]["o"] = {
        "a": 1,
        "k": [
            {"t": 0, "s": [100], "i": {"x": [1], "y": [1]}, "o": {"x": [0], "y": [0]}},
            {"t": hold_end, "s": [100], "i": {"x": [0.4], "y": [1]}, "o": {"x": [0.2], "y": [1]}},
            {"t": exit_end, "s": [0], "i": {"x": [1], "y": [1]}, "o": {"x": [0], "y": [0]}},
            {"t": total_frames, "s": [0]}
        ]
    }

# Guardar en JSON, RAW y JS
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(d) + ";\n")

print("Animación con Entrada -> Pausa -> Salida suave generada exitosamente!")

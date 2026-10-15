import json
import math
import copy

# Cargar la animación adaptada
input_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

with open(input_path, "r", encoding="utf-8") as f:
    d = json.load(f)

# Extenderemos la línea de tiempo de 100 a 200 frames (2 segundos adicionales a 50 fps)
# Los primeros 100 frames son la ENTRADA (Intro con rebote de Evanthia)
# Del frame 101 al 200 es el LOOP ORGÁNICO INFINITO (Idle breathing, flotación y brillo de monedas)
# Al regresar del frame 200 al 101, la animación hace un loop perfecto continuo sin volver al frame 0!

intro_end = 100
total_frames = 200
idle_duration = total_frames - intro_end # 100 frames

d["op"] = total_frames

# 1. Extender 'op' de todas las capas para que no desaparezcan
for l in d["layers"]:
    l["op"] = total_frames + 1

# 2. Agregar 'Idle Floating' a las capas principales (Juego, Limpio, Frame, Bcoin)
# Definimos funciones de oscilación seno suave
def append_idle_position(layer, base_x, base_y, amp_y=12, amp_x=0, phase=0.0):
    p_k = layer["ks"]["p"]["k"]
    # Si p_k termina en intro_end, añadimos frames hasta total_frames
    for t in range(intro_end + 1, total_frames + 1):
        rel_t = (t - intro_end) / idle_duration # 0.0 -> 1.0
        # Ciclo senoidal completo (2 * PI) para que el frame 200 conecte exactamente con el 101
        angle = rel_t * 2 * math.pi + phase
        curr_y = round(base_y + math.sin(angle) * amp_y, 2)
        curr_x = round(base_x + math.sin(angle * 0.5) * amp_x, 2)
        p_k.append({
            "t": t,
            "s": [curr_x, curr_y, 0],
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })

def append_idle_scale(layer, base_sx, base_sy, amp_s=3.0, phase=0.0):
    s_prop = layer["ks"]["s"]
    if not isinstance(s_prop.get("k"), list) or not isinstance(s_prop["k"][0], dict):
        return
    s_k = s_prop["k"]
    for t in range(intro_end + 1, total_frames + 1):
        rel_t = (t - intro_end) / idle_duration
        angle = rel_t * 2 * math.pi + phase
        scale_mod = 1.0 + (math.sin(angle) * amp_s) / 100.0
        curr_sx = round(base_sx * scale_mod, 2)
        curr_sy = round(base_sy * scale_mod, 2)
        s_k.append({
            "t": t,
            "s": [curr_sx, curr_sy, 100],
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })

# Aplicar a las 5 letras de "Juego"
# Bases finales en frame 100:
# letter_M (J): [121, 328, 0]
# letter_A (u): [232, 328, 0]
# letter_J (e): [316, 331, 0]
# letter_O (g): [413, 328, 0]
# letter_R (o): [514, 328, 0]
letter_bases = [
    ("letter_M", 121, 328, 0.0),
    ("letter_A", 232, 328, 0.4),
    ("letter_J", 316, 331, 0.8),
    ("letter_O", 413, 328, 1.2),
    ("letter_R", 514, 328, 1.6),
]

for nm, bx, by, ph in letter_bases:
    l = [layer for layer in d["layers"] if layer["nm"] == nm][0]
    append_idle_position(l, bx, by, amp_y=8, phase=ph)

# Aplicar a 'jackpot' ("Limpio"): base [306, 473, 0]
l_limpio = [layer for layer in d["layers"] if layer["nm"] == "jackpot"][0]
append_idle_position(l_limpio, 306, 473, amp_y=10, phase=math.pi * 0.5)

# Aplicar a 'bcoin' (Moneda Dólar): base [302, 188, 0]
# Además de levitar, la moneda hace un brillo/scale pulse sutil
l_bcoin = [layer for layer in d["layers"] if layer["nm"] == "bcoin"][0]
append_idle_position(l_bcoin, 302, 188, amp_y=12, phase=math.pi)
append_idle_scale(l_bcoin, 140, 140, amp_s=5.0, phase=0.0)

# Aplicar a 'frame' (Marco del diamante): base [306, 385, 0]
# Como 'p' en frame era estático {'a': 0, 'k': [306, 385, 0]}, lo convertimos en animado del frame 100 al 200
l_frame = [layer for layer in d["layers"] if layer["nm"] == "frame"][0]
frame_p_k = []
# De 0 a 100 estático en [306, 385, 0]
for t in range(0, intro_end + 1):
    frame_p_k.append({
        "t": t,
        "s": [306, 385, 0],
        "i": {"x": [1], "y": [1]},
        "o": {"x": [0], "y": [0]}
    })
# De 101 a 200 oscilando suavemente (respiración del marco)
for t in range(intro_end + 1, total_frames + 1):
    rel_t = (t - intro_end) / idle_duration
    angle = rel_t * 2 * math.pi
    curr_y = round(385 + math.sin(angle) * 6, 2)
    frame_p_k.append({
        "t": t,
        "s": [306, curr_y, 0],
        "i": {"x": [1], "y": [1]},
        "o": {"x": [0], "y": [0]}
    })
l_frame["ks"]["p"] = {"a": 1, "k": frame_p_k}

# Mantener las monedas secundarias orbitando suavemente durante el idle
coin_layers = [l for l in d["layers"] if "coin" in l["nm"] and l["nm"] != "bcoin"]
for i, c_l in enumerate(coin_layers):
    p_k = c_l["ks"]["p"]["k"]
    last_s = p_k[-1]["s"]
    bx, by = last_s[0], last_s[1]
    phase = (i / len(coin_layers)) * 2 * math.pi
    for t in range(intro_end + 1, total_frames + 1):
        rel_t = (t - intro_end) / idle_duration
        angle = rel_t * 2 * math.pi + phase
        curr_y = round(by + math.sin(angle) * 7, 2)
        curr_x = round(bx + math.cos(angle) * 4, 2)
        p_k.append({
            "t": t,
            "s": [curr_x, curr_y, 0],
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })

# Guardar todo
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(d) + ";\n")

print("Animación extendida con Idle Loop continuo orgánico sin reinicio brusco!")

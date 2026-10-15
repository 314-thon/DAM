import json
import copy

# Rutas
orig_json_path = r"d:\github\DAM\lottie-studio\major-jackpot.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

# 1. Cargar el JSON que ya tiene los assets perfectos (tipografía, Limpio, moneda $)
with open(out_json_path, "r", encoding="utf-8") as f:
    d = json.load(f)

# Cargar el original limpio para tomar las curvas exactas de los 100 frames
with open(orig_json_path, "r", encoding="utf-8") as f:
    orig = json.load(f)

# ESTRUCTURA ESPEJO (Ping-Pong / Reverse exacto):
# 0 -> 100: Entrada original de Evanthia (101 frames: 0 a 100)
# 101 -> 140: Pausa en reposo completo (el logo se aprecia en pantalla)
# 141 -> 241: Salida en REVERSA EXACTA (Frame por frame el espejo inverso de 100 a 0)
# 242 -> 250: Breve pausa en vacío antes de iniciar el nuevo ciclo

intro_frames = 100
pause_duration = 40
pause_end = intro_frames + pause_duration # 140
reverse_duration = intro_frames # 100
total_frames = pause_end + reverse_duration + 10 # 250 frames

d["op"] = total_frames

def mirror_property(orig_prop, prop_type="val", offset_y=0):
    if not isinstance(orig_prop.get("k"), list) or not isinstance(orig_prop["k"][0], dict):
        # Propiedad estática simple
        return copy.deepcopy(orig_prop)
    
    # Extraer los keyframes de entrada (t: 0 a 100)
    in_kfs = [kf for kf in orig_prop["k"] if kf["t"] <= 100]
    if not in_kfs:
        return copy.deepcopy(orig_prop)
    
    # Si hay offset_y (como en Limpio), aplicarlo a in_kfs
    result_kfs = []
    for kf in in_kfs:
        new_kf = copy.deepcopy(kf)
        if offset_y != 0 and prop_type == "p":
            new_kf["s"][1] += offset_y
            if "e" in new_kf:
                new_kf["e"][1] += offset_y
        result_kfs.append(new_kf)
    
    last_val = copy.deepcopy(result_kfs[-1]["s"])
    
    # 2. Pausa (101 -> pause_end)
    result_kfs.append({
        "t": pause_end,
        "s": copy.deepcopy(last_val),
        "i": {"x": [1], "y": [1]},
        "o": {"x": [0], "y": [0]}
    })
    
    # 3. Salida en espejo exacto (pause_end + 1 -> pause_end + 100)
    # Recorremos in_kfs en orden inverso: de frame 100 hacia frame 0
    # Creamos un mapa de t -> valor
    val_map = {kf["t"]: kf["s"] for kf in result_kfs if kf["t"] <= 100}
    
    for t_step in range(1, 101):
        # t_step va de 1 a 100
        # el target en el espejo va del frame (100 - t_step)
        source_frame = 100 - t_step
        # buscar valor más cercano
        while source_frame not in val_map and source_frame > 0:
            source_frame -= 1
        mirror_val = copy.deepcopy(val_map[source_frame])
        
        current_time = pause_end + t_step
        result_kfs.append({
            "t": current_time,
            "s": mirror_val,
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })
    
    # Pausa final en estado inicial
    init_val = copy.deepcopy(result_kfs[0]["s"])
    result_kfs.append({
        "t": total_frames,
        "s": init_val,
        "i": {"x": [1], "y": [1]},
        "o": {"x": [0], "y": [0]}
    })
    
    return {"a": 1, "k": result_kfs}

# Aplicar a cada capa
for i, l in enumerate(d["layers"]):
    orig_l = orig["layers"][i]
    l["op"] = total_frames + 1
    
    is_jackpot = (l["nm"] == "jackpot")
    y_off = 45 if is_jackpot else 0
    
    # Espejo de Posición
    l["ks"]["p"] = mirror_property(orig_l["ks"]["p"], "p", offset_y=y_off)
    
    # Espejo de Escala
    # Para letter_J, recordamos que igualamos su escala a letter_M
    if l["nm"] == "letter_J":
        l["ks"]["s"] = mirror_property(orig["layers"][5]["ks"]["s"], "s")
    else:
        l["ks"]["s"] = mirror_property(orig_l["ks"]["s"], "s")
    
    # Espejo de Rotación
    l["ks"]["r"] = mirror_property(orig_l["ks"]["r"], "r")
    
    # Opacidad (si era animada en el original, o 100% durante la animación)
    if isinstance(orig_l["ks"]["o"].get("k"), list) and isinstance(orig_l["ks"]["o"]["k"][0], dict):
        l["ks"]["o"] = mirror_property(orig_l["ks"]["o"], "o")
    else:
        # 100% visible durante la entrada, pausa y salida, 0% en los últimos frames de pausa
        l["ks"]["o"] = {
            "a": 1,
            "k": [
                {"t": 0, "s": [100]},
                {"t": pause_end + reverse_duration, "s": [100]},
                {"t": pause_end + reverse_duration + 1, "s": [0]},
                {"t": total_frames, "s": [0]}
            ]
        }

# Guardar
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(d) + ";\n")

print("Animación ESPEJO 100% perfecta generada exitosamente!")

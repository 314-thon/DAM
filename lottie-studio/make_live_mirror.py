import json
import math
import copy

orig_json_path = r"d:\github\DAM\lottie-studio\major-jackpot.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

with open(out_json_path, "r", encoding="utf-8") as f:
    d = json.load(f)

with open(orig_json_path, "r", encoding="utf-8") as f:
    orig = json.load(f)

# ESTRUCTURA DINÁMICA CON MOVIMIENTO VIVO PERPETUO:
# 0 -> 100: Entrada triunfal (curvas de Evanthia)
# 101 -> 140: INTERMEDIO VIVO (Idle breathing / flotación senoidal, NUNCA estático)
# 141 -> 241: Salida en espejo exacto (reversa hacia afuera)
# 242 -> 245: Micro-transición suave al frame 0

intro_frames = 100
bridge_duration = 40 # frames 101 a 140
bridge_end = intro_frames + bridge_duration # 140
reverse_duration = intro_frames # 100 frames (141 a 240)
total_frames = bridge_end + reverse_duration + 5 # 245 frames

d["op"] = total_frames

def mirror_property_with_live_bridge(orig_prop, prop_type="val", offset_y=0, amp_y=0, amp_s=0, phase=0.0):
    if not isinstance(orig_prop.get("k"), list) or not isinstance(orig_prop["k"][0], dict):
        return copy.deepcopy(orig_prop)
    
    in_kfs = [kf for kf in orig_prop["k"] if kf["t"] <= 100]
    if not in_kfs:
        return copy.deepcopy(orig_prop)
    
    result_kfs = []
    val_map = {}
    for kf in in_kfs:
        new_kf = copy.deepcopy(kf)
        if offset_y != 0 and prop_type == "p":
            new_kf["s"][1] += offset_y
            if "e" in new_kf:
                new_kf["e"][1] += offset_y
        result_kfs.append(new_kf)
        val_map[new_kf["t"]] = new_kf["s"]
    
    # Valor exacto donde termina la entrada en frame 100
    base_val = copy.deepcopy(val_map[100])
    
    # 2. EL PUENTE INTERMEDIO NUNCA SE QUEDA QUIETO:
    # Genera una respiración/ondulación senoidal completa que arranca en base_val y regresa a base_val en frame 140
    for t_bridge in range(1, bridge_duration + 1):
        t_curr = intro_frames + t_bridge
        rel = t_bridge / bridge_duration # 0.0 -> 1.0
        # Ciclo senoidal: sin(rel * 2 * pi) empieza en 0, sube, baja y termina exactamente en 0
        sine_offset = math.sin(rel * 2 * math.pi + phase)
        
        bridge_val = copy.deepcopy(base_val)
        if prop_type == "p" and amp_y != 0:
            bridge_val[1] = round(base_val[1] + sine_offset * amp_y, 2)
        elif prop_type == "s" and amp_s != 0:
            factor = 1.0 + (sine_offset * amp_s) / 100.0
            bridge_val[0] = round(base_val[0] * factor, 2)
            bridge_val[1] = round(base_val[1] * factor, 2)
        
        result_kfs.append({
            "t": t_curr,
            "s": bridge_val,
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })
    
    # 3. Salida en espejo exacto desde frame 141 hasta 240
    for t_step in range(1, 101):
        source_frame = 100 - t_step
        while source_frame not in val_map and source_frame > 0:
            source_frame -= 1
        mirror_val = copy.deepcopy(val_map[source_frame])
        
        current_time = bridge_end + t_step
        result_kfs.append({
            "t": current_time,
            "s": mirror_val,
            "i": {"x": [1], "y": [1]},
            "o": {"x": [0], "y": [0]}
        })
    
    # Micro cierre
    init_val = copy.deepcopy(val_map[0])
    result_kfs.append({
        "t": total_frames,
        "s": init_val,
        "i": {"x": [1], "y": [1]},
        "o": {"x": [0], "y": [0]}
    })
    
    return {"a": 1, "k": result_kfs}

# Parámetros de respiración para que siempre esté vivo:
# Las letras de "Juego" tienen un oleaje sutil
letter_amps = {
    "letter_M": (6.0, 0.0),
    "letter_A": (6.0, 0.4),
    "letter_J": (7.0, 0.8),
    "letter_O": (6.0, 1.2),
    "letter_R": (6.0, 1.6),
}

for i, l in enumerate(d["layers"]):
    orig_l = orig["layers"][i]
    l["op"] = total_frames + 1
    nm = l["nm"]
    
    is_jackpot = (nm == "jackpot")
    y_off = 45 if is_jackpot else 0
    
    amp_y = 0.0
    amp_s = 0.0
    phase = 0.0
    
    if nm in letter_amps:
        amp_y, phase = letter_amps[nm]
    elif is_jackpot: # Limpio
        amp_y = 7.0
        phase = math.pi * 0.5
    elif nm == "bcoin": # Moneda $
        amp_y = 8.0
        amp_s = 4.0
        phase = math.pi
    elif "coin" in nm: # Monedas secundarias
        amp_y = 5.0
        phase = (i / 20.0) * 2 * math.pi
    
    # Posición con puente vivo
    l["ks"]["p"] = mirror_property_with_live_bridge(orig_l["ks"]["p"], "p", offset_y=y_off, amp_y=amp_y, phase=phase)
    
    # Escala con puente vivo
    if nm == "letter_J":
        l["ks"]["s"] = mirror_property_with_live_bridge(orig["layers"][5]["ks"]["s"], "s")
    else:
        l["ks"]["s"] = mirror_property_with_live_bridge(orig_l["ks"]["s"], "s", amp_s=amp_s, phase=phase)
    
    # Rotación
    l["ks"]["r"] = mirror_property_with_live_bridge(orig_l["ks"]["r"], "r")
    
    # Opacidad continua (100% hasta el final de la reversa)
    if isinstance(orig_l["ks"]["o"].get("k"), list) and isinstance(orig_l["ks"]["o"]["k"][0], dict):
        l["ks"]["o"] = mirror_property_with_live_bridge(orig_l["ks"]["o"], "o")
    else:
        l["ks"]["o"] = {
            "a": 1,
            "k": [
                {"t": 0, "s": [100]},
                {"t": bridge_end + reverse_duration, "s": [100]},
                {"t": bridge_end + reverse_duration + 1, "s": [0]},
                {"t": total_frames, "s": [0]}
            ]
        }

# Guardar todo
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(d, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(d) + ";\n")

print("Animación con movimiento continuo permanente (sin frames congelados) lista!")

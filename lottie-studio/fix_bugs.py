import json
import base64
import os
import copy
from PIL import Image, ImageDraw, ImageFont

# Rutas
font_path = r"d:\github\DAM\app\src\main\res\font\space_grotesk_bold.ttf"
orig_json_path = r"d:\github\DAM\lottie-studio\major-jackpot.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

with open(orig_json_path, "r", encoding="utf-8") as f:
    lottie = json.load(f)

# Colores Oficiales exactos de Color.kt
c_juego_start = (186, 104, 200) # #BA68C8
c_juego_mid   = (96, 147, 177)  # #6093B1
c_juego_end   = (20, 224, 195)  # #14E0C3

c_limpio_start = (107, 137, 175) # #6B89AF
c_limpio_mid   = (143, 103, 169) # #8F67A9
c_limpio_end   = (192, 84, 168)  # #C054A8

def interpolate_grad(stops, t):
    if t <= stops[0][0]:
        return stops[0][1]
    if t >= stops[-1][0]:
        return stops[-1][1]
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i+1]
        if t0 <= t <= t1:
            ratio = (t - t0) / (t1 - t0)
            return (
                int(c0[0] + (c1[0] - c0[0]) * ratio),
                int(c0[1] + (c1[1] - c0[1]) * ratio),
                int(c0[2] + (c1[2] - c0[2]) * ratio)
            )
    return stops[-1][1]

juego_stops = [(0.0, c_juego_start), (0.5, c_juego_mid), (1.0, c_juego_end)]
limpio_stops = [(0.0, c_limpio_start), (0.5, c_limpio_mid), (1.0, c_limpio_end)]

font_size = 72
font = ImageFont.truetype(font_path, font_size)

# Renderizamos cada letra INDIVIDUALMENTE para eliminar por completo cualquier artefacto o rayita de corte
letter_chars = [
    {"char": "J", "progress": 0.05, "w": 70, "h": 100},
    {"char": "u", "progress": 0.28, "w": 70, "h": 100},
    {"char": "e", "progress": 0.50, "w": 70, "h": 100},
    {"char": "g", "progress": 0.72, "w": 70, "h": 115}, # 'g' tiene descendente más largo
    {"char": "o", "progress": 0.95, "w": 70, "h": 100},
]

rendered_letters = []

for l_info in letter_chars:
    ch = l_info["char"]
    w, h = l_info["w"], l_info["h"]
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    
    # Máscara limpia solo con esa letra
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    
    bbox = font.getbbox(ch)
    # Centrado horizontal exacto dentro de la caja de 70px
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    x = (w - tw) // 2 - bbox[0]
    # Alineación vertical sobre línea base común (baseline y = 15)
    y = 15
    d.text((x, y), ch, font=font, fill=255)
    
    # Relleno del color de gradiente correspondiente a esta letra
    col = interpolate_grad(juego_stops, l_info["progress"])
    solid = Image.new("RGBA", (w, h), (*col, 255))
    img.paste(solid, (0, 0), mask)
    
    rendered_letters.append({"img": img, "w": w, "h": h, "bbox": bbox})

# RENDERIZAMOS "Limpio"
font_limpio = ImageFont.truetype(font_path, 68)
text_l = "Limpio"
bbox_l = font_limpio.getbbox(text_l)
tw_l = bbox_l[2] - bbox_l[0]
th_l = bbox_l[3] - bbox_l[1]

limpio_w = tw_l + 20
limpio_h = th_l + 25

mask_l = Image.new("L", (limpio_w, limpio_h), 0)
d_l = ImageDraw.Draw(mask_l)
d_l.text((10 - bbox_l[0], 10 - bbox_l[1]), text_l, font=font_limpio, fill=255)

grad_l = Image.new("RGBA", (limpio_w, limpio_h), (0, 0, 0, 0))
for px in range(limpio_w):
    rel_t = max(0.0, min(1.0, px / max(1, limpio_w)))
    col = interpolate_grad(limpio_stops, rel_t)
    for py in range(limpio_h):
        grad_l.putpixel((px, py), (*col, 255))

limpio_img = Image.new("RGBA", (limpio_w, limpio_h), (0, 0, 0, 0))
limpio_img.paste(grad_l, (0, 0), mask_l)

# MONEDA DÓLAR "$"
def render_dollar_coin(size=(80, 80)):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size[0] // 2, size[1] // 2
    r_outer = 38
    r_inner = 31

    draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=(138, 86, 17, 255))
    draw.ellipse([cx - r_outer + 2, cy - r_outer + 2, cx + r_outer - 2, cy + r_outer - 2], fill=(245, 195, 68, 255))
    draw.ellipse([cx - r_inner - 2, cy - r_inner - 2, cx + r_inner + 2, cy + r_inner + 2], fill=(201, 145, 36, 255))
    draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=(236, 183, 58, 255))

    font_dollar = ImageFont.truetype(font_path, 44)
    dollar_char = "$"
    bbox = font_dollar.getbbox(dollar_char)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    dx = cx - tw / 2 - bbox[0]
    dy = cy - th / 2 - bbox[1]

    draw.text((dx, dy + 2), dollar_char, font=font_dollar, fill=(145, 94, 20, 255))
    draw.text((dx, dy), dollar_char, font=font_dollar, fill=(255, 245, 200, 255))
    return img

def to_b64(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("utf-8")

# Inyectar letras en assets
asset_map = ["M", "A", "J", "O", "R"]
for i, aid in enumerate(asset_map):
    rl = rendered_letters[i]
    for a in lottie["assets"]:
        if a["id"] == aid:
            a["w"] = rl["w"]
            a["h"] = rl["h"]
            a["p"] = to_b64(rl["img"])
            break

# Inyectar Limpio
for a in lottie["assets"]:
    if a["id"] == "jackpot":
        a["w"] = limpio_w
        a["h"] = limpio_h
        a["p"] = to_b64(limpio_img)
        break

# Inyectar moneda dólar
dollar_img = render_dollar_coin((80, 80))
for a in lottie["assets"]:
    if a["id"] == "bcoin":
        a["p"] = to_b64(dollar_img)
        break

# MODIFICAR POSICIÓN DE LIMPIO PARA SEPARARLO Y QUE NO SE SOLAPE
# Desplazamos la posición vertical Y de la capa jackpot hacia abajo 45 píxeles más
offset_y = 45
jackpot_layer = [l for l in lottie["layers"] if l["nm"] == "jackpot"][0]
for kf in jackpot_layer["ks"]["p"]["k"]:
    kf["s"][1] += offset_y
    if "e" in kf:
        kf["e"][1] += offset_y

# Homogeneizar escala en capa letter_J
m_layer = [l for l in lottie["layers"] if l["nm"] == "letter_M"][0]
j_layer = [l for l in lottie["layers"] if l["nm"] == "letter_J"][0]
j_layer["ks"]["s"]["k"] = copy.deepcopy(m_layer["ks"]["s"]["k"])

# Configurar anclajes centrados limpios
for l in lottie["layers"]:
    if l["nm"] == "letter_M": # J
        l["ks"]["a"]["k"] = [rendered_letters[0]["w"] / 2, 45, 0]
    elif l["nm"] == "letter_A": # u
        l["ks"]["a"]["k"] = [rendered_letters[1]["w"] / 2, 45, 0]
    elif l["nm"] == "letter_J": # e
        l["ks"]["a"]["k"] = [rendered_letters[2]["w"] / 2, 45, 0]
    elif l["nm"] == "letter_O": # g
        l["ks"]["a"]["k"] = [rendered_letters[3]["w"] / 2, 45, 0]
    elif l["nm"] == "letter_R": # o
        l["ks"]["a"]["k"] = [rendered_letters[4]["w"] / 2, 45, 0]
    elif l["nm"] == "jackpot": # Limpio
        l["ks"]["a"]["k"] = [limpio_w / 2, limpio_h / 2, 0]

# Guardar todo
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(lottie) + ";\n")

print("Listo! Rayitas eliminadas al 100% y Limpio desplazado hacia abajo sin solapamiento.")

import json
import base64
import os
from PIL import Image, ImageDraw, ImageFont

# Rutas
font_path = r"d:\github\DAM\app\src\main\res\font\space_grotesk_bold.ttf"
orig_json_path = r"d:\github\DAM\lottie-studio\major-jackpot.json"
out_json_path = r"d:\github\DAM\lottie-studio\juego_limpio_jackpot_adapted.json"
out_raw_path = r"d:\github\DAM\app\src\main\res\raw\juego_limpio_jackpot.json"
out_js_path = r"d:\github\DAM\lottie-studio\adapted-jackpot-data.js"

with open(orig_json_path, "r", encoding="utf-8") as f:
    lottie = json.load(f)

def hex_to_rgb(hex_str):
    h = hex_str.lstrip('#')
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

c_juego_start = hex_to_rgb("BA68C8")
c_juego_mid   = hex_to_rgb("6093B1")
c_juego_end   = hex_to_rgb("14E0C3")

c_limpio_start = hex_to_rgb("6B89AF")
c_limpio_mid   = hex_to_rgb("8F67A9")
c_limpio_end   = hex_to_rgb("C054A8")

def interpolate_gradient(stops, t):
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

# Calibramos el render de cada letra de JUEGO
# Como la capa letter_J tiene escala 100% y las demás 153%, multiplicamos font_size de esa capa por 1.53 para que queden idénticas!
def render_letter(char, size=(100, 100), font_size=64, progress=0.0):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    font = ImageFont.truetype(font_path, font_size)

    bbox = font.getbbox(char)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    x = (size[0] - tw) / 2 - bbox[0]
    y = (size[1] - th) / 2 - bbox[1]

    # Sombra oscura sólida profunda
    shadow_mask = Image.new("L", size, 0)
    shadow_draw = ImageDraw.Draw(shadow_mask)
    shadow_draw.text((x, y + 4), char, font=font, fill=255)
    shadow_img = Image.new("RGBA", size, (13, 17, 23, 230))
    img.paste(shadow_img, (0, 0), shadow_mask)

    # Relleno con el gradiente oficial
    char_color = interpolate_gradient(juego_stops, progress)
    main_fill = Image.new("RGBA", size, (*char_color, 255))
    
    text_mask = Image.new("L", size, 0)
    text_draw = ImageDraw.Draw(text_mask)
    text_draw.text((x, y), char, font=font, fill=255)

    img.paste(main_fill, (0, 0), text_mask)
    return img

# Render para LIMPIO en MAYÚSCULAS con tamaño balanceado 1:1 con JUEGO
def render_limpio_banner(size=(280, 85), font_size=56):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    font = ImageFont.truetype(font_path, font_size)
    text = "LIMPIO"

    bbox = font.getbbox(text)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    x = (size[0] - tw) / 2 - bbox[0]
    y = (size[1] - th) / 2 - bbox[1]

    # Sombra
    shadow_mask = Image.new("L", size, 0)
    shadow_draw = ImageDraw.Draw(shadow_mask)
    shadow_draw.text((x, y + 4), text, font=font, fill=255)
    shadow_img = Image.new("RGBA", size, (18, 17, 24, 230))
    img.paste(shadow_img, (0, 0), shadow_mask)

    text_mask = Image.new("L", size, 0)
    text_draw = ImageDraw.Draw(text_mask)
    text_draw.text((x, y), text, font=font, fill=255)

    gradient_img = Image.new("RGBA", size, (0, 0, 0, 0))
    for px in range(size[0]):
        rel_t = max(0.0, min(1.0, (px - x) / max(1, tw)))
        col = interpolate_gradient(limpio_stops, rel_t)
        for py in range(size[1]):
            gradient_img.putpixel((px, py), (*col, 255))

    img.paste(gradient_img, (0, 0), text_mask)
    return img

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

def img_to_base64(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("utf-8")

# Letras en MAYÚSCULAS: J - U - E - G - O
# Compensación de tamaño:
# La capa 'letter_J' (originalmente la J gigante central que bajaba hasta abajo) tiene escala 100% en la animación.
# Las otras 4 capas tienen escala 153%.
# Por lo tanto, para que la letra central 'E' tenga EXACTAMENTE la misma altura visual en pantalla que J, U, G, O:
# fontSize_base = 62.
# Para 'E' (que está en letter_J): fontSize = round(62 * 1.53) = 95.
# Así en pantalla se ven 100% idénticas de altura!

letter_specs = [
    {"char": "J", "w": 75, "h": 75, "fs": 62, "progress": 0.05},
    {"char": "U", "w": 75, "h": 75, "fs": 62, "progress": 0.28},
    {"char": "E", "w": 76, "h": 122, "fs": 95, "progress": 0.50},  # En asset J original
    {"char": "G", "w": 75, "h": 75, "fs": 62, "progress": 0.72},
    {"char": "O", "w": 75, "h": 75, "fs": 62, "progress": 0.95},
]

asset_ids = ["M", "A", "J", "O", "R"]
for i, spec in enumerate(letter_specs):
    target_id = asset_ids[i]
    for a in lottie["assets"]:
        if a["id"] == target_id:
            img = render_letter(spec["char"], size=(spec["w"], spec["h"]), font_size=spec["fs"], progress=spec["progress"])
            a["w"] = spec["w"]
            a["h"] = spec["h"]
            a["p"] = img_to_base64(img)
            break

# Banner LIMPIO en mayúsculas
limpio_img = render_limpio_banner(size=(280, 85), font_size=58)
for a in lottie["assets"]:
    if a["id"] == "jackpot":
        a["w"] = 280
        a["h"] = 85
        a["p"] = img_to_base64(limpio_img)
        break

# Moneda Dólar
dollar_coin_img = render_dollar_coin(size=(80, 80))
for a in lottie["assets"]:
    if a["id"] == "bcoin":
        a["p"] = img_to_base64(dollar_coin_img)
        break

# Puntos de anclaje para centrado exacto
for l in lottie["layers"]:
    if l["nm"] == "letter_M": # J
        l["ks"]["a"]["k"] = [letter_specs[0]["w"] / 2, letter_specs[0]["h"] / 2, 0]
    elif l["nm"] == "letter_A": # U
        l["ks"]["a"]["k"] = [letter_specs[1]["w"] / 2, letter_specs[1]["h"] / 2, 0]
    elif l["nm"] == "letter_J": # E
        l["ks"]["a"]["k"] = [letter_specs[2]["w"] / 2, letter_specs[2]["h"] / 2, 0]
    elif l["nm"] == "letter_O": # G
        l["ks"]["a"]["k"] = [letter_specs[3]["w"] / 2, letter_specs[3]["h"] / 2, 0]
    elif l["nm"] == "letter_R": # O
        l["ks"]["a"]["k"] = [letter_specs[4]["w"] / 2, letter_specs[4]["h"] / 2, 0]
    elif l["nm"] == "jackpot": # LIMPIO
        l["ks"]["a"]["k"] = [140, 42.5, 0]

with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(lottie) + ";\n")

print("Letras calibradas! JUEGO y LIMPIO ahora están niveladas y en proporción exacta 1:1.")

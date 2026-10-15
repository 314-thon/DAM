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

# RENDERIZAMOS PRIMERO "Juego" COMPLETO PARA CORTAR EXACTAMENTE CADA LETRA EN SU POSICIÓN NATURAL
# "Juego" en WelcomeScreen: text="Juego", fontSize=64sp, letterSpacing=-1.5sp
# Para el asset Lottie generamos en alta resolución (fontSize = 80)
total_w = 460
total_h = 130
font_juego = ImageFont.truetype(font_path, 80)

full_juego_img = Image.new("RGBA", (total_w, total_h), (0, 0, 0, 0))
mask_juego = Image.new("L", (total_w, total_h), 0)
d_juego = ImageDraw.Draw(mask_juego)

# Medimos el texto "Juego"
text_j = "Juego"
# Dibujar el texto puro idéntico al Composable de Android
d_juego.text((10, 10), text_j, font=font_juego, fill=255)

# Crear imagen con el gradiente horizontal exacto
grad_juego = Image.new("RGBA", (total_w, total_h), (0, 0, 0, 0))
bbox_j = font_juego.getbbox(text_j)
text_x0 = 10 + bbox_j[0]
text_x1 = 10 + bbox_j[2]
text_width = text_x1 - text_x0

for px in range(total_w):
    rel_t = max(0.0, min(1.0, (px - text_x0) / max(1, text_width)))
    col = interpolate_grad(juego_stops, rel_t)
    for py in range(total_h):
        grad_juego.putpixel((px, py), (*col, 255))

full_juego_img.paste(grad_juego, (0, 0), mask_juego)

# Medimos las posiciones X de cada letra: J, u, e, g, o
letter_chars = ["J", "u", "e", "g", "o"]
# Obtener offsets de cada caracter
letter_crops = []
cum_x = 10
for i, ch in enumerate(letter_chars):
    # Medir letra individual
    b_ch = font_juego.getbbox(ch)
    # Medir substring hasta esta letra y la anterior
    prev_str = text_j[:i]
    curr_str = text_j[:i+1]
    x_start = 10 + (font_juego.getbbox(prev_str)[2] if prev_str else 0)
    x_end = 10 + font_juego.getbbox(curr_str)[2]
    # Ancho de la caja para la letra con margen
    w_box = max(50, x_end - x_start + 10)
    h_box = 110
    
    # Crear imagen para esta letra
    ch_img = Image.new("RGBA", (w_box, h_box), (0, 0, 0, 0))
    # Recortar de full_juego_img
    crop_area = (x_start - 5, 0, x_end + 5, h_box)
    cropped = full_juego_img.crop(crop_area)
    ch_img.paste(cropped, (0, 0))
    letter_crops.append({"img": ch_img, "w": w_box, "h": h_box})

# AHORA RENDERIZAMOS "Limpio" EXACTAMENTE COMO EN EL COMPOSABLE
font_limpio = ImageFont.truetype(font_path, 80)
text_l = "Limpio"
bbox_l = font_limpio.getbbox(text_l)
w_limpio_raw = bbox_l[2] - bbox_l[0]
h_limpio_raw = bbox_l[3] - bbox_l[1]

limpio_canvas_w = w_limpio_raw + 40
limpio_canvas_h = h_limpio_raw + 40

mask_limpio = Image.new("L", (limpio_canvas_w, limpio_canvas_h), 0)
d_limpio = ImageDraw.Draw(mask_limpio)
lx0 = 20 - bbox_l[0]
ly0 = 20 - bbox_l[1]
d_limpio.text((lx0, ly0), text_l, font=font_limpio, fill=255)

grad_limpio = Image.new("RGBA", (limpio_canvas_w, limpio_canvas_h), (0, 0, 0, 0))
for px in range(limpio_canvas_w):
    rel_t = max(0.0, min(1.0, (px - 20) / max(1, w_limpio_raw)))
    col = interpolate_grad(limpio_stops, rel_t)
    for py in range(limpio_canvas_h):
        grad_limpio.putpixel((px, py), (*col, 255))

limpio_final_img = Image.new("RGBA", (limpio_canvas_w, limpio_canvas_h), (0, 0, 0, 0))
limpio_final_img.paste(grad_limpio, (0, 0), mask_limpio)

# RENDERIZAMOS LA MONEDA CON EL SIGNO DÓLAR "$"
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

def img_to_b64(img):
    import io
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("utf-8")

# Inyectamos en Lottie
# Para igualar la escala de las capas:
# Modificamos los keyframes de escala de letter_J para que coincidan EXACTAMENTE con letter_M
# Así todas las capas tienen la misma escala nativa!
m_layer = [l for l in lottie["layers"] if l["nm"] == "letter_M"][0]
j_layer = [l for l in lottie["layers"] if l["nm"] == "letter_J"][0]
# Copiamos la escala de M a J
import copy
j_layer["ks"]["s"]["k"] = copy.deepcopy(m_layer["ks"]["s"]["k"])

# Inyectar las 5 letras
asset_map = ["M", "A", "J", "O", "R"]
for i, aid in enumerate(asset_map):
    c_info = letter_crops[i]
    for a in lottie["assets"]:
        if a["id"] == aid:
            a["w"] = c_info["w"]
            a["h"] = c_info["h"]
            a["p"] = img_to_b64(c_info["img"])
            break

# Inyectar Limpio
for a in lottie["assets"]:
    if a["id"] == "jackpot":
        a["w"] = limpio_canvas_w
        a["h"] = limpio_canvas_h
        a["p"] = img_to_b64(limpio_final_img)
        break

# Inyectar moneda dólar
dollar_img = render_dollar_coin((80, 80))
for a in lottie["assets"]:
    if a["id"] == "bcoin":
        a["p"] = img_to_b64(dollar_img)
        break

# Puntos de anclaje
for l in lottie["layers"]:
    if l["nm"] == "letter_M": # J
        l["ks"]["a"]["k"] = [letter_crops[0]["w"] / 2, letter_crops[0]["h"] / 2, 0]
    elif l["nm"] == "letter_A": # u
        l["ks"]["a"]["k"] = [letter_crops[1]["w"] / 2, letter_crops[1]["h"] / 2, 0]
    elif l["nm"] == "letter_J": # e
        l["ks"]["a"]["k"] = [letter_crops[2]["w"] / 2, letter_crops[2]["h"] / 2, 0]
    elif l["nm"] == "letter_O": # g
        l["ks"]["a"]["k"] = [letter_crops[3]["w"] / 2, letter_crops[3]["h"] / 2, 0]
    elif l["nm"] == "letter_R": # o
        l["ks"]["a"]["k"] = [letter_crops[4]["w"] / 2, letter_crops[4]["h"] / 2, 0]
    elif l["nm"] == "jackpot": # Limpio
        l["ks"]["a"]["k"] = [limpio_canvas_w / 2, limpio_canvas_h / 2, 0]

# Guardar todo
with open(out_json_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_raw_path, "w", encoding="utf-8") as f:
    json.dump(lottie, f, indent=2)

with open(out_js_path, "w", encoding="utf-8") as f:
    f.write("window.ADAPTED_JACKPOT_DATA = " + json.dumps(lottie) + ";\n")

print("Generado 100% idéntico al Composable oficial de WelcomeScreen.kt!")

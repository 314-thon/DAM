import math
import json
import base64

# Creamos un SVG nítido y de calidad con el logo y estética de "Juego Limpio" en estilo Major Jackpot
svg_markup = '''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500" width="500" height="500">
  <defs>
    <!-- Filtros de Glow Neón -->
    <filter id="glow-cyan" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="8" result="blur"/>
      <feMerge>
        <feMergeNode in="blur"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
    
    <filter id="glow-pink" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="10" result="blur"/>
      <feMerge>
        <feMergeNode in="blur"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>

    <!-- Gradientes -->
    <linearGradient id="grad-badge" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#2a1236" />
      <stop offset="50%" stop-color="#140d1f" />
      <stop offset="100%" stop-color="#0b0814" />
    </linearGradient>

    <linearGradient id="grad-cyan" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#00E5FF" />
      <stop offset="50%" stop-color="#38E1FF" />
      <stop offset="100%" stop-color="#70F0FF" />
    </linearGradient>

    <linearGradient id="grad-magenta" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#FF2A7A" />
      <stop offset="50%" stop-color="#E91E63" />
      <stop offset="100%" stop-color="#9C27B0" />
    </linearGradient>

    <linearGradient id="grad-gold" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#FFE600" />
      <stop offset="100%" stop-color="#FFAE00" />
    </linearGradient>
  </defs>

  <!-- Aura Resplandeciente -->
  <ellipse cx="250" cy="250" rx="190" ry="120" fill="#E91E63" opacity="0.15" filter="url(#glow-pink)"/>
  <ellipse cx="250" cy="250" rx="160" ry="90" fill="#00E5FF" opacity="0.12" filter="url(#glow-cyan)"/>

  <!-- Borde Exterior Neón Pulido -->
  <rect x="55" y="125" width="390" height="250" rx="36" fill="none" stroke="#E91E63" stroke-width="6" opacity="0.8" filter="url(#glow-pink)"/>

  <!-- Placa Principal Estilo Casino / Arcade Badge -->
  <rect x="62" y="132" width="376" height="236" rx="30" fill="url(#grad-badge)" stroke="#00E5FF" stroke-width="3.5" filter="url(#glow-cyan)"/>
  <rect x="70" y="140" width="360" height="220" rx="24" fill="none" stroke="rgba(255,255,255,0.12)" stroke-width="1.5"/>

  <!-- Cinta Superior "MAJOR" Banner -->
  <path d="M 160 115 L 340 115 L 325 152 L 175 152 Z" fill="#E91E63" filter="url(#glow-pink)"/>
  <rect x="180" y="122" width="140" height="22" rx="6" fill="#140D1F" />
  
  <!-- Estrellas en la cinta -->
  <polygon points="194,129 196,135 202,135 197,138 199,144 194,140 189,144 191,138 186,135 192,135" fill="#FFE600"/>
  <text x="250" y="138" font-family="'Space Grotesk', system-ui, sans-serif" font-weight="900" font-size="12" fill="#00E5FF" text-anchor="middle" letter-spacing="3">100% PREVENTIVO</text>
  <polygon points="306,129 308,135 314,135 309,138 311,144 306,140 301,144 303,138 298,135 304,135" fill="#FFE600"/>

  <!-- Texto 1: JUEGO -->
  <text x="250" y="222" font-family="'Space Grotesk', system-ui, sans-serif" font-weight="900" font-size="62" fill="url(#grad-cyan)" text-anchor="middle" letter-spacing="4" filter="url(#glow-cyan)">JUEGO</text>

  <!-- Divisor Neón Central -->
  <line x1="110" y1="242" x2="390" y2="242" stroke="#E91E63" stroke-width="3" opacity="0.6"/>
  <circle cx="250" cy="242" r="6" fill="#FFE600" filter="url(#glow-cyan)"/>

  <!-- Texto 2: LIMPIO -->
  <text x="250" y="318" font-family="'Space Grotesk', system-ui, sans-serif" font-weight="900" font-size="68" fill="url(#grad-magenta)" text-anchor="middle" letter-spacing="5" filter="url(#glow-pink)">LIMPIO</text>

  <!-- Subtítulo inferior -->
  <rect x="150" y="338" width="200" height="20" rx="10" fill="rgba(0, 229, 255, 0.12)" stroke="#00E5FF" stroke-width="1"/>
  <text x="250" y="352" font-family="'Space Grotesk', system-ui, sans-serif" font-weight="700" font-size="11" fill="#FFFFFF" text-anchor="middle" letter-spacing="2">SIN APUESTAS • DEUDA $0</text>
</svg>'''

# Convertir SVG a Data URI Base64
svg_b64 = "data:image/svg+xml;base64," + base64.b64encode(svg_markup.encode('utf-8')).decode('utf-8')

# Creamos la animación Lottie que mueve y hace flotar el badge con física de Evanthia
total_frames = 120
fps = 60

# Sparkle 1: Top Right
# Sparkle 2: Bottom Left
lottie_anim = {
  "v": "5.7.4",
  "fr": fps,
  "ip": 0,
  "op": total_frames,
  "w": 500,
  "h": 500,
  "nm": "Juego Limpio - Major Jackpot Style",
  "ddd": 0,
  "assets": [
    {
      "id": "image_badge",
      "w": 500,
      "h": 500,
      "u": "",
      "p": svg_b64,
      "e": 1
    }
  ],
  "layers": [
    # Sparkle 1 (Estrella dorada de 4 puntas giratoria arriba a la derecha)
    {
      "ddd": 0,
      "ind": 1,
      "ty": 4,
      "nm": "Gold Sparkle Top",
      "sr": 1,
      "ks": {
        "o": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0], "e": [100] },
            { "t": 20, "s": [100], "e": [0] },
            { "t": 40, "s": [0] },
            { "t": 60, "s": [0], "e": [100] },
            { "t": 80, "s": [100], "e": [0] },
            { "t": 100, "s": [0] }
          ]
        },
        "r": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0], "e": [90] },
            { "t": 40, "s": [90], "e": [180] },
            { "t": 80, "s": [180], "e": [270] },
            { "t": 120, "s": [270] }
          ]
        },
        "p": { "a": 0, "k": [410, 115, 0] },
        "a": { "a": 0, "k": [0, 0, 0] },
        "s": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0, 0, 100], "e": [130, 130, 100] },
            { "t": 20, "s": [130, 130, 100], "e": [0, 0, 100] },
            { "t": 40, "s": [0, 0, 100] },
            { "t": 60, "s": [0, 0, 100], "e": [120, 120, 100] },
            { "t": 80, "s": [120, 120, 100], "e": [0, 0, 100] },
            { "t": 100, "s": [0, 0, 100] }
          ]
        }
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "sr",
              "sy": 1,
              "pt": { "a": 0, "k": 4 },
              "p": { "a": 0, "k": [0, 0] },
              "r": { "a": 0, "k": 0 },
              "ir": { "a": 0, "k": 7 },
              "is": { "a": 0, "k": 0 },
              "or": { "a": 0, "k": 26 },
              "os": { "a": 0, "k": 0 }
            },
            {
              "ty": "fl",
              "c": { "a": 0, "k": [1, 0.9, 0.1, 1] },
              "o": { "a": 0, "k": 100 }
            },
            {
              "ty": "tr",
              "p": { "a": 0, "k": [0, 0] },
              "a": { "a": 0, "k": [0, 0] },
              "s": { "a": 0, "k": [100, 100] },
              "r": { "a": 0, "k": 0 },
              "o": { "a": 0, "k": 100 }
            }
          ]
        }
      ],
      "ip": 0,
      "op": 120,
      "st": 0
    },
    # Sparkle 2 (Cyan Sparkle abajo a la izquierda)
    {
      "ddd": 0,
      "ind": 2,
      "ty": 4,
      "nm": "Cyan Sparkle Bottom",
      "sr": 1,
      "ks": {
        "o": {
          "a": 1,
          "k": [
            { "t": 25, "s": [0], "e": [100] },
            { "t": 45, "s": [100], "e": [0] },
            { "t": 65, "s": [0] },
            { "t": 85, "s": [0], "e": [100] },
            { "t": 105, "s": [100], "e": [0] }
          ]
        },
        "r": {
          "a": 1,
          "k": [
            { "t": 0, "s": [0], "e": [-180] },
            { "t": 120, "s": [-180] }
          ]
        },
        "p": { "a": 0, "k": [85, 385, 0] },
        "a": { "a": 0, "k": [0, 0, 0] },
        "s": {
          "a": 1,
          "k": [
            { "t": 25, "s": [0, 0, 100], "e": [110, 110, 100] },
            { "t": 45, "s": [110, 110, 100], "e": [0, 0, 100] },
            { "t": 65, "s": [0, 0, 100] },
            { "t": 85, "s": [0, 0, 100], "e": [110, 110, 100] },
            { "t": 105, "s": [110, 110, 100], "e": [0, 0, 100] }
          ]
        }
      },
      "shapes": [
        {
          "ty": "gr",
          "it": [
            {
              "ty": "sr",
              "sy": 1,
              "pt": { "a": 0, "k": 4 },
              "p": { "a": 0, "k": [0, 0] },
              "r": { "a": 0, "k": 0 },
              "ir": { "a": 0, "k": 5 },
              "is": { "a": 0, "k": 0 },
              "or": { "a": 0, "k": 20 },
              "os": { "a": 0, "k": 0 }
            },
            {
              "ty": "fl",
              "c": { "a": 0, "k": [0.0, 0.9, 1.0, 1] },
              "o": { "a": 0, "k": 100 }
            },
            {
              "ty": "tr",
              "p": { "a": 0, "k": [0, 0] },
              "a": { "a": 0, "k": [0, 0] },
              "s": { "a": 0, "k": [100, 100] },
              "r": { "a": 0, "k": 0 },
              "o": { "a": 0, "k": 100 }
            }
          ]
        }
      ],
      "ip": 0,
      "op": 120,
      "st": 0
    },
    # Layer 3: La Placa / Logo con animación física (Levitación + Balanceo suave)
    {
      "ddd": 0,
      "ind": 3,
      "ty": 2,
      "nm": "Jackpot Emblem",
      "refId": "image_badge",
      "sr": 1,
      "ks": {
        "o": { "a": 0, "k": 100 },
        "r": {
          "a": 1,
          "k": [
            { "t": 0, "s": [-2], "e": [2], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 60, "s": [2], "e": [-2], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 120, "s": [-2] }
          ]
        },
        "p": {
          "a": 1,
          "k": [
            { "t": 0, "s": [250, 250, 0], "e": [250, 238, 0], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 60, "s": [250, 238, 0], "e": [250, 250, 0], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 120, "s": [250, 250, 0] }
          ]
        },
        "a": { "a": 0, "k": [250, 250, 0] },
        "s": {
          "a": 1,
          "k": [
            { "t": 0, "s": [98, 98, 100], "e": [103, 103, 100], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 60, "s": [103, 103, 100], "e": [98, 98, 100], "i": { "x": [0.4], "y": [1] }, "o": { "x": [0.4], "y": [0] } },
            { "t": 120, "s": [98, 98, 100] }
          ]
        }
      },
      "ao": 0,
      "ip": 0,
      "op": 120,
      "st": 0,
      "bm": 0
    }
  ]
}

# Guardamos en JSON y JS
with open("d:/github/DAM/lottie-studio/juego_limpio_jackpot.json", "w", encoding="utf-8") as f:
    json.dump(lottie_anim, f, indent=2)

with open("d:/github/DAM/app/src/main/res/raw/juego_limpio_jackpot.json", "w", encoding="utf-8") as f:
    json.dump(lottie_anim, f, indent=2)

with open("d:/github/DAM/lottie-studio/jackpot-data.js", "w", encoding="utf-8") as f:
    f.write("window.JUEGO_LIMPIO_JACKPOT_DATA = " + json.dumps(lottie_anim) + ";\n")

print("Generado correctamente con render garantizado!")

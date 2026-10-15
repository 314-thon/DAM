/**
 * Lottie Studio Local - Motor de Manipulación y Generador PNG a Lottie
 */

// Estado global de la aplicación
const state = {
  currentAnimation: null,
  currentAnimationData: null,
  isPaused: true,
  isLoop: true,
  currentSpeed: 1.0,
  uploadedImageBase64: null,
  uploadedImageWidth: 500,
  uploadedImageHeight: 500,
  fileName: "animation"
};

// Elementos DOM
const dropzone = document.getElementById("dropzone");
const fileInput = document.getElementById("fileInput");
const lottieStage = document.getElementById("lottieStage");
const emptyState = document.getElementById("emptyState");
const btnPlayPause = document.getElementById("btnPlayPause");
const labelPlay = document.getElementById("labelPlay");
const btnStop = document.getElementById("btnStop");
const btnPrevFrame = document.getElementById("btnPrevFrame");
const btnNextFrame = document.getElementById("btnNextFrame");
const btnLoop = document.getElementById("btnLoop");
const timelineScrubber = document.getElementById("timelineScrubber");
const currentFrameText = document.getElementById("currentFrameText");
const totalFramesText = document.getElementById("totalFramesText");
const btnDownload = document.getElementById("btnDownload");
const btnLoadOriginal = document.getElementById("btnLoadOriginal");
const btnLoadJackpot = document.getElementById("btnLoadJackpot");
const badgeFileLoaded = document.getElementById("badgeFileLoaded");
const btnGeneratePngAnim = document.getElementById("btnGeneratePngAnim");
const animPreset = document.getElementById("animPreset");
const animDuration = document.getElementById("animDuration");
const animFps = document.getElementById("animFps");
const animIntensity = document.getElementById("animIntensity");
const intensityVal = document.getElementById("intensityVal");

// Setup Dropzone & Uploads
dropzone.addEventListener("click", () => fileInput.click());

dropzone.addEventListener("dragover", (e) => {
  e.preventDefault();
  dropzone.classList.add("border-cyan-400", "bg-slate-900/60");
});

dropzone.addEventListener("dragleave", () => {
  dropzone.classList.remove("border-cyan-400", "bg-slate-900/60");
});

dropzone.addEventListener("drop", (e) => {
  e.preventDefault();
  dropzone.classList.remove("border-cyan-400", "bg-slate-900/60");
  if (e.dataTransfer.files && e.dataTransfer.files[0]) {
    handleFile(e.dataTransfer.files[0]);
  }
});

fileInput.addEventListener("change", (e) => {
  if (e.target.files && e.target.files[0]) {
    handleFile(e.target.files[0]);
  }
});

animIntensity.addEventListener("input", (e) => {
  intensityVal.innerText = `${e.target.value}%`;
});

// Manejador de archivos (PNG o JSON)
function handleFile(file) {
  const extension = file.name.split('.').pop().toLowerCase();
  state.fileName = file.name.substring(0, file.name.lastIndexOf('.')) || "animation";

  if (extension === "json") {
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const json = JSON.parse(event.target.result);
        loadLottieData(json);
        badgeFileLoaded.innerText = file.name;
        badgeFileLoaded.classList.add("text-emerald-400");
      } catch (err) {
        alert("Error al parsear el JSON de Lottie: " + err.message);
      }
    };
    reader.readAsText(file);
  } else if (["png", "jpg", "jpeg", "webp", "svg"].includes(extension)) {
    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        state.uploadedImageBase64 = event.target.result;
        state.uploadedImageWidth = img.width || 500;
        state.uploadedImageHeight = img.height || 500;
        badgeFileLoaded.innerText = `${file.name} (${img.width}x${img.height})`;
        badgeFileLoaded.classList.add("text-cyan-400");
        // Generar animación automáticamente con el preset seleccionado
        generateLottieFromImage();
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  } else {
    alert("Formato no soportado. Selecciona un archivo .png o .json de Lottie.");
  }
}

// Cargar y Renderizar Lottie
function loadLottieData(animData) {
  state.currentAnimationData = animData;

  // Destruir anterior si existe
  if (state.currentAnimation) {
    state.currentAnimation.destroy();
  }

  emptyState.classList.add("hidden");

  state.currentAnimation = lottie.loadAnimation({
    container: lottieStage,
    renderer: 'svg',
    loop: state.isLoop,
    autoplay: true,
    animationData: animData
  });

  state.isPaused = false;
  labelPlay.innerText = "Pause";
  btnDownload.removeAttribute("disabled");

  // Metadata
  document.getElementById("metaVersion").innerText = animData.v || "5.x";
  document.getElementById("metaDimensions").innerText = `${animData.w || 500} x ${animData.h || 500} px`;
  document.getElementById("metaFrames").innerText = Math.round(animData.op - animData.ip) || animData.op || "-";
  document.getElementById("metaFps").innerText = `${animData.fr || 60} fps`;
  document.getElementById("metaLayers").innerText = animData.layers ? animData.layers.length : 0;

  // Setup Timeline
  const totalFrames = animData.op - animData.ip;
  timelineScrubber.max = totalFrames;
  timelineScrubber.value = 0;
  totalFramesText.innerText = `${Math.round(totalFrames)}f`;

  state.currentAnimation.addEventListener("enterFrame", (e) => {
    timelineScrubber.value = Math.round(e.currentTime);
    currentFrameText.innerText = `${Math.round(e.currentTime)}f`;
  });

  state.currentAnimation.setSpeed(state.currentSpeed);
}

// Generador de Animaciones Lottie desde PNG
function generateLottieFromImage() {
  if (!state.uploadedImageBase64) {
    alert("Primero debes subir un PNG o imagen.");
    return;
  }

  const preset = animPreset.value;
  const duration = parseFloat(animDuration.value) || 2.0;
  const fps = parseInt(animFps.value) || 60;
  const totalFrames = Math.round(duration * fps);
  const factor = (parseInt(animIntensity.value) || 100) / 100;

  const w = 500;
  const h = 500;
  const cx = w / 2;
  const cy = h / 2;

  // Calculamos escala proporcional para que entre en 300x300
  const maxDim = Math.max(state.uploadedImageWidth, state.uploadedImageHeight);
  const targetSize = 280;
  const baseScale = (targetSize / maxDim) * 100;

  let posProp = { a: 0, k: [cx, cy, 0] };
  let scaleProp = { a: 0, k: [baseScale, baseScale, 100] };
  let rotProp = { a: 0, k: 0 };
  let opacityProp = { a: 0, k: 100 };

  // Curvas de animación (Lottie Keyframes)
  if (preset === "bounce") {
    // Rebote vertical con Squash & Stretch
    const f0 = 0;
    const f1 = Math.round(totalFrames * 0.25);
    const f2 = Math.round(totalFrames * 0.5);
    const f3 = Math.round(totalFrames * 0.75);
    const f4 = totalFrames;

    const bounceHeight = 60 * factor;
    posProp = {
      a: 1,
      k: [
        { t: f0, s: [cx, cy, 0], e: [cx, cy - bounceHeight, 0], i: { x: [0.2], y: [1] }, o: { x: [0.8], y: [0] } },
        { t: f1, s: [cx, cy - bounceHeight, 0], e: [cx, cy, 0], i: { x: [0.8], y: [0] }, o: { x: [0.2], y: [1] } },
        { t: f2, s: [cx, cy, 0], e: [cx, cy - bounceHeight * 0.4, 0], i: { x: [0.2], y: [1] }, o: { x: [0.8], y: [0] } },
        { t: f3, s: [cx, cy - bounceHeight * 0.4, 0], e: [cx, cy, 0], i: { x: [0.8], y: [0] }, o: { x: [0.2], y: [1] } },
        { t: f4, s: [cx, cy, 0] }
      ]
    };
    scaleProp = {
      a: 1,
      k: [
        { t: f0, s: [baseScale * 1.15, baseScale * 0.85, 100], e: [baseScale * 0.95, baseScale * 1.05, 100] },
        { t: f1, s: [baseScale * 0.95, baseScale * 1.05, 100], e: [baseScale * 1.2, baseScale * 0.8, 100] },
        { t: f2, s: [baseScale * 1.2, baseScale * 0.8, 100], e: [baseScale, baseScale, 100] },
        { t: f4, s: [baseScale, baseScale, 100] }
      ]
    };
  } else if (preset === "float") {
    // Flotación suave (Levitation)
    const mid = Math.round(totalFrames / 2);
    const floatDist = 25 * factor;
    posProp = {
      a: 1,
      k: [
        { t: 0, s: [cx, cy, 0], e: [cx, cy - floatDist, 0], i: { x: [0.4], y: [1] }, o: { x: [0.4], y: [0] } },
        { t: mid, s: [cx, cy - floatDist, 0], e: [cx, cy, 0], i: { x: [0.4], y: [1] }, o: { x: [0.4], y: [0] } },
        { t: totalFrames, s: [cx, cy, 0] }
      ]
    };
    rotProp = {
      a: 1,
      k: [
        { t: 0, s: [-3 * factor], e: [3 * factor] },
        { t: mid, s: [3 * factor], e: [-3 * factor] },
        { t: totalFrames, s: [-3 * factor] }
      ]
    };
  } else if (preset === "pulse") {
    // Latido / Escala
    const mid = Math.round(totalFrames / 2);
    const scaleUp = baseScale * (1 + 0.18 * factor);
    scaleProp = {
      a: 1,
      k: [
        { t: 0, s: [baseScale, baseScale, 100], e: [scaleUp, scaleUp, 100], i: { x: [0.4], y: [1] }, o: { x: [0.4], y: [0] } },
        { t: mid, s: [scaleUp, scaleUp, 100], e: [baseScale, baseScale, 100], i: { x: [0.4], y: [1] }, o: { x: [0.4], y: [0] } },
        { t: totalFrames, s: [baseScale, baseScale, 100] }
      ]
    };
  } else if (preset === "spin") {
    // Rotación Continua 360°
    rotProp = {
      a: 1,
      k: [
        { t: 0, s: [0], e: [360] },
        { t: totalFrames, s: [360] }
      ]
    };
  } else if (preset === "wobble") {
    // Balanceo pendular
    const f1 = Math.round(totalFrames * 0.25);
    const f2 = Math.round(totalFrames * 0.5);
    const f3 = Math.round(totalFrames * 0.75);
    const deg = 18 * factor;
    rotProp = {
      a: 1,
      k: [
        { t: 0, s: [0], e: [deg] },
        { t: f1, s: [deg], e: [-deg] },
        { t: f2, s: [-deg], e: [deg * 0.5] },
        { t: f3, s: [deg * 0.5], e: [0] },
        { t: totalFrames, s: [0] }
      ]
    };
  } else if (preset === "pop") {
    // Pop In con rebote
    scaleProp = {
      a: 1,
      k: [
        { t: 0, s: [0, 0, 100], e: [baseScale * 1.25, baseScale * 1.25, 100], i: { x: [0.2], y: [1] }, o: { x: [0.8], y: [0] } },
        { t: Math.round(totalFrames * 0.4), s: [baseScale * 1.25, baseScale * 1.25, 100], e: [baseScale * 0.9, baseScale * 0.9, 100] },
        { t: Math.round(totalFrames * 0.7), s: [baseScale * 0.9, baseScale * 0.9, 100], e: [baseScale, baseScale, 100] },
        { t: totalFrames, s: [baseScale, baseScale, 100] }
      ]
    };
    opacityProp = {
      a: 1,
      k: [
        { t: 0, s: [0], e: [100] },
        { t: Math.round(totalFrames * 0.3), s: [100] }
      ]
    };
  }

  // Estructura oficial del JSON de Lottie (Bodymovin) con Asset PNG integrado
  const lottieJson = {
    v: "5.7.4",
    fr: fps,
    ip: 0,
    op: totalFrames,
    w: w,
    h: h,
    nm: state.fileName,
    ddd: 0,
    assets: [
      {
        id: "image_0",
        w: state.uploadedImageWidth,
        h: state.uploadedImageHeight,
        u: "",
        p: state.uploadedImageBase64,
        e: 1
      }
    ],
    layers: [
      {
        ddd: 0,
        ind: 1,
        ty: 2, // Layer type 2 = Image
        nm: "PNG Layer",
        refId: "image_0",
        sr: 1,
        ks: {
          o: opacityProp,
          r: rotProp,
          p: posProp,
          a: { a: 0, k: [state.uploadedImageWidth / 2, state.uploadedImageHeight / 2, 0] },
          s: scaleProp
        },
        ao: 0,
        ip: 0,
        op: totalFrames,
        st: 0,
        bm: 0
      }
    ]
  };

  loadLottieData(lottieJson);
}

btnGeneratePngAnim.addEventListener("click", generateLottieFromImage);

// Botones de Reproducción
btnPlayPause.addEventListener("click", () => {
  if (!state.currentAnimation) return;
  if (state.isPaused) {
    state.currentAnimation.play();
    state.isPaused = false;
    labelPlay.innerText = "Pause";
  } else {
    state.currentAnimation.pause();
    state.isPaused = true;
    labelPlay.innerText = "Play";
  }
});

btnStop.addEventListener("click", () => {
  if (!state.currentAnimation) return;
  state.currentAnimation.stop();
  state.isPaused = true;
  labelPlay.innerText = "Play";
  timelineScrubber.value = 0;
  currentFrameText.innerText = "0f";
});

btnPrevFrame.addEventListener("click", () => {
  if (!state.currentAnimation) return;
  state.currentAnimation.pause();
  state.isPaused = true;
  labelPlay.innerText = "Play";
  const frame = Math.max(0, state.currentAnimation.currentFrame - 1);
  state.currentAnimation.goToAndStop(frame, true);
});

btnNextFrame.addEventListener("click", () => {
  if (!state.currentAnimation) return;
  state.currentAnimation.pause();
  state.isPaused = true;
  labelPlay.innerText = "Play";
  const frame = Math.min(state.currentAnimation.totalFrames - 1, state.currentAnimation.currentFrame + 1);
  state.currentAnimation.goToAndStop(frame, true);
});

btnLoop.addEventListener("click", () => {
  state.isLoop = !state.isLoop;
  if (state.currentAnimation) {
    state.currentAnimation.loop = state.isLoop;
  }
  btnLoop.innerText = `Loop: ${state.isLoop ? 'ON' : 'OFF'}`;
  btnLoop.classList.toggle("text-cyan-400", state.isLoop);
  btnLoop.classList.toggle("text-slate-500", !state.isLoop);
});

timelineScrubber.addEventListener("input", (e) => {
  if (!state.currentAnimation) return;
  state.currentAnimation.pause();
  state.isPaused = true;
  labelPlay.innerText = "Play";
  state.currentAnimation.goToAndStop(parseFloat(e.target.value), true);
});

// Velocidad
document.querySelectorAll(".btnSpeed").forEach(btn => {
  btn.addEventListener("click", (e) => {
    document.querySelectorAll(".btnSpeed").forEach(b => {
      b.classList.remove("bg-slate-800", "text-cyan-400", "font-bold");
      b.classList.add("text-slate-400");
    });
    btn.classList.add("bg-slate-800", "text-cyan-400", "font-bold");
    btn.classList.remove("text-slate-400");
    state.currentSpeed = parseFloat(btn.dataset.speed);
    if (state.currentAnimation) {
      state.currentAnimation.setSpeed(state.currentSpeed);
    }
  });
});

// Fondo
document.querySelectorAll(".btnBg").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".btnBg").forEach(b => {
      b.className = "btnBg flex-1 py-1.5 text-xs rounded border border-slate-700 bg-slate-900 text-slate-400 hover:text-slate-200";
    });
    btn.className = "btnBg flex-1 py-1.5 text-xs rounded border border-cyan-500 bg-slate-800 text-cyan-400 font-medium";

    const previewContainer = document.getElementById("previewContainer");
    previewContainer.classList.remove("grid-pattern", "bg-slate-950", "bg-white");
    if (btn.dataset.bg === "grid") {
      previewContainer.classList.add("grid-pattern", "bg-slate-950");
    } else if (btn.dataset.bg === "dark") {
      previewContainer.classList.add("bg-slate-950");
    } else if (btn.dataset.bg === "light") {
      previewContainer.classList.add("bg-white");
    }
  });
});

// Descargar Lottie JSON
btnDownload.addEventListener("click", () => {
  if (!state.currentAnimationData) return;
  const jsonStr = JSON.stringify(state.currentAnimationData, null, 2);
  const blob = new Blob([jsonStr], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `${state.fileName || "animation"}.json`;
  a.click();
  URL.revokeObjectURL(url);
});

// Cargar Original Evanthia
if (btnLoadOriginal) {
  btnLoadOriginal.addEventListener("click", () => {
    if (window.ORIGINAL_JACKPOT_DATA) {
      state.fileName = "major-jackpot-evanthia";
      badgeFileLoaded.innerText = "major-jackpot.json (Original)";
      badgeFileLoaded.className = "text-[10px] text-amber-400 truncate max-w-[140px]";
      loadLottieData(window.ORIGINAL_JACKPOT_DATA);
    }
  });
}

// Cargar Logo Adaptado a Juego Limpio
btnLoadJackpot.addEventListener("click", () => {
  if (window.ADAPTED_JACKPOT_DATA) {
    state.fileName = "juego_limpio_jackpot";
    badgeFileLoaded.innerText = "juego_limpio_jackpot.json";
    badgeFileLoaded.className = "text-[10px] text-pink-400 truncate max-w-[140px]";
    loadLottieData(window.ADAPTED_JACKPOT_DATA);
  }
});

// Demo Sample
btnSample.addEventListener("click", () => {
  // Animación vectorial pura nativa Lottie de demostración
  const sampleData = {
    v: "5.5.2",
    fr: 60,
    ip: 0,
    op: 120,
    w: 400,
    h: 400,
    nm: "Demo Star",
    layers: [
      {
        ddd: 0,
        ind: 1,
        ty: 4,
        nm: "Star Shape",
        sr: 1,
        ks: {
          o: { a: 0, k: 100 },
          r: {
            a: 1,
            k: [
              { t: 0, s: [0], e: [360] },
              { t: 120, s: [360] }
            ]
          },
          p: { a: 0, k: [200, 200, 0] },
          a: { a: 0, k: [0, 0, 0] },
          s: {
            a: 1,
            k: [
              { t: 0, s: [80, 80, 100], e: [120, 120, 100] },
              { t: 60, s: [120, 120, 100], e: [80, 80, 100] },
              { t: 120, s: [80, 80, 100] }
            ]
          }
        },
        shapes: [
          {
            ty: "gr",
            it: [
              {
                ty: "sr",
                sy: 1,
                pt: { a: 0, k: 5 },
                p: { a: 0, k: [0, 0] },
                r: { a: 0, k: 0 },
                ir: { a: 0, k: 40 },
                is: { a: 0, k: 0 },
                or: { a: 0, k: 90 },
                os: { a: 0, k: 0 }
              },
              {
                ty: "fl",
                c: { a: 0, k: [0.06, 0.78, 0.9, 1] }, // Cyan color
                o: { a: 0, k: 100 }
              },
              {
                ty: "tr",
                p: { a: 0, k: [0, 0] },
                a: { a: 0, k: [0, 0] },
                s: { a: 0, k: [100, 100] },
                r: { a: 0, k: 0 },
                o: { a: 0, k: 100 }
              }
            ]
          }
        ],
        ip: 0,
        op: 120,
        st: 0
      }
    ]
  };
  state.fileName = "demo-star";
  badgeFileLoaded.innerText = "demo-star.json";
  loadLottieData(sampleData);
});

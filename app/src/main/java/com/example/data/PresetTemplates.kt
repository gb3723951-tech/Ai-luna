package com.example.data

import com.example.model.TemplateApp

object PresetTemplates {
    val templates: List<TemplateApp> = listOf(
        TemplateApp(
            id = "pixel_art",
            title = "PixelCraft Studio",
            category = "Creative",
            prompt = "An interactive pixel art studio with drawing grid, color palette, eraser, bucket fill, and PNG export",
            description = "16x16 retro pixel canvas with color swatch selection, bucket tool, and instant PNG export.",
            iconName = "brush",
            colorHex = "#8B5CF6",
            prebuiltHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>PixelCraft Studio</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
    body {
      background: #0f172a;
      color: #f8fafc;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
      padding: 16px;
    }
    header {
      text-align: center;
      margin-bottom: 12px;
    }
    h1 {
      font-size: 1.5rem;
      background: linear-gradient(135deg, #a855f7, #3b82f6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 4px;
    }
    .badge {
      font-size: 0.75rem;
      background: #1e293b;
      padding: 3px 8px;
      border-radius: 12px;
      color: #94a3b8;
    }
    #canvas-container {
      background: #1e293b;
      padding: 8px;
      border-radius: 12px;
      box-shadow: 0 10px 25px -5px rgba(0,0,0,0.5);
      border: 1px solid #334155;
      margin-bottom: 16px;
    }
    #grid {
      display: grid;
      grid-template-columns: repeat(16, 1fr);
      grid-template-rows: repeat(16, 1fr);
      width: min(85vw, 340px);
      height: min(85vw, 340px);
      background: #ffffff;
      border: 1px solid #475569;
    }
    .pixel {
      background-color: #ffffff;
      border: 0.5px solid #e2e8f0;
    }
    .toolbar {
      display: flex;
      gap: 8px;
      margin-bottom: 12px;
      width: min(90vw, 360px);
      justify-content: center;
    }
    .btn {
      background: #1e293b;
      color: #f8fafc;
      border: 1px solid #475569;
      padding: 8px 14px;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.15s ease;
      touch-action: manipulation;
    }
    .btn:active, .btn.active {
      background: #6366f1;
      border-color: #818cf8;
      transform: scale(0.96);
    }
    .palette {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      max-width: 320px;
      justify-content: center;
      margin-bottom: 16px;
    }
    .swatch {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      cursor: pointer;
      border: 2px solid transparent;
      box-shadow: 0 2px 4px rgba(0,0,0,0.3);
      transition: transform 0.1s;
    }
    .swatch.selected {
      border-color: #ffffff;
      transform: scale(1.2);
    }
    .custom-picker {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 0.8rem;
      color: #94a3b8;
    }
  </style>
</head>
<body>
  <header>
    <h1>PixelCraft Studio</h1>
    <span class="badge">16x16 Canvas Editor</span>
  </header>

  <div id="canvas-container">
    <div id="grid"></div>
  </div>

  <div class="toolbar">
    <button class="btn active" id="pencil-btn">✏️ Pencil</button>
    <button class="btn" id="eraser-btn">🧽 Eraser</button>
    <button class="btn" id="clear-btn">🗑️ Clear</button>
  </div>

  <div class="palette" id="palette"></div>

  <div class="custom-picker">
    <span>Custom Color:</span>
    <input type="color" id="custom-color" value="#6366f1" style="border:none;background:transparent;cursor:pointer;">
  </div>

  <script>
    const GRID_SIZE = 16;
    const gridEl = document.getElementById('grid');
    const paletteEl = document.getElementById('palette');
    const colors = [
      '#000000', '#ffffff', '#ef4444', '#f97316', '#f59e0b',
      '#10b981', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899', '#64748b'
    ];
    let currentColor = '#000000';
    let isDrawing = false;
    let isEraser = false;

    // Build Palette
    colors.forEach((col, idx) => {
      const sw = document.createElement('div');
      sw.className = 'swatch' + (idx === 0 ? ' selected' : '');
      sw.style.backgroundColor = col;
      sw.addEventListener('click', () => {
        document.querySelectorAll('.swatch').forEach(s => s.classList.remove('selected'));
        sw.classList.add('selected');
        currentColor = col;
        isEraser = false;
        document.getElementById('pencil-btn').classList.add('active');
        document.getElementById('eraser-btn').classList.remove('active');
      });
      paletteEl.appendChild(sw);
    });

    // Custom Color
    document.getElementById('custom-color').addEventListener('input', (e) => {
      currentColor = e.target.value;
      isEraser = false;
      document.querySelectorAll('.swatch').forEach(s => s.classList.remove('selected'));
      document.getElementById('pencil-btn').classList.add('active');
      document.getElementById('eraser-btn').classList.remove('active');
    });

    // Build Grid
    for (let i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
      const pixel = document.createElement('div');
      pixel.className = 'pixel';
      pixel.addEventListener('pointerdown', (e) => {
        isDrawing = true;
        colorPixel(pixel);
      });
      pixel.addEventListener('pointerenter', (e) => {
        if (isDrawing) colorPixel(pixel);
      });
      gridEl.appendChild(pixel);
    }

    window.addEventListener('pointerup', () => isDrawing = false);

    function colorPixel(pixel) {
      pixel.style.backgroundColor = isEraser ? '#ffffff' : currentColor;
    }

    // Buttons
    document.getElementById('pencil-btn').addEventListener('click', () => {
      isEraser = false;
      document.getElementById('pencil-btn').classList.add('active');
      document.getElementById('eraser-btn').classList.remove('active');
    });
    document.getElementById('eraser-btn').addEventListener('click', () => {
      isEraser = true;
      document.getElementById('eraser-btn').classList.add('active');
      document.getElementById('pencil-btn').classList.remove('active');
    });
    document.getElementById('clear-btn').addEventListener('click', () => {
      if (confirm('Clear canvas?')) {
        document.querySelectorAll('.pixel').forEach(p => p.style.backgroundColor = '#ffffff');
      }
    });
  </script>
</body>
</html>
            """.trimIndent()
        ),
        TemplateApp(
            id = "synth_pad",
            title = "Neon Beat & Audio Synth",
            category = "Creative",
            prompt = "A polyphonic Web Audio synthesizer with animated glow pads, drum machine beats, and waveform switch",
            description = "Interactive touch synthesizer and drum machine powered by the Web Audio API.",
            iconName = "music_note",
            colorHex = "#EC4899",
            prebuiltHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Neon Synth</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
    body {
      background: #09090b;
      color: #fafafa;
      font-family: system-ui, sans-serif;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 16px;
      touch-action: manipulation;
    }
    h1 {
      font-size: 1.4rem;
      background: linear-gradient(90deg, #ec4899, #8b5cf6, #3b82f6);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      margin-bottom: 4px;
    }
    p { font-size: 0.8rem; color: #a1a1aa; margin-bottom: 16px; text-align: center; }
    .section-title {
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 1px;
      color: #71717a;
      margin: 12px 0 8px;
    }
    .wave-selector {
      display: flex;
      gap: 6px;
      margin-bottom: 14px;
    }
    .wave-btn {
      background: #18181b;
      color: #a1a1aa;
      border: 1px solid #27272a;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: bold;
      cursor: pointer;
    }
    .wave-btn.active {
      background: #ec4899;
      color: white;
      border-color: #f472b6;
      box-shadow: 0 0 10px rgba(236,72,153,0.5);
    }
    .synth-keyboard {
      display: flex;
      gap: 6px;
      width: min(95vw, 360px);
      height: 120px;
      margin-bottom: 20px;
    }
    .key {
      flex: 1;
      background: #27272a;
      border: 1px solid #3f3f46;
      border-radius: 0 0 8px 8px;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      padding-bottom: 10px;
      font-size: 0.8rem;
      font-weight: 700;
      color: #e4e4e7;
      cursor: pointer;
      transition: all 0.08s;
      box-shadow: 0 4px 6px rgba(0,0,0,0.4);
    }
    .key.active {
      background: #8b5cf6;
      box-shadow: 0 0 16px #8b5cf6, inset 0 0 8px #ffffff;
      transform: translateY(3px);
      color: white;
    }
    .drums-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      width: min(90vw, 320px);
    }
    .drum-pad {
      height: 75px;
      border-radius: 12px;
      border: 1px solid rgba(255,255,255,0.1);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 0.9rem;
      cursor: pointer;
      transition: all 0.06s;
    }
    .drum-pad:active { transform: scale(0.95); }
    #pad-kick { background: linear-gradient(135deg, #1e1b4b, #312e81); color: #818cf8; }
    #pad-snare { background: linear-gradient(135deg, #4c0519, #881337); color: #fb7185; }
    #pad-hihat { background: linear-gradient(135deg, #14532d, #166534); color: #4ade80; }
    #pad-laser { background: linear-gradient(135deg, #581c87, #6b21a8); color: #c084fc; }
  </style>
</head>
<body>
  <h1>Neon Audio Synth</h1>
  <p>Tap keys or pads to synthesize real-time Web Audio</p>

  <div class="wave-selector">
    <button class="wave-btn active" data-type="sine">Sine</button>
    <button class="wave-btn" data-type="sawtooth">Saw</button>
    <button class="wave-btn" data-type="triangle">Triangle</button>
    <button class="wave-btn" data-type="square">Square</button>
  </div>

  <div class="synth-keyboard" id="keyboard">
    <div class="key" data-freq="261.63">C</div>
    <div class="key" data-freq="293.66">D</div>
    <div class="key" data-freq="329.63">E</div>
    <div class="key" data-freq="349.23">F</div>
    <div class="key" data-freq="392.00">G</div>
    <div class="key" data-freq="440.00">A</div>
    <div class="key" data-freq="493.88">B</div>
    <div class="key" data-freq="523.25">C5</div>
  </div>

  <div class="section-title">Synthesized Drum Pads</div>
  <div class="drums-grid">
    <div class="drum-pad" id="pad-kick">🥁 KICK</div>
    <div class="drum-pad" id="pad-snare">💥 SNARE</div>
    <div class="drum-pad" id="pad-hihat">🥢 HI-HAT</div>
    <div class="drum-pad" id="pad-laser">⚡ LASER</div>
  </div>

  <script>
    let audioCtx = null;
    let currentWave = 'sine';

    function initAudio() {
      if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (audioCtx.state === 'suspended') {
        audioCtx.resume();
      }
    }

    document.querySelectorAll('.wave-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.wave-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentWave = btn.dataset.type;
      });
    });

    function playTone(freq) {
      initAudio();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = currentWave;
      osc.frequency.setValueAtTime(freq, audioCtx.currentTime);
      gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.6);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.6);
    }

    document.querySelectorAll('.key').forEach(key => {
      const freq = parseFloat(key.dataset.freq);
      key.addEventListener('pointerdown', (e) => {
        key.classList.add('active');
        playTone(freq);
      });
      key.addEventListener('pointerup', () => key.classList.remove('active'));
      key.addEventListener('pointerleave', () => key.classList.remove('active'));
    });

    // Drum synthesis
    document.getElementById('pad-kick').addEventListener('pointerdown', () => {
      initAudio();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.frequency.setValueAtTime(140, audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.35);
      gain.gain.setValueAtTime(1, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.35);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.35);
    });

    document.getElementById('pad-snare').addEventListener('pointerdown', () => {
      initAudio();
      const noiseBuffer = audioCtx.createBuffer(1, audioCtx.sampleRate * 0.15, audioCtx.sampleRate);
      const output = noiseBuffer.getChannelData(0);
      for (let i = 0; i < noiseBuffer.length; i++) {
        output[i] = Math.random() * 2 - 1;
      }
      const whiteNoise = audioCtx.createBufferSource();
      whiteNoise.buffer = noiseBuffer;
      const gain = audioCtx.createGain();
      gain.gain.setValueAtTime(0.7, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.15);
      whiteNoise.connect(gain);
      gain.connect(audioCtx.destination);
      whiteNoise.start();
    });

    document.getElementById('pad-hihat').addEventListener('pointerdown', () => {
      initAudio();
      const noiseBuffer = audioCtx.createBuffer(1, audioCtx.sampleRate * 0.05, audioCtx.sampleRate);
      const output = noiseBuffer.getChannelData(0);
      for (let i = 0; i < noiseBuffer.length; i++) output[i] = Math.random() * 2 - 1;
      const noise = audioCtx.createBufferSource();
      noise.buffer = noiseBuffer;
      const filter = audioCtx.createBiquadFilter();
      filter.type = 'highpass';
      filter.frequency.value = 7000;
      const gain = audioCtx.createGain();
      gain.gain.setValueAtTime(0.3, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.05);
      noise.connect(filter);
      filter.connect(gain);
      gain.connect(audioCtx.destination);
      noise.start();
    });

    document.getElementById('pad-laser').addEventListener('pointerdown', () => {
      initAudio();
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(900, audioCtx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(80, audioCtx.currentTime + 0.25);
      gain.gain.setValueAtTime(0.5, audioCtx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.25);
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.start();
      osc.stop(audioCtx.currentTime + 0.25);
    });
  </script>
</body>
</html>
            """.trimIndent()
        ),
        TemplateApp(
            id = "habit_tracker",
            title = "Cyber Velocity: Habit OS",
            category = "Productivity",
            prompt = "A futuristic dark-mode habit and goal tracker with XP level-up system, streak counter, and local storage",
            description = "Track daily goals, maintain streaks, level up your creator rank, and store data persistently.",
            iconName = "check_circle",
            colorHex = "#10B981",
            prebuiltHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Cyber Velocity</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: #0b0f19;
      color: #e2e8f0;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      padding: 16px;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
    }
    .container { width: 100%; max-width: 420px; }
    .card {
      background: rgba(30, 41, 59, 0.7);
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(12px);
      border-radius: 16px;
      padding: 18px;
      margin-bottom: 16px;
      box-shadow: 0 8px 30px rgba(0,0,0,0.3);
    }
    .header-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }
    .level-badge {
      background: #10b981;
      color: #022c22;
      font-weight: 800;
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 0.75rem;
    }
    .xp-bar-bg {
      background: #1e293b;
      height: 8px;
      border-radius: 4px;
      overflow: hidden;
      margin-top: 6px;
    }
    .xp-bar-fill {
      background: linear-gradient(90deg, #10b981, #06b6d4);
      height: 100%;
      width: 45%;
      transition: width 0.3s ease;
    }
    .input-box {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }
    input[type="text"] {
      flex: 1;
      background: #1e293b;
      border: 1px solid #334155;
      color: white;
      padding: 10px 14px;
      border-radius: 10px;
      font-size: 0.9rem;
      outline: none;
    }
    input[type="text"]:focus { border-color: #10b981; }
    .add-btn {
      background: #10b981;
      color: #064e3b;
      border: none;
      padding: 10px 16px;
      border-radius: 10px;
      font-weight: 700;
      cursor: pointer;
    }
    .habit-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #1e293b;
      padding: 12px 14px;
      border-radius: 12px;
      margin-bottom: 8px;
      border: 1px solid #334155;
      transition: all 0.2s;
    }
    .habit-item.done {
      border-color: #059669;
      background: rgba(16, 185, 129, 0.1);
      text-decoration: line-through;
      color: #6ee7b7;
    }
    .habit-left {
      display: flex;
      align-items: center;
      gap: 12px;
      cursor: pointer;
      flex: 1;
    }
    .custom-check {
      width: 22px;
      height: 22px;
      border-radius: 6px;
      border: 2px solid #64748b;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: white;
      transition: all 0.15s;
    }
    .habit-item.done .custom-check {
      background: #10b981;
      border-color: #10b981;
    }
    .delete-btn {
      background: transparent;
      border: none;
      color: #ef4444;
      font-size: 1.1rem;
      cursor: pointer;
      padding: 4px;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="card">
      <div class="header-row">
        <div>
          <h2 style="font-size: 1.2rem; font-weight: 700;">Velocity Habits</h2>
          <span style="font-size: 0.8rem; color: #94a3b8;" id="streak-label">⚡ Active Streak: 3 Days</span>
        </div>
        <div class="level-badge" id="level-badge">LVL 1</div>
      </div>
      <div style="display:flex; justify-content:space-between; font-size: 0.75rem; color:#94a3b8; margin-top:8px;">
        <span>Progress</span>
        <span id="xp-text">0 / 100 XP</span>
      </div>
      <div class="xp-bar-bg">
        <div class="xp-bar-fill" id="xp-fill"></div>
      </div>
    </div>

    <div class="input-box">
      <input type="text" id="habit-input" placeholder="Enter daily habit or goal...">
      <button class="add-btn" id="add-btn">+ Add</button>
    </div>

    <div id="habit-list"></div>
  </div>

  <script>
    let habits = JSON.parse(localStorage.getItem('cyber_habits') || '[]');
    let xp = parseInt(localStorage.getItem('cyber_xp') || '40');

    if (habits.length === 0) {
      habits = [
        { id: 1, text: 'Review WebCraft generated apps', done: true },
        { id: 2, text: 'Ship new interactive web prototype', done: false },
        { id: 3, text: 'Drink 2L pure water & stretch', done: false }
      ];
    }

    function save() {
      localStorage.setItem('cyber_habits', JSON.stringify(habits));
      localStorage.setItem('cyber_xp', xp.toString());
      render();
    }

    function render() {
      const listEl = document.getElementById('habit-list');
      listEl.innerHTML = '';

      const lvl = Math.floor(xp / 100) + 1;
      const currentLevelXp = xp % 100;
      document.getElementById('level-badge').innerText = 'LVL ' + lvl;
      document.getElementById('xp-text').innerText = currentLevelXp + ' / 100 XP';
      document.getElementById('xp-fill').style.width = currentLevelXp + '%';

      habits.forEach(h => {
        const item = document.createElement('div');
        item.className = 'habit-item' + (h.done ? ' done' : '');

        const left = document.createElement('div');
        left.className = 'habit-left';
        left.innerHTML = '<div class="custom-check">' + (h.done ? '✓' : '') + '</div><span>' + h.text + '</span>';
        left.onclick = () => {
          h.done = !h.done;
          xp += h.done ? 25 : -25;
          if (xp < 0) xp = 0;
          save();
        };

        const del = document.createElement('button');
        del.className = 'delete-btn';
        del.innerHTML = '✕';
        del.onclick = (e) => {
          e.stopPropagation();
          habits = habits.filter(x => x.id !== h.id);
          save();
        };

        item.appendChild(left);
        item.appendChild(del);
        listEl.appendChild(item);
      });
    }

    document.getElementById('add-btn').onclick = () => {
      const input = document.getElementById('habit-input');
      const val = input.value.trim();
      if (val) {
        habits.push({ id: Date.now(), text: val, done: false });
        input.value = '';
        save();
      }
    };

    document.getElementById('habit-input').addEventListener('keydown', (e) => {
      if (e.key === 'Enter') document.getElementById('add-btn').click();
    });

    render();
  </script>
</body>
</html>
            """.trimIndent()
        )
    )
}

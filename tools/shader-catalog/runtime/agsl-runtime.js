/* global window, document */
// Browser-side WebGL2 runtime shared by the thumbnail renderer (Playwright)
// and the GitHub Pages gallery. Plain script — no bundler needed.
//
// window.Agsl = { createRunner, renderLesson, painters, argbToVec4 }
(function () {
  const VERTEX_SHADER = `#version 300 es
void main() {
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2) * 2.0 - 1.0;
    gl_Position = vec4(p, 0.0, 1.0);
}`;

  const UNIFORM_RX = /uniform\s+(float|int|vec2|vec3|vec4|ivec2|sampler2D)\s+(\w+)\s*(?:\[\s*(\d+)\s*\])?\s*;/g;

  function stripComments(src) {
    return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
  }

  function argbToVec4(hex) {
    const h = hex.replace('#', '');
    const a = parseInt(h.slice(0, 2), 16) / 255;
    const r = parseInt(h.slice(2, 4), 16) / 255;
    const g = parseInt(h.slice(4, 6), 16) / 255;
    const b = parseInt(h.slice(6, 8), 16) / 255;
    return [r, g, b, a];
  }

  function createRunner(canvas, options) {
    const gl = canvas.getContext('webgl2', Object.assign({
      premultipliedAlpha: false,
      alpha: true,
      antialias: false,
      preserveDrawingBuffer: true,
    }, options || {}));
    if (!gl) throw new Error('WebGL2 is not available');

    const vao = gl.createVertexArray();
    gl.bindVertexArray(vao);
    const programs = new Map();

    function compile(type, src) {
      const sh = gl.createShader(type);
      gl.shaderSource(sh, src);
      gl.compileShader(sh);
      if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
        const log = gl.getShaderInfoLog(sh);
        gl.deleteShader(sh);
        throw new Error(log);
      }
      return sh;
    }

    const vertex = compile(gl.VERTEX_SHADER, VERTEX_SHADER);

    /** Compile + link a fragment program (cached by key). */
    function program(key, glsl) {
      if (programs.has(key)) return programs.get(key);
      const fragment = compile(gl.FRAGMENT_SHADER, glsl);
      const prog = gl.createProgram();
      gl.attachShader(prog, vertex);
      gl.attachShader(prog, fragment);
      gl.linkProgram(prog);
      if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) throw new Error(gl.getProgramInfoLog(prog));
      const uniforms = new Map();
      for (const m of stripComments(glsl).matchAll(UNIFORM_RX)) {
        uniforms.set(m[2], { type: m[1], size: m[3] ? Number(m[3]) : 1, loc: gl.getUniformLocation(prog, m[2]) });
      }
      const entry = { prog, uniforms };
      programs.set(key, entry);
      return entry;
    }

    function texture(source, w, h) {
      const tex = gl.createTexture();
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      if (source) gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
      else gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
      return tex;
    }

    function updateTexture(tex, source) {
      gl.bindTexture(gl.TEXTURE_2D, tex);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
    }

    function target(w, h) {
      const tex = texture(null, w, h);
      const fbo = gl.createFramebuffer();
      gl.bindFramebuffer(gl.FRAMEBUFFER, fbo);
      gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
      gl.bindFramebuffer(gl.FRAMEBUFFER, null);
      return { fbo, tex, w, h };
    }

    /**
     * Draw one full-viewport pass.
     * @param entry   result of program()
     * @param values  { uniformName: number | number[] | Float32Array | {texture} }
     * @param opts    { target, viewport:[x,y,w,h], clear:[r,g,b,a], blend:boolean, contentFlip:0|1 }
     */
    function draw(entry, values, opts) {
      opts = opts || {};
      const t = opts.target || null;
      gl.bindFramebuffer(gl.FRAMEBUFFER, t ? t.fbo : null);
      const W = t ? t.w : gl.drawingBufferWidth;
      const H = t ? t.h : gl.drawingBufferHeight;
      const vp = opts.viewport || [0, 0, W, H];
      gl.viewport(vp[0], vp[1], vp[2], vp[3]);
      if (opts.clear) {
        gl.disable(gl.SCISSOR_TEST);
        gl.clearColor(opts.clear[0], opts.clear[1], opts.clear[2], opts.clear[3]);
        gl.clear(gl.COLOR_BUFFER_BIT);
      }
      if (opts.blend) {
        gl.enable(gl.BLEND);
        gl.blendFuncSeparate(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA, gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
      } else {
        gl.disable(gl.BLEND);
      }
      gl.useProgram(entry.prog);
      let unit = 0;
      const all = Object.assign({
        agslx_canvas: [vp[2], vp[3]],
        agslx_origin: [vp[0], vp[1]],
        agslx_contentFlip: opts.contentFlip || 0,
      }, values);
      for (const [name, u] of entry.uniforms) {
        const v = all[name];
        if (v === undefined || u.loc === null) continue;
        switch (u.type) {
          case 'float':
            if (u.size > 1 || (typeof v !== 'number' && v.length > 1 && u.size > 1)) gl.uniform1fv(u.loc, v);
            else gl.uniform1f(u.loc, typeof v === 'number' ? v : v[0]);
            break;
          case 'int': gl.uniform1i(u.loc, v); break;
          case 'vec2': gl.uniform2f(u.loc, v[0], v[1]); break;
          case 'vec3': gl.uniform3f(u.loc, v[0], v[1], v[2]); break;
          case 'vec4': gl.uniform4f(u.loc, v[0], v[1], v[2], v[3]); break;
          case 'ivec2': gl.uniform2i(u.loc, v[0], v[1]); break;
          case 'sampler2D':
            gl.activeTexture(gl.TEXTURE0 + unit);
            gl.bindTexture(gl.TEXTURE_2D, v.texture || v);
            gl.uniform1i(u.loc, unit);
            unit++;
            break;
          default: break;
        }
      }
      gl.drawArrays(gl.TRIANGLES, 0, 3);
      gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    }

    return { gl, canvas, program, texture, updateTexture, target, draw, programs };
  }

  // -------------------------------------------------------------------------
  // 2D painters that stand in for the Compose content a RenderEffect samples.
  // -------------------------------------------------------------------------
  const painters = {
    /** SampleContent(): gradient + translucent card (ui/feature/common/SampleContent.kt). */
    sampleCard(ctx, w, h) {
      const s = Math.min(w, h) / 340; // ≈ px per dp for a 340dp preview
      const g = ctx.createLinearGradient(0, 0, w, h);
      g.addColorStop(0, '#0F2027');
      g.addColorStop(0.5, '#203A43');
      g.addColorStop(1, '#2C5364');
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, w, h);
      const pad = 24 * s;
      const inner = 24 * s;
      const title = 24 * s;
      const body = 14 * s;
      const cardW = w - pad * 2;
      const lines = wrap(ctx, 'AGSL post-effect applied via RenderEffect on this composable.', `${body}px sans-serif`, cardW - inner * 2);
      const cardH = inner * 2 + title * 1.25 + lines.length * body * 1.4 + 6 * s;
      const x = pad;
      const y = (h - cardH) / 2;
      ctx.fillStyle = 'rgba(16,20,24,0.8)';
      roundRect(ctx, x, y, cardW, cardH, 16 * s);
      ctx.fill();
      ctx.fillStyle = '#FFFFFF';
      ctx.font = `bold ${title}px sans-serif`;
      ctx.textBaseline = 'top';
      ctx.fillText('Sample Card', x + inner, y + inner);
      ctx.fillStyle = 'rgba(255,255,255,0.8)';
      ctx.font = `${body}px sans-serif`;
      lines.forEach((line, i) => ctx.fillText(line, x + inner, y + inner + title * 1.25 + 6 * s + i * body * 1.4));
    },

    /** RippleBackdrop(): black, indigo nebula, starfield, green halo, Android bot, caption. */
    rippleBackdrop(ctx, w, h, opts) {
      opts = opts || {};
      const t = opts.time || 0;
      const breath = ((t * 1000) % 7000) / 7000 * Math.PI * 2;
      ctx.fillStyle = '#000';
      ctx.fillRect(0, 0, w, h);
      const neb = ctx.createRadialGradient(w / 2, h / 2, 0, w / 2, h / 2, Math.max(w, h) * 0.85);
      neb.addColorStop(0, 'rgba(56,44,122,0)');
      neb.addColorStop(0.55, 'rgba(56,44,122,0)');
      neb.addColorStop(1, 'rgba(56,44,122,0.08)');
      ctx.fillStyle = neb;
      ctx.fillRect(0, 0, w, h);
      // Stars — deterministic pseudo-random constellation, slow upward drift.
      let seed = 12345;
      const rnd = () => { seed = (seed * 1664525 + 1013904223) >>> 0; return seed / 4294967296; };
      const layers = [[50, 0.5, 1.2, 0.3, 0.55, 8], [35, 1.0, 1.8, 0.55, 0.8, 15], [15, 1.5, 2.6, 0.8, 1.0, 24]];
      const s = Math.min(w, h) / 400;
      for (const [count, r0, r1, a0, a1, speed] of layers) {
        for (let i = 0; i < count; i++) {
          const warm = rnd() < 0.06;
          const x = rnd() * w;
          const y0 = rnd() * h;
          const r = (r0 + (r1 - r0) * rnd()) * s;
          const a = a0 + (a1 - a0) * rnd();
          const y = (((y0 - speed * s * t) % h) + h) % h;
          ctx.fillStyle = warm ? `rgba(255,233,196,${a})` : `rgba(255,255,255,${a})`;
          ctx.beginPath();
          ctx.arc(x, y, r, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      // Halo + bot, centred; caption below.
      const botSize = Math.min(w, h) * (opts.botScale || 0.55);
      const cx = w / 2;
      const cy = h / 2 - botSize * 0.08;
      const haloAlpha = 0.10 + 0.08 * Math.sin(breath);
      const halo = ctx.createRadialGradient(cx, cy, 0, cx, cy, botSize * 0.67);
      halo.addColorStop(0, `rgba(61,220,132,${haloAlpha})`);
      halo.addColorStop(1, 'rgba(61,220,132,0)');
      ctx.globalCompositeOperation = 'lighter';
      ctx.fillStyle = halo;
      ctx.fillRect(cx - botSize, cy - botSize, botSize * 2, botSize * 2);
      ctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = 0.88 + 0.12 * Math.sin(breath);
      painters.androidBot(ctx, cx - botSize / 2, cy - botSize / 2, botSize, opts.gaze || [0.15, 0.1]);
      ctx.globalAlpha = 0.55 + 0.20 * Math.sin(breath + Math.PI / 2);
      ctx.fillStyle = '#FFF';
      ctx.font = `300 ${11 * s}px sans-serif`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'top';
      if (ctx.letterSpacing !== undefined) ctx.letterSpacing = `${6 * s}px`;
      ctx.fillText('TAP TO DISTURB THE SURFACE', cx + 3 * s, cy + botSize / 2 + 28 * s);
      if (ctx.letterSpacing !== undefined) ctx.letterSpacing = '0px';
      ctx.textAlign = 'left';
      ctx.globalAlpha = 1;
    },

    /** Port of AndroidBot.drawAndroidBot — 5u × 6u grid. */
    androidBot(ctx, x0, y0, size, gaze) {
      const color = '#3DDC84';
      const w = size;
      const h = size;
      const unit = Math.min(w / 5, h / 6.5) * 0.95;
      const ox = x0 + (w - 5 * unit) / 2;
      const oy = y0 + (h - 6 * unit) / 2;
      const cx = ox + 2.5 * unit;
      ctx.strokeStyle = color;
      ctx.fillStyle = color;
      ctx.lineCap = 'round';
      ctx.lineWidth = unit * 0.12;
      ctx.beginPath();
      ctx.moveTo(ox + 1.85 * unit, oy + 0.05 * unit); ctx.lineTo(ox + 1.6 * unit, oy + 0.5 * unit);
      ctx.moveTo(ox + 3.15 * unit, oy + 0.05 * unit); ctx.lineTo(ox + 3.4 * unit, oy + 0.5 * unit);
      ctx.stroke();
      const headR = 2 * unit;
      const headFlatY = oy + 2.5 * unit;
      ctx.beginPath();
      ctx.arc(cx, headFlatY, headR, Math.PI, 0);
      ctx.closePath();
      ctx.fill();
      const bodyTop = headFlatY + 0.05 * unit;
      const bodyBottom = bodyTop + 2.5 * unit;
      const bodyLeft = ox + 1 * unit;
      const bodyRight = ox + 4 * unit;
      const corner = 0.45 * unit;
      ctx.beginPath();
      ctx.moveTo(bodyLeft, bodyTop);
      ctx.lineTo(bodyRight, bodyTop);
      ctx.lineTo(bodyRight, bodyBottom - corner);
      ctx.arcTo(bodyRight, bodyBottom, bodyRight - corner, bodyBottom, corner);
      ctx.lineTo(bodyLeft + corner, bodyBottom);
      ctx.arcTo(bodyLeft, bodyBottom, bodyLeft, bodyBottom - corner, corner);
      ctx.closePath();
      ctx.fill();
      const armW = 0.7 * unit, armH = 2 * unit, armTop = bodyTop + 0.15 * unit, armGap = 0.18 * unit;
      roundRect(ctx, bodyLeft - armGap - armW, armTop, armW, armH, armW / 2); ctx.fill();
      roundRect(ctx, bodyRight + armGap, armTop, armW, armH, armW / 2); ctx.fill();
      const legW = 0.7 * unit, legH = 1.2 * unit, legTop = bodyBottom - 0.05 * unit, legInset = 0.6 * unit;
      roundRect(ctx, bodyLeft + legInset, legTop, legW, legH, legW / 2); ctx.fill();
      roundRect(ctx, bodyRight - legInset - legW, legTop, legW, legH, legW / 2); ctx.fill();
      const sclera = 0.32 * unit;
      const pupil = sclera * 0.55;
      const maxOff = sclera - pupil;
      const eyeOff = 0.95 * unit;
      const eyeY = headFlatY - 0.85 * unit;
      for (const ex of [cx - eyeOff, cx + eyeOff]) {
        ctx.fillStyle = '#FFF';
        ctx.beginPath(); ctx.arc(ex, eyeY, sclera, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = '#0E0E12';
        ctx.beginPath(); ctx.arc(ex + maxOff * gaze[0], eyeY + maxOff * gaze[1], pupil, 0, Math.PI * 2); ctx.fill();
      }
      ctx.fillStyle = color;
    },
  };

  function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function wrap(ctx, text, font, maxW) {
    ctx.font = font;
    const words = text.split(' ');
    const lines = [];
    let line = '';
    for (const word of words) {
      const trial = line ? `${line} ${word}` : word;
      if (ctx.measureText(trial).width > maxW && line) { lines.push(line); line = word; }
      else line = trial;
    }
    if (line) lines.push(line);
    return lines;
  }

  // -------------------------------------------------------------------------
  // Lesson orchestration — knows how each render mode is wired in the app.
  // -------------------------------------------------------------------------

  /**
   * @param runner  createRunner() result
   * @param lesson  catalog entry with .glsl (+ .extraGlsl for showcases)
   * @param state   { time, touch: {x,y,t}|null, values: {uniform: number|'#AARRGGBB'}, ripples: Float32Array(64) }
   */
  function renderLesson(runner, lesson, state) {
    const W = runner.gl.drawingBufferWidth;
    const H = runner.gl.drawingBufferHeight;
    const values = uniformValues(lesson, state, W, H);

    if (lesson.renderMode === 'RENDER_EFFECT') {
      const scratch = scratchCanvas(runner, W, H);
      painters.sampleCard(scratch.ctx, W, H);
      runner.updateTexture(scratch.tex, scratch.canvas);
      values.content = scratch.tex;
      runner.draw(runner.program(lesson.id, lesson.glsl), values, { clear: [0, 0, 0, 0] });
      return;
    }

    if (lesson.id === 'showcase-05-ripple-on-tap') {
      const scratch = scratchCanvas(runner, W, H);
      painters.rippleBackdrop(scratch.ctx, W, H, { time: state.time, gaze: state.gaze });
      runner.updateTexture(scratch.tex, scratch.canvas);
      values.content = scratch.tex;
      values.rip = state.ripples || emptyRipples();
      runner.draw(runner.program(lesson.id, lesson.glsl), values, { clear: [0, 0, 0, 1] });
      return;
    }

    if (lesson.id === 'showcase-06-codex-splash') {
      // Pass 1: atmosphere → offscreen. Pass 2: SDF icon tile, centred, alpha-blended.
      // Pass 3: water RenderEffect samples the composed scene (a FBO, so flip=1).
      const scene = runner._codexTarget && runner._codexTarget.w === W && runner._codexTarget.h === H
        ? runner._codexTarget : (runner._codexTarget = runner.target(W, H));
      runner.draw(runner.program(lesson.id + ':bg', lesson.glsl), { resolution: [W, H], time: state.time }, { target: scene, clear: [0, 0, 0, 1] });
      const icon = Math.round(Math.min(W, H) * (W > H * 1.2 ? 0.55 : 0.62));
      const ix = Math.round((W - icon) / 2);
      const iy = Math.round((H - icon) / 2);
      runner.draw(runner.program(lesson.id + ':icon', lesson.extraGlsl[0]), { resolution: [icon, icon], time: state.time }, { target: scene, viewport: [ix, iy, icon, icon], blend: true });
      runner.draw(runner.program(lesson.id + ':water', lesson.extraGlsl[1]), {
        content: scene.tex,
        iResolution: [W, H],
        iTime: state.time,
        rip: state.ripples || emptyRipples(),
      }, { clear: [0, 0, 0, 1], contentFlip: 1 });
      return;
    }

    runner.draw(runner.program(lesson.id, lesson.glsl), values, { clear: [0, 0, 0, 0] });
  }

  function emptyRipples() {
    const arr = new Float32Array(64);
    for (let i = 0; i < 16; i++) { arr[i * 4 + 2] = -1000; arr[i * 4 + 3] = 0; }
    return arr;
  }

  function scratchCanvas(runner, W, H) {
    if (!runner._scratch || runner._scratch.canvas.width !== W || runner._scratch.canvas.height !== H) {
      const canvas = document.createElement('canvas');
      canvas.width = W;
      canvas.height = H;
      runner._scratch = { canvas, ctx: canvas.getContext('2d'), tex: runner.texture(null, W, H) };
    }
    return runner._scratch;
  }

  function uniformValues(lesson, state, W, H) {
    const values = {};
    const touch = state.touch;
    for (const u of lesson.uniforms) {
      switch (u.name) {
        case 'resolution': values.resolution = [W, H]; break;
        case 'iResolution': values.iResolution = [W, H]; break;
        case 'time': values.time = state.time; break;
        case 'iTime': values.iTime = state.time; break;
        case 'touchPos': values.touchPos = touch ? [touch.x, touch.y] : [-1, -1]; break;
        case 'touchTime': values.touchTime = touch ? touch.t : -1; break;
        default: break;
      }
    }
    for (const c of lesson.controls) {
      const v = state.values && state.values[c.uniform] !== undefined ? state.values[c.uniform] : c.default;
      values[c.uniform] = c.type === 'color' ? argbToVec4(v) : v;
    }
    return values;
  }

  window.Agsl = { createRunner, renderLesson, painters, argbToVec4, emptyRipples };
})();

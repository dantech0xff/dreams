#!/usr/bin/env node
// Generates docs/index.html — a self-contained, static gallery of every lesson
// with live WebGL2 previews (AGSL transpiled to GLSL ES 3.00), sliders for the
// lesson controls, tap interaction, the AGSL source, and deep links to the
// Kotlin files. Serves from GitHub Pages (/docs) or straight from file://.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { transpileAgsl } from './agsl-to-glsl.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');
const OUT = path.join(ROOT, 'docs', 'index.html');
const REPO = 'https://github.com/dantech0xff/dreams';
const BRANCH = process.env.DREAMS_BRANCH || 'master';

const catalog = JSON.parse(fs.readFileSync(path.join(ROOT, 'docs', 'catalog', 'lessons.json'), 'utf8'));
const runtimeJs = fs.readFileSync(path.join(HERE, 'runtime', 'agsl-runtime.js'), 'utf8');

// Category accents mirror ui/theme/Color.kt.
const ACCENT = {
  BASICS: '#8CFF80', PATTERNS: '#CFFF5E', COLOR: '#FF4FD8', SDF: '#35F6FF', NOISE: '#72F6B1', MOTION: '#B7FF6A',
  FRACTALS: '#9AB6FF', LIGHTING: '#FFB000', INTERACTIVE: '#35F6FF', POSTFX: '#FF4FD8', SHOWCASE: '#FFC857',
};

const HERO = [
  'noise-06-warped-lava', 'fractals-02-julia', 'showcase-06-codex-splash', 'patterns-06-kaleidoscope-fold',
  'sdf-03-metaballs', 'showcase-05-ripple-on-tap', 'lighting-02-phong', 'noise-04-voronoi', 'motion-04-pendulum-chain',
];

const lessons = catalog.lessons.map((l) => {
  const main = transpileAgsl(l.agslSource);
  const extras = l.extraAgslSources.map((s) => transpileAgsl(s));
  return {
    id: l.id,
    title: l.title,
    category: l.category,
    complexity: l.complexity,
    conceptIntro: l.conceptIntro,
    learningNotes: l.learningNotes,
    controls: l.controls,
    renderMode: l.renderMode,
    screenRecordingHint: l.screenRecordingHint,
    uniforms: l.uniforms,
    agslSource: l.agslSource,
    extraAgslSources: l.extraAgslSources,
    glsl: main.glsl,
    extraGlsl: extras.map((e) => e.glsl),
    source: l.source,
    hasTouch: l.uniforms.some((u) => u.name === 'touchPos'),
    hasTime: l.uniforms.some((u) => u.name === 'time' || u.name === 'iTime'),
  };
});

const data = {
  repo: REPO,
  branch: BRANCH,
  categories: catalog.categories.map((c) => ({ ...c, accent: ACCENT[c.name] || '#8CFF80' })),
  lessons,
  hero: HERO,
};

// JSON inside <script> — escape the only dangerous sequence.
const dataJson = JSON.stringify(data).replace(/<\//g, '<\\/');

const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dreams — AGSL Engineer Playground · ${lessons.length} runnable shader lessons</title>
<meta name="description" content="Learn Android Graphics Shading Language (AGSL) with Kotlin and Jetpack Compose: ${lessons.length} runnable lessons with live previews, sliders and source.">
<meta property="og:title" content="Dreams — AGSL Engineer Playground">
<meta property="og:description" content="${lessons.length} runnable AGSL shader lessons for Jetpack Compose, previewed live in your browser.">
<meta property="og:image" content="gallery/poster-hero.png">
<link rel="icon" href="data:image/svg+xml,${encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32"><rect width="32" height="32" rx="8" fill="#04100B"/><path d="M6 20 L12 10 L18 18 L22 13 L26 20" fill="none" stroke="#8CFF80" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>')}">
<style>
:root{
  --bg:#04100B;--surface:#0B1812;--low:#102219;--high:#183428;--highest:#244A39;
  --ink:#E7FFF1;--mute:#8FB8A1;--line:#2F5F49;
  --green:#8CFF80;--cyan:#35F6FF;--amber:#FFC857;--magenta:#FF4FD8;--red:#FF5A5F;
  --mono:ui-monospace,SFMono-Regular,Menlo,Consolas,"Liberation Mono",monospace;
  --sans:system-ui,-apple-system,"Segoe UI",Roboto,Inter,sans-serif;
}
*{box-sizing:border-box}
html{color-scheme:dark;scroll-behavior:smooth}
body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 var(--sans);
  background-image:radial-gradient(1200px 500px at 20% -10%,rgba(140,255,128,.08),transparent 60%),
    radial-gradient(900px 400px at 90% 0%,rgba(53,246,255,.06),transparent 60%)}
a{color:var(--green)}a:hover{color:#fff}
button{font:inherit}
.wrap{max-width:1180px;margin:0 auto;padding:0 20px}
header.top{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:18px 0;border-bottom:1px solid var(--line)}
.brand{display:flex;align-items:center;gap:12px;text-decoration:none;color:var(--ink)}
.brand svg{width:34px;height:34px}
.brand b{font-size:18px;letter-spacing:.2px}.brand small{display:block;color:var(--mute);font-size:12px;font-family:var(--mono)}
nav.links{display:flex;gap:6px;flex-wrap:wrap}
nav.links a{padding:7px 12px;border:1px solid var(--line);border-radius:999px;color:var(--ink);text-decoration:none;font-size:13px;background:var(--surface)}
nav.links a:hover{border-color:var(--green)}
nav.links a.primary{background:var(--green);color:#04100B;border-color:var(--green);font-weight:600}

.hero{display:grid;grid-template-columns:1.1fr .9fr;gap:28px;align-items:center;padding:40px 0 28px}
@media (max-width:860px){.hero{grid-template-columns:1fr}}
.hero h1{font-size:clamp(30px,4.4vw,48px);line-height:1.08;margin:0 0 14px;letter-spacing:-.5px}
.hero h1 em{font-style:normal;color:var(--green)}
.hero p.lead{color:var(--mute);font-size:17px;margin:0 0 18px;max-width:56ch}
.stats{display:flex;gap:18px;flex-wrap:wrap;margin:16px 0 22px}
.stat{font-family:var(--mono);font-size:13px;color:var(--mute)}.stat b{display:block;font-size:24px;color:var(--ink);font-family:var(--sans)}
.cta{display:flex;gap:10px;flex-wrap:wrap}
.btn{display:inline-flex;align-items:center;gap:8px;padding:10px 16px;border-radius:12px;border:1px solid var(--line);background:var(--surface);color:var(--ink);text-decoration:none;font-weight:600;cursor:pointer}
.btn:hover{border-color:var(--green)}.btn.primary{background:var(--green);border-color:var(--green);color:#04100B}.btn.primary:hover{background:#a6ff9d}
.stage{position:relative;aspect-ratio:1;max-width:520px;width:100%;justify-self:center;border-radius:28px;overflow:hidden;
  box-shadow:0 30px 80px rgba(0,0,0,.55),0 0 0 1px var(--line);background:var(--high)}
.stage canvas,.stage img{position:absolute;inset:0;width:100%;height:100%;display:block;cursor:pointer}
.stage .cap{position:absolute;left:14px;right:14px;bottom:14px;display:flex;justify-content:space-between;align-items:center;gap:10px;
  padding:10px 14px;border-radius:14px;background:rgba(4,16,11,.72);backdrop-filter:blur(10px);font-family:var(--mono);font-size:12px;color:var(--mute);pointer-events:none}
.stage .cap b{color:var(--ink);font-family:var(--sans);font-size:14px}
.dot{display:inline-block;width:8px;height:8px;border-radius:50%;background:var(--green);box-shadow:0 0 10px var(--green)}
.live{display:inline-flex;align-items:center;gap:6px}

.toolbar{position:sticky;top:0;z-index:5;background:rgba(4,16,11,.85);backdrop-filter:blur(12px);border-bottom:1px solid var(--line);padding:12px 0}
.toolbar .row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.chips{display:flex;gap:6px;flex-wrap:wrap;flex:1}
.chip{border:1px solid var(--line);background:var(--surface);color:var(--ink);border-radius:999px;padding:6px 12px;font-size:13px;cursor:pointer;display:inline-flex;gap:8px;align-items:center}
.chip .c{width:8px;height:8px;border-radius:50%;background:var(--acc,var(--green))}
.chip small{color:var(--mute);font-family:var(--mono)}
.chip.on{background:var(--high);border-color:var(--acc,var(--green))}
.search{border:1px solid var(--line);background:var(--surface);color:var(--ink);border-radius:12px;padding:8px 12px;min-width:220px;font:inherit}
.search:focus{outline:none;border-color:var(--green)}

section.cat{padding:26px 0 6px}
section.cat h2{margin:0 0 4px;font-size:22px;display:flex;align-items:center;gap:10px}
section.cat h2 .c{width:10px;height:10px;border-radius:50%;background:var(--acc)}
section.cat p.tag{margin:0 0 14px;color:var(--mute)}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(210px,1fr));gap:14px}
.card{position:relative;background:var(--surface);border:1px solid var(--line);border-radius:18px;overflow:hidden;cursor:pointer;text-align:left;padding:0;color:inherit;
  transition:transform .18s ease,border-color .18s ease,box-shadow .18s ease}
.card:hover,.card:focus-visible{transform:translateY(-3px);border-color:var(--acc);box-shadow:0 14px 40px rgba(0,0,0,.45),0 0 0 1px var(--acc);outline:none}
.card img{display:block;width:100%;aspect-ratio:1;object-fit:cover;background:var(--high)}
.card .body{padding:11px 12px 12px}
.card h3{margin:0 0 4px;font-size:15px;line-height:1.3}
.card p{margin:0;color:var(--mute);font-size:12.5px;line-height:1.45;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.meta{display:flex;justify-content:space-between;align-items:center;margin-top:8px;font-family:var(--mono);font-size:11px;color:var(--acc)}
.bolts span{opacity:.25}.bolts span.on{opacity:1}
.badge{position:absolute;top:10px;left:10px;padding:3px 8px;border-radius:999px;background:rgba(4,16,11,.7);backdrop-filter:blur(6px);font-family:var(--mono);font-size:10.5px;color:var(--ink)}
.empty{color:var(--mute);padding:40px 0;text-align:center}

/* modal */
.modal{position:fixed;inset:0;z-index:20;display:none;align-items:center;justify-content:center;padding:20px;background:rgba(0,0,0,.72);backdrop-filter:blur(6px)}
.modal.open{display:flex}
.sheet{position:relative;width:min(1140px,100%);max-height:100%;overflow:auto;background:var(--surface);border:1px solid var(--line);border-radius:24px;
  display:grid;grid-template-columns:minmax(300px,520px) 1fr;gap:0}
@media (max-width:900px){.sheet{grid-template-columns:1fr}}
.pane{padding:22px}
.pane.left{border-right:1px solid var(--line);background:var(--low)}
@media (max-width:900px){.pane.left{border-right:0;border-bottom:1px solid var(--line)}}
.view{position:relative;aspect-ratio:1;border-radius:20px;overflow:hidden;background:linear-gradient(var(--highest),var(--high));box-shadow:0 18px 40px rgba(0,0,0,.4)}
.view canvas{width:100%;height:100%;display:block;touch-action:none;cursor:crosshair}
.view .hint{position:absolute;left:12px;bottom:12px;padding:6px 10px;border-radius:10px;background:rgba(4,16,11,.7);font-family:var(--mono);font-size:11px;color:var(--mute);pointer-events:none}
.view .err{position:absolute;inset:0;display:none;align-items:center;justify-content:center;padding:20px;color:var(--red);font-family:var(--mono);font-size:12px;white-space:pre-wrap;background:rgba(4,16,11,.9)}
.controls{margin-top:16px;display:grid;gap:10px}
.ctl{display:grid;grid-template-columns:1fr auto;gap:4px 12px;align-items:center;font-size:13px}
.ctl label{color:var(--ink)}.ctl output{font-family:var(--mono);color:var(--mute);font-size:12px}
.ctl input[type=range]{grid-column:1/-1;width:100%;accent-color:var(--acc)}
.ctl input[type=color]{width:44px;height:28px;border:1px solid var(--line);border-radius:8px;background:none;padding:0}
.rowbtns{display:flex;gap:8px;margin-top:14px;flex-wrap:wrap}
.mini{padding:7px 11px;border-radius:10px;border:1px solid var(--line);background:var(--surface);color:var(--ink);cursor:pointer;font-size:12.5px}
.mini:hover{border-color:var(--acc)}
.close{position:absolute;top:12px;right:12px;width:36px;height:36px;border-radius:50%;border:1px solid var(--line);background:var(--surface);color:var(--ink);cursor:pointer;font-size:18px;z-index:2}
.pane.right h2{margin:0 0 6px;font-size:26px;line-height:1.15;padding-right:44px}
.crumbs{font-family:var(--mono);font-size:12px;color:var(--acc);display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin-bottom:12px}
.crumbs .sep{color:var(--line)}
.intro{font-size:15.5px;color:var(--ink);margin:0 0 14px}
.notes{margin:0 0 14px;padding:12px 14px;background:var(--high);border-radius:12px}
.notes h4{margin:0 0 6px;font-size:12px;letter-spacing:.8px;text-transform:uppercase;color:var(--mute);font-family:var(--mono)}
.notes ul{margin:0;padding-left:18px;color:var(--ink);font-size:14px}
.hintline{font-size:13px;color:var(--mute);margin:0 0 14px}
.tabs{display:flex;gap:4px;border-bottom:1px solid var(--line);margin-bottom:10px}
.tab{padding:8px 12px;border:0;background:none;color:var(--mute);cursor:pointer;border-bottom:2px solid transparent;font-family:var(--mono);font-size:12.5px}
.tab.on{color:var(--ink);border-bottom-color:var(--acc)}
pre.code{margin:0;padding:14px 0;background:var(--bg);border:1px solid var(--line);border-radius:12px;overflow:auto;max-height:520px;font:12.5px/1.55 var(--mono);color:var(--ink)}
pre.code .ln{display:inline-block;width:3.2em;padding-right:1em;text-align:right;color:var(--mute);opacity:.7;user-select:none;padding-left:8px}
pre.code .k{color:var(--cyan)}pre.code .t{color:var(--green)}pre.code .n{color:var(--amber)}pre.code .c{color:var(--mute);font-style:italic}pre.code .f{color:var(--magenta)}
.srcactions{display:flex;gap:8px;margin-top:10px;flex-wrap:wrap}
.navlr{display:flex;justify-content:space-between;gap:10px;margin-top:18px;padding-top:14px;border-top:1px solid var(--line)}
footer{padding:36px 0 50px;color:var(--mute);font-size:13px;border-top:1px solid var(--line);margin-top:40px}
footer .row{display:flex;justify-content:space-between;gap:16px;flex-wrap:wrap}
kbd{font-family:var(--mono);font-size:11px;padding:1px 5px;border:1px solid var(--line);border-radius:4px;color:var(--mute)}
.nowebgl{display:none;margin:10px 0 0;padding:10px 12px;border:1px solid var(--amber);border-radius:12px;color:var(--amber);font-size:13px}
</style>
</head>
<body>
<div class="wrap">
  <header class="top">
    <a class="brand" href="#">
      <svg viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="8" fill="#0B1812" stroke="#2F5F49"/><path d="M6 20 L12 10 L18 18 L22 13 L26 20" fill="none" stroke="#8CFF80" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>
      <span><b>Dreams</b><small>AGSL Engineer Playground</small></span>
    </a>
    <nav class="links">
      <a href="${REPO}#readme">README</a>
      <a href="${REPO}/blob/${BRANCH}/docs/learning-path.md">Learning path</a>
      <a href="${REPO}/blob/${BRANCH}/docs/agsl-cheatsheet.md">AGSL cheatsheet</a>
      <a class="primary" href="${REPO}">GitHub ★</a>
    </nav>
  </header>

  <section class="hero">
    <div>
      <h1>Learn <em>AGSL</em> shaders<br>with Kotlin &amp; Compose.</h1>
      <p class="lead">${lessons.length} bite-sized, runnable fragment shaders — from a solid colour to fBM lava, SDF metaballs, Julia sets and RenderEffects that bend real Compose UI. Every lesson below runs <b>live in your browser</b>, transpiled from the exact AGSL source that ships in the Android app.</p>
      <div class="stats">
        <div class="stat"><b>${lessons.length}</b>lessons</div>
        <div class="stat"><b>${data.categories.length}</b>categories</div>
        <div class="stat"><b>${lessons.reduce((a, l) => a + l.controls.length, 0)}</b>live controls</div>
        <div class="stat"><b>MIT</b>licensed</div>
      </div>
      <div class="cta">
        <a class="btn primary" href="#lessons">Browse the lessons ↓</a>
        <a class="btn" href="${REPO}#build--run">Run it on a device</a>
      </div>
      <p class="nowebgl" id="nowebgl">Your browser has no WebGL2 — showing static thumbnails instead of live previews.</p>
    </div>
    <div class="stage" id="stage">
      <canvas id="heroCanvas" aria-label="Live shader preview"></canvas>
      <img id="heroImg" alt="" hidden>
      <div class="cap"><span><b id="heroTitle">—</b><br><span id="heroSub">—</span></span><span class="live"><span class="dot"></span>LIVE · click to open</span></div>
    </div>
  </section>

  <div class="toolbar" id="lessons">
    <div class="row">
      <div class="chips" id="chips"></div>
      <input class="search" id="search" type="search" placeholder="Search lessons… (fbm, sdf, touch)" aria-label="Search lessons">
    </div>
  </div>

  <main id="main"></main>

  <footer>
    <div class="row">
      <span>MIT · <a href="${REPO}">dantech0xff/dreams</a> · Previews are an AGSL→GLSL ES 3.00 transpile of the shipped source; colour management and <code>half</code> precision differ slightly from a real device.</span>
      <span>Generated by <a href="${REPO}/tree/${BRANCH}/tools/shader-catalog">tools/shader-catalog</a> · <kbd>←</kbd> <kbd>→</kbd> switch lesson · <kbd>Esc</kbd> close</span>
    </div>
  </footer>
</div>

<div class="modal" id="modal" role="dialog" aria-modal="true" aria-labelledby="mTitle">
  <div class="sheet" id="sheet">
    <button class="close" id="close" aria-label="Close">×</button>
    <div class="pane left">
      <div class="view" id="view">
        <canvas id="mCanvas"></canvas>
        <div class="hint" id="mHint"></div>
        <div class="err" id="mErr"></div>
      </div>
      <div class="controls" id="mControls"></div>
      <div class="rowbtns">
        <button class="mini" id="btnPause">⏸ Pause</button>
        <button class="mini" id="btnReset">↺ Reset controls</button>
        <button class="mini" id="btnPng">⤓ Save PNG</button>
      </div>
    </div>
    <div class="pane right">
      <div class="crumbs" id="mCrumbs"></div>
      <h2 id="mTitle"></h2>
      <p class="intro" id="mIntro"></p>
      <div class="notes" id="mNotes"></div>
      <p class="hintline" id="mRec"></p>
      <div class="tabs" id="mTabs"></div>
      <pre class="code" id="mCode"></pre>
      <div class="srcactions">
        <button class="mini" id="btnCopy">Copy AGSL</button>
        <a class="mini" id="lnkKotlin" target="_blank" rel="noopener">Open Kotlin source ↗</a>
        <a class="mini" id="lnkPerma">Permalink #</a>
      </div>
      <div class="navlr">
        <button class="mini" id="btnPrev">← Previous</button>
        <button class="mini" id="btnNext">Next →</button>
      </div>
    </div>
  </div>
</div>

<script id="data" type="application/json">${dataJson}</script>
<script>${runtimeJs}</script>
<script>
(function(){
  const DATA = JSON.parse(document.getElementById('data').textContent);
  const byId = new Map(DATA.lessons.map(l => [l.id, l]));
  const cats = new Map(DATA.categories.map(c => [c.name, c]));
  const $ = (id) => document.getElementById(id);
  const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
  const kotlinUrl = (l) => DATA.repo + '/blob/' + DATA.branch + '/' + l.source.file + '#L' + l.source.line;
  const supportsWebGL2 = (() => { try { const c = document.createElement('canvas'); return !!c.getContext('webgl2'); } catch (e) { return false; } })();
  if (!supportsWebGL2) $('nowebgl').style.display = 'block';

  // ---------- grid ----------
  let filter = 'ALL', query = '';
  function bolts(n){ let s=''; for (let i=0;i<5;i++) s += '<span class="'+(i<n?'on':'')+'">⚡</span>'; return '<span class="bolts">'+s+'</span>'; }
  function card(l){
    const c = cats.get(l.category);
    return '<button class="card" style="--acc:'+c.accent+'" data-id="'+l.id+'" aria-label="Open '+esc(l.title)+'">'
      + '<img loading="lazy" src="gallery/'+l.id+'.png" alt="'+esc(l.title)+' preview">'
      + '<span class="badge">'+(l.renderMode==='RENDER_EFFECT'?'RenderEffect':l.renderMode==='CUSTOM'?'Showcase':l.hasTouch?'Touch':'ShaderBrush')+'</span>'
      + '<div class="body"><h3>'+esc(l.title)+'</h3><p>'+esc(l.conceptIntro)+'</p>'
      + '<div class="meta"><span>'+esc(c.displayName.toUpperCase())+(l.controls.length?' · '+l.controls.length+' ctrl':'')+'</span>'+bolts(l.complexity)+'</div></div></button>';
  }
  function renderGrid(){
    const q = query.trim().toLowerCase();
    const main = $('main'); let html=''; let shown=0;
    for (const c of DATA.categories){
      if (filter!=='ALL' && filter!==c.name) continue;
      const ls = DATA.lessons.filter(l => l.category===c.name && (!q || (l.title+' '+l.conceptIntro+' '+l.id+' '+l.learningNotes.join(' ')+' '+l.agslSource).toLowerCase().includes(q)));
      if (!ls.length) continue;
      shown += ls.length;
      html += '<section class="cat" style="--acc:'+c.accent+'" id="cat-'+c.name.toLowerCase()+'"><h2><span class="c"></span>'+esc(c.displayName)+' <small style="color:var(--mute);font-family:var(--mono);font-size:13px">'+ls.length+'</small></h2><p class="tag">'+esc(c.tagline)+'</p><div class="grid">'+ls.map(card).join('')+'</div></section>';
    }
    main.innerHTML = html || '<p class="empty">No lesson matches “'+esc(query)+'”.</p>';
  }
  function renderChips(){
    const chips = $('chips');
    const all = '<button class="chip '+(filter==='ALL'?'on':'')+'" data-cat="ALL"><span class="c"></span>All <small>'+DATA.lessons.length+'</small></button>';
    chips.innerHTML = all + DATA.categories.map(c => '<button class="chip '+(filter===c.name?'on':'')+'" style="--acc:'+c.accent+'" data-cat="'+c.name+'"><span class="c"></span>'+esc(c.displayName)+' <small>'+c.count+'</small></button>').join('');
  }
  $('chips').addEventListener('click', (e) => { const b = e.target.closest('.chip'); if (!b) return; filter = b.dataset.cat; renderChips(); renderGrid(); });
  $('search').addEventListener('input', (e) => { query = e.target.value; renderGrid(); });
  $('main').addEventListener('click', (e) => { const b = e.target.closest('.card'); if (b) openLesson(b.dataset.id); });
  renderChips(); renderGrid();

  // ---------- live rendering ----------
  function makePlayer(canvas, opts){
    opts = opts || {};
    let runner = null;
    try { runner = supportsWebGL2 ? Agsl.createRunner(canvas, { preserveDrawingBuffer: true }) : null; } catch (e) { runner = null; }
    const st = { lesson: null, time: 0, last: null, paused: false, touch: null, values: {}, ripples: Agsl.emptyRipples(), gaze: [0.15, 0.1], lastEmit: -1, autoRipple: !!opts.autoRipple, raf: 0, error: null };
    function fit(){
      const r = canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const w = Math.max(1, Math.round(r.width * dpr)), h = Math.max(1, Math.round(r.height * dpr));
      if (canvas.width !== w || canvas.height !== h) { canvas.width = w; canvas.height = h; }
    }
    function frame(now){
      st.raf = requestAnimationFrame(frame);
      if (!runner || !st.lesson) return;
      if (st.last == null) st.last = now;
      const dt = Math.min(0.1, (now - st.last) / 1000); st.last = now;
      if (!st.paused) st.time += dt;
      fit();
      if (st.autoRipple && st.lesson.renderMode === 'CUSTOM' && st.time - st.lastEmit > 1.4) {
        emitRipple((0.25 + 0.5 * Math.random()) * canvas.width, (0.25 + 0.5 * Math.random()) * canvas.height, 0.6 + Math.random() * 0.8);
      }
      try { Agsl.renderLesson(runner, st.lesson, st); st.error = null; }
      catch (e) { st.error = String(e.message || e); st.lesson = null; if (opts.onError) opts.onError(st.error); }
    }
    function emitRipple(px, py, strength){
      const s = st.slot = ((st.slot || 0) + 1) % 16;
      st.ripples[s*4] = px; st.ripples[s*4+1] = py; st.ripples[s*4+2] = st.time; st.ripples[s*4+3] = strength;
      st.lastEmit = st.time;
    }
    function pointer(e){
      if (!st.lesson) return;
      const r = canvas.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width, y = (e.clientY - r.top) / r.height;
      if (st.lesson.hasTouch) st.touch = { x, y, t: st.time };
      if (st.lesson.renderMode === 'CUSTOM') {
        if (e.type === 'pointermove' && st.time - st.lastEmit < 0.06) return;
        emitRipple(x * canvas.width, y * canvas.height, e.pressure ? Math.min(1.5, Math.max(0.3, e.pressure * 1.2)) : 1.0);
        st.gaze = [Math.max(-1, Math.min(1, (x - 0.5) * 2.2)), Math.max(-1, Math.min(1, (y - 0.5) * 2.2))];
      }
    }
    canvas.addEventListener('pointerdown', (e) => { canvas.setPointerCapture(e.pointerId); pointer(e); });
    canvas.addEventListener('pointermove', (e) => { if (e.buttons) pointer(e); });
    return {
      state: st,
      available: !!runner,
      load(lesson, keepTime){ st.lesson = lesson; st.touch = null; st.values = {}; st.ripples = Agsl.emptyRipples(); st.gaze = [0.15, 0.1]; st.lastEmit = -1; if (!keepTime) st.time = 0; st.paused = false; st.error = null; },
      start(){ if (!st.raf) st.raf = requestAnimationFrame(frame); },
      stop(){ cancelAnimationFrame(st.raf); st.raf = 0; st.last = null; },
      snapshot(){ return canvas.toDataURL('image/png'); },
    };
  }

  // ---------- hero ----------
  const hero = makePlayer($('heroCanvas'), { autoRipple: true });
  let heroIdx = 0, heroTimer = 0;
  function showHero(i){
    heroIdx = (i + DATA.hero.length) % DATA.hero.length;
    const l = byId.get(DATA.hero[heroIdx]); if (!l) return;
    $('heroTitle').textContent = l.title; $('heroSub').textContent = cats.get(l.category).displayName + ' · ' + l.id;
    if (hero.available) { hero.load(l, true); }
    else { $('heroImg').src = 'gallery/' + l.id + '.png'; $('heroImg').hidden = false; $('heroCanvas').hidden = true; }
    $('stage').dataset.id = l.id;
  }
  showHero(0); hero.start();
  heroTimer = setInterval(() => { if (!document.hidden && !$('modal').classList.contains('open')) showHero(heroIdx + 1); }, 9000);
  $('stage').addEventListener('click', () => openLesson($('stage').dataset.id));

  // ---------- modal ----------
  const player = makePlayer($('mCanvas'), { onError: (m) => { $('mErr').textContent = 'Preview failed to compile in this browser:\\n' + m; $('mErr').style.display = 'flex'; } });
  let current = null, tab = 'agsl';
  function ctlHtml(l){
    return l.controls.map((c, i) => c.type === 'float'
      ? '<div class="ctl"><label for="c'+i+'">'+esc(c.name)+' <small style="color:var(--mute);font-family:var(--mono)">'+esc(c.uniform)+'</small></label><output id="o'+i+'">'+fmt(c.default)+'</output><input id="c'+i+'" type="range" min="'+c.min+'" max="'+c.max+'" step="'+((c.max-c.min)/200)+'" value="'+c.default+'" data-u="'+c.uniform+'" data-i="'+i+'"></div>'
      : '<div class="ctl"><label for="c'+i+'">'+esc(c.name)+' <small style="color:var(--mute);font-family:var(--mono)">'+esc(c.uniform)+'</small></label><input id="c'+i+'" type="color" value="'+argbToCss(c.default)+'" data-u="'+c.uniform+'" data-i="'+i+'" data-a="'+c.default.slice(1,3)+'"></div>'
    ).join('') || '<p class="hintline" style="margin:0">This lesson has no sliders — it is all about the code.</p>';
  }
  const fmt = (v) => (Math.abs(v) >= 100 ? v.toFixed(0) : Math.abs(v) >= 10 ? v.toFixed(1) : v.toFixed(2));
  const argbToCss = (h) => '#' + h.slice(3);
  function highlight(src){
    return esc(src)
      .replace(/(\\/\\/[^\\n]*)/g, '<span class="c">$1</span>')
      .replace(/\\b(uniform|layout|const|return|if|else|for|while|break|continue|in|out|shader)\\b/g, '<span class="k">$1</span>')
      .replace(/\\b(half[234]?|float[234]?|int[234]?|bool|void|float[234]x[234]|half[234]x[234])\\b/g, '<span class="t">$1</span>')
      .replace(/\\b(\\d+\\.\\d+|\\d+)\\b/g, '<span class="n">$1</span>')
      .replace(/\\b(main|eval|mix|smoothstep|step|fract|floor|length|dot|sin|cos|atan|pow|exp|sqrt|clamp|abs|min|max|normalize|distance|mod|sign|reflect|log2)(?=\\()/g, '<span class="f">$1</span>');
  }
  function codeBlock(src){
    const lines = src.split('\\n');
    return lines.map((ln, i) => '<span class="ln">'+(i+1)+'</span>'+highlight(ln)).join('\\n');
  }
  function renderTabs(){
    const l = current; if (!l) return;
    const tabs = [['agsl','AGSL · main']];
    l.extraAgslSources.forEach((_, i) => tabs.push(['extra'+i, 'AGSL · layer '+(i+2)]));
    tabs.push(['glsl','GLSL (browser)']);
    $('mTabs').innerHTML = tabs.map(([k, label]) => '<button class="tab '+(tab===k?'on':'')+'" data-tab="'+k+'">'+label+'</button>').join('');
    const src = tab==='agsl' ? l.agslSource : tab==='glsl' ? l.glsl : l.extraAgslSources[Number(tab.slice(5))];
    $('mCode').innerHTML = codeBlock(src);
  }
  $('mTabs').addEventListener('click', (e) => { const b = e.target.closest('.tab'); if (!b) return; tab = b.dataset.tab; renderTabs(); });
  function openLesson(id, push){
    const l = byId.get(id); if (!l) return;
    current = l; tab = 'agsl';
    const c = cats.get(l.category);
    $('sheet').style.setProperty('--acc', c.accent);
    $('mCrumbs').innerHTML = '<span>'+esc(c.displayName.toUpperCase())+'</span><span class="sep">/</span><span>'+esc(l.id)+'</span><span class="sep">/</span>'+bolts(l.complexity)
      + '<span class="sep">/</span><span>'+(l.renderMode==='RENDER_EFFECT'?'RenderEffect over Compose UI':l.renderMode==='CUSTOM'?'Custom showcase ('+(1+l.extraAgslSources.length)+' shaders)':'ShaderBrush')+'</span>';
    $('mTitle').textContent = l.title;
    $('mIntro').textContent = l.conceptIntro;
    $('mNotes').style.display = l.learningNotes.length ? '' : 'none';
    $('mNotes').innerHTML = '<h4>What to notice</h4><ul>'+l.learningNotes.map(n => '<li>'+esc(n)+'</li>').join('')+'</ul>';
    $('mRec').textContent = l.screenRecordingHint ? '🎥 Recording hint: ' + l.screenRecordingHint : '';
    $('mControls').innerHTML = ctlHtml(l);
    $('mHint').textContent = l.hasTouch ? 'Click / drag the preview — it becomes touchPos + touchTime' : l.renderMode==='CUSTOM' ? 'Click / drag to emit ripples' : l.hasTime ? 'Animated with the time uniform' : 'Static shader — move the sliders';
    $('lnkKotlin').href = kotlinUrl(l);
    $('lnkPerma').href = '#' + l.id;
    $('mErr').style.display = 'none';
    renderTabs();
    $('modal').classList.add('open');
    document.body.style.overflow = 'hidden';
    if (player.available) { player.load(l); player.start(); $('mCanvas').hidden = false; }
    else { $('view').style.background = 'url(gallery/'+l.id+'.png) center/cover'; $('mCanvas').hidden = true; }
    $('btnPause').textContent = '⏸ Pause';
    if (push !== false) history.replaceState(null, '', '#' + l.id);
  }
  function closeModal(){ $('modal').classList.remove('open'); document.body.style.overflow = ''; player.stop(); history.replaceState(null, '', location.pathname + location.search); }
  $('close').addEventListener('click', closeModal);
  $('modal').addEventListener('click', (e) => { if (e.target === $('modal')) closeModal(); });
  $('mControls').addEventListener('input', (e) => {
    const inp = e.target; if (!inp.dataset.u) return;
    if (inp.type === 'range') { player.state.values[inp.dataset.u] = Number(inp.value); const o = $('o'+inp.dataset.i); if (o) o.textContent = fmt(Number(inp.value)); }
    else { player.state.values[inp.dataset.u] = '#' + inp.dataset.a + inp.value.slice(1).toUpperCase(); }
  });
  $('btnPause').addEventListener('click', () => { player.state.paused = !player.state.paused; $('btnPause').textContent = player.state.paused ? '▶ Play' : '⏸ Pause'; });
  $('btnReset').addEventListener('click', () => { player.state.values = {}; player.state.touch = null; $('mControls').innerHTML = ctlHtml(current); });
  $('btnPng').addEventListener('click', () => { const a = document.createElement('a'); a.download = current.id + '.png'; a.href = player.snapshot(); a.click(); });
  $('btnCopy').addEventListener('click', async () => { try { await navigator.clipboard.writeText(current.agslSource); $('btnCopy').textContent = 'Copied ✓'; setTimeout(() => $('btnCopy').textContent = 'Copy AGSL', 1200); } catch (e) { /* ignore */ } });
  const step = (d) => { const i = DATA.lessons.findIndex(l => l.id === current.id); openLesson(DATA.lessons[(i + d + DATA.lessons.length) % DATA.lessons.length].id); };
  $('btnPrev').addEventListener('click', () => step(-1));
  $('btnNext').addEventListener('click', () => step(1));
  document.addEventListener('keydown', (e) => {
    if (!$('modal').classList.contains('open')) return;
    if (e.key === 'Escape') closeModal();
    else if (e.key === 'ArrowRight') step(1);
    else if (e.key === 'ArrowLeft') step(-1);
    else if (e.key === ' ' && e.target === document.body) { e.preventDefault(); $('btnPause').click(); }
  });
  document.addEventListener('visibilitychange', () => { if (document.hidden) { hero.stop(); player.stop(); } else { hero.start(); if ($('modal').classList.contains('open')) player.start(); } });
  if (location.hash.length > 1 && byId.has(location.hash.slice(1))) openLesson(location.hash.slice(1), false);
  window.addEventListener('hashchange', () => { const id = location.hash.slice(1); if (byId.has(id)) openLesson(id, false); });
  window.__dreams = { DATA, openLesson, player, hero };
})();
</script>
</body>
</html>
`;

fs.writeFileSync(OUT, html);
console.log(`Wrote ${path.relative(ROOT, OUT)} (${(html.length / 1024).toFixed(0)} KB, ${lessons.length} lessons)`);

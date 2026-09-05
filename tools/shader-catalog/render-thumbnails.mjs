#!/usr/bin/env node
// Renders every lesson in docs/catalog/lessons.json through the AGSL→GLSL
// transpiler in headless Chromium (WebGL2 + SwiftShader) and writes
// docs/gallery/<lesson-id>.png plus contact-sheet posters.
//
//   node tools/shader-catalog/render-thumbnails.mjs            # everything
//   node tools/shader-catalog/render-thumbnails.mjs --check    # compile only
//   node tools/shader-catalog/render-thumbnails.mjs --only sdf-01-circle
//   node tools/shader-catalog/render-thumbnails.mjs --size 512
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { transpileAgsl } from './agsl-to-glsl.mjs';
import { launchChromium, harnessHtml, ROOT } from './browser.mjs';
import { THUMB_STATES, thumbState } from './thumb-states.mjs';

const args = process.argv.slice(2);
const flag = (name) => args.includes(name);
const opt = (name, def) => { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : def; };

const SIZE = Number(opt('--size', 400));
// Render at 2× and downsample: closer to device pixel density for px-based
// uniforms (blur radius, pixelate cell size…) and free anti-aliasing.
const SCALE = Number(opt('--scale', 2));
const RENDER = SIZE * SCALE;
// The app draws previews on a vertical surfaceContainerHighest→High gradient
// (dark theme); transparent shader output (Dissolve, Codex icon) composites onto it.
const BACKDROP = ['#244A39', '#183428'];
const ONLY = opt('--only', null);
const CHECK_ONLY = flag('--check');
const GALLERY = path.join(ROOT, 'docs', 'gallery');
const catalog = JSON.parse(fs.readFileSync(path.join(ROOT, 'docs', 'catalog', 'lessons.json'), 'utf8'));

function prepare(lesson) {
  const main = transpileAgsl(lesson.agslSource);
  const extras = lesson.extraAgslSources.map((s) => transpileAgsl(s));
  return {
    ...lesson,
    glsl: main.glsl,
    extraGlsl: extras.map((e) => e.glsl),
    notes: [...main.notes, ...extras.flatMap((e) => e.notes)],
  };
}

async function main() {
  const lessons = catalog.lessons.filter((l) => !ONLY || l.id === ONLY).map(prepare);
  const browser = await launchChromium();
  const page = await browser.newPage({ viewport: { width: RENDER, height: RENDER } });
  page.on('console', (m) => { if (m.type() === 'error') console.error('[page]', m.text()); });
  await page.setContent(harnessHtml(RENDER));

  // 1. Compile every program.
  const failures = [];
  for (const l of lessons) {
    const err = await page.evaluate(({ lesson }) => {
      try {
        const r = window.__runner;
        if (lesson.id === 'showcase-06-codex-splash') {
          r.program(lesson.id + ':bg', lesson.glsl);
          r.program(lesson.id + ':icon', lesson.extraGlsl[0]);
          r.program(lesson.id + ':water', lesson.extraGlsl[1]);
        } else {
          r.program(lesson.id, lesson.glsl);
          lesson.extraGlsl.forEach((g, i) => r.program(`${lesson.id}#extra-${i}`, g));
        }
        return null;
      } catch (e) { return String(e.message || e); }
    }, { lesson: l });
    if (err) failures.push({ id: l.id, err });
    if (l.notes.length) console.log(`note ${l.id}: ${l.notes.join('; ')}`);
  }
  if (failures.length) {
    for (const f of failures) console.error(`\nCOMPILE FAILED ${f.id}\n${f.err}`);
    await browser.close();
    process.exit(1);
  }
  console.log(`✓ ${lessons.length} lessons compile under WebGL2`);
  if (CHECK_ONLY) { await browser.close(); return; }

  // 2. Render thumbnails.
  fs.mkdirSync(GALLERY, { recursive: true });
  for (const l of lessons) {
    const state = thumbState(l, RENDER);
    const dataUrl = await page.evaluate(({ lesson, state, size, backdrop }) => {
      const r = window.__runner;
      if (state.ripples) state.ripples = new Float32Array(state.ripples);
      window.Agsl.renderLesson(r, lesson, state);
      const out = document.createElement('canvas');
      out.width = size; out.height = size;
      const ctx = out.getContext('2d');
      const g = ctx.createLinearGradient(0, 0, 0, size);
      g.addColorStop(0, backdrop[0]);
      g.addColorStop(1, backdrop[1]);
      ctx.fillStyle = g;
      ctx.fillRect(0, 0, size, size);
      ctx.imageSmoothingQuality = 'high';
      ctx.drawImage(r.canvas, 0, 0, size, size);
      return out.toDataURL('image/png');
    }, { lesson: l, state: { ...state, ripples: state.ripples ? Array.from(state.ripples) : null }, size: SIZE, backdrop: BACKDROP });
    const png = Buffer.from(dataUrl.split(',')[1], 'base64');
    fs.writeFileSync(path.join(GALLERY, `${l.id}.png`), png);
    console.log(`  ${l.id}.png (${(png.length / 1024).toFixed(0)} KB)`);
  }

  // 3. Posters (only on a full run).
  if (!ONLY) {
    await renderPosters(page);
  }
  await browser.close();
}

async function renderPosters(page) {
  const tile = 200;
  const gap = 8;
  const load = (id) => `data:image/png;base64,${fs.readFileSync(path.join(GALLERY, `${id}.png`)).toString('base64')}`;

  async function sheet(ids, cols, file) {
    const rows = Math.ceil(ids.length / cols);
    const w = cols * tile + (cols - 1) * gap;
    const h = rows * tile + (rows - 1) * gap;
    const dataUrl = await page.evaluate(async ({ imgs, cols, tile, gap, w, h }) => {
      const c = document.createElement('canvas');
      c.width = w; c.height = h;
      const ctx = c.getContext('2d');
      ctx.clearRect(0, 0, w, h);
      await Promise.all(imgs.map((src, i) => new Promise((res) => {
        const im = new Image();
        im.onload = () => {
          const x = (i % cols) * (tile + gap);
          const y = Math.floor(i / cols) * (tile + gap);
          ctx.save();
          ctx.beginPath();
          const r = 14;
          ctx.roundRect(x, y, tile, tile, r);
          ctx.clip();
          ctx.drawImage(im, x, y, tile, tile);
          ctx.restore();
          res();
        };
        im.src = src;
      })));
      return c.toDataURL('image/png');
    }, { imgs: ids.map(load), cols, tile, gap, w, h });
    const png = Buffer.from(dataUrl.split(',')[1], 'base64');
    fs.writeFileSync(path.join(GALLERY, file), png);
    console.log(`  ${file} (${(png.length / 1024).toFixed(0)} KB)`);
  }

  // Hero contact sheet for the README header: crowd-pleasers across categories.
  await sheet(THUMB_STATES.hero, 6, 'poster-hero.png');
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main().catch((e) => { console.error(e); process.exit(1); });
}

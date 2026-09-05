// Shared Playwright/Chromium bootstrap. Works with a local `npm i playwright`
// or the globally-installed module (PLAYWRIGHT_GLOBAL / npm root -g).
import { createRequire } from 'node:module';
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
export const ROOT = path.resolve(HERE, '..', '..');

function loadPlaywright() {
  const require = createRequire(import.meta.url);
  try { return require('playwright'); } catch { /* fall through */ }
  const candidates = [process.env.PLAYWRIGHT_GLOBAL].filter(Boolean);
  try { candidates.push(path.join(execSync('npm root -g', { encoding: 'utf8' }).trim(), 'playwright')); } catch { /* ignore */ }
  for (const c of candidates) {
    if (fs.existsSync(c)) return createRequire(import.meta.url)(c);
  }
  throw new Error('playwright not found — run `npm i -D playwright && npx playwright install chromium` in tools/shader-catalog');
}

export async function launchChromium() {
  const { chromium } = loadPlaywright();
  return chromium.launch({
    args: [
      // Software GL so this works on GPU-less CI runners and containers.
      '--use-gl=angle', '--use-angle=swiftshader', '--enable-unsafe-swiftshader',
      '--ignore-gpu-blocklist', '--disable-gpu-sandbox',
    ],
  });
}

export const RUNTIME_JS = fs.readFileSync(path.join(HERE, 'runtime', 'agsl-runtime.js'), 'utf8');

/** Minimal harness page with one WebGL2 canvas and the runtime loaded. */
export function harnessHtml(size) {
  return `<!doctype html><html><head><meta charset="utf-8"><style>
  html,body{margin:0;background:#183428}
  canvas{display:block}
  </style></head><body>
  <canvas id="c" width="${size}" height="${size}"></canvas>
  <script>${RUNTIME_JS}</script>
  <script>window.__runner = Agsl.createRunner(document.getElementById('c'));</script>
  </body></html>`;
}

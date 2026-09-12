#!/usr/bin/env node
// Every lesson in the catalog must have a committed thumbnail: the README table
// and the gallery both emit <img src="docs/gallery/<id>.png"> unconditionally, so
// a missing file ships as a broken image. CI does not re-render PNGs (text
// rasterisation is not reproducible across machines), so it checks existence here.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const GALLERY = path.join(ROOT, 'docs', 'gallery');
const catalog = JSON.parse(fs.readFileSync(path.join(ROOT, 'docs', 'catalog', 'lessons.json'), 'utf8'));

const missing = catalog.lessons.filter((l) => !fs.existsSync(path.join(GALLERY, `${l.id}.png`))).map((l) => l.id);
const expected = new Set(catalog.lessons.map((l) => `${l.id}.png`));
const orphans = fs.readdirSync(GALLERY)
  .filter((f) => f.endsWith('.png') && !f.startsWith('poster-') && !expected.has(f));

if (missing.length) {
  console.error(`Missing thumbnails in docs/gallery: ${missing.join(', ')}`);
  console.error('Render them with: node tools/shader-catalog/render-thumbnails.mjs --only <lesson-id>');
}
if (orphans.length) {
  console.error(`Thumbnails with no matching lesson (delete them): ${orphans.join(', ')}`);
}
if (!fs.existsSync(path.join(GALLERY, 'poster-hero.png'))) {
  console.error('Missing docs/gallery/poster-hero.png (README header image)');
  process.exit(1);
}
if (missing.length || orphans.length) process.exit(1);
console.log(`✓ ${catalog.lessons.length} thumbnails present, no orphans`);

---
name: testing-web-gallery
description: How to run and test the docs/index.html AGSL shader web gallery locally (static server, WebGL2-capable Chrome flags, CDP verification hooks).
---

# Testing the Dreams web gallery (docs/index.html)

The gallery at `docs/index.html` is a fully static page — the lesson catalog (with transpiled GLSL) is inlined in `<script id="data">`, so the only server requirement is serving `docs/` so `gallery/<id>.png` thumbnails resolve:

```bash
cd <repo>/docs && python3 -m http.server 8471   # http://localhost:8471/
```

## WebGL2 in the on-screen browser

Chrome for Testing on this box (managed instance, CDP on port from `--remote-debugging-port`, profile `~/.browser_data_dir`) launches **without** `--enable-unsafe-swiftshader`. On Chrome 137+ software WebGL is blocked without it, so the gallery shows the yellow "browser has no WebGL2" banner and falls back to static thumbnails — easy to mistake for working live renders since thumbnails come from the same shaders. Verify the banner is actually hidden before trusting canvas screenshots.

Fix: relaunch the same chrome binary with its existing flags **plus** `--enable-unsafe-swiftshader` (capture them from `/proc/<pid>/cmdline` and keep `--remote-debugging-port` + `--user-data-dir` unchanged). Verify with `document.createElement('canvas').getContext('webgl2')` over CDP — must be truthy and `#nowebgl` must be `display:none`.

## Objective verification hooks (all exposed on `window`)

- `__dreams.DATA` — catalog; `__dreams.openLesson(id)` — opens the lesson modal.
- `__dreams.player.state` — `{ touch: {x,y,t} | null, values: {uniform: number}, ripples: Float32Array(64), paused }`. After clicking `#mCanvas`, `state.touch` should be non-null for `hasTouch` lessons; `CUSTOM` showcases set touch AND ripple slots.
- `#mErr` displays shader compile errors — check it's `display:none` after open.
- Canvas pixels: `#mCanvas` has `preserveDrawingBuffer:true`, so `drawImage` it onto a 2d canvas and `getImageData` for luminance stats; or `toDataURL('image/png')` for artifacts.
- Deep links: `#<lesson-id>` works on fresh load AND via `hashchange` (no reload needed).

Playwright is at `tools/shader-catalog/node_modules` — connect to the visible browser with `chromium.connectOverCDP('http://localhost:<port>')` so `page.mouse`/`page.evaluate` drive the same browser being recorded. `Home`/`End` keys set range sliders to min/max; Esc closes the modal; Left/Right arrows switch lessons.

## Gotchas

- Card thumbnails are `loading="lazy"` — `naturalWidth==0` until scrolled into view; scroll or `scrollIntoView()` before flagging broken images.
- `fractals-05-burning-ship` has NO time uniform (static — changes only via sliders), and its Zoom slider centers on a set-interior point: zoom ≳10 renders a fully black frame by construction.
- `interactive-05-lens-flare` Intensity=0 → fully black (intensity is a direct multiplier). Slider-extreme checks should allow for legitimately dark outputs.
- RENDER_EFFECT lessons (postfx-*) paint a `sampleCard` texture — the preview should show a UI card, not a blank field.

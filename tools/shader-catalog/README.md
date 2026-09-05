# tools/shader-catalog

Node scripts that turn the Kotlin lesson sources into everything the repo shows off:

| Script | Produces | Notes |
|---|---|---|
| `extract-lessons.mjs` | `docs/catalog/lessons.json` | Reads every lesson `object` under `app/src/main/java/com/dantech/dreams/data/lesson/source/`, resolves Kotlin string templates (`$NOISE_HELPERS`, `${centeredUv()}`, `${MAX_RIPPLES * SLOT_FLOATS}`) and applies `trimIndent()` exactly like the Kotlin stdlib, so the AGSL in the JSON is what `LessonModel.agslSource` holds at runtime. Validates ids, category counts and that every control targets a declared uniform. |
| `agsl-to-glsl.mjs` | in-memory GLSL ES 3.00 | Textual AGSL → WebGL2 transpile: `half*`/`float*` → `vec*`, `layout(color)` stripped, `uniform shader` + `.eval()` → `sampler2D` lookup, `half4 main(float2)` wrapped so `fragCoord` is top-left/y-down in local pixels, GLSL-reserved identifiers renamed, SkSL intrinsics polyfilled. |
| `render-thumbnails.mjs` | `docs/gallery/<id>.png`, `poster-*.png` | Headless Chromium + SwiftShader (no GPU needed). `--check` only compiles every lesson (CI). `--only <id>` for one lesson. Hero frames per lesson live in `thumb-states.mjs`. |
| `build-site.mjs` | `docs/index.html` | Self-contained GitHub Pages gallery: live WebGL previews, sliders and colour pickers bound to the lesson controls, tap → `touchPos`/`touchTime`, AGSL + GLSL source tabs, deep links (`#lesson-id`), links to the Kotlin file. |
| `update-readme.mjs` | README catalog section | Rewrites everything between `<!-- catalog:start -->` and `<!-- catalog:end -->`. |
| `runtime/agsl-runtime.js` | (browser) | WebGL2 runner shared by the renderer and the site. Knows how each render mode is wired in the app: `BRUSH` → one pass; `RENDER_EFFECT` → paints a stand-in for `SampleContent()` into a texture bound as `content`; the two showcases → multi-pass (backdrop with the Android bot / atmosphere + icon + water RenderEffect). |

## Usage

```bash
cd tools/shader-catalog
npm install                          # playwright
npx playwright install chromium      # once

npm run extract   # lessons.json
npm run check     # compile all 56 lessons under WebGL2
npm run thumbs    # render thumbnails + posters
npm run site      # docs/index.html
npm run readme    # README lesson table
npm run all       # everything above
```

Preview the gallery locally with any static server, e.g. `npx serve ../../docs`, or just open `docs/index.html`.

## Adding a lesson

Nothing to configure: register the lesson in Kotlin, run `npm run all`, commit the regenerated `lessons.json`, the new PNG, `README.md` and `docs/index.html`. CI fails if `lessons.json`, `README.md` or `docs/index.html` are stale. If the default frame (t = 2.5 s, default slider values) does not photograph well, add an override to `thumb-states.mjs`.

## Fidelity caveats

The browser preview is a faithful port of the math, not of Skia: colours are not colour-managed, `half` runs at full precision, and `content` is a canvas stand-in for the real Compose subtree. Anything that looks wrong on device but right here (or vice-versa) is worth an issue.

// Per-lesson "hero frame" for the static thumbnails: which instant of the
// animation to freeze, where the simulated tap landed, and which slider
// values to use when the defaults do not photograph well.
export const DEFAULT_TIME = 2.5;

const rippleSlots = (list) => {
  const arr = new Array(64).fill(0);
  for (let i = 0; i < 16; i++) { arr[i * 4 + 2] = -1000; arr[i * 4 + 3] = 0; }
  list.forEach(([x, y, t0, st], i) => { arr[i * 4] = x; arr[i * 4 + 1] = y; arr[i * 4 + 2] = t0; arr[i * 4 + 3] = st; });
  return arr;
};

export const THUMB_STATES = {
  overrides: {
    'basics-02-animated-color': { time: 0.9 },
    'sdf-03-metaballs': { time: 1.1 },
    'noise-04-voronoi': { time: 1.7 },
    'motion-01-easing': { time: 1.2 },
    'motion-02-harmonics': { time: 0.6 },
    'fractals-02-julia': { time: 46.2 }, // c lands inside the period-2 bulb → connected, intricate set
    'lighting-01-lambert': { time: 1.2 },
    'lighting-02-phong': { time: 0.8 },
    'lighting-04-terminator': { time: 2.0 },
    'interactive-02-ripple': { time: 3.0, touch: { x: 0.58, y: 0.44, t: 2.15 } },
    'interactive-04-heat-stripes': { time: 3.0, touch: { x: 0.58, y: 0.44, t: 2.55 } },
    'interactive-01-spotlight': { touch: { x: 0.62, y: 0.40, t: 0 } },
    'interactive-03-pull-field': { touch: { x: 0.62, y: 0.40, t: 0 } },
    'postfx-03-ripple-tap': { time: 1.3 },
    'postfx-04-dissolve': { time: 5.1 },
    'postfx-05-displacement-glass': { time: 2.0 },
    'showcase-05-ripple-on-tap': {
      time: 3.0,
      gaze: [0.35, 0.25],
      ripples: (S) => rippleSlots([
        [S * 0.68, S * 0.40, 3.0 - 0.55, 1.1],
        [S * 0.30, S * 0.62, 3.0 - 1.05, 0.9],
        [S * 0.55, S * 0.74, 3.0 - 1.65, 0.7],
      ]),
    },
    'showcase-06-codex-splash': {
      time: 2.2,
      ripples: (S) => rippleSlots([
        [S * 0.64, S * 0.46, 2.2 - 0.45, 1.3],
        [S * 0.36, S * 0.58, 2.2 - 1.1, 0.9],
      ]),
    },
  },
  hero: [
    'noise-06-warped-lava', 'fractals-02-julia', 'patterns-06-kaleidoscope-fold', 'sdf-03-metaballs',
    'lighting-02-phong', 'showcase-06-codex-splash',
    'noise-04-voronoi', 'patterns-04-truchet', 'color-01-cosine-palette', 'fractals-03-newton',
    'postfx-05-displacement-glass', 'showcase-05-ripple-on-tap',
  ],
};

export function thumbState(lesson, size = 400) {
  const o = THUMB_STATES.overrides[lesson.id] || {};
  const time = o.time ?? DEFAULT_TIME;
  const hasTouch = lesson.uniforms.some((u) => u.name === 'touchPos');
  return {
    time,
    touch: o.touch ?? (hasTouch ? { x: 0.62, y: 0.42, t: time - 0.85 } : null),
    values: o.values || {},
    gaze: o.gaze,
    ripples: typeof o.ripples === 'function' ? o.ripples(size) : o.ripples,
  };
}

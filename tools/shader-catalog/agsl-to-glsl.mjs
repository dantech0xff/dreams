// AGSL → GLSL ES 3.00 transpiler for browser previews.
//
// AGSL is Skia's SkSL dialect; the lessons in this repo only use the subset
// that maps 1:1 onto GLSL ES 3.00 (WebGL2). This module performs that mapping
// textually so the *same* AGSL source that ships in the APK can be previewed
// on GitHub Pages and rendered into README thumbnails.
//
// What is translated:
//   • half/half2/half3/half4 → float/vec2/vec3/vec4 (WebGL has no half type)
//   • float2/float3/float4, halfNxN/floatNxN → vecN / matN
//   • int2.., bool2.. → ivecN / bvecN
//   • `layout(color) uniform half4 c;` → `uniform vec4 c;`
//   • `uniform shader content;` + `content.eval(coord)` → sampler2D lookup
//   • `half4 main(float2 fragCoord)` → wrapped so fragCoord is top-left,
//     y-down, in pixels — exactly what Skia hands a RuntimeShader.
//   • SkSL-only intrinsics (saturate, unpremul, …) get small polyfills
//   • identifiers that are reserved words in GLSL (or that shadow GLSL
//     built-in functions) are renamed with a trailing underscore
//
// What is NOT emulated: colour management (AGSL runs in the destination
// colour space), `sk_FragCoord`, child shader sampling modes, and `half`
// precision behaviour (everything runs at highp).

const RESERVED = new Set(`
attribute varying coherent restrict readonly writeonly resource atomic_uint noperspective patch sample
subroutine common partition active asm class union enum typedef template this goto inline noinline
public static extern external interface long short double half fixed unsigned superp input output
hvec2 hvec3 hvec4 dvec2 dvec3 dvec4 fvec2 fvec3 fvec4 sampler3DRect filter sizeof cast namespace using
`.trim().split(/\s+/));

const BUILTIN_FUNCTIONS = new Set(`
radians degrees sin cos tan asin acos atan sinh cosh tanh asinh acosh atanh pow exp log exp2 log2 sqrt
inversesqrt abs sign floor trunc round roundEven ceil fract mod modf min max clamp mix step smoothstep
isnan isinf floatBitsToInt floatBitsToUint intBitsToFloat uintBitsToFloat packSnorm2x16 packUnorm2x16
unpackSnorm2x16 unpackUnorm2x16 packHalf2x16 unpackHalf2x16 length distance dot cross normalize faceforward
reflect refract matrixCompMult outerProduct transpose determinant inverse lessThan lessThanEqual greaterThan
greaterThanEqual equal notEqual any all not textureSize texture textureProj textureLod textureOffset texelFetch
texelFetchOffset textureProjOffset textureLodOffset textureProjLod textureProjLodOffset textureGrad
textureGradOffset textureProjGrad textureProjGradOffset dFdx dFdy fwidth
`.trim().split(/\s+/));

function stripComments(src) {
  return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
}

/** Rename an identifier everywhere it is used as a variable (not as a call). */
function renameIdentifier(src, from, to) {
  return src.replace(new RegExp(`\\b${from}\\b(?!\\s*\\()`, 'g'), to);
}

export function transpileAgsl(agsl) {
  let glsl = agsl;
  const notes = [];

  // 1. child shaders → sampler2D
  const children = [...stripComments(glsl).matchAll(/uniform\s+shader\s+(\w+)\s*;/g)].map((m) => m[1]);
  glsl = glsl.replace(/uniform\s+shader\s+(\w+)\s*;/g, 'uniform sampler2D $1;');
  for (const child of children) {
    glsl = glsl.replace(new RegExp(`\\b${child}\\.eval\\(`, 'g'), `agslx_eval(${child}, `);
  }

  // 2. layout(color)
  glsl = glsl.replace(/layout\s*\(\s*color\s*\)\s*/g, '');

  // 3. scalar/vector/matrix types
  glsl = glsl
    .replace(/\b(?:half|float)([234])x([234])\b/g, (_, r, c) => (r === c ? `mat${r}` : `mat${r}x${c}`))
    .replace(/\b(?:half|float)([234])\b/g, 'vec$1')
    .replace(/\bhalf\b/g, 'float')
    .replace(/\bint([234])\b/g, 'ivec$1')
    .replace(/\bbool([234])\b/g, 'bvec$1')
    .replace(/\bshort\b/g, 'int')
    .replace(/\bushort\b/g, 'uint');

  // 4. entry point
  const mainRx = /\bvec4\s+main\s*\(\s*vec2\s+(\w+)\s*\)/;
  const main = glsl.match(mainRx);
  if (!main) throw new Error('No `half4 main(float2 fragCoord)` entry point found');
  glsl = glsl.replace(mainRx, 'vec4 agslx_main(vec2 $1)');

  // 5. identifiers that collide with GLSL reserved words / built-in functions
  const code = stripComments(glsl);
  const declared = new Set();
  for (const m of code.matchAll(/\b(?:float|int|uint|bool|[ib]?vec[234]|mat[234])\s+([A-Za-z_]\w*)\s*(?:=|;|,|\))/g)) declared.add(m[1]);
  for (const m of code.matchAll(/\b(?:float|int|uint|bool|[ib]?vec[234]|mat[234])\s+([A-Za-z_]\w*)\s*\(/g)) declared.add(m[1]); // user functions
  for (const id of declared) {
    if (RESERVED.has(id) || BUILTIN_FUNCTIONS.has(id)) {
      const to = `${id}_`;
      // Only rename the variable-style uses; calls to the *builtin* keep working.
      glsl = renameIdentifier(glsl, id, to);
      notes.push(`renamed \`${id}\` → \`${to}\` (GLSL reserved/built-in)`);
    }
  }

  // 6. polyfills for SkSL intrinsics
  const polyfills = [];
  if (/\bsaturate\s*\(/.test(code)) polyfills.push('#define saturate(x) clamp((x), 0.0, 1.0)');
  if (/\bunpremul\s*\(/.test(code)) polyfills.push('vec4 unpremul(vec4 c) { return c.a > 0.0 ? vec4(c.rgb / c.a, c.a) : c; }');
  if (/\btoLinearSrgb\s*\(/.test(code)) polyfills.push('vec3 toLinearSrgb(vec3 c) { return pow(c, vec3(2.2)); }');
  if (/\bfromLinearSrgb\s*\(/.test(code)) polyfills.push('vec3 fromLinearSrgb(vec3 c) { return pow(c, vec3(1.0 / 2.2)); }');

  const header = [
    '#version 300 es',
    'precision highp float;',
    'precision highp int;',
    'precision highp sampler2D;',
    'out vec4 agslx_fragColor;',
    '// Injected by tools/shader-catalog/agsl-to-glsl.mjs',
    'uniform vec2 agslx_canvas;        // size of the area the shader fills, in px',
    'uniform vec2 agslx_origin;        // viewport origin, so fragCoord is local',
    'uniform float agslx_contentFlip;  // 1.0 when `content` is a render target',
    'vec4 agslx_eval(sampler2D s, vec2 c) {',
    '    vec2 uv = c / agslx_canvas;',
    '    uv.y = mix(uv.y, 1.0 - uv.y, agslx_contentFlip);',
    '    return texture(s, uv);',
    '}',
    ...polyfills,
    '',
  ].join('\n');

  const footer = [
    '',
    'void main() {',
    '    // Skia hands AGSL a top-left origin, y-down fragCoord; GL is bottom-left, y-up.',
    '    vec2 agslx_fc = vec2(gl_FragCoord.x - agslx_origin.x, agslx_canvas.y - (gl_FragCoord.y - agslx_origin.y));',
    `    agslx_fragColor = agslx_main(agslx_fc);`,
    '}',
    '',
  ].join('\n');

  return { glsl: header + glsl + footer, children, notes };
}

export const VERTEX_SHADER = `#version 300 es
void main() {
    // Full-screen triangle: 3 vertices cover the clip-space square.
    vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2) * 2.0 - 1.0;
    gl_Position = vec4(p, 0.0, 1.0);
}
`;

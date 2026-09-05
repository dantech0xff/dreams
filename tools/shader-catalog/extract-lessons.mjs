#!/usr/bin/env node
// Extracts every lesson registered in app/src/main/java/.../data/lesson/source/**
// into docs/catalog/lessons.json — the single source of truth for the README
// catalog, the web gallery and the thumbnail renderer.
//
// It is a deliberately small, dependency-free Kotlin "reader" tuned to the
// conventions used by lesson files (object + `val id` + `private val SOURCE`
// + LessonRegistry.register(LessonModel(...))). It resolves Kotlin string
// templates ($NOISE_HELPERS, ${centeredUv()}, ${MAX_RIPPLES * SLOT_FLOATS})
// and applies trimIndent() the same way the Kotlin stdlib does, so the AGSL
// in the JSON is byte-identical to what LessonModel.agslSource holds at runtime.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, '..', '..');
const LESSON_DIR = 'app/src/main/java/com/dantech/dreams/data/lesson';
const SOURCE_DIR = `${LESSON_DIR}/source`;
const OUT = path.join(ROOT, 'docs', 'catalog', 'lessons.json');

// ---------------------------------------------------------------------------
// Lexical helpers: walk Kotlin text while skipping strings and comments.
// ---------------------------------------------------------------------------

/** Index just past the end of the string/comment starting at i, or -1. */
function skipLiteral(text, i) {
  if (text.startsWith('"""', i)) {
    const end = text.indexOf('"""', i + 3);
    if (end < 0) throw new Error('Unterminated raw string');
    let j = end + 3;
    while (text[j] === '"') j++; // extra quotes belong to the string
    return j;
  }
  if (text[i] === '"') {
    let j = i + 1;
    while (j < text.length && text[j] !== '"') {
      if (text[j] === '\\') j++;
      j++;
    }
    return j + 1;
  }
  if (text.startsWith('//', i)) {
    const nl = text.indexOf('\n', i);
    return nl < 0 ? text.length : nl;
  }
  if (text.startsWith('/*', i)) {
    const end = text.indexOf('*/', i + 2);
    return end < 0 ? text.length : end + 2;
  }
  return -1;
}

/** Index of the bracket matching text[open] (one of ( { [). */
function matchBracket(text, open) {
  const pairs = { '(': ')', '{': '}', '[': ']' };
  const close = pairs[text[open]];
  let depth = 0;
  for (let i = open; i < text.length; ) {
    const skip = skipLiteral(text, i);
    if (skip >= 0) { i = skip; continue; }
    const ch = text[i];
    if (ch === text[open]) depth++;
    else if (ch === close) { depth--; if (depth === 0) return i; }
    i++;
  }
  throw new Error('Unbalanced brackets');
}

/** Split an argument list on top-level commas. */
function splitArgs(text) {
  const out = [];
  let depth = 0;
  let start = 0;
  for (let i = 0; i < text.length; ) {
    const skip = skipLiteral(text, i);
    if (skip >= 0) { i = skip; continue; }
    const ch = text[i];
    if ('({['.includes(ch)) depth++;
    else if (')}]'.includes(ch)) depth--;
    else if (ch === ',' && depth === 0) { out.push(text.slice(start, i)); start = i + 1; }
    i++;
  }
  const last = text.slice(start);
  if (last.trim()) out.push(last);
  return out.map((s) => s.trim()).filter(Boolean);
}

// ---------------------------------------------------------------------------
// Kotlin string semantics.
// ---------------------------------------------------------------------------

function indentWidth(line) {
  const m = line.match(/^[ \t]*/);
  return m ? m[0].length : 0;
}

/** Faithful port of kotlin.text.trimIndent(). */
export function kotlinTrimIndent(s) {
  const lines = s.split('\n');
  const nonBlank = lines.filter((l) => l.trim().length > 0);
  const minIndent = nonBlank.length ? Math.min(...nonBlank.map(indentWidth)) : 0;
  const last = lines.length - 1;
  return lines
    .map((line, i) => ((i === 0 || i === last) && line.trim().length === 0 ? null : line.slice(minIndent)))
    .filter((l) => l !== null)
    .join('\n');
}

function unescapeKotlin(s) {
  return s.replace(/\\(.)/g, (_, c) => ({ n: '\n', t: '\t', r: '\r', '"': '"', '\\': '\\', $: '$', "'": "'" }[c] ?? c));
}

// ---------------------------------------------------------------------------
// Symbol table: raw string constants, int constants, string-returning funs.
// ---------------------------------------------------------------------------

const symbols = {
  strings: new Map(), // NAME -> { raw, trimIndent }
  ints: new Map(),    // NAME -> number
  funs: new Map(),    // name -> raw string body
};

function collectSymbols(text) {
  const rawConst = /(?:private\s+|internal\s+)?(?:const\s+)?val\s+([A-Za-z_]\w*)\s*=\s*"""([\s\S]*?)"""(\s*\.trimIndent\(\))?/g;
  for (const m of text.matchAll(rawConst)) {
    if (m[1] === 'SOURCE') continue; // object-scoped, handled per object
    symbols.strings.set(m[1], { raw: m[2], trimIndent: Boolean(m[3]) });
  }
  const intConst = /(?:private\s+|internal\s+)?const\s+val\s+([A-Z_][A-Z0-9_]*)\s*=\s*(-?\d+)\b(?!\s*\.)/g;
  for (const m of text.matchAll(intConst)) symbols.ints.set(m[1], Number(m[2]));
  const strFun = /(?:private\s+)?fun\s+([a-z]\w*)\(\)\s*=\s*"""([\s\S]*?)"""/g;
  for (const m of text.matchAll(strFun)) symbols.funs.set(m[1], m[2]);
}

function resolveTemplates(raw, scope) {
  return raw
    .replace(/\$\{([^}]+)\}/g, (_, expr) => {
      const e = expr.trim();
      const call = e.match(/^([A-Za-z_]\w*)\(\)$/);
      if (call) {
        const body = symbols.funs.get(call[1]);
        if (body === undefined) throw new Error(`Unknown template function ${call[1]}`);
        return body;
      }
      const arith = e.replace(/[A-Za-z_]\w*/g, (id) => {
        const v = scope.ints.get(id) ?? symbols.ints.get(id);
        if (v === undefined) throw new Error(`Unknown template symbol ${id} in \${${e}}`);
        return String(v);
      });
      if (!/^[\d\s+\-*/()]+$/.test(arith)) throw new Error(`Unsupported template expression ${e}`);
      return String(Function(`"use strict"; return (${arith});`)());
    })
    .replace(/\$([A-Za-z_]\w*)/g, (_, id) => {
      const s = scope.strings.get(id) ?? symbols.strings.get(id);
      if (s) return resolveTemplates(s.raw, scope);
      const n = scope.ints.get(id) ?? symbols.ints.get(id);
      if (n !== undefined) return String(n);
      throw new Error(`Unknown template symbol $${id}`);
    });
}

function resolveString(entry, scope) {
  const text = resolveTemplates(entry.raw, scope);
  return entry.trimIndent ? kotlinTrimIndent(text) : text;
}

// ---------------------------------------------------------------------------
// Expression evaluation for LessonModel(...) arguments.
// ---------------------------------------------------------------------------

function evalExpr(expr, scope) {
  const e = expr.trim();
  if (e === 'null') return null;
  if (e.startsWith('{')) return undefined; // lambda (postEffectContent / customPreview)
  let m;
  if ((m = e.match(/^"""([\s\S]*)"""(\s*\.trimIndent\(\))?$/))) {
    return resolveString({ raw: m[1], trimIndent: Boolean(m[2]) }, scope);
  }
  if ((m = e.match(/^"((?:[^"\\]|\\.)*)"$/))) return unescapeKotlin(m[1]);
  if ((m = e.match(/^(-?\d+)$/))) return Number(m[1]);
  if ((m = e.match(/^(-?\d*\.?\d+)f$/))) return Number(m[1]);
  if ((m = e.match(/^LessonCategory\.(\w+)$/))) return { enum: 'LessonCategory', value: m[1] };
  if ((m = e.match(/^LessonRenderMode\.(\w+)$/))) return { enum: 'LessonRenderMode', value: m[1] };
  if ((m = e.match(/^Color\(0x([0-9A-Fa-f]{8})\)$/))) return { color: '#' + m[1].toUpperCase() };
  if ((m = e.match(/^persistentListOf\(([\s\S]*)\)$/))) return splitArgs(m[1]).map((a) => evalExpr(a, scope));
  if ((m = e.match(/^LessonControl\.(FloatRange|ColorPicker)\(([\s\S]*)\)$/))) {
    const kind = m[1];
    const args = splitArgs(m[2]);
    const positional = kind === 'FloatRange'
      ? ['name', 'uniformName', 'min', 'max', 'default']
      : ['name', 'uniformName', 'default'];
    const obj = { type: kind };
    args.forEach((a, i) => {
      const named = a.match(/^([a-zA-Z]\w*)\s*=\s*([\s\S]*)$/);
      const key = named ? named[1] : positional[i];
      const val = evalExpr(named ? named[2] : a, scope);
      obj[key] = val;
    });
    return obj;
  }
  if ((m = e.match(/^([A-Za-z_]\w*)(\.trimIndent\(\))?$/))) {
    const id = m[1];
    const s = scope.strings.get(id) ?? symbols.strings.get(id);
    if (!s) throw new Error(`Unknown identifier ${id}`);
    return resolveString({ raw: s.raw, trimIndent: s.trimIndent || Boolean(m[2]) }, scope);
  }
  throw new Error(`Cannot evaluate: ${e.slice(0, 80)}`);
}

// ---------------------------------------------------------------------------
// Parsing lesson objects.
// ---------------------------------------------------------------------------

function lineOf(text, index) {
  return text.slice(0, index).split('\n').length;
}

function parseObjects(text, relFile) {
  const lessons = [];
  const objRx = /\bobject\s+([A-Z]\w*)\s*\{/g;
  for (const m of text.matchAll(objRx)) {
    const openIdx = m.index + m[0].length - 1;
    const closeIdx = matchBracket(text, openIdx);
    const body = text.slice(openIdx + 1, closeIdx);
    const regIdx = body.indexOf('LessonRegistry.register(');
    if (regIdx < 0) continue;

    const scope = { strings: new Map(), ints: new Map() };
    const src = body.match(/private\s+val\s+SOURCE\s*=\s*"""([\s\S]*?)"""(\s*\.trimIndent\(\))?/);
    if (src) scope.strings.set('SOURCE', { raw: src[1], trimIndent: Boolean(src[2]) });
    const idMatch = body.match(/val\s+id\s*=\s*"([^"]+)"/);
    scope.strings.set('id', { raw: idMatch ? idMatch[1] : '', trimIndent: false });

    const callOpen = regIdx + 'LessonRegistry.register'.length;
    const callClose = matchBracket(body, callOpen);
    const inner = body.slice(callOpen + 1, callClose).trim();
    const fn = inner.match(/^(\w+)\(/);
    if (!fn) throw new Error(`${relFile}: unexpected register() payload in ${m[1]}`);
    const argText = inner.slice(fn[0].length, matchBracket(inner, fn[0].length - 1));
    const args = splitArgs(argText);

    const fields = {};
    if (fn[1] === 'LessonModel') {
      for (const a of args) {
        const named = a.match(/^([a-zA-Z]\w*)\s*=\s*([\s\S]*)$/);
        if (!named) throw new Error(`${relFile}: positional LessonModel arg in ${m[1]}: ${a}`);
        fields[named[1]] = evalExpr(named[2], scope);
      }
    } else if (fn[1] === 'postFxLesson') {
      const positional = ['id', 'title', 'complexity', 'intro', 'source', 'controls', 'hint'];
      args.forEach((a, i) => {
        const named = a.match(/^([a-zA-Z]\w*)\s*=\s*([\s\S]*)$/);
        fields[named ? named[1] : positional[i]] = evalExpr(named ? named[2] : a, scope);
      });
      fields.conceptIntro = fields.intro;
      fields.agslSource = fields.source;
      fields.screenRecordingHint = fields.hint;
      fields.category = { enum: 'LessonCategory', value: 'POSTFX' };
      fields.renderMode = { enum: 'LessonRenderMode', value: 'RENDER_EFFECT' };
    } else {
      throw new Error(`${relFile}: unknown lesson factory ${fn[1]} in ${m[1]}`);
    }

    lessons.push({
      objectName: m[1],
      file: relFile,
      line: lineOf(text, m.index),
      fields,
    });
  }
  return lessons;
}

function walk(dir) {
  const out = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) out.push(...walk(p));
    else if (entry.name.endsWith('.kt')) out.push(p);
  }
  return out.sort();
}

// ---------------------------------------------------------------------------
// Uniform detection (mirrors ui/feature/common/ShaderUniformBindings.kt).
// ---------------------------------------------------------------------------

export function stripComments(src) {
  return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
}

function detectUniforms(agsl) {
  const uniforms = [];
  const rx = /(layout\s*\(\s*color\s*\)\s+)?uniform\s+(\w+)\s+(\w+)\s*(\[\s*(\d+)\s*\])?\s*;/g;
  for (const m of stripComments(agsl).matchAll(rx)) {
    uniforms.push({
      name: m[3],
      type: m[2],
      ...(m[5] ? { arraySize: Number(m[5]) } : {}),
      ...(m[1] ? { color: true } : {}),
    });
  }
  return uniforms;
}

// ---------------------------------------------------------------------------
// Main.
// ---------------------------------------------------------------------------

export function extract() {
  const files = walk(path.join(ROOT, SOURCE_DIR));
  const texts = new Map(files.map((f) => [f, fs.readFileSync(f, 'utf8')]));
  for (const t of texts.values()) collectSymbols(t);

  // Category metadata from the enum.
  const catText = fs.readFileSync(path.join(ROOT, LESSON_DIR, 'LessonCategory.kt'), 'utf8');
  const categories = [...catText.matchAll(/^\s*([A-Z]+)\("([^"]+)",\s*"([^"]+)"\)/gm)].map((m) => ({
    name: m[1],
    displayName: m[2],
    tagline: m[3],
  }));

  // Registration order: LessonRegistry.bootstrap() → *Bootstrap.touch() bodies.
  const regText = fs.readFileSync(path.join(ROOT, LESSON_DIR, 'LessonRegistry.kt'), 'utf8');
  const bootstrapOrder = [...regText.matchAll(/source\.(\w+)\.(\w+)Bootstrap\.touch\(\)/g)].map((m) => m[1]);
  const touchOrder = [];
  for (const pkg of bootstrapOrder) {
    const bootFile = files.find((f) => f.includes(`/source/${pkg}/`) && f.endsWith('Bootstrap.kt'));
    if (!bootFile) throw new Error(`No Bootstrap file for package ${pkg}`);
    const body = texts.get(bootFile);
    for (const m of body.matchAll(/^\s*([A-Z]\w*)\.id\s*$/gm)) touchOrder.push(m[1]);
  }

  const parsed = [];
  for (const f of files) {
    const rel = path.relative(ROOT, f).split(path.sep).join('/');
    parsed.push(...parseObjects(texts.get(f), rel));
  }

  const byObject = new Map(parsed.map((p) => [p.objectName, p]));
  const missing = touchOrder.filter((o) => !byObject.has(o));
  if (missing.length) throw new Error(`Bootstrap references unknown objects: ${missing.join(', ')}`);
  const unreferenced = parsed.filter((p) => !touchOrder.includes(p.objectName)).map((p) => p.objectName);
  if (unreferenced.length) throw new Error(`Lessons never touched by a Bootstrap: ${unreferenced.join(', ')}`);

  const lessons = touchOrder.map((objectName) => {
    const p = byObject.get(objectName);
    const f = p.fields;
    const controls = (f.controls ?? []).map((c) => {
      if (c.type === 'FloatRange') {
        return { type: 'float', name: c.name, uniform: c.uniformName, min: c.min, max: c.max, default: c.default };
      }
      return { type: 'color', name: c.name, uniform: c.uniformName, default: c.default.color };
    });
    const agslSource = f.agslSource;
    const uniforms = detectUniforms(agslSource);
    for (const c of controls) {
      const decl = uniforms.find((u) => u.name === c.uniform);
      const ok = decl && (c.type === 'float' ? decl.type === 'float' : decl.type === 'half4');
      if (!ok) throw new Error(`${f.id}: control ${c.uniform} has no matching uniform declaration`);
    }
    return {
      id: f.id,
      title: f.title,
      category: f.category.value,
      complexity: f.complexity,
      conceptIntro: f.conceptIntro,
      learningNotes: f.learningNotes ?? [],
      controls,
      renderMode: f.renderMode?.value ?? 'BRUSH',
      screenRecordingHint: f.screenRecordingHint ?? null,
      uniforms,
      agslSource,
      extraAgslSources: f.extraAgslSources ?? [],
      source: { file: p.file, line: p.line, object: objectName },
    };
  });

  const ids = new Set();
  for (const l of lessons) {
    if (ids.has(l.id)) throw new Error(`Duplicate lesson id ${l.id}`);
    ids.add(l.id);
  }

  return {
    $schema: 'dreams-lesson-catalog/1',
    generatedBy: 'tools/shader-catalog/extract-lessons.mjs',
    sourceDir: SOURCE_DIR,
    categories: categories.map((c) => ({ ...c, count: lessons.filter((l) => l.category === c.name).length })),
    lessons,
  };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const catalog = extract();
  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, JSON.stringify(catalog, null, 2) + '\n');
  const perCat = catalog.categories.map((c) => `${c.displayName}=${c.count}`).join(', ');
  console.log(`Wrote ${path.relative(ROOT, OUT)} — ${catalog.lessons.length} lessons (${perCat})`);
}

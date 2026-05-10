#!/usr/bin/env bash
# Fast grep-based pre-pass for the kta-code-review skill.
# Framework-agnostic: no project- or library-specific patterns.
# Output format: file:line: <rule> — for direct citation in review reports.
#
# Usage:
#   scripts/run-static-checks.sh <path>
#   scripts/run-static-checks.sh .                    # whole tree
#   scripts/run-static-checks.sh src/main/kotlin
#   scripts/run-static-checks.sh path/to/File.kt
#
# Exits 0 always; this is a finder, not a gate.

set -u

target="${1:-.}"

if [[ ! -e "$target" ]]; then
  echo "error: path does not exist: $target" >&2
  exit 2
fi

# Prefer ripgrep when available; fall back to grep -rn.
if command -v rg >/dev/null 2>&1; then
  scan() { rg --no-heading --line-number --with-filename --color=never -t kotlin "$@" "$target" 2>/dev/null; }
else
  scan() { grep -HrIn --include='*.kt' --include='*.kts' "$@" "$target" 2>/dev/null; }
fi

emit() {
  local rule="$1"; shift
  local pattern="$1"; shift
  local extra=("$@")
  scan ${extra[@]+"${extra[@]}"} -e "$pattern" | while IFS=: read -r f l rest; do
    printf '%s:%s: %s — %s\n' "$f" "$l" "$rule" "$(echo "$rest" | sed 's/^[[:space:]]*//' | cut -c1-160)"
  done
}

# Concurrency
emit "GlobalScope"                  'GlobalScope\.(launch|async)'
emit "runBlocking-in-source"        'runBlocking[[:space:]]*[({]'
emit "direct-Dispatchers"           'Dispatchers\.(IO|Main|Default|Unconfined)'
emit "swallowed-CancellationException" 'catch[[:space:]]*\([[:space:]]*[a-zA-Z_]+[[:space:]]*:[[:space:]]*Exception'

# Null safety / casts
emit "force-unwrap"                 '!!\.'
emit "unchecked-cast"               '\bas[[:space:]]+[A-Z][A-Za-z0-9_<>?]*[[:space:]]*$' || true
emit "unchecked-cast-list"          'as[[:space:]]+List<'

# Logic / error handling
emit "printStackTrace"              '\.printStackTrace\(\)'
emit "TODO-or-FIXME"                '\b(TODO|FIXME)\b'
emit "empty-catch"                  'catch[[:space:]]*\([^)]*\)[[:space:]]*\{[[:space:]]*\}'

# Crypto / weak primitives
emit "weak-hash-MD5"                'MessageDigest\.getInstance\([[:space:]]*"MD5"'
emit "weak-hash-SHA1"               'MessageDigest\.getInstance\([[:space:]]*"SHA-?1"'
emit "AES-ECB"                      'Cipher\.getInstance\([[:space:]]*"AES/ECB'
emit "insecure-random"              '\b(Random|kotlin\.random\.Random)\.(nextBytes|nextInt|nextLong)\('

# Transport
emit "http-url"                     '"http://[a-zA-Z]'
emit "trust-all-hostname"           'HostnameVerifier[[:space:]]*\{[[:space:]]*_[[:space:]]*,[[:space:]]*_[[:space:]]*->[[:space:]]*true'

# Secrets (heuristic — redact in the review report, never echo)
emit "possible-secret-literal"      '(api[_-]?key|secret|password|passwd|token|credential)[[:space:]]*=[[:space:]]*"[^"]{8,}"'
emit "possible-aws-key"             '"AKIA[0-9A-Z]{16}"'
emit "possible-github-token"        '"gh[pousr]_[A-Za-z0-9]{20,}"'
emit "possible-stripe-key"          '"sk_(live|test)_[A-Za-z0-9]{16,}"'
emit "possible-google-api-key"      '"AIza[0-9A-Za-z_-]{20,}"'
emit "possible-jwt"                 '"eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.'

# Logging that may leak
emit "log-with-token-or-password"   '(Log\.[a-z]+|Timber\.[a-z]+|println|print|logger\.[a-z]+|log\.[a-z]+)[^"]*"[^"]*\$\{?[a-zA-Z_]*(token|password|secret|email|apiKey)'

# Time / determinism (testability hint)
emit "wallclock-now"                '\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|Date)\(\)|\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime)\.now\('
emit "uuid-randomUUID"              'UUID\.randomUUID\('
emit "Thread-sleep"                 'Thread\.sleep\('

# Allocation / perf hints
emit "regex-inline"                 'Regex\([[:space:]]*"'
emit "string-format-loop-hint"      'String\.format\('

# Visibility / API hygiene
emit "lateinit-val-substitute"      'lateinit[[:space:]]+var[[:space:]]+[a-zA-Z_][a-zA-Z0-9_]*[[:space:]]*:[[:space:]]*(String|Int|Long|Boolean|Float|Double)\b'

exit 0

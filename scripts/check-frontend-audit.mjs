#!/usr/bin/env node
/**
 * Frontend production-dependency security gate for CI.
 *
 * `npm audit --audit-level=high` at the monorepo root is too broad: it fails the
 * frontend job on advisories in the separate mobile/Expo app and in build/test
 * tooling that never reach the shipped browser bundle. This gate scopes the audit
 * to the frontend workspace's PRODUCTION deps and fails on any high/critical
 * advisory — EXCEPT a small allowlist of documented false positives that npm's
 * workspace audit cannot prune (transitive Node-only libs reachable solely through
 * a devDependency).
 *
 * A new real high/critical in a shipped frontend dependency still fails the build.
 *
 * Allowlist (id → why):
 *   GHSA-96hv-2xvq-fx4p  ws memory-exhaustion DoS — reachable ONLY via jsdom, a
 *                        test-time devDependency; ws is a Node WebSocket lib and is
 *                        never bundled into the browser app.
 */
import { execSync } from 'node:child_process'

const ALLOWLIST = new Set(['GHSA-96hv-2xvq-fx4p'])

let raw = ''
try {
  raw = execSync('npm audit --workspace=frontend --omit=dev --audit-level=high --json', {
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'ignore'],
  })
} catch (err) {
  // npm audit exits non-zero when advisories are found; the JSON is still on stdout.
  raw = err.stdout ? err.stdout.toString() : ''
}

if (!raw.trim()) {
  console.error('check-frontend-audit: npm audit produced no output')
  process.exit(2)
}

let report
try {
  report = JSON.parse(raw)
} catch {
  console.error('check-frontend-audit: could not parse npm audit JSON')
  process.exit(2)
}

const advisoryId = url => {
  const m = /GHSA-[a-z0-9-]+/i.exec(url || '')
  return m ? m[0] : ''
}

const offenders = []
for (const [name, v] of Object.entries(report.vulnerabilities || {})) {
  if (v.severity !== 'high' && v.severity !== 'critical') continue
  const urls = (v.via || [])
    .filter(x => typeof x === 'object')
    .map(x => x.url)
    .filter(Boolean)
  // Allowlisted only when EVERY high/critical source for this package is allowlisted.
  const allAllowlisted = urls.length > 0 && urls.every(u => ALLOWLIST.has(advisoryId(u)))
  if (!allAllowlisted) offenders.push(`${name} (${v.severity})`)
}

if (offenders.length > 0) {
  console.error('High/critical advisories in frontend production deps:')
  for (const o of offenders) console.error(`  - ${o}`)
  console.error('\nReview and fix, or extend the allowlist in scripts/check-frontend-audit.mjs if it is a documented false positive.')
  process.exit(1)
}

console.log('Frontend production audit clean (allowlisted dev-only false positives: ws via jsdom).')

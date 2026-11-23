import * as THREE from 'three'
import { mergeGeometries } from 'three/examples/jsm/utils/BufferGeometryUtils.js'

/** Generatore pseudo-casuale con seme: stessa sala e stesse persone a ogni caricamento. */
export function rng(seed) {
  return () => {
    seed |= 0
    seed = (seed + 0x6d2b79f5) | 0
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

export const ease = (k) => (k < 0.5 ? 4 * k * k * k : 1 - Math.pow(-2 * k + 2, 3) / 2)

/** Sposta i vertici con un rumore a base di seni: rocce e ossa non sembrano più sfere perfette. */
export function noiseDeform(g, amp, f, s = 1) {
  const p = g.attributes.position
  const v = new THREE.Vector3()
  for (let i = 0; i < p.count; i++) {
    v.fromBufferAttribute(p, i)
    const n =
      Math.sin(v.x * f + s) * Math.sin(v.y * f * 1.3 + s * 2) * Math.sin(v.z * f * 0.9 + s * 3) +
      0.5 * Math.sin(v.x * f * 2.7 + s) * Math.sin(v.z * f * 2.3 + v.y * 3)
    v.multiplyScalar(1 + n * amp)
    p.setXYZ(i, v.x, v.y, v.z)
  }
  g.computeVertexNormals()
  return g
}

// mergeGeometries vuole geometrie con gli stessi attributi: tolgo indici e coordinate uv
export const flat = (g) => {
  const n = g.index ? g.toNonIndexed() : g
  if (n.attributes.uv) n.deleteAttribute('uv')
  return n
}
export const merge = (arr) => {
  const m = mergeGeometries(arr.map(flat))
  m.computeVertexNormals()
  return m
}

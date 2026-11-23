import * as THREE from 'three'
import { flat, merge, noiseDeform, rng } from './utilita'

const V = THREE.Vector3
const SUPPORTO = 0x1d1f24

// Ogni reperto è fatto di poche parti (geometria + materiale) costruite a partire da forme semplici.
function partiReperto(tipo) {
  const R = rng(tipo.length * 977 + 3)
  const P = []
  if (tipo === 'cranio') {
    const calotta = new THREE.SphereGeometry(0.3, 40, 28)
    calotta.scale(1, 0.82, 1.12)
    noiseDeform(calotta, 0.06, 7, 2)
    calotta.translate(0, 0.44, 0)
    const muso = new THREE.CylinderGeometry(0.11, 0.17, 0.36, 24)
    muso.rotateX(Math.PI / 2.6)
    muso.translate(0, 0.28, 0.27)
    const zanna = (s) =>
      new THREE.TubeGeometry(
        new THREE.CatmullRomCurve3([new V(s * 0.1, 0.22, 0.28), new V(s * 0.17, 0.08, 0.45), new V(s * 0.22, 0.1, 0.62), new V(s * 0.19, 0.22, 0.74)]),
        40, 0.03, 10,
      )
    const base = new THREE.CylinderGeometry(0.16, 0.18, 0.04, 32)
    base.translate(0, 0.02, 0)
    P.push({ geo: merge([calotta, muso]), color: 0xa88f68, rough: 0.85 })
    P.push({ geo: merge([zanna(1), zanna(-1)]), color: 0xf0e5c8, rough: 0.45 })
    P.push({ geo: base, color: SUPPORTO, metal: 0.9, rough: 0.3 })
  } else if (tipo === 'geode') {
    const guscio = new THREE.SphereGeometry(0.34, 48, 24, 0, Math.PI * 2, 0, Math.PI / 2)
    guscio.rotateX(-Math.PI / 2)
    noiseDeform(guscio, 0.08, 6, 5)
    guscio.translate(0, 0.38, 0)
    const cristalli = []
    for (let k = 0; k < 90; k++) {
      const d = new V(R() * 2 - 1, R() * 2 - 1, -R()).normalize()
      if (d.z > -0.15) d.z = -0.15 - R() * 0.3
      d.normalize()
      const c = new THREE.ConeGeometry(0.03 + R() * 0.02, 0.08 + R() * 0.08, 6)
      c.translate(0, 0.05, 0)
      c.applyQuaternion(new THREE.Quaternion().setFromUnitVectors(new V(0, 1, 0), d.clone().negate()))
      c.translate(d.x * 0.3, d.y * 0.3 + 0.38, d.z * 0.3)
      cristalli.push(c)
    }
    const bordo = new THREE.TorusGeometry(0.335, 0.03, 8, 64)
    noiseDeform(bordo, 0.05, 9, 1)
    bordo.translate(0, 0.38, 0)
    const base = new THREE.CylinderGeometry(0.14, 0.17, 0.04, 32)
    base.translate(0, 0.02, 0)
    P.push({ geo: flat(guscio), color: 0x6b6660, rough: 0.95, side: THREE.DoubleSide })
    P.push({ geo: merge(cristalli), color: 0x8e4dff, rough: 0.12, metal: 0.1, emissive: 0x2a0a66 })
    P.push({ geo: bordo, color: 0xe9e3ff, rough: 0.3 })
    P.push({ geo: base, color: SUPPORTO, metal: 0.9, rough: 0.3 })
  } else if (tipo === 'pirite') {
    const cubi = [[0, 0, 0, 0.26], [0.2, 0, 0.08, 0.18], [-0.17, 0, 0.1, 0.16], [0.05, 0.26, -0.02, 0.17], [-0.08, 0, -0.18, 0.2], [0.15, 0.18, 0.12, 0.11], [-0.14, 0.16, 0.05, 0.1]].map(
      ([x, y, z, s]) => {
        const b = new THREE.BoxGeometry(s, s, s)
        b.rotateY(R() * 1.2)
        b.rotateX((R() - 0.5) * 0.3)
        b.translate(x, y + s / 2 + 0.06, z)
        return b
      },
    )
    const roccia = new THREE.IcosahedronGeometry(0.3, 2)
    roccia.scale(1.2, 0.25, 1)
    noiseDeform(roccia, 0.15, 7, 3)
    roccia.translate(0, 0.05, 0)
    P.push({ geo: merge(cubi), color: 0xd8b13a, metal: 1, rough: 0.25 })
    P.push({ geo: flat(roccia), color: 0x4a4540, rough: 0.95 })
  } else if (tipo === 'ammonite') {
    // camere sempre più grandi lungo una spirale logaritmica
    const camere = []
    for (let k = 0; k < 96; k++) {
      const a = k * 0.19
      const r = 0.035 * Math.exp(0.105 * a)
      const s = 0.012 + 0.07 * Math.pow(k / 95, 1.3)
      const sfera = new THREE.SphereGeometry(s, 14, 10)
      sfera.translate(Math.cos(a) * r, Math.sin(a) * r + 0.36, 0)
      camere.push(sfera)
    }
    const asta = new THREE.CylinderGeometry(0.012, 0.012, 0.14, 8)
    asta.translate(0, 0.08, 0)
    const base = new THREE.CylinderGeometry(0.13, 0.15, 0.03, 32)
    base.translate(0, 0.015, 0)
    P.push({ geo: merge(camere), color: 0xc79a55, metal: 0.75, rough: 0.32 })
    P.push({ geo: merge([asta, base]), color: SUPPORTO, metal: 0.9, rough: 0.3 })
  } else {
    // druse di quarzo: prismi esagonali con la punta, cresciuti su una roccia
    const roccia = new THREE.IcosahedronGeometry(0.26, 2)
    roccia.scale(1.3, 0.45, 1)
    noiseDeform(roccia, 0.14, 7, 8)
    roccia.translate(0, 0.1, 0)
    const cristalli = []
    for (let k = 0; k < 12; k++) {
      const h = 0.16 + R() * 0.3
      const w = 0.03 + R() * 0.03
      const prisma = new THREE.CylinderGeometry(w, w, h, 6)
      prisma.translate(0, h / 2, 0)
      const punta = new THREE.ConeGeometry(w, w * 1.8, 6)
      punta.translate(0, h + w * 0.9, 0)
      const g = merge([prisma, punta])
      g.rotateZ((R() - 0.5) * 1.1)
      g.rotateX((R() - 0.5) * 1.1)
      g.translate((R() - 0.5) * 0.36, 0.13, (R() - 0.5) * 0.24)
      cristalli.push(g)
    }
    P.push({ geo: flat(roccia), color: 0x8a8178, rough: 0.95 })
    P.push({ geo: merge(cristalli), color: 0xf2f8ff, rough: 0.04, vetro: true })
  }
  return P
}

/** Modello 3D del reperto, pronto da mettere nella teca. */
export function creaReperto(tipo, ambiente) {
  const g = new THREE.Group()
  partiReperto(tipo).forEach((p) => {
    const m = p.vetro
      ? new THREE.MeshPhysicalMaterial({ color: p.color, roughness: p.rough, metalness: 0, transparent: true, opacity: 0.6, envMap: ambiente, envMapIntensity: 2.4, clearcoat: 1 })
      : new THREE.MeshStandardMaterial({ color: p.color, roughness: p.rough ?? 0.6, metalness: p.metal ?? 0, envMap: ambiente, envMapIntensity: 1, emissive: p.emissive ?? 0, side: p.side ?? THREE.FrontSide })
    g.add(new THREE.Mesh(p.geo, m))
  })
  return g
}

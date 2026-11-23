import * as THREE from 'three'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'
import { clone as cloneSkinned } from 'three/examples/jsm/utils/SkeletonUtils.js'
import { rng } from './utilita'

// Avatar dei visitatori: un uomo (Ready Player Me, dagli esempi di three.js) e una donna (Avaturn, dagli esempi
// del progetto TalkingHead, uso non commerciale), animati con le pose di attesa della libreria Ready Player Me.
// I file .glb stanno in public/models e si scaricano solo la prima volta che si apre la sala.
const BASE = `${import.meta.env.BASE_URL}models/`
const ANIM_UOMO = [1, 2, 3, 4, 5].map((n) => `attesa-uomo-${n}.glb`)
const ANIM_DONNA = [1, 2, 3].map((n) => `attesa-donna-${n}.glb`)

const SKIN = [0xffffff, 0xf3dccb, 0xe0b896, 0xc08a64, 0x8a5a3c, 0xffe9dc]
const MAGLIE = [0xffffff, 0x2c3e66, 0x8c2f39, 0x2f6b4f, 0xd9c7a3, 0x3a3a40, 0x6b4c9a, 0xc77d3a, 0x9fb7d4]
const PANTALONI = [0xffffff, 0x2b2f3a, 0x4a3a2c, 0x1f2a44, 0x6d6f73, 0xb8a98c]

let caricamento = null // promessa condivisa: tornando sulla pagina i modelli non si riscaricano

/** Texture e materiali dei modelli originali: sono condivisi da tutti i cloni, quindi non vanno mai liberati. */
export const TEXTURE_CONDIVISE = new Set()
export const MATERIALI_CONDIVISI = new Set()

function segnaCondivisi(scena) {
  scena.traverse((o) => {
    if (!o.isMesh) return
    o.userData.condiviso = true
    MATERIALI_CONDIVISI.add(o.material)
    for (const k in o.material) {
      const v = o.material[k]
      if (v && v.isTexture) TEXTURE_CONDIVISE.add(v)
    }
  })
}

// Altezza del modello dai piedi alla cima della testa, per scalarlo a una statura realistica
function altezza(scena, predefinita) {
  scena.updateMatrixWorld(true)
  const p = new THREE.Vector3()
  let cima = null
  let piedi = Infinity
  scena.traverse((o) => {
    if (!o.isBone) return
    o.getWorldPosition(p)
    if (/HeadTop_End$/.test(o.name)) cima = p.y
    if (/(Toe_End|ToeBase|Foot)$/.test(o.name)) piedi = Math.min(piedi, p.y)
  })
  if (piedi === Infinity) piedi = 0
  return Math.max(0.5, (cima !== null ? cima - piedi : predefinita) * 1.01)
}

async function clipDi(loader, file) {
  const g = await loader.loadAsync(BASE + file)
  const clip = g.animations[0]
  clip.tracks = clip.tracks.filter((t) => t.name.endsWith('.quaternion')) // solo rotazioni: niente scivolamenti
  return clip
}

/**
 * Le pose femminili sono registrate su uno scheletro in T-pose, l'avatar Avaturn sta in A-pose.
 * Riporto ogni fotogramma in coordinate mondo: a ogni osso della donna applico la stessa rotazione
 * "rispetto alla posa di riposo" che ha l'osso corrispondente dell'uomo.
 */
function adattaAnimazione(sorgente, clip, destinazione, fps = 30) {
  const Q = THREE.Quaternion
  const ossaS = {}
  const ossaD = {}
  sorgente.traverse((o) => o.isBone && (ossaS[o.name] = o))
  destinazione.traverse((o) => o.isBone && (ossaD[o.name] = o))
  sorgente.updateMatrixWorld(true)
  destinazione.updateMatrixWorld(true)
  const profondita = (o) => {
    let d = 0
    while (o.parent) {
      d++
      o = o.parent
    }
    return d
  }
  const coppie = Object.keys(ossaS)
    .map((n) => [ossaS[n], ossaD[n]])
    .filter((c) => c[1])
    .sort((a, b) => profondita(a[1]) - profondita(b[1]))
  const salvaS = Object.values(ossaS).map((b) => [b, b.quaternion.clone()])
  const salvaD = coppie.map(([, d]) => [d, d.quaternion.clone()])

  // 1) allineo la posa di riposo della donna a quella dell'uomo, osso per osso (direzione verso il figlio)
  const corrispondente = new Map(coppie)
  const preferiti = ['Spine', 'Neck', 'Head', 'HandMiddle1', 'Hand', 'ForeArm', 'Leg', 'Foot', 'ToeBase']
  const a = new THREE.Vector3()
  const b = new THREE.Vector3()
  const c = new THREE.Vector3()
  const d = new THREE.Vector3()
  const qp = new Q()
  coppie.forEach(([s, t]) => {
    const figli = s.children.filter((f) => f.isBone && corrispondente.has(f))
    if (!figli.length) return
    const figlio = figli.find((f) => preferiti.some((p) => f.name.endsWith(p))) || figli[0]
    s.getWorldPosition(a)
    figlio.getWorldPosition(b)
    b.sub(a).normalize()
    t.getWorldPosition(c)
    corrispondente.get(figlio).getWorldPosition(d)
    d.sub(c).normalize()
    if (b.lengthSq() < 0.5 || d.lengthSq() < 0.5) return
    const nuova = new Q().setFromUnitVectors(d, b).multiply(t.getWorldQuaternion(new Q()))
    t.parent.getWorldQuaternion(qp)
    t.quaternion.copy(qp.invert().multiply(nuova))
    t.updateMatrixWorld(true)
  })

  // 2) campiono la clip sull'uomo e ricopio le rotazioni sulla donna
  const riposoS = coppie.map(([s]) => s.getWorldQuaternion(new Q()).invert())
  const riposoD = coppie.map(([, t]) => t.getWorldQuaternion(new Q()))
  const mixer = new THREE.AnimationMixer(sorgente)
  mixer.clipAction(clip).play()
  const n = Math.max(2, Math.ceil(clip.duration * fps) + 1)
  const tempi = new Float32Array(n)
  const valori = coppie.map(() => new Float32Array(n * 4))
  const qs = new Q()
  const qpad = new Q()
  const qt = new Q()
  for (let i = 0; i < n; i++) {
    const t = Math.min(clip.duration, i / fps)
    tempi[i] = t
    mixer.setTime(t)
    sorgente.updateMatrixWorld(true)
    coppie.forEach(([s, dest], k) => {
      s.getWorldQuaternion(qs)
      qt.copy(qs).multiply(riposoS[k]).multiply(riposoD[k])
      dest.parent.getWorldQuaternion(qpad)
      dest.quaternion.copy(qpad.invert().multiply(qt))
      dest.updateMatrixWorld(true)
      dest.quaternion.toArray(valori[k], i * 4)
    })
  }
  mixer.stopAllAction()
  salvaS.forEach(([o, q]) => o.quaternion.copy(q))
  salvaD.forEach(([o, q]) => o.quaternion.copy(q))
  sorgente.updateMatrixWorld(true)
  destinazione.updateMatrixWorld(true)
  return new THREE.AnimationClip(
    clip.name + '_adattata',
    clip.duration,
    coppie.map(([, t], k) => new THREE.QuaternionKeyframeTrack(t.name + '.quaternion', tempi, valori[k])),
  )
}

async function scarica() {
  const loader = new GLTFLoader()
  const modelli = []
  const uomo = await loader.loadAsync(BASE + 'uomo.glb')
  const clipUomo = await Promise.all(ANIM_UOMO.map((f) => clipDi(loader, f)))
  segnaCondivisi(uomo.scene)
  modelli.push({ scena: uomo.scene, scala: 1.76 / altezza(uomo.scene, 1.75), clip: clipUomo, tipo: 'uomo' })
  try {
    const donna = await loader.loadAsync(BASE + 'donna.glb')
    const clipDonna = await Promise.all(ANIM_DONNA.map((f) => clipDi(loader, f)))
    const adattate = clipDonna.map((c) => adattaAnimazione(uomo.scene, c, donna.scene))
    segnaCondivisi(donna.scene)
    modelli.push({ scena: donna.scene, scala: 1.68 / altezza(donna.scene, 1.7), clip: adattate, tipo: 'donna' })
  } catch (e) {
    // senza la donna la sala funziona lo stesso, solo con visitatori maschi
    console.warn('Avatar femminile non caricato', e)
  }
  return modelli
}

export function caricaModelli() {
  caricamento ??= scarica().catch((e) => {
    caricamento = null // al prossimo tentativo si riprova
    throw e
  })
  return caricamento
}

/** Crea il visitatore del posto n: aspetto, altezza e posa dipendono solo dal numero, quindi sono sempre gli stessi. */
export function creaPersona(modelli, n) {
  const R = rng(n * 131 + 7)
  const base = modelli[modelli.length > 1 && R() < 0.5 ? 1 : 0]
  const o = cloneSkinned(base.scena)
  o.scale.setScalar(base.scala * (0.9 + R() * 0.14))
  const scegli = (lista) => new THREE.Color(lista[Math.floor(R() * lista.length)])
  const pelle = scegli(SKIN)
  const maglia = scegli(MAGLIE)
  const pantaloni = scegli(PANTALONI)
  const barba = R() < 0.45
  const cappello = R() < 0.3
  const parti = []
  o.traverse((m) => {
    if (!m.isMesh) return
    m.frustumCulled = false
    const nascosta = (m.name === 'Wolf3D_Beard' && !barba) || (m.material.name === 'Wolf3D_Headwear' && !cappello)
    if (nascosta) {
      m.visible = false
      return
    }
    const vero = m.material.clone()
    vero.envMapIntensity = 0.8
    vero.metalness = 0
    vero.roughness = Math.max(0.55, vero.roughness ?? 0.8)
    const nome = m.material.name
    if (base.tipo === 'donna') {
      if (/look/.test(m.name)) vero.color.multiply(new THREE.Color([0xffffff, 0x9fb2e0, 0xd08a8a, 0xe8d6b8, 0xb8b8b8, 0x8fb39a][Math.floor(R() * 6)]))
      else if (/hair/.test(m.name)) vero.color.multiply(new THREE.Color([0xffffff, 0x7a5a40, 0xffe2b0, 0x4a3a30][Math.floor(R() * 4)]))
    } else if (nome === 'Wolf3D_Skin' || nome === 'Wolf3D_Body') vero.color.multiply(pelle)
    else if (nome === 'Wolf3D_Outfit_Top') vero.color.multiply(maglia)
    else if (nome === 'Wolf3D_Outfit_Bottom') vero.color.multiply(pantaloni)
    parti.push({ m, vero })
  })
  const clip = base.clip[Math.floor(R() * base.clip.length)]
  const mixer = new THREE.AnimationMixer(o)
  const azione = mixer.clipAction(clip)
  azione.time = R() * clip.duration // non si muovono tutti allo stesso tempo
  azione.timeScale = 0.85 + R() * 0.3
  azione.play()
  return { o, parti, mixer, dissolvenza: 1, modo: null }
}

/** 'fantasma' = posto libero (figura trasparente), 'vera' = posto scelto o occupato. */
export function vestiPersona(p, modo, materialeFantasma) {
  p.parti.forEach(({ m, vero }) => (m.material = modo === 'fantasma' ? materialeFantasma : vero))
  p.modo = modo
}

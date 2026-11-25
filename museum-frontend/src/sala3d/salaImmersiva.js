import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js'
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js'
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js'
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js'
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js'
import { caricaModelli, creaPersona, MATERIALI_CONDIVISI, TEXTURE_CONDIVISE, vestiPersona } from './persone'
import { creaReperto } from './reperti3D'
import { FRAMMENTO_CONO, FRAMMENTO_SCHERMO, frammentoProiezione, VERTICE_CONO, VERTICE_PROIEZIONE, VERTICE_SCHERMO } from './shader'
import { blobTex, floorTex, haloTex, numeroTex, textTex, wallGlowTex } from './texture'
import { ease, rng } from './utilita'

const V = THREE.Vector3
const C = THREE.Color
const MAX_POSTI = 20 // come il limite di capienza del backend
const RIDOTTO = matchMedia('(prefers-reduced-motion: reduce)').matches

// Il backend non ha un "tema" per l'itinerario: lo scelgo dall'id, così ogni itinerario ha sempre la sua atmosfera
const TEMI = [
  { luce: 0x3b6dff, schermo: 0 }, // cristalli
  { luce: 0x1fc7a6, schermo: 1 }, // ere geologiche
  { luce: 0x2aa7ff, schermo: 2 }, // mare giurassico
]

/**
 * Distribuisce i visitatori davanti ai punti di interesse (schermo, teche, pannelli, centro sala),
 * a piccoli gruppi e girati verso ciò che guardano, come in un museo vero.
 * Alla fine li numero da davanti a dietro e da sinistra a destra: il numero è quello del posto nel backend.
 */
function disponiPosti(n, punti, limiti, seme) {
  const R = rng(seme)
  const fuori = []
  const pesoTotale = punti.reduce((a, p) => a + p.w, 0)
  const quote = punti.map((p) => Math.floor((n * p.w) / pesoTotale))
  let resto = n - quote.reduce((a, q) => a + q, 0)
  const ordine = punti.map((_, i) => i).sort((i, j) => punti[j].w - punti[i].w)
  for (let k = 0; resto > 0; k++, resto--) quote[ordine[k % ordine.length]]++

  punti.forEach((p, pi) => {
    for (let k = 0; k < quote[pi]; k++) {
      let ok = false
      let tentativi = 0
      let x = 0
      let z = 0
      while (!ok && tentativi < 500) {
        tentativi++
        const g = tentativi / 150 // se non trovo spazio allargo piano piano il raggio
        const a = (R() - 0.5) * p.spread * (1 + g * 0.4)
        const r = p.r0 + R() * (p.r1 - p.r0) + g * 0.9
        const base = Math.atan2(p.nx, p.nz)
        x = p.x + Math.sin(base + a) * r
        z = p.z + Math.cos(base + a) * r
        ok =
          x > limiti.x0 && x < limiti.x1 && z > limiti.z0 && z < limiti.z1 &&
          fuori.every((o) => Math.hypot(o.x - x, o.z - z) > 1.2) &&
          limiti.ostacoli.every((o) => Math.hypot(o.x - x, o.z - z) > o.r)
      }
      fuori.push({ x, z, verso: Math.atan2(p.x - x + (R() - 0.5) * 0.8, p.z - z) })
    }
  })
  fuori.sort((a, b) => Math.round(a.z / 1.8) - Math.round(b.z / 1.8) || a.x - b.x)
  fuori.forEach((p, i) => (p.n = i + 1))
  return fuori
}

/**
 * Crea la sala immersiva dentro "contenitore" e la collega a chi la usa tramite callback.
 * Restituisce i comandi per aggiornare i posti e per distruggere tutto quando si lascia la pagina.
 */
export async function creaSala(contenitore, { tour, reperti, occupati = [], selezionati = [], onPosto, onReperto, onPassaggio, onAvviso }) {
  const modelli = await caricaModelli() // prima i modelli: se il download fallisce non resta niente da liberare
  const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' }) // lancia un errore se manca WebGL
  renderer.setPixelRatio(Math.min(devicePixelRatio, 1.25)) // oltre non si nota la differenza e la scheda video fatica di più
  renderer.toneMapping = THREE.ACESFilmicToneMapping
  renderer.toneMappingExposure = 1.05
  const tela = renderer.domElement
  tela.style.display = 'block'
  tela.style.touchAction = 'none'

  const generatore = new THREE.PMREMGenerator(renderer)
  const ambiente = generatore.fromScene(new RoomEnvironment(), 0.04).texture

  contenitore.prepend(tela)
  const mondo = costruisciSala()
  const camera = new THREE.PerspectiveCamera(42, 1, 0.1, 120)
  const casa = vistaIniziale()
  camera.position.copy(casa.p).add(new V(0, 2, 4))

  const controlli = new OrbitControls(camera, tela)
  Object.assign(controlli, {
    enableDamping: true, dampingFactor: 0.08, enablePan: false, minDistance: 4, maxDistance: 26,
    maxPolarAngle: 1.45, minPolarAngle: 0.3, minAzimuthAngle: -1.2, maxAzimuthAngle: 1.2,
  })
  controlli.target.copy(casa.t)

  // Volo morbido della telecamera da un punto all'altro (ingresso, teca, vista dal posto)
  let volo = null
  const volaA = (p, t, durata = 1.6) => {
    volo = { p0: camera.position.clone(), t0: controlli.target.clone(), p1: p.clone(), t1: t.clone(), inizio: performance.now(), durata: (RIDOTTO ? 0.01 : durata) * 1000 }
  }
  volaA(casa.p, casa.t, 1.2)

  const composer = new EffectComposer(renderer)
  composer.addPass(new RenderPass(mondo.scena, camera))
  const bagliore = new UnrealBloomPass(new THREE.Vector2(1, 1), 0.55, 0.5, 0.82)
  composer.addPass(bagliore)
  composer.addPass(new OutputPass())

  function ridimensiona() {
    const w = contenitore.clientWidth
    const h = contenitore.clientHeight
    if (!w || !h) return
    renderer.setSize(w, h, false)
    tela.style.width = '100%'
    tela.style.height = '100%'
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    composer.setSize(w, h)
  }
  const osservatore = new ResizeObserver(ridimensiona)
  osservatore.observe(contenitore)
  ridimensiona()

  // ---------- costruzione della sala a partire dall'itinerario ----------
  function costruisciSala() {
    const tema = TEMI[((tour.id % TEMI.length) + TEMI.length) % TEMI.length]
    const LUCE = new C(tema.luce)
    const cap = Math.min(tour.capienza, MAX_POSTI)
    // la stanza cresce con la capienza
    const W = Math.max(16, Math.sqrt(cap) * 3.3 + 5)
    const zBack = -9.2
    const D = Math.max(12.5, cap * 0.3 + 8.5)
    const zFront = zBack + D
    const H = 5.2
    const cz = (zBack + zFront) / 2
    const scena = new THREE.Scene()
    scena.background = new C(0x02040a)
    scena.fog = new THREE.Fog(0x02040a, 22, 48)

    // pavimento
    const ft = floorTex()
    ft.repeat.set(W / 6, D / 6)
    const pavimento = new THREE.Mesh(new THREE.PlaneGeometry(W, D), new THREE.MeshStandardMaterial({ map: ft, roughness: 0.55, metalness: 0.05, envMap: ambiente, envMapIntensity: 0.35 }))
    pavimento.rotation.x = -Math.PI / 2
    pavimento.position.z = cz
    scena.add(pavimento)

    // pareti: solo tre, la quarta è aperta verso chi guarda (come una casa delle bambole)
    const bagliorePareti = wallGlowTex()
    const parete = (w, x, z, ry) => {
      const m = new THREE.Mesh(new THREE.PlaneGeometry(w, H), new THREE.MeshStandardMaterial({ color: 0x0b1530, roughness: 0.9, emissive: LUCE.clone().multiplyScalar(0.55), emissiveMap: bagliorePareti }))
      m.position.set(x, H / 2, z)
      m.rotation.y = ry
      scena.add(m)
    }
    parete(W, 0, zBack, 0)
    parete(D, -W / 2, cz, Math.PI / 2)
    parete(D, W / 2, cz, -Math.PI / 2)
    const soffitto = new THREE.Mesh(new THREE.PlaneGeometry(W, D), new THREE.MeshStandardMaterial({ color: 0x05070d, roughness: 1 }))
    soffitto.rotation.x = Math.PI / 2
    soffitto.position.set(0, H, cz)
    scena.add(soffitto)

    // strisce LED a terra e sotto il soffitto
    const led = new THREE.MeshBasicMaterial({ color: LUCE.clone().multiplyScalar(2.2), toneMapped: false })
    ;[[W, 0, zBack + 0.03, 0], [D, -W / 2 + 0.03, cz, Math.PI / 2], [D, W / 2 - 0.03, cz, -Math.PI / 2]].forEach(([w, x, z, ry]) =>
      [0.04, H - 0.04].forEach((y) => {
        const s = new THREE.Mesh(new THREE.PlaneGeometry(w, 0.05), led)
        s.position.set(x, y, z)
        s.rotation.y = ry
        scena.add(s)
      }),
    )

    // faretti a incasso (si vedono bene nella vista dal posto)
    const faretto = new THREE.MeshBasicMaterial({ color: 0xfff4e0 })
    const discoFaretto = new THREE.CircleGeometry(0.09, 20)
    for (let x = -W / 2 + 2.5; x < W / 2 - 1; x += 3.4) {
      for (let z = zBack + 2; z < zFront - 1; z += 3.4) {
        const d = new THREE.Mesh(discoFaretto, faretto)
        d.rotation.x = Math.PI / 2
        d.position.set(x, H - 0.01, z)
        scena.add(d)
      }
    }

    // schermo animato sul fondo
    const sw = Math.min(W - 6, 11)
    const sh = sw / 2.6
    const materialeSchermo = new THREE.ShaderMaterial({ uniforms: { uT: { value: 0 }, uMode: { value: tema.schermo }, uCol: { value: LUCE } }, vertexShader: VERTICE_SCHERMO, fragmentShader: FRAMMENTO_SCHERMO, toneMapped: false })
    const schermo = new THREE.Mesh(new THREE.PlaneGeometry(sw, sh), materialeSchermo)
    schermo.position.set(0, 2.75, zBack + 0.04)
    scena.add(schermo)
    const cornice = new THREE.Mesh(new THREE.PlaneGeometry(sw + 0.16, sh + 0.16), new THREE.MeshStandardMaterial({ color: 0x02030a, roughness: 0.4 }))
    cornice.position.set(0, 2.75, zBack + 0.035)
    scena.add(cornice)
    const riflesso = new THREE.PointLight(LUCE, 18, 12, 1.6)
    riflesso.position.set(0, 2.6, zBack + 2.2)
    scena.add(riflesso)

    // pannelli retroilluminati sulle pareti laterali, uno per reperto a rotazione
    const pannelli = []
    const alone = haloTex()
    let indice = 0
    ;[-1, 1].forEach((lato) => {
      for (let z = zBack + 2.4; z < zFront - 2; z += 3.6) {
        const g = new THREE.Group()
        g.position.set(lato * (W / 2 - 0.02), 2.3, z)
        g.rotation.y = (-lato * Math.PI) / 2
        scena.add(g)
        pannelli.push({ x: (lato * W) / 2, z, nx: -lato, nz: 0 })
        const sfondo = new THREE.Mesh(new THREE.PlaneGeometry(2.6, 2.1), new THREE.MeshBasicMaterial({ map: alone, color: LUCE.clone().multiplyScalar(2), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, toneMapped: false }))
        sfondo.position.z = 0.01
        g.add(sfondo)
        const reperto = reperti[indice++ % reperti.length]
        const testo = textTex([reperto.nome, 'Scopri il reperto in sala'], { w: 512, h: 300, bg: '#060a16', fg: '#dbe6ff', font: '600 44px Inter, sans-serif' })
        const pannello = new THREE.Mesh(new THREE.PlaneGeometry(1.7, 1.0), new THREE.MeshBasicMaterial({ map: testo, toneMapped: false }))
        pannello.position.z = 0.03
        g.add(pannello)
      }
    })

    // cono di luce del proiettore a soffitto
    const area = { x: 0, z: cz + 1, rx: W / 2 - 2.5, rz: D / 2 - 2.5 }
    const cono = new THREE.Mesh(
      new THREE.CylinderGeometry(0.08, 1, 1, 48, 1, true),
      new THREE.ShaderMaterial({ transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide, uniforms: { c: { value: LUCE } }, vertexShader: VERTICE_CONO, fragmentShader: FRAMMENTO_CONO }),
    )
    {
      // il cono parte dal soffitto; il corpo del proiettore non lo disegno perché dall'alto copriva il centro della sala
      const alto = new V(0, H - 0.5, area.z + area.rz + 1.2)
      const direzione = new V(area.x, 0, area.z).sub(alto)
      const raggio = Math.max(area.rx, area.rz) * 0.9
      cono.scale.set(raggio, direzione.length(), raggio)
      cono.position.copy(alto).addScaledVector(direzione, 0.5)
      cono.quaternion.setFromUnitVectors(new V(0, -1, 0), direzione.normalize())
      scena.add(cono)
    }
    const materialeProiezione = new THREE.ShaderMaterial({
      transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, toneMapped: false,
      uniforms: {
        uT: { value: 0 },
        uCol: { value: LUCE.clone().lerp(new C(0xffffff), 0.25) },
        uSpots: { value: Array.from({ length: MAX_POSTI }, () => new THREE.Vector4()) },
        uN: { value: 0 },
        uRip: { value: Array.from({ length: 8 }, () => new THREE.Vector4(0, 0, -99, 0)) },
        uArea: { value: new THREE.Vector4(area.x, area.z, area.rx + 2, area.rz + 2) },
        uHot: { value: new THREE.Vector3(0, cz + 0.6, 1.15) },
      },
      vertexShader: VERTICE_PROIEZIONE,
      fragmentShader: frammentoProiezione(MAX_POSTI),
    })
    const proiezione = new THREE.Mesh(new THREE.PlaneGeometry(W, D), materialeProiezione)
    proiezione.rotation.x = -Math.PI / 2
    proiezione.position.set(0, 0.006, cz)
    scena.add(proiezione)

    // luci
    scena.add(new THREE.HemisphereLight(0x8fb0ff, 0x0a0c12, 0.35))
    const principale = new THREE.SpotLight(0xe8eeff, 110, 30, 0.7, 0.8, 1.2)
    principale.position.set(-1, H - 0.2, cz + 3)
    principale.target.position.set(0, 0, cz + 1)
    scena.add(principale, principale.target)
    const dallAlto = new THREE.DirectionalLight(0xdfe8ff, 1.1)
    dallAlto.position.set(1.5, 14, cz + 3)
    dallAlto.target.position.set(0, 0, cz)
    scena.add(dallAlto, dallAlto.target)
    const frontale = new THREE.DirectionalLight(0x9fb8ff, 0.6)
    frontale.position.set(0, 6, zFront + 8)
    scena.add(frontale)

    // teche disposte ad arco, tante quanti sono i reperti
    const teche = []
    const nT = reperti.length
    reperti.forEach((ex, k) => {
      const t = nT === 1 ? 0 : k / (nT - 1) - 0.5
      const ampiezza = Math.min(W - 4.5, nT * 3.4)
      const g = new THREE.Group()
      g.position.set(t * ampiezza, 0, zBack + 2.3 + Math.pow(Math.abs(t) * 2, 2) * 1.6)
      g.rotation.y = -t * 0.9
      scena.add(g)
      const plinto = new THREE.Mesh(new THREE.BoxGeometry(1.1, 1, 1.1), new THREE.MeshStandardMaterial({ color: 0xece6da, roughness: 0.7, emissive: 0x2a2a30 }))
      plinto.position.y = 0.5
      g.add(plinto)
      const piano = new THREE.Mesh(new THREE.BoxGeometry(1.0, 0.03, 1.0), new THREE.MeshStandardMaterial({ color: 0x9aa5b4, roughness: 0.5 }))
      piano.position.y = 1.015
      g.add(piano)
      const geoVetro = new THREE.BoxGeometry(1.04, 0.92, 1.04)
      const vetro = new THREE.Mesh(geoVetro, new THREE.MeshPhysicalMaterial({ color: 0xffffff, roughness: 0.03, transparent: true, opacity: 0.12, envMap: ambiente, envMapIntensity: 2, depthWrite: false }))
      vetro.position.y = 1.47
      g.add(vetro)
      const spigoli = new THREE.LineSegments(new THREE.EdgesGeometry(geoVetro), new THREE.LineBasicMaterial({ color: 0xdfe6f2, transparent: true, opacity: 0.55 }))
      spigoli.position.y = 1.47
      g.add(spigoli)
      const modello = creaReperto(ex.tipo, ambiente)
      modello.position.y = 1.03
      modello.scale.setScalar(1.12)
      g.add(modello)
      const targhetta = new THREE.Mesh(new THREE.PlaneGeometry(0.66, 0.18), new THREE.MeshStandardMaterial({ map: textTex(ex.nome, { font: '600 34px Georgia, serif' }), metalness: 0.5, roughness: 0.45, envMap: ambiente }))
      targhetta.position.set(0, 0.62, 0.556)
      g.add(targhetta)
      const luceTeca = new THREE.SpotLight(0xfff0d8, 12, 7, 0.3, 0.55, 1.3)
      luceTeca.position.set(0, H - 0.1, 0.4)
      luceTeca.target = modello
      g.add(luceTeca)
      teche.push({ g, vetro, ex })
    })

    // posti: una persona per posto, con ombra e numero proiettato a terra
    const materialeFantasma = new THREE.MeshStandardMaterial({ color: 0x081226, emissive: LUCE.clone().lerp(new C(0xffffff), 0.2), emissiveIntensity: 0.55, roughness: 0.4, transparent: true, opacity: 0.26, depthWrite: false })
    const geoOmbra = new THREE.PlaneGeometry(0.9, 0.9)
    const materialeOmbra = new THREE.MeshBasicMaterial({ map: blobTex(), transparent: true, depthWrite: false, opacity: 0.75 })
    const geoNumero = new THREE.PlaneGeometry(0.9, 0.45)
    const geoPresa = new THREE.CylinderGeometry(0.42, 0.42, 1.8, 10)
    const materialePresa = new THREE.MeshBasicMaterial()
    const puntiInteresse = [
      { x: 0, z: zBack, nx: 0, nz: 1, r0: 4.6, r1: 7.2, spread: 1.1, w: Math.max(2, cap / 10) },
      ...teche.map((t) => ({ x: t.g.position.x, z: t.g.position.z, nx: Math.sin(t.g.rotation.y), nz: Math.cos(t.g.rotation.y), r0: 1.3, r1: 2.4, spread: 1.7, w: 1.4 })),
      ...pannelli.filter((p) => p.z < zFront - 3.5).map((p) => ({ ...p, r0: 1.2, r1: 2.2, spread: 1.1, w: 0.6 })),
      { x: 0, z: cz + 0.6, nx: 0, nz: 1, r0: 1.7, r1: 2.5, spread: 6.28, w: Math.max(1.5, cap / 9) },
    ]
    const limiti = {
      x0: -W / 2 + 0.9, x1: W / 2 - 0.9, z0: zBack + 1.2, z1: zFront - 1.4,
      ostacoli: [...teche.map((t) => ({ x: t.g.position.x, z: t.g.position.z, r: 1.15 })), { x: 0, z: cz + 0.6, r: 1.6 }],
    }
    const posti = disponiPosti(cap, puntiInteresse, limiti, tour.id * 31 + cap)
    const seats = posti.map((p) => {
      const persona = creaPersona(modelli, p.n)
      persona.o.position.set(p.x, 0, p.z)
      persona.o.rotation.y = p.verso
      scena.add(persona.o)
      const ombra = new THREE.Mesh(geoOmbra, materialeOmbra)
      ombra.rotation.x = -Math.PI / 2
      ombra.position.set(p.x, 0.004, p.z)
      ombra.renderOrder = -1
      scena.add(ombra)
      const numero = new THREE.Mesh(
        geoNumero,
        new THREE.MeshBasicMaterial({ map: numeroTex(p.n), transparent: true, depthWrite: false, toneMapped: false }),
      )
      numero.rotation.x = -Math.PI / 2
      numero.position.set(p.x, 0.008, p.z + 0.78)
      scena.add(numero)
      // cilindro invisibile più grande della persona: rende il clic facile anche sulle figure sottili
      const presa = new THREE.Mesh(geoPresa, materialePresa)
      presa.visible = false
      presa.position.set(p.x, 0.9, p.z)
      scena.add(presa)
      const s = { ...p, st: 'free', persona, numero, presa, hover: 0 }
      presa.userData.posto = s
      vestiPersona(persona, 'fantasma', materialeFantasma)
      return s
    })
    return { scena, seats, teche, materialeProiezione, materialeSchermo, materialeFantasma, indiceOnda: 0, W, D, zBack, zFront, cz }
  }

  function vistaIniziale() {
    return { p: new V(0, 5 + mondo.D * 0.36, mondo.zFront + 3 + mondo.D * 0.22), t: new V(0, 0.6, mondo.cz - 0.4) }
  }

  // ---------- stato dei posti ----------
  const orologio = new THREE.Clock()
  let passaggio = null // posto sotto il mouse
  let vistaPosto = null // posto da cui si sta guardando la sala

  function onda(s, negativa = false) {
    mondo.materialeProiezione.uniforms.uRip.value[mondo.indiceOnda++ % 8].set(s.x, s.z, orologio.elapsedTime, negativa ? 1 : 0)
  }

  function impostaStato(s, st) {
    const prima = s.persona.modo
    s.st = st
    vestiPersona(s.persona, st === 'free' ? 'fantasma' : 'vera', mondo.materialeFantasma)
    if (prima === 'fantasma' && st !== 'free') {
      // la persona "si materializza" con una dissolvenza
      s.persona.dissolvenza = 0
      s.persona.parti.forEach((p) => {
        p.vero.transparent = true
        p.vero.opacity = 0
      })
    }
  }

  function aggiorna(nuoviOccupati, nuoviSelezionati, conOnde = true) {
    const occ = new Set(nuoviOccupati)
    const sel = new Set(nuoviSelezionati)
    mondo.seats.forEach((s) => {
      const st = occ.has(s.n) ? 'occ' : sel.has(s.n) ? 'sel' : 'free'
      if (st === s.st) return
      const eraScelto = s.st === 'sel'
      impostaStato(s, st)
      if (conOnde && st === 'sel') onda(s)
      if (conOnde && st === 'occ' && eraScelto) onda(s, true) // qualcuno l'ha preso prima di noi
    })
    if (vistaPosto && vistaPosto.st !== 'sel') tornaAllaSala()
  }
  aggiorna(occupati, selezionati, false)

  // Compilo gli shader prima del primo fotogramma: senza questo passaggio la pagina si blocca
  // per qualche secondo al primo disegno (succede soprattutto su Windows)
  try {
    await renderer.compileAsync(mondo.scena, camera)
  } catch {
    // se il browser non supporta la compilazione in parallelo si compila al primo fotogramma come prima
  }

  function sincronizzaProiezione() {
    const u = mondo.materialeProiezione.uniforms
    u.uN.value = mondo.seats.length
    mondo.seats.forEach((s, i) => u.uSpots.value[i].set(s.x, s.z, s.st === 'free' ? 0 : s.st === 'sel' ? 1 : 2, s.hover))
  }

  // ---------- telecamera ----------
  let timerDistanza = null
  function distanzaMinimaTemporanea(valore, durata) {
    controlli.minDistance = valore
    clearTimeout(timerDistanza)
    timerDistanza = setTimeout(() => (controlli.minDistance = 4), durata)
  }

  function vistaDalPosto(n) {
    const s = mondo.seats.find((x) => x.n === n)
    if (!s) return false
    if (vistaPosto) vistaPosto.persona.o.visible = true
    vistaPosto = s
    s.persona.o.visible = false // altrimenti la telecamera finirebbe dentro la testa
    clearTimeout(timerDistanza)
    controlli.minDistance = 0.3
    volaA(new V(s.x, 1.6, s.z + 0.1), new V(s.x * 0.3, 1.9, mondo.zBack), 1.8)
    return true
  }

  function tornaAllaSala() {
    if (vistaPosto) {
      vistaPosto.persona.o.visible = true
      vistaPosto = null
    }
    const casa = vistaIniziale()
    volaA(casa.p, casa.t, 1.6)
    distanzaMinimaTemporanea(controlli.minDistance, 1700)
  }

  // ---------- mouse e touch ----------
  const raggio = new THREE.Raycaster()
  const ndc = new THREE.Vector2()
  let giu = null
  const puntaRaggio = (e) => {
    const r = tela.getBoundingClientRect()
    ndc.set(((e.clientX - r.left) / r.width) * 2 - 1, -((e.clientY - r.top) / r.height) * 2 + 1)
    raggio.setFromCamera(ndc, camera)
  }
  const prese = () => mondo.seats.filter((s) => s !== vistaPosto).map((s) => s.presa)

  function alPremere(e) {
    giu = { x: e.clientX, y: e.clientY }
  }

  function alRilascio(e) {
    if (!giu) return
    const spostamento = Math.hypot(e.clientX - giu.x, e.clientY - giu.y)
    giu = null
    if (spostamento >= 6) return // era un trascinamento per ruotare la sala, non un clic
    puntaRaggio(e)
    const colpito = raggio.intersectObjects(prese())[0]
    if (colpito) {
      const s = colpito.object.userData.posto
      if (s.st === 'occ') {
        onda(s, true)
        onAvviso?.(`Il posto ${s.n} è già prenotato`)
      } else {
        onPosto?.(s.n)
      }
      return
    }
    const teca = raggio.intersectObjects(mondo.teche.map((t) => t.vetro))[0]
    if (teca) {
      const t = mondo.teche.find((x) => x.vetro === teca.object)
      const g = t.g
      const davanti = new V(Math.sin(g.rotation.y), 0, Math.cos(g.rotation.y))
      if (vistaPosto) {
        vistaPosto.persona.o.visible = true
        vistaPosto = null
      }
      distanzaMinimaTemporanea(1.5, 1700)
      volaA(g.position.clone().addScaledVector(davanti, 2.8).setY(2.1), g.position.clone().setY(1.35), 1.6)
      onReperto?.(t.ex)
    }
  }

  function alMovimento(e) {
    puntaRaggio(e)
    const colpito = raggio.intersectObjects(prese())[0]
    passaggio = colpito ? colpito.object.userData.posto : null
    tela.style.cursor = passaggio ? (passaggio.st === 'occ' ? 'not-allowed' : 'pointer') : ''
    if (!passaggio || giu) return onPassaggio?.(null)
    const r = contenitore.getBoundingClientRect()
    onPassaggio?.({ n: passaggio.n, stato: passaggio.st, x: e.clientX - r.left, y: e.clientY - r.top })
  }

  function allUscita() {
    passaggio = null
    onPassaggio?.(null)
  }

  tela.addEventListener('pointerdown', alPremere)
  tela.addEventListener('pointerup', alRilascio)
  tela.addEventListener('pointermove', alMovimento)
  tela.addEventListener('pointerleave', allUscita)

  // ---------- ciclo di disegno ----------
  renderer.setAnimationLoop(() => {
    const dt = Math.min(orologio.getDelta(), 0.05)
    const t = orologio.elapsedTime
    if (volo) {
      const k = Math.min(1, (performance.now() - volo.inizio) / volo.durata)
      const e = ease(k)
      camera.position.lerpVectors(volo.p0, volo.p1, e)
      controlli.target.lerpVectors(volo.t0, volo.t1, e)
      if (k >= 1) volo = null
    }
    controlli.update()
    mondo.materialeProiezione.uniforms.uT.value = t
    mondo.materialeSchermo.uniforms.uT.value = t
    mondo.materialeFantasma.emissiveIntensity = 0.5 + 0.08 * Math.sin(t * 2)
    mondo.seats.forEach((s) => {
      s.persona.mixer.update(dt)
      s.hover += ((passaggio === s && s.st !== 'occ' ? 1 : 0) - s.hover) * Math.min(1, dt * 10)
      if (s.persona.dissolvenza < 1) {
        s.persona.dissolvenza = Math.min(1, s.persona.dissolvenza + dt * 1.4)
        s.persona.parti.forEach((p) => {
          p.vero.opacity = s.persona.dissolvenza
          if (s.persona.dissolvenza >= 1) p.vero.transparent = false
        })
      }
      s.numero.material.color.setHex(s.st === 'sel' ? 0x5dffb4 : s.st === 'occ' ? 0xff6f80 : 0xcfe0ff)
      s.numero.material.opacity = s.st === 'occ' ? 0.7 : 0.9 + 0.1 * s.hover
    })
    sincronizzaProiezione()
    composer.render(dt)
  })

  // ---------- pulizia: alla chiusura della pagina si libera tutta la memoria della scheda video ----------
  function distruggi() {
    renderer.setAnimationLoop(null)
    clearTimeout(timerDistanza)
    osservatore.disconnect()
    tela.removeEventListener('pointerdown', alPremere)
    tela.removeEventListener('pointerup', alRilascio)
    tela.removeEventListener('pointermove', alMovimento)
    tela.removeEventListener('pointerleave', allUscita)
    controlli.dispose()
    mondo.seats.forEach((s) => s.persona.mixer.stopAllAction())

    const geometrie = new Set()
    const materiali = new Set()
    mondo.scena.traverse((o) => {
      if (o.geometry && !o.userData.condiviso) geometrie.add(o.geometry)
      ;(Array.isArray(o.material) ? o.material : [o.material]).forEach((m) => m && materiali.add(m))
    })
    mondo.seats.forEach((s) => s.persona.parti.forEach((p) => materiali.add(p.vero)))
    geometrie.forEach((g) => g.dispose())
    materiali.forEach((m) => {
      if (MATERIALI_CONDIVISI.has(m)) return
      for (const k in m) {
        const v = m[k]
        if (v && v.isTexture && v !== ambiente && !TEXTURE_CONDIVISE.has(v)) v.dispose()
      }
      if (m.uniforms) Object.values(m.uniforms).forEach((u) => u.value?.isTexture && u.value.dispose())
      m.dispose()
    })
    bagliore.dispose()
    composer.dispose()
    ambiente.dispose()
    generatore.dispose()
    renderer.dispose()
    renderer.forceContextLoss()
    tela.remove()
  }

  return {
    aggiorna,
    vistaDalPosto,
    tornaAllaSala,
    get inVistaPosto() {
      return !!vistaPosto
    },
    distruggi,
  }
}

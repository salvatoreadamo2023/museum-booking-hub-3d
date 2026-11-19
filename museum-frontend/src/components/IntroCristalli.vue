<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const emit = defineEmits(['entra'])
const tela = ref(null)

let animazione = 0
let pulizia = () => {}

// Cristallo di quarzo: prisma esagonale con una punta sopra e una più corta sotto
function cristallo(r, h, p) {
  const v = [[0, h / 2 + p, 0]]
  for (let i = 0; i < 6; i++) v.push([r * Math.cos((i * Math.PI) / 3), h / 2, r * Math.sin((i * Math.PI) / 3)])
  for (let i = 0; i < 6; i++) v.push([r * Math.cos((i * Math.PI) / 3), -h / 2, r * Math.sin((i * Math.PI) / 3)])
  v.push([0, -h / 2 - p * 0.6, 0])
  const f = []
  for (let i = 0; i < 6; i++) {
    const j = (i + 1) % 6
    f.push([0, 1 + j, 1 + i], [1 + i, 1 + j, 7 + j, 7 + i], [13, 7 + i, 7 + j])
  }
  return { v, f }
}

const normalizza = (a) => {
  const l = Math.hypot(...a)
  return a.map((x) => x / l)
}

function ruota([x, y, z], ay, az) {
  ;[x, y] = [x * Math.cos(az) - y * Math.sin(az), x * Math.sin(az) + y * Math.cos(az)]
  ;[x, z] = [x * Math.cos(ay) + z * Math.sin(ay), -x * Math.sin(ay) + z * Math.cos(ay)]
  return [x, y, z]
}

onMounted(() => {
  const cv = tela.value
  const ctx = cv.getContext('2d')
  const fermo = matchMedia('(prefers-reduced-motion: reduce)').matches
  const luce = normalizza([-0.4, 0.7, 0.6])
  const pezzi = [
    { g: cristallo(0.42, 1.3, 0.55), x: 0, y: 0.05, z: 0, inc: 0.08, vel: 0.22 },
    { g: cristallo(0.22, 0.7, 0.3), x: -0.62, y: -0.55, z: 0.25, inc: -0.55, vel: 0.3 },
    { g: cristallo(0.16, 0.5, 0.24), x: 0.6, y: -0.62, z: -0.1, inc: 0.62, vel: 0.36 },
  ]
  const polvere = Array.from({ length: 140 }, () => ({
    x: Math.random(), y: Math.random(), r: Math.random() * 1.6 + 0.3,
    v: Math.random() * 0.00025 + 0.00008, f: Math.random() * 6,
  }))

  let W = 0, H = 0, mx = 0, my = 0, tx = 0, ty = 0

  function misura() {
    const dpr = Math.min(devicePixelRatio || 1, 2)
    W = cv.clientWidth
    H = cv.clientHeight
    cv.width = W * dpr
    cv.height = H * dpr
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  function muoviMouse(e) {
    tx = e.clientX / W - 0.5
    ty = e.clientY / H - 0.5
  }

  function disegna(t) {
    mx += (tx - mx) * 0.05
    my += (ty - my) * 0.05
    ctx.clearRect(0, 0, W, H)
    const stretto = W < 640
    const cx = stretto ? W * 0.5 : W * 0.68
    const cy = stretto ? H * 0.3 : H * 0.46
    const scala = Math.min(W, H) * (stretto ? 0.26 : 0.34)

    // fascio di luce dall'alto
    const fascio = ctx.createLinearGradient(cx, 0, cx, H)
    fascio.addColorStop(0, 'rgba(245,199,126,0.16)')
    fascio.addColorStop(1, 'rgba(245,199,126,0)')
    ctx.fillStyle = fascio
    ctx.beginPath()
    ctx.moveTo(cx - scala * 0.25, 0)
    ctx.lineTo(cx + scala * 0.25, 0)
    ctx.lineTo(cx + scala * 1.5, H)
    ctx.lineTo(cx - scala * 1.5, H)
    ctx.fill()

    // polvere che sale lentamente dentro il fascio
    for (const d of polvere) {
      if (!fermo) {
        d.y -= d.v
        if (d.y < 0) d.y = 1
      }
      const px = cx + (d.x - 0.5) * scala * 3 * (0.3 + d.y) + Math.sin(t / 2000 + d.f) * 6 + mx * 30
      const py = d.y * H + my * 20
      ctx.fillStyle = `rgba(245,199,126,${0.25 + 0.35 * Math.sin(t / 900 + d.f) ** 2})`
      ctx.beginPath()
      ctx.arc(px, py, d.r, 0, 7)
      ctx.fill()
    }

    // alone sul basamento
    const base = ctx.createRadialGradient(cx, cy + scala * 0.95, 0, cx, cy + scala * 0.95, scala * 1.2)
    base.addColorStop(0, 'rgba(217,119,6,0.28)')
    base.addColorStop(1, 'rgba(217,119,6,0)')
    ctx.fillStyle = base
    ctx.beginPath()
    ctx.ellipse(cx, cy + scala * 0.95, scala * 1.2, scala * 0.22, 0, 0, 7)
    ctx.fill()

    // raccolgo le facce di tutti i cristalli e le disegno dalla più lontana (algoritmo del pittore)
    const facce = []
    const angolo = fermo ? 0.6 : t / 1000
    for (const c of pezzi) {
      const punti = c.g.v.map((p) => {
        let q = ruota(p, angolo * c.vel * 2 + mx * 0.8, c.inc)
        q = [q[0] + c.x, q[1] + c.y + Math.sin(t / 1400 + c.vel * 9) * 0.02, q[2] + c.z]
        q = ruota(q, mx * 0.35, 0)
        const k = 3.2 / (3.2 - q[2]) // prospettiva semplice
        return { x: cx + q[0] * scala * k, y: cy - (q[1] - my * 0.15) * scala * k, z: q[2], q }
      })
      for (const f of c.g.f) {
        const [a, b, d] = [punti[f[0]].q, punti[f[1]].q, punti[f[2]].q]
        const u = [b[0] - a[0], b[1] - a[1], b[2] - a[2]]
        const w = [d[0] - a[0], d[1] - a[1], d[2] - a[2]]
        const n = normalizza([u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0]])
        facce.push({ pt: f.map((i) => punti[i]), z: f.reduce((s, i) => s + punti[i].z, 0) / f.length, n })
      }
    }
    facce.sort((p, q) => p.z - q.z)
    for (const f of facce) {
      const lam = Math.max(0, f.n[0] * luce[0] + f.n[1] * luce[1] + f.n[2] * luce[2])
      const davanti = f.n[2] > 0
      ctx.beginPath()
      f.pt.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)))
      ctx.closePath()
      ctx.fillStyle = davanti
        ? `rgba(${(200 + lam * 55) | 0},${(190 + lam * 50) | 0},${(170 + lam * 60) | 0},${0.08 + lam * 0.32})`
        : 'rgba(120,90,50,0.06)'
      ctx.fill()
      ctx.strokeStyle = davanti ? `rgba(245,199,126,${0.45 + lam * 0.5})` : 'rgba(245,199,126,0.14)'
      ctx.lineWidth = davanti ? 1.1 : 0.6
      ctx.stroke()
    }
    if (!fermo) animazione = requestAnimationFrame(disegna)
  }

  misura()
  addEventListener('resize', misura)
  cv.parentElement.addEventListener('pointermove', muoviMouse)
  animazione = requestAnimationFrame(disegna)

  pulizia = () => {
    cancelAnimationFrame(animazione)
    removeEventListener('resize', misura)
    cv.parentElement?.removeEventListener('pointermove', muoviMouse)
  }
})

onBeforeUnmount(() => pulizia())

// si entra anche scorrendo verso il basso, con uno swipe o premendo Invio
let inizioTocco = null
function rotella(e) {
  if (e.deltaY > 10) emit('entra')
}
function tocco(e) {
  if (inizioTocco !== null && inizioTocco - e.touches[0].clientY > 40) {
    inizioTocco = null
    emit('entra')
  }
}
</script>

<template>
  <section
    class="intro"
    aria-label="Ingresso"
    @wheel.passive="rotella"
    @touchstart.passive="inizioTocco = $event.touches[0].clientY"
    @touchmove.passive="tocco"
  >
    <canvas ref="tela" class="intro-scena" aria-hidden="true"></canvas>
    <div class="intro-reperto">
      <b>Quarzo ialino</b>
      SiO₂ · sistema trigonale<br />
      durezza 7 Mohs · densità 2,65 g/cm³
    </div>
    <div class="intro-testo">
      <span class="riga intro-marchio">Galleria Geologica</span>
      <h1 class="riga intro-titolo">Quattro miliardi di anni,<br /><em>un posto in sala.</em></h1>
      <p class="riga intro-desc">Prenota la visita, scegli il tuo posto nel salone in 3D e scopri da vicino fossili, geodi e cristalli.</p>
      <span class="riga"><button class="intro-entra" type="button" @click="emit('entra')">Entra nel museo <span aria-hidden="true">↓</span></button></span>
    </div>
    <div class="intro-scorri">scorri o premi invio</div>
  </section>
</template>

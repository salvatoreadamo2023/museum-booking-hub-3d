import * as THREE from 'three'
import { rng } from './utilita'

// Tutte le texture della sala sono disegnate al volo su un canvas: niente immagini da scaricare.

export function canvasTex(w, h, draw, srgb = true) {
  const c = document.createElement('canvas')
  c.width = w
  c.height = h
  draw(c.getContext('2d'), w, h)
  const t = new THREE.CanvasTexture(c)
  if (srgb) t.colorSpace = THREE.SRGBColorSpace
  return t
}

/** Testo su una o più righe (la prima più grande): targhette, pannelli, numeri dei posti. */
export function textTex(lines, { w = 512, h = 128, bg = '#b08d57', fg = '#2a1f12', font = '600 40px Georgia, serif', align = 'center' } = {}) {
  const righe = Array.isArray(lines) ? lines : [lines]
  return canvasTex(w, h, (g) => {
    if (bg) {
      g.fillStyle = bg
      g.fillRect(0, 0, w, h)
    }
    g.fillStyle = fg
    g.textAlign = align
    g.textBaseline = 'middle'
    righe.forEach((riga, i) => {
      g.font = i === 0 ? font : font.replace(/\d+px/, (m) => Math.round(parseInt(m) * 0.62) + 'px')
      g.fillText(riga, align === 'center' ? w / 2 : 16, (h / (righe.length + 1)) * (i + 1))
    })
  })
}

/** Numero del posto proiettato a terra: bianco con un bordo scuro, si legge anche sul pavimento illuminato. */
export function numeroTex(n) {
  return canvasTex(160, 80, (g, w, h) => {
    g.font = '700 54px Inter, Arial, sans-serif'
    g.textAlign = 'center'
    g.textBaseline = 'middle'
    g.lineJoin = 'round'
    g.lineWidth = 10
    g.strokeStyle = 'rgba(2, 4, 10, 0.85)'
    g.strokeText(String(n), w / 2, h / 2 + 2)
    g.fillStyle = '#ffffff'
    g.fillText(String(n), w / 2, h / 2 + 2)
  })
}

/** Ombra morbida sotto ogni persona (costa molto meno delle ombre vere). */
export function blobTex() {
  return canvasTex(128, 128, (g) => {
    const gr = g.createRadialGradient(64, 64, 0, 64, 64, 64)
    gr.addColorStop(0, 'rgba(0,0,0,.85)')
    gr.addColorStop(0.45, 'rgba(0,0,0,.45)')
    gr.addColorStop(1, 'rgba(0,0,0,0)')
    g.fillStyle = gr
    g.fillRect(0, 0, 128, 128)
  }, false)
}

/** Pavimento in resina scura con piccole imperfezioni e giunti. */
export function floorTex() {
  const t = canvasTex(1024, 1024, (g, w, h) => {
    const R = rng(4)
    g.fillStyle = '#2b2f37'
    g.fillRect(0, 0, w, h)
    for (let i = 0; i < 14000; i++) {
      g.fillStyle = `rgba(${R() < 0.5 ? '0,0,0' : '200,210,230'},${R() * 0.06})`
      g.fillRect(R() * w, R() * h, 1 + R() * 3, 1 + R() * 3)
    }
    g.strokeStyle = 'rgba(0,0,0,.35)'
    g.lineWidth = 2
    for (let k = 0; k <= 4; k++) {
      g.beginPath()
      g.moveTo(k * 256, 0)
      g.lineTo(k * 256, h)
      g.stroke()
      g.beginPath()
      g.moveTo(0, k * 256)
      g.lineTo(w, k * 256)
      g.stroke()
    }
  })
  t.wrapS = t.wrapT = THREE.RepeatWrapping
  t.anisotropy = 8
  return t
}

/** Luce LED "dipinta" sul muro: più forte in basso e in alto, dove ci sono le strisce. */
export function wallGlowTex() {
  return canvasTex(8, 256, (g, w, h) => {
    const gr = g.createLinearGradient(0, h, 0, 0)
    gr.addColorStop(0, 'rgba(255,255,255,1)')
    gr.addColorStop(0.12, 'rgba(255,255,255,.35)')
    gr.addColorStop(0.4, 'rgba(255,255,255,.06)')
    gr.addColorStop(0.92, 'rgba(255,255,255,.04)')
    gr.addColorStop(1, 'rgba(255,255,255,.3)')
    g.fillStyle = gr
    g.fillRect(0, 0, w, h)
  })
}

/** Alone rettangolare dietro i pannelli retroilluminati. */
export function haloTex() {
  return canvasTex(256, 256, (g, w, h) => {
    const img = g.createImageData(w, h)
    for (let y = 0; y < h; y++) {
      for (let x = 0; x < w; x++) {
        const dx = Math.max(0, Math.abs(x - w / 2) - w * 0.3) / (w * 0.2)
        const dy = Math.max(0, Math.abs(y - h / 2) - h * 0.3) / (h * 0.2)
        const a = Math.pow(1 - Math.min(1, Math.hypot(dx, dy)), 2.2)
        const i = (y * w + x) * 4
        img.data[i] = img.data[i + 1] = img.data[i + 2] = 255
        img.data[i + 3] = a * 255
      }
    }
    g.putImageData(img, 0, 0)
  })
}

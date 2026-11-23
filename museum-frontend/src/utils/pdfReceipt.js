import { descrizionePosti, formatDataOra, formatEuro } from './formatters'

/** Ricevuta PDF di una prenotazione (stesso aspetto del prototipo). */
export async function scaricaRicevuta(prenotazione, titolare) {
  // jsPDF è pesante: lo scarico solo quando serve davvero una ricevuta
  const { jsPDF } = await import('jspdf')
  const doc = new jsPDF()
  const posti = descrizionePosti(prenotazione.posti)
  const tariffe = Object.entries(
    prenotazione.posti.reduce((conteggio, p) => ({ ...conteggio, [p.tariffa]: (conteggio[p.tariffa] ?? 0) + 1 }), {}),
  )
    .map(([nome, quanti]) => `${nome} x${quanti}`)
    .join(', ')

  // Intestazione Museo
  doc.setFillColor(180, 83, 9) // Colore #b45309
  doc.rect(0, 0, 210, 25, 'F')

  doc.setTextColor(255, 255, 255)
  doc.setFont('helvetica', 'bold')
  doc.setFontSize(15)
  doc.text('GALLERIA GEOLOGICA - RICEVUTA PRENOTAZIONE', 15, 16)

  // Info Prenotazione
  doc.setTextColor(15, 23, 42)
  doc.setFontSize(11)
  doc.setFont('helvetica', 'bold')
  doc.text(`CODICE PRENOTAZIONE: ${prenotazione.codice}`, 15, 38)

  doc.setLineWidth(0.5)
  doc.setDrawColor(226, 232, 240)
  doc.line(15, 42, 195, 42)

  // Dettagli
  doc.setFontSize(10)
  const riga = (etichetta, valore, y, colore) => {
    doc.setTextColor(15, 23, 42)
    doc.setFont('helvetica', 'normal')
    doc.text(etichetta, 15, y)
    if (colore) doc.setTextColor(...colore)
    doc.setFont('helvetica', 'bold')
    doc.text(valore, 65, y)
  }

  riga('Titolare:', titolare, 52)
  riga('Itinerario:', prenotazione.tour, 60)
  riga('Stato:', prenotazione.stato === 'CONFERMATA' ? 'CONFERMATA' : 'ANNULLATA', 68,
    prenotazione.stato === 'CONFERMATA' ? [21, 128, 61] : [185, 28, 28])
  riga('Data & Orario:', formatDataOra(prenotazione.data, prenotazione.orario), 76)
  riga('Posti:', posti, 84)
  riga('Tariffe:', tariffe, 92)
  riga('Costo Totale:', formatEuro(prenotazione.totale), 100)

  doc.setTextColor(15, 23, 42)
  doc.line(15, 108, 195, 108)

  // QR Code Fittizio (Disegnato Vettoriale)
  doc.setFont('helvetica', 'bold')
  doc.text("PASS D'INGRESSO / QR CODE:", 15, 120)

  const startX = 15
  const startY = 126
  const size = 40

  doc.setFillColor(248, 250, 252)
  doc.rect(startX, startY, size, size, 'F')
  doc.setDrawColor(15, 23, 42)
  doc.rect(startX, startY, size, size, 'S')

  // Finder Patterns
  const finder = (x, y) => {
    doc.setFillColor(15, 23, 42)
    doc.rect(x, y, 10, 10, 'F')
    doc.setFillColor(255, 255, 255)
    doc.rect(x + 2, y + 2, 6, 6, 'F')
    doc.setFillColor(15, 23, 42)
    doc.rect(x + 4, y + 4, 2, 2, 'F')
  }
  finder(startX + 3, startY + 3)
  finder(startX + 27, startY + 3)
  finder(startX + 3, startY + 27)

  // Moduli / Pixel Fittizi
  doc.setFillColor(15, 23, 42)
  const pixel = [
    [16, 5], [20, 5], [22, 7], [18, 9],
    [5, 16], [7, 18], [9, 16], [16, 16], [20, 18], [24, 16],
    [16, 22], [18, 20], [22, 22], [24, 24],
    [28, 16], [30, 18], [32, 22], [28, 28],
  ]
  pixel.forEach(([x, y]) => doc.rect(startX + x, startY + y, 2, 2, 'F'))

  // Note di pie' pagina
  doc.setFont('helvetica', 'italic')
  doc.setFontSize(8)
  doc.setTextColor(100, 116, 139)
  doc.text("Mostra questo QR Code al tornello all'ingresso del museo.", 15, 174)
  doc.text('Galleria Geologica — Museum Booking Hub 3D', 15, 179)

  doc.save(`Dettaglio_Prenotazione_${prenotazione.codice}.pdf`)
}

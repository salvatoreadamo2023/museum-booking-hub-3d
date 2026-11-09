package com.museumhub.booking.controller;

import com.jayway.jsonpath.JsonPath;
import com.museumhub.booking.entity.Reservation;
import com.museumhub.booking.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest extends ApiTestBase {

    private final LocalDate data = LocalDate.now().plusDays(7);

    /** Prenota come visitatore e restituisce l'id della prenotazione. */
    private int prenotaEId(String token, int tour, String orario, String... posti) throws Exception {
        String body = prenota(token, bodyPrenotazione(tour, data, orario, posti))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void visitatorePrenotaEIlTotaleSegueLeTariffe() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        prenota(token, bodyPrenotazione(tour, data, "09:30",
                posto(1, tariffaId("Intero")), posto(2, tariffaId("Under 16"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codice").value(startsWith("MBH-")))
                .andExpect(jsonPath("$.stato").value("CONFERMATA"))
                .andExpect(jsonPath("$.posti[0].prezzo").value(12.0))
                .andExpect(jsonPath("$.posti[1].tariffa").value("Under 16"))
                .andExpect(jsonPath("$.posti[1].prezzo").value(6.0))
                .andExpect(jsonPath("$.totale").value(18.0))
                .andExpect(jsonPath("$.annullabile").value(true))
                .andExpect(jsonPath("$.passata").value(false));
    }

    @Test
    void postoGiaPrenotatoDa409() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        prenotaEId(registra("a@test.it"), tour, "09:30", posto(3, intero));
        prenota(registra("b@test.it"), bodyPrenotazione(tour, data, "09:30", posto(3, intero)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio").value("Posto già prenotato"));
    }

    @Test
    void stessoPostoInUnAltraFasciaENonInConflitto() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        prenotaEId(registra("a@test.it"), tour, "09:30", posto(3, intero));
        prenota(registra("b@test.it"), bodyPrenotazione(tour, data, "11:00", posto(3, intero)))
                .andExpect(status().isCreated());
    }

    @Test
    void laMappaMostraIPostiOccupati() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        prenotaEId(token, tour, "09:30", posto(1, tariffaId("Intero")), posto(2, tariffaId("Intero")));
        mockMvc.perform(get("/api/tours/" + tour + "/seats").param("data", data.toString()).param("orario", "09:30")
                        .header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occupati", hasSize(2)))
                .andExpect(jsonPath("$.liberi").value(8));
    }

    @Test
    void postoOltreLaCapienzaDa400() throws Exception {
        int tour = creaTour();
        prenota(registra("a@test.it"), bodyPrenotazione(tour, data, "09:30", posto(11, tariffaId("Intero"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postoRipetutoDa400() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        prenota(registra("a@test.it"), bodyPrenotazione(tour, data, "09:30", posto(1, intero), posto(1, intero)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dataPassataDa400() throws Exception {
        int tour = creaTour();
        prenota(registra("a@test.it"),
                bodyPrenotazione(tour, LocalDate.now().minusDays(1), "09:30", posto(1, tariffaId("Intero"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fasciaInesistenteDa404() throws Exception {
        int tour = creaTour();
        prenota(registra("a@test.it"), bodyPrenotazione(tour, data, "15:00", posto(1, tariffaId("Intero"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void senzaPostiDa400() throws Exception {
        int tour = creaTour();
        prenota(registra("a@test.it"), bodyPrenotazione(tour, data, "09:30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ognunoVedeSoloLeSuePrenotazioni() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        String a = registra("a@test.it");
        String b = registra("b@test.it");
        prenotaEId(a, tour, "09:30", posto(1, intero));
        prenotaEId(b, tour, "09:30", posto(2, intero));

        mockMvc.perform(get("/api/reservations").header(AUTH, bearer(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/reservations").header(AUTH, bearer(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].posti[0].numero").value(2));
    }

    @Test
    void ilVisitatoreDisdiceEIPostiTornanoLiberi() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        int id = prenotaEId(token, tour, "09:30", posto(1, tariffaId("Intero")));

        mockMvc.perform(delete("/api/reservations/" + id).header(AUTH, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("ANNULLATA"))
                .andExpect(jsonPath("$.posti", hasSize(0)));
        mockMvc.perform(get("/api/tours/" + tour + "/seats").param("data", data.toString()).param("orario", "09:30")
                        .header(AUTH, bearer(token)))
                .andExpect(jsonPath("$.occupati", hasSize(0)));
        // il posto puo' essere prenotato di nuovo
        prenota(registra("b@test.it"), bodyPrenotazione(tour, data, "09:30", posto(1, tariffaId("Intero"))))
                .andExpect(status().isCreated());
    }

    @Test
    void disdireDueVolteDa409() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        int id = prenotaEId(token, tour, "09:30", posto(1, tariffaId("Intero")));
        mockMvc.perform(delete("/api/reservations/" + id).header(AUTH, bearer(token))).andExpect(status().isOk());
        mockMvc.perform(delete("/api/reservations/" + id).header(AUTH, bearer(token)))
                .andExpect(status().isConflict());
    }

    @Test
    void nonSiPuoDisdireLaPrenotazioneDiUnAltro() throws Exception {
        int tour = creaTour();
        int id = prenotaEId(registra("a@test.it"), tour, "09:30", posto(1, tariffaId("Intero")));
        mockMvc.perform(delete("/api/reservations/" + id).header(AUTH, bearer(registra("b@test.it"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void oltreIlLimiteDiUnOraIlVisitatoreNonPuoDisdireMaLAdminSi() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        Reservation passata = inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "09:30", 1);

        mockMvc.perform(delete("/api/reservations/" + passata.getId()).header(AUTH, bearer(token)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/reservations/" + passata.getId()).header(AUTH, bearer(tokenAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stato").value("ANNULLATA"));
    }

    @Test
    void laVisitaPassataEMarcataPassata() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        inserisciPrenotazione("a@test.it", tour, LocalDate.now().minusDays(1), "09:30", 1);
        mockMvc.perform(get("/api/reservations").header(AUTH, bearer(token)))
                .andExpect(jsonPath("$[0].passata").value(true))
                .andExpect(jsonPath("$[0].annullabile").value(false));
    }

    @Test
    void adminRiducePostiEMantieneIPrimi() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        int id = prenotaEId(registra("a@test.it"), tour, "09:30", posto(1, intero), posto(2, intero), posto(3, intero));

        mockMvc.perform(put("/api/reservations/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content("{\"orario\":\"09:30\",\"numeroPosti\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posti", hasSize(2)))
                .andExpect(jsonPath("$.posti[1].numero").value(2))
                .andExpect(jsonPath("$.totale").value(24.0));
    }

    @Test
    void adminAggiungePostiConTariffaPredefinita() throws Exception {
        int tour = creaTour();
        int id = prenotaEId(registra("a@test.it"), tour, "09:30", posto(1, tariffaId("Under 16")));

        mockMvc.perform(put("/api/reservations/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content("{\"orario\":\"09:30\",\"numeroPosti\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posti", hasSize(3)))
                .andExpect(jsonPath("$.posti[0].tariffa").value("Under 16"))
                .andExpect(jsonPath("$.posti[1].tariffa").value("Intero"))
                .andExpect(jsonPath("$.totale").value(30.0));
    }

    @Test
    void adminSpostaLaPrenotazioneInUnAltraFascia() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        int id = prenotaEId(registra("a@test.it"), tour, "09:30", posto(1, intero), posto(2, intero));
        prenotaEId(registra("b@test.it"), tour, "11:00", posto(1, intero));

        // nella fascia 11:00 il posto 1 e' occupato: la prenotazione riceve i primi posti liberi
        mockMvc.perform(put("/api/reservations/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content("{\"orario\":\"11:00\",\"numeroPosti\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orario").value("11:00"))
                .andExpect(jsonPath("$.posti[0].numero").value(2))
                .andExpect(jsonPath("$.posti[1].numero").value(3));
        mockMvc.perform(get("/api/tours/" + tour + "/seats").param("data", data.toString()).param("orario", "09:30")
                        .header(AUTH, bearer(tokenAdmin())))
                .andExpect(jsonPath("$.occupati", hasSize(0)));
    }

    @Test
    void senzaSpazioNellaNuovaFasciaDa409() throws Exception {
        int tour = creaTour();
        long intero = tariffaId("Intero");
        int id = prenotaEId(registra("a@test.it"), tour, "09:30", posto(1, intero));
        prenotaEId(registra("b@test.it"), tour, "11:00", postiDa(1, 10, intero));

        mockMvc.perform(put("/api/reservations/" + id).header(AUTH, bearer(tokenAdmin())).contentType(JSON)
                        .content("{\"orario\":\"11:00\",\"numeroPosti\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.messaggio").value("Posti insufficienti nella fascia selezionata"));
    }

    @Test
    void ilVisitatoreNonPuoModificareLePrenotazioni() throws Exception {
        int tour = creaTour();
        String token = registra("a@test.it");
        int id = prenotaEId(token, tour, "09:30", posto(1, tariffaId("Intero")));
        mockMvc.perform(put("/api/reservations/" + id).header(AUTH, bearer(token)).contentType(JSON)
                        .content("{\"orario\":\"09:30\",\"numeroPosti\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void senzaTokenDa401() throws Exception {
        mockMvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized());
    }
}

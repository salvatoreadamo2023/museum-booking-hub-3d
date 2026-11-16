# Documentazione API REST — Museum Booking Hub 3D

Descrizione delle 24 API del backend. Con il backend avviato si possono provare tutte da Swagger:
**http://localhost:8080/swagger-ui/index.html**

### Autenticazione

Login e registrazione impostano due cookie `HttpOnly` e `SameSite=Strict` (e `Secure` in produzione): `mbh_access` (token di accesso, 15 minuti) e `mbh_refresh`
(sessione, 7 giorni). Il token non è nel corpo della risposta. Il browser invia il cookie da solo; un client diverso (script, Swagger) può usare anche
`Authorization: Bearer <token>`. Tutte le API, tranne `/api/auth/login`, `/register`, `/refresh` e `/logout`, richiedono un token di accesso valido.

Ruolo e stato dell'utente sono riletti dal database a ogni richiesta: un account sospeso perde subito l'accesso. Il logout revoca la sessione.

Ruoli: `VISITATORE` e `ADMIN`.

## Riepilogo api e descrizione

- POST : `/api/auth/login` Fase di login e accesso
- POST : `/api/auth/register` Registrazione
- POST : `/api/auth/refresh` Cookie di refresh con nuovo token di accesso
- POST : `/api/auth/logout` Logout
- GET : `/api/auth/me` Utente della sessione corrente
- GET : `/api/tours` Itinerari con orari, posti liberi e prezzi
- GET : `/api/tours/gestione` Itinerari paginati per la gestione (admin)
- POST : `/api/tours` Crea itinerario (admin)
- PUT : `/api/tours/{id}` Modifica itinerario (admin)
- DELETE : `/api/tours/{id}` Elimina itinerario (admin)
- GET : `/api/tours/{id}/seats` Posti occupati di una fascia oraria
- GET : `/api/reservations` Prenotazioni dell'utente
- GET : `/api/reservations/registro` Registro di tutte le prenotazioni, paginato (admin)
- POST : `/api/reservations` Nuova prenotazione
- PUT : `/api/reservations/{id}` Modifica prenotazione (admin)
- DELETE : `/api/reservations/{id}` Disdetta prenotazione
- GET : `/api/users` Elenco utenti, paginato (admin)
- POST : `/api/users` Crea utente (admin)
- PUT : `/api/users/{id}` Modifica utente (admin)
- DELETE : `/api/users/{id}` Elimina utente (admin)
- GET : `/api/feedback` Elenco feedback, paginato (admin)
- POST : `/api/feedback` Lascia un feedback (visitatore)
- GET : `/api/stats` Indicatori e grafici (admin)
- GET : `/api/admin/sistema` Stato del backend: CPU, memoria, thread e richieste (admin)
package com.museumhub.booking.service;

import com.museumhub.booking.dto.SistemaResponse;
import com.museumhub.booking.dto.SistemaResponse.Concorrenza;
import com.museumhub.booking.dto.SistemaResponse.Memoria;
import com.museumhub.booking.dto.SistemaResponse.Processore;
import com.museumhub.booking.dto.SistemaResponse.Richieste;
import com.museumhub.booking.monitor.RequestStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;

/**
 * Legge dalla JVM, tramite le interfacce di gestione standard di Java (JMX), le informazioni su processore, memoria
 * e thread, e le unisce ai contatori delle richieste. Sono misure del processo Java, non dell'hardware: non
 * riguardano cache o frequenza del processore.
 */
@Service
@RequiredArgsConstructor
public class SistemaService {

    private static final long MB = 1024L * 1024L;

    private final RequestStats stats;

    public SistemaResponse rileva() {
        return new SistemaResponse(Instant.now(), processore(), memoria(), concorrenza(), richieste());
    }

    private Processore processore() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        // L'uso della CPU del processo e' offerto solo dall'estensione HotSpot dell'interfaccia
        Double cpu = os instanceof com.sun.management.OperatingSystemMXBean esteso
                ? percentuale(esteso.getProcessCpuLoad())
                : null;
        return new Processore(os.getAvailableProcessors(), cpu);
    }

    private Memoria memoria() {
        MemoryMXBean memoria = ManagementFactory.getMemoryMXBean();
        long gcConteggio = 0;
        long gcTempoMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcConteggio += Math.max(0, gc.getCollectionCount());
            gcTempoMs += Math.max(0, gc.getCollectionTime());
        }
        return new Memoria(memoria.getHeapMemoryUsage().getUsed() / MB, Runtime.getRuntime().maxMemory() / MB,
                gcConteggio, gcTempoMs);
    }

    private Concorrenza concorrenza() {
        return new Concorrenza(ManagementFactory.getThreadMXBean().getThreadCount());
    }

    private Richieste richieste() {
        return new Richieste(stats.getTotali(), arrotonda(stats.getTempoMedioMs()), arrotonda(stats.getTempoMassimoMs()));
    }

    /** Da frazione (0-1) a percentuale con un decimale; i valori negativi indicano "non disponibile". */
    private Double percentuale(double frazione) {
        return frazione < 0 ? null : Math.round(frazione * 1000) / 10.0;
    }

    private double arrotonda(double valore) {
        return Math.round(valore * 100) / 100.0;
    }
}

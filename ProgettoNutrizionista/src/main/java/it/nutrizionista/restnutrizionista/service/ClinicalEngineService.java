package it.nutrizionista.restnutrizionista.service;

import it.nutrizionista.restnutrizionista.dto.ConflittoClinicoDto;
import it.nutrizionista.restnutrizionista.dto.MotivoValutazioneDto;
import it.nutrizionista.restnutrizionista.dto.ValutazioneClinicaDto;
import it.nutrizionista.restnutrizionista.engine.AlimentoRuleValidator;
import it.nutrizionista.restnutrizionista.engine.TagStandardAllergeneMapping;
import it.nutrizionista.restnutrizionista.entity.AlimentoBase;
import it.nutrizionista.restnutrizionista.entity.AvversionePersonale;
import it.nutrizionista.restnutrizionista.entity.Cliente;
import it.nutrizionista.restnutrizionista.enums.LivelloAllerta;
import it.nutrizionista.restnutrizionista.enums.TagStandard;
import it.nutrizionista.restnutrizionista.repository.AvversionePersonaleRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestratore del Medical Decision Support System (MDSS).
 *
 * Design Pattern: Chain of Responsibility con priorità @Order.
 * Tutti i bean che implementano AlimentoRuleValidator vengono iniettati automaticamente
 * da Spring nella lista 'validators', ordinati per @Order. Aggiungere un nuovo validatore
 * richiede SOLO di creare un nuovo @Component che implementa l'interfaccia — zero modifiche qui.
 *
 * Anti N+1 Pattern: La blacklist personale del paziente viene caricata UNA SOLA VOLTA
 * con JOIN FETCH (tramite findByClienteIdWithAlimenti) e poi riusata per ogni alimento
 * passato al metodo valuta(). Non si tocca mai il DB all'interno dei singoli validatori.
 *
 * Garanzie architetturali:
 * - Immutabilità: i Set passati ai validatori sono wrappati con Set.copyOf()
 * - Fail-Fast: configurabile via clinica.engine.fail-fast (default: false = referto esaustivo)
 * - Batch: valutaInBatch() usa parallelStream() per elaborazione massiva
 */
@Service
public class ClinicalEngineService {

    private final List<AlimentoRuleValidator> validators;
    private final AvversionePersonaleRepository avversionePersonaleRepository;
    private final boolean failFast;

    // Iniezione esplicita delle dipendenze (No Lombok — pattern AlimentoBase)
    public ClinicalEngineService(
            List<AlimentoRuleValidator> validators,
            AvversionePersonaleRepository avversionePersonaleRepository,
            @Value("${clinica.engine.fail-fast:false}") boolean failFast) {
        this.validators = validators;
        this.avversionePersonaleRepository = avversionePersonaleRepository;
        this.failFast = failFast;
    }

    /**
     * Valuta UN singolo alimento per UN paziente specifico.
     * Usa questa firma quando il cliente è già disponibile in memoria (es. all'interno
     * di PastoTemplateApplyService dove il Cliente è già caricato dalla request).
     *
     * @param alimento     L'alimento da valutare.
     * @param cliente      Il cliente (il suo tagStandard è già EAGER loaded).
     * @return             La ValutazioneClinicaDto aggregata con il livello peggiore trovato.
     */
    public ValutazioneClinicaDto valuta(AlimentoBase alimento, Cliente cliente) {
        Set<AvversionePersonale> blacklist =
                avversionePersonaleRepository.findByClienteIdWithAlimenti(cliente.getId());
        return eseguiChain(alimento, cliente.getTagStandard(), blacklist);
    }

    /**
     * Valuta UN singolo alimento con i dati del paziente già in memoria (RAM-first).
     * Usa questa firma nel CatalogoAlimentiService per validare le ricerche paginate,
     * passando la blacklist pre-fetchata una volta sola all'esterno del loop.
     *
     * @param alimento         L'alimento da valutare.
     * @param tagStandard      Set<TagStandard> del paziente già in RAM.
     * @param blacklistManuale Set<AvversionePersonale> del paziente già in RAM (JOIN FETCH esterno).
     * @return                 La ValutazioneClinicaDto aggregata.
     */
    public ValutazioneClinicaDto valuta(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {
        return eseguiChain(alimento, tagStandard, blacklistManuale);
    }

    /**
     * Valuta una LISTA di alimenti in parallelo per un paziente specifico.
     * Ottimizzato per le ricerche paginate nel catalogo (20-100 alimenti).
     *
     * Il pre-fetch della blacklist avviene UNA SOLA VOLTA, poi viene riusata
     * in modo thread-safe (Set immutabile) per ogni alimento del batch.
     *
     * @param alimenti     Lista di alimenti da valutare (es. risultati di una Page).
     * @param cliente      Il cliente (il suo tagStandard è già EAGER loaded).
     * @return             Lista di ValutazioneClinicaDto nello STESSO ORDINE degli alimenti input.
     */
    public List<ValutazioneClinicaDto> valutaInBatch(List<AlimentoBase> alimenti, Cliente cliente) {
        if (alimenti == null || alimenti.isEmpty()) {
            return Collections.emptyList();
        }

        // Pre-fetch UNA VOLTA in RAM, poi distribuito parallelamente
        Set<TagStandard> tagsImmutabili = Set.copyOf(cliente.getTagStandard());
        Set<AvversionePersonale> blacklistImmutabile = Set.copyOf(
                avversionePersonaleRepository.findByClienteIdWithAlimenti(cliente.getId())
        );

        // ⚠️ INVARIANTE guard-fetch: ogni associazione LAZY letta dai validatori DEVE essere
        // inizializzata QUI, sul thread della transazione, PRIMA del parallelStream. La Session
        // Hibernate non è thread-safe: accedere a una LAZY da un worker del ForkJoinPool (con
        // open-in-view=false) causerebbe LazyInitializationException/accesso concorrente alla Session.
        // 'tracce' arriva già via JOIN FETCH dai repository; 'allergeni' (letta da AllergeneRule) NON
        // è fetch-joined (eviterebbe il prodotto cartesiano di una 2ª @ElementCollection) → la
        // pre-inizializziamo in batch (@BatchSize=50 → ~ceil(N/50) query). Una futura regola che
        // legge un'altra LAZY va aggiunta qui.
        alimenti.forEach(a -> Hibernate.initialize(a.getAllergeni()));

        return alimenti.parallelStream()
                .map(alimento -> eseguiChain(alimento, tagsImmutabili, blacklistImmutabile))
                .collect(Collectors.toList());
    }

    /**
     * Valuta una collezione di alimenti contro un paziente e restituisce SOLO i conflitti clinici
     * (non-SAFE: {@code WARNING} + {@code ALERT_GRAVE}), pronti per il riepilogo batch dei percorsi
     * che applicano template / duplicano schede (finding F-D1a). Ogni conflitto porta il livello, i
     * motivi concatenati e il flag {@code allergeneDichiarato} (caso potenzialmente letale).
     *
     * @param cliente  paziente TARGET (mai il contesto della sorgente).
     * @param alimenti alimenti distinti da valutare.
     * @return         un {@link ConflittoClinicoDto} per ogni alimento non-SAFE (gli alimenti SAFE/INFO
     *                 sono omessi); lista vuota se nessun conflitto.
     */
    public List<ConflittoClinicoDto> conflittiClinici(Cliente cliente, Collection<AlimentoBase> alimenti) {
        if (alimenti == null || alimenti.isEmpty()) {
            return Collections.emptyList();
        }
        List<AlimentoBase> lista = new ArrayList<>(alimenti);
        List<ValutazioneClinicaDto> results = valutaInBatch(lista, cliente);

        List<ConflittoClinicoDto> conflitti = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            ValutazioneClinicaDto v = results.get(i);
            // Solo i livelli bloccanti/di avviso: SAFE e INFO ("non verificato") non sono conflitti.
            if (v.stato() != LivelloAllerta.WARNING && v.stato() != LivelloAllerta.ALERT_GRAVE) {
                continue;
            }
            AlimentoBase a = lista.get(i);
            String motivi = v.motivi().stream()
                    .map(MotivoValutazioneDto::messaggio)
                    .collect(Collectors.joining(" "));
            boolean allergene = v.motivi().stream()
                    .anyMatch(m -> isAllergeneDichiarato(m.codiceTrigger()));
            conflitti.add(new ConflittoClinicoDto(a.getId(), a.getNome(), v.stato(), motivi, allergene));
        }
        return conflitti;
    }

    /**
     * True se il {@code codiceTrigger} di un motivo corrisponde a un allergene dichiarato, secondo la
     * mappa AUTOREVOLE {@link TagStandardAllergeneMapping} (non un semplice prefisso {@code ALL_}).
     * Parse difensivo: i codici non-enum (es. {@code AVVERSIONE_ALERT_GRAVE}, {@code PAT_IPERTENSIONE_GRAVE})
     * ritornano {@code false} senza sollevare eccezioni.
     */
    private boolean isAllergeneDichiarato(String codiceTrigger) {
        if (codiceTrigger == null || codiceTrigger.isBlank()) {
            return false;
        }
        try {
            return TagStandardAllergeneMapping.allergeneFor(TagStandard.valueOf(codiceTrigger)) != null;
        } catch (IllegalArgumentException notATagStandard) {
            return false;
        }
    }

    /**
     * Metodo privato: itera tutti i validatori della chain e aggrega i risultati.
     * L'algoritmo sceglierà sempre il livello di allerta più grave trovato.
     *
     * Garanzie:
     * - I Set vengono wrappati in Set.copyOf() prima di essere passati ai validatori
     *   (protezione contro mutazioni accidentali da parte di sviluppatori futuri).
     * - Se failFast è abilitato, la chain si interrompe al primo ALERT_GRAVE.
     */
    private ValutazioneClinicaDto eseguiChain(
            AlimentoBase alimento,
            Set<TagStandard> tagStandard,
            Set<AvversionePersonale> blacklistManuale) {

        // Defensive copy: i validatori NON possono modificare i dati in RAM
        Set<TagStandard> tagsReadOnly = Set.copyOf(tagStandard);
        Set<AvversionePersonale> blacklistReadOnly = Set.copyOf(blacklistManuale);

        LivelloAllerta livelloAggregato = LivelloAllerta.SAFE;
        List<MotivoValutazioneDto> motiviAggregati = new ArrayList<>();

        for (AlimentoRuleValidator validator : validators) {
            ValutazioneClinicaDto parziale = validator.valida(alimento, tagsReadOnly, blacklistReadOnly);

            // Aggrega i motivi se c'è un problema
            if (parziale.stato() != LivelloAllerta.SAFE) {
                motiviAggregati.addAll(parziale.motivi());

                // Promuove il livello se quello attuale è peggiore
                if (ordine(parziale.stato()) > ordine(livelloAggregato)) {
                    livelloAggregato = parziale.stato();
                }

                // Fail-Fast: se configurato, interrompe al primo ALERT_GRAVE
                if (failFast && livelloAggregato == LivelloAllerta.ALERT_GRAVE) {
                    break;
                }
            }
        }

        return new ValutazioneClinicaDto(livelloAggregato, List.copyOf(motiviAggregati));
    }

    /**
     * Definisce la gravità ordinale degli stati per poter confrontarli.
     * Questo metodo rende il codice immune a futuri riordinamenti dell'enum.
     */
    private int ordine(LivelloAllerta livello) {
        return switch (livello) {
            case SAFE        -> 0;
            case INFO        -> 1;
            case WARNING     -> 2;
            case ALERT_GRAVE -> 3;
        };
    }
}

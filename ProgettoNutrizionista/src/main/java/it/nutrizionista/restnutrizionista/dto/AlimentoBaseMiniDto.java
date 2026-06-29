package it.nutrizionista.restnutrizionista.dto;

/**
 * Vista leggera di un alimento per gli alimenti annidati nei TEMPLATE
 * (schede-template, pasti-template, ricette): solo i campi mostrati a schermo
 * (nome, categoria, misura, macro). Sostituisce {@link AlimentoBaseDto} (classe
 * da ~30 campi, quasi tutti null nei template) per alleggerire il payload di
 * GET /api/schede-template/{id} & co. Il campo {@code macroNutrienti} mantiene
 * lo stesso nome atteso dal frontend.
 */
public record AlimentoBaseMiniDto(
        Long id,
        String nome,
        String categoria,
        Double misuraInGrammi,
        MacroMiniDto macroNutrienti) {
}

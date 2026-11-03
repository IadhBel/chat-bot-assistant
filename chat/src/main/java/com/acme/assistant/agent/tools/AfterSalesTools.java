package com.acme.assistant.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class AfterSalesTools {

    private static final Set<String> OPEN_RECALL_VINS = Set.of(
            "VF1DEMO1234567890",
            "VF1RAPPEL00000001"
    );

    @Tool("Indique le plan d'entretien type selon l'énergie du véhicule")
    public String maintenancePlan(@P("electric, hybrid ou petrol") String energy) {
        return switch (energy.toLowerCase(Locale.ROOT)) {
            case "electric", "electrique", "électrique" ->
                    "Électrique: contrôle annuel (trains, freins, liquide batterie, logiciel). Pas de vidange moteur.";
            case "hybrid", "hybride" ->
                    "Hybride E-Tech: vidange selon carnet (souvent 1 an / 15 000 à 30 000 km). Chaîne de distribution sur motorisations récentes.";
            default ->
                    "Thermique: vidange 1 an / 15 000 km, CT à 4 ans puis tous les 2 ans. Vérifier le carnet du VIN.";
        };
    }

    @Tool("Vérifie si un VIN de démonstration a une campagne de rappel ouverte")
    public String recallStatus(@P("VIN 17 caractères") String vin) {
        String normalized = vin.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 17) {
            return "VIN invalide: 17 caractères attendus.";
        }
        if (OPEN_RECALL_VINS.contains(normalized)) {
            return "Campagne logicielle ouverte sur ce VIN. Prise de rendez-vous atelier, immobilisation quelques heures.";
        }
        return "Aucune campagne ouverte dans le référentiel de démonstration pour ce VIN.";
    }
}

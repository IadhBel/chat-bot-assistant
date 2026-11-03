package com.acme.assistant.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VehicleCatalogTools {

    private static final Map<String, String> CATALOG = Map.of(
            "clio", "Clio E-Tech full hybrid 145 ch — citadine, usage quotidien, non 100% électrique.",
            "captur", "Captur E-Tech full hybrid — crossover B, hybride non rechargeable.",
            "austral", "Austral E-Tech full hybrid 200 ch — SUV C, 4x2.",
            "rafale", "Rafale E-Tech — SUV coupé, hybride ; version 4x4 300 ch selon millésime.",
            "megane", "Mégane E-Tech 100% électrique, batterie 60 kWh, jusqu'à 450 km WLTP, DC 130 kW.",
            "mégane", "Mégane E-Tech 100% électrique, batterie 60 kWh, jusqu'à 450 km WLTP, DC 130 kW.",
            "scenic", "Scenic E-Tech 100% électrique — monospace compact électrique.",
            "5", "Acme 5 E-Tech 100% électrique, batteries 40 ou 52 kWh, jusqu'à 412 km WLTP."
    );

    @Tool("Retourne la fiche commerciale courte d'un modèle Acme à partir de son nom")
    public String findVehicle(@P("Nom ou mot-clé du modèle, ex. Megane, Acme 5, Austral") String modelName) {
        String needle = modelName.toLowerCase(Locale.ROOT);
        List<String> hits = CATALOG.entrySet().stream()
                .filter(entry -> needle.contains(entry.getKey()) || entry.getKey().contains(needle))
                .map(Map.Entry::getValue)
                .distinct()
                .toList();
        if (hits.isEmpty()) {
            return "Aucun modèle correspondant à '%s' dans le catalogue concierge.".formatted(modelName);
        }
        return String.join("\n", hits);
    }
}

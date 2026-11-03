package com.acme.assistant.infrastructure.llm;

final class StubResponses {

    private StubResponses() {
    }

    static String reply(String prompt) {
        String lower = prompt.toLowerCase();
        if (isClassification(lower)) {
            return classify(lower);
        }
        if (containsAny(lower, "autonomie", "batterie", "kwh", "recharge", "wlpt", "wltp")) {
            return "D'après le corpus Acme : la Mégane E-Tech annonce jusqu'à 450 km WLTP "
                    + "(batterie 60 kWh) et la Acme 5 E-Tech jusqu'à 412 km sur la batterie 52 kWh. "
                    + "La charge DC 15-80 % se fait en environ 30 minutes sur borne compatible. "
                    + "Pour un chiffre au VIN, un conseiller technique confirmera en atelier Expert électrique.";
        }
        if (containsAny(lower, "garantie", "entretien", "révision", "revision", "vidange", "rappel")) {
            return "Garantie constructeur : 3 ans / 100 000 km. Batterie électrique : 8 ans / 160 000 km "
                    + "à 70 % de capacité minimale. Entretien électrique : contrôle annuel, pas de vidange moteur. "
                    + "Les rappels se vérifient par VIN en atelier Acme.";
        }
        if (containsAny(lower, "essai", "concession", "prix", "gamme", "rendez-vous", "rdv")) {
            return "La gamme particulière couvre hybride (Clio, Captur, Austral, Rafale) et 100 % électrique "
                    + "(Mégane E-Tech, Acme 5, Scenic E-Tech). Un essai se réserve en concession ou Acme Store "
                    + "avec pièce d'identité et permis. Je peux vous orienter vers un site Expert électrique.";
        }
        return "Je suis l'assistant Acme. Je peux vous aider sur la gamme, l'électrique, "
                + "l'entretien et le réseau. Précisez le modèle ou votre question.";
    }

    static String classify(String lower) {
        if (containsAny(lower, "entretien", "garantie", "révision", "revision", "vidange",
                "rappel", "après-vente", "apres-vente", "sav", "atelier")) {
            return "AFTER_SALES";
        }
        if (containsAny(lower, "batterie", "autonomie", "moteur", "technique", "e-tech",
                "mégane", "megane", "recharge", "diagnostic", "kwh", "wlpt", "wltp")) {
            return "TECHNICAL";
        }
        return "CONCIERGE";
    }

    private static boolean isClassification(String lower) {
        return lower.contains("classifie")
                || lower.contains("classify")
                || lower.contains("intent")
                || lower.contains("intention");
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}

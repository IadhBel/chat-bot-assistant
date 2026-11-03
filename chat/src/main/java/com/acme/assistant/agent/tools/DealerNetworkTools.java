package com.acme.assistant.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class DealerNetworkTools {

    private record Dealer(String city, String name, boolean electricExpert) {
    }

    private static final List<Dealer> DEALERS = List.of(
            new Dealer("paris", "Acme Store Paris 15e", true),
            new Dealer("lyon", "Acme Lyon Gerland", true),
            new Dealer("lille", "Acme Lille La Madeleine", false),
            new Dealer("toulouse", "Acme Toulouse Borderouge", true),
            new Dealer("nantes", "Acme Nantes Beaujoire", false)
    );

    @Tool("Trouve une concession Acme proche d'une ville, en précisant si l'atelier est Expert électrique")
    public String findDealer(@P("Ville ou agglomération, ex. Lyon") String city) {
        String needle = city.toLowerCase(Locale.ROOT);
        return DEALERS.stream()
                .filter(dealer -> dealer.city.contains(needle) || needle.contains(dealer.city))
                .findFirst()
                .map(dealer -> "%s — Expert électrique: %s".formatted(
                        dealer.name, dealer.electricExpert ? "oui" : "non"))
                .orElse("Pas de site dans l'échantillon pour '%s'. Orienter vers le store locator Acme."
                        .formatted(city));
    }
}

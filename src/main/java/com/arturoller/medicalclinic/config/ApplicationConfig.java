package com.arturoller.medicalclinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ApplicationConfig {
    @Bean
    public OpenAPI medicalClinicOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Medical Clinic API")
                .version("1.0")
                .description("""
                        API przychodni: użytkownicy, pacjenci, lekarze, placówki i terminy wizyt.

                        **Konwencje**
                        - Daty `yyyy-MM-dd`, daty z czasem ISO-8601 bez strefy (`2030-01-15T09:00:00`), czas serwera.
                        - Listy są stronicowane: `page` (od 0), `size` (domyślnie 10), `sort=pole,asc|desc`. Niedozwolone pole sortowania → 400.
                        - Każdy błąd ma format `ErrorMessageDto`: `{ "message": "...", "status": "NOT_FOUND" }`.
                        - Pacjent i lekarz są zawsze powiązani z istniejącym użytkownikiem (`userId`).
                        """)
                .contact(new Contact()
                        .name("Medical Clinic")
                        .url("https://github.com/arturoller/medicalclinic")));
    }

    @Bean
    public OpenApiCustomizer internalServerErrorResponse() {
        Map<String, String> example = new LinkedHashMap<>();
        example.put("message", "Nieoczekiwany błąd serwera");
        example.put("status", "INTERNAL_SERVER_ERROR");

        return openApi -> openApi.getPaths().values().forEach(path -> {
            path.readOperations().forEach(operation -> {
                operation.getResponses().addApiResponse("500", new ApiResponse()
                        .description("Nieoczekiwany błąd serwera")
                        .content(new Content()
                                .addMediaType("application/json", new MediaType()
                                        .schema( new Schema<>()
                                                .$ref("#/components/schemas/ErrorMessageDto")
                                        )
                                        .example(example)
                                )
                        )
                );
            });
        });
    }

    @Bean
    public OpenApiCustomizer polishDefaultDescriptions() {
        Map<String, String> responses = Map.of(
                "200", "Sukces",
                "201", "Utworzono",
                "204", "Wykonano – odpowiedź bez treści");
        Map<String, String> pageableParameters = Map.of(
                "page", "Numer strony (od 0)",
                "size", "Rozmiar strony",
                "sort", "Sortowanie w formacie `pole,asc|desc`, np. `lastName,desc`. Parametr można podać kilka razy.");
        return openApi -> openApi.getPaths().values().forEach(path ->
                path.readOperations().forEach(operation -> {
                    operation.getResponses().forEach((code, response) -> {
                        if (responses.containsKey(code)) {
                            response.setDescription(responses.get(code));
                        }
                    });
                    if (operation.getParameters() != null) {
                        operation.getParameters().stream()
                                .filter(parameter -> "query".equals(parameter.getIn()))
                                .filter(parameter -> pageableParameters.containsKey(parameter.getName()))
                                .forEach(parameter -> parameter.setDescription(pageableParameters.get(parameter.getName())));
                    }
                }));
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

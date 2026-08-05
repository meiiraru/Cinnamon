package cinnamon.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static cinnamon.Client.LOGGER;

public class NetUtils {

    public static String getResponseFromUrl(String url) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200)
                    return response.body();
                else
                    LOGGER.error("Failed to fetch HTTP data. HTTP Status Code: %s", response.statusCode());
            } catch (Exception e) {
                LOGGER.error("Failed to fetch HTTP data", e);
            }
        }

        return null;
    }
}

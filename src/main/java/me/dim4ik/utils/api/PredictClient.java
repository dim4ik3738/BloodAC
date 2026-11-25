package me.dim4ik.utils.api;

import com.google.gson.Gson;
import me.dim4ik.BloodAC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PredictClient {
    private static final String predictUrl = "https://predict.bloodac.space";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final Gson gson = new Gson();

    private static final long logInterval = 60_000L;
    private static final Map<String, Long> lastLogged = new ConcurrentHashMap<>();

    private static void logError(String key) {
        long now = System.currentTimeMillis();
        boolean[] shouldLog = {false};

        lastLogged.compute(key, (k, last) -> {
            if (last == null || now - last >= logInterval) {
                shouldLog[0] = true;
                return now;
            }
            return last;
        });

        if (shouldLog[0]) {
            BloodAC.getInstance().getLogger().warning(key);
        }
    }

    public static float predictAim(float[] inputData, String playerName, boolean isFull) {
        return sendFloat("/predict/aim", inputData, playerName, isFull);
    }

    public static float predictAttack(float[] inputData, String playerName, boolean isFull) {
        return sendFloat("/predict/attack", inputData, playerName, isFull);
    }

    private static float sendFloat(String endpoint, float[] inputData, String playerName, boolean isFull) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("inputData", inputData);
            body.put("playerName", playerName);
            body.put("isFull", isFull);
            body.put("isTest", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(predictUrl + endpoint))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .header("Key", BloodAC.licenseKey)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body() == null ? "" : response.body().trim();

            if (responseBody.isEmpty()) {
                logError("Empty response from server (HTTP " + response.statusCode() + ")");
                return -1;
            }

            try {
                return Float.parseFloat(responseBody);
            } catch (NumberFormatException e) {
                String shortBody = responseBody.length() > 100 ? responseBody.substring(0, 100) : responseBody;
                logError("Bad server response (HTTP " + response.statusCode() + "): " + shortBody);
                return -1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        } catch (Exception e) {
            logError(e.toString());
            return -1;
        }
    }

    public static String getLatestVersion() {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("clientVersion", BloodAC.getInstance().getDescription().getVersion());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(predictUrl + "/api/plugin/version"))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception e) {
            logError(e.toString());
            return "";
        }
    }
}
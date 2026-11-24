package br.edu.cesmac.saneamapa.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário geoespacial para cálculo de distância (Haversine),
 * verificação ponto-em-polígono (Ray Casting) e anonimização de trecho de rua (LGPD).
 *
 * <p>Polígonos são guardados como JSON {@code [[lat, lon], ...]}. O GeoJSON usa a ordem inversa
 * ({@code [lon, lat]}); a conversão é feita na importação por {@link #converterAnelGeoJson(JsonNode)}.
 */
public final class GeoUtils {

    private static final Logger log = LoggerFactory.getLogger(GeoUtils.class);
    private static final double RAIO_TERRA_METROS = 6_371_000.0;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoUtils() {
    }

    public static double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_METROS * c;
    }

    /**
     * Lê e valida um polígono JSON {@code [[lat, lon], ...]}.
     *
     * @throws IllegalArgumentException com o motivo, quando o JSON não é um polígono válido
     */
    public static List<double[]> lerPoligono(String poligonoJson) {
        if (poligonoJson == null || poligonoJson.isBlank()) {
            throw new IllegalArgumentException("polígono ausente");
        }
        JsonNode raiz;
        try {
            raiz = MAPPER.readTree(poligonoJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("o polígono não é um JSON válido");
        }
        if (raiz == null || !raiz.isArray()) {
            throw new IllegalArgumentException("o polígono deve ser uma lista de pares [latitude, longitude]");
        }
        List<double[]> vertices = new ArrayList<>();
        for (JsonNode par : raiz) {
            if (!par.isArray() || par.size() < 2 || !par.get(0).isNumber() || !par.get(1).isNumber()) {
                throw new IllegalArgumentException("cada vértice deve ser um par numérico [latitude, longitude]");
            }
            double lat = par.get(0).asDouble();
            double lon = par.get(1).asDouble();
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("vértice fora da faixa de latitude e longitude: [" + lat + ", " + lon + "]");
            }
            vertices.add(new double[]{lat, lon});
        }
        if (vertices.size() < 3) {
            throw new IllegalArgumentException("o polígono precisa de ao menos 3 vértices");
        }
        return vertices;
    }

    /**
     * Converte o anel externo de uma geometria GeoJSON (Polygon ou MultiPolygon, em {@code [lon, lat]})
     * para o JSON interno {@code [[lat, lon], ...]}. No MultiPolygon usa o primeiro polígono.
     *
     * @throws IllegalArgumentException quando a geometria está ausente ou não é um polígono
     */
    public static String converterAnelGeoJson(JsonNode geometria) {
        if (geometria == null || geometria.isMissingNode() || geometria.isNull()) {
            throw new IllegalArgumentException("geometria ausente");
        }
        String tipo = geometria.path("type").asText("");
        JsonNode coords = geometria.path("coordinates");
        JsonNode anel = switch (tipo) {
            case "Polygon" -> coords.path(0);
            case "MultiPolygon" -> coords.path(0).path(0);
            default -> throw new IllegalArgumentException("geometria do tipo '" + tipo + "'; o setor precisa ser Polygon ou MultiPolygon");
        };
        if (!anel.isArray() || anel.isEmpty()) {
            throw new IllegalArgumentException("polígono sem coordenadas");
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < anel.size(); i++) {
            JsonNode par = anel.get(i);
            if (!par.isArray() || par.size() < 2) {
                throw new IllegalArgumentException("coordenada inválida no polígono");
            }
            if (i > 0) sb.append(',');
            // GeoJSON: [longitude, latitude] -> interno: [latitude, longitude]
            sb.append('[').append(par.get(1).asDouble()).append(',').append(par.get(0).asDouble()).append(']');
        }
        String json = sb.append(']').toString();
        lerPoligono(json);
        return json;
    }

    /**
     * Verifica se a coordenada (lat, lon) está contida no polígono JSON [[lat, lon], ...].
     * Um polígono inválido gravado no banco não derruba a consulta, mas fica registrado no log.
     */
    public static boolean pontoDentroDoPoligono(double lat, double lon, String poligonoJson) {
        if (poligonoJson == null || poligonoJson.isBlank()) {
            return false;
        }
        List<double[]> vertices;
        try {
            vertices = lerPoligono(poligonoJson);
        } catch (IllegalArgumentException e) {
            log.warn("Polígono inválido ignorado na verificação de ponto ({}): {}", e.getMessage(),
                    poligonoJson.length() > 120 ? poligonoJson.substring(0, 120) + "..." : poligonoJson);
            return false;
        }
        boolean dentro = false;
        int n = vertices.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double yi = vertices.get(i)[0];
            double xi = vertices.get(i)[1];
            double yj = vertices.get(j)[0];
            double xj = vertices.get(j)[1];

            boolean intersecta = ((yi > lat) != (yj > lat))
                    && (lon < (xj - xi) * (lat - yi) / ((yj - yi) == 0 ? 1e-12 : (yj - yi)) + xi);
            if (intersecta) {
                dentro = !dentro;
            }
        }
        return dentro;
    }

    /**
     * RF002 Obs1 / LGPD: fora da equipe interna exibe apenas trecho de rua, sem número de imóvel.
     */
    public static String derivarTrechoRua(String logradouro, String pontoReferencia) {
        String base = (logradouro != null && !logradouro.isBlank())
                ? logradouro.trim().replaceAll(",?\\s*(n[º°o]?\\.?\\s*)?\\d+.*$", "").trim()
                : null;
        if (base != null && !base.isBlank()) {
            return base + ", trecho sinalizado";
        }
        if (pontoReferencia != null && !pontoReferencia.isBlank()) {
            return "Proximidades de " + pontoReferencia.trim();
        }
        return "Trecho de rua identificado no mapa";
    }
}

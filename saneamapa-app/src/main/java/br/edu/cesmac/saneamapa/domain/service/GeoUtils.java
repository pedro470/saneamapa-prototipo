package br.edu.cesmac.saneamapa.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Utilitário geoespacial para cálculo de distância (Haversine),
 * verificação ponto-em-polígono (Ray Casting) e anonimização de trecho de rua (LGPD).
 */
public final class GeoUtils {

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
     * Verifica se a coordenada (lat, lon) está contida no polígono JSON [[lat, lon], ...].
     */
    public static boolean pontoDentroDoPoligono(double lat, double lon, String poligonoJson) {
        if (poligonoJson == null || poligonoJson.isBlank()) {
            return false;
        }
        try {
            List<List<Double>> vertices = MAPPER.readValue(poligonoJson, new TypeReference<>() {});
            if (vertices.size() < 3) {
                return false;
            }
            boolean dentro = false;
            int n = vertices.size();
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double yi = vertices.get(i).get(0);
                double xi = vertices.get(i).get(1);
                double yj = vertices.get(j).get(0);
                double xj = vertices.get(j).get(1);

                boolean intersecta = ((yi > lat) != (yj > lat))
                        && (lon < (xj - xi) * (lat - yi) / ((yj - yi) == 0 ? 1e-12 : (yj - yi)) + xi);
                if (intersecta) {
                    dentro = !dentro;
                }
            }
            return dentro;
        } catch (Exception e) {
            return false;
        }
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

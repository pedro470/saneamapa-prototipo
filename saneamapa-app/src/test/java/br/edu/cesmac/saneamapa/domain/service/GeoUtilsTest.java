package br.edu.cesmac.saneamapa.domain.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RF020: o GeoJSON do Censo vem em [lon, lat]; o sistema guarda e consulta em [lat, lon].
 */
class GeoUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void converteGeoJsonParaLatLonECasaPontoDentroDoSetor() throws Exception {
        JsonNode geometria = MAPPER.readTree("""
                {"type":"Polygon","coordinates":[[[-35.755,-9.675],[-35.755,-9.665],[-35.742,-9.665],[-35.742,-9.675],[-35.755,-9.675]]]}
                """);

        String interno = GeoUtils.converterAnelGeoJson(geometria);

        assertTrue(interno.startsWith("[[-9.675,-35.755]"), interno);
        assertTrue(GeoUtils.pontoDentroDoPoligono(-9.670, -35.750, interno));
        assertFalse(GeoUtils.pontoDentroDoPoligono(-9.700, -35.750, interno));
    }

    @Test
    void recusaGeometriaQueNaoEPoligono() throws Exception {
        JsonNode ponto = MAPPER.readTree("{\"type\":\"Point\",\"coordinates\":[-35.75,-9.67]}");
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.converterAnelGeoJson(ponto));
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.converterAnelGeoJson(MAPPER.missingNode()));
    }

    @Test
    void lerPoligonoApontaOMotivoDoErro() {
        IllegalArgumentException poucos = assertThrows(IllegalArgumentException.class,
                () -> GeoUtils.lerPoligono("[[-9.6,-35.7],[-9.5,-35.7]]"));
        assertEquals("o polígono precisa de ao menos 3 vértices", poucos.getMessage());
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.lerPoligono("[[-135.7,-9.6],[1,2],[3,4]]"));
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.lerPoligono("não é json"));
        // Polígono inválido no banco não derruba a consulta: só não casa.
        assertFalse(GeoUtils.pontoDentroDoPoligono(-9.6, -35.7, "não é json"));
    }
}

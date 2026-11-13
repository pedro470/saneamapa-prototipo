package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.service.GeoUtils;

public class GeometriaPoligono implements GeometriaCamada {

    private final String poligonoCoordenadasJson;

    public GeometriaPoligono(String poligonoCoordenadasJson) {
        this.poligonoCoordenadasJson = poligonoCoordenadasJson;
    }

    @Override
    public boolean contemPonto(double latitude, double longitude) {
        return GeoUtils.pontoDentroDoPoligono(latitude, longitude, poligonoCoordenadasJson);
    }

    @Override
    public String descrever() {
        return "Polígono";
    }
}

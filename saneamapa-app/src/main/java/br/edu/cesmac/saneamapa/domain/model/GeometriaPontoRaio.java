package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.service.GeoUtils;

public class GeometriaPontoRaio implements GeometriaCamada {

    private final double centroLatitude;
    private final double centroLongitude;
    private final int raioMetros;

    public GeometriaPontoRaio(double centroLatitude, double centroLongitude, int raioMetros) {
        this.centroLatitude = centroLatitude;
        this.centroLongitude = centroLongitude;
        this.raioMetros = raioMetros;
    }

    @Override
    public boolean contemPonto(double latitude, double longitude) {
        return GeoUtils.calcularDistanciaMetros(centroLatitude, centroLongitude, latitude, longitude) <= raioMetros;
    }

    @Override
    public String descrever() {
        return "Ponto, raio " + raioMetros + " m";
    }
}

package br.edu.cesmac.saneamapa.domain.model;

/**
 * Interface de abstração e polimorfismo para geometrias de camadas de vulnerabilidade (RF006).
 */
public interface GeometriaCamada {

    boolean contemPonto(double latitude, double longitude);

    String descrever();
}

package br.edu.cesmac.saneamapa.domain.service;

import java.util.regex.Pattern;

public final class CpfUtils {

    private static final Pattern CPF_FORMATADO = Pattern.compile("^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$");
    private static final Pattern TELEFONE_FORMATADO = Pattern.compile("^\\(\\d{2}\\) \\d{4,5}-\\d{4}$");

    private CpfUtils() {
    }

    public static boolean isCpfValido(String cpf) {
        if (cpf == null || !CPF_FORMATADO.matcher(cpf).matches()) {
            return false;
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            return false;
        }
        int d1 = calcularDigito(digitos, 9, 10);
        int d2 = calcularDigito(digitos, 10, 11);
        return d1 == (digitos.charAt(9) - '0') && d2 == (digitos.charAt(10) - '0');
    }

    private static int calcularDigito(String digitos, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (digitos.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    public static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) {
            return "***.***.***-**";
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) {
            return "***.***.***-**";
        }
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    public static boolean isTelefoneValido(String telefone) {
        return telefone != null && TELEFONE_FORMATADO.matcher(telefone).matches();
    }
}

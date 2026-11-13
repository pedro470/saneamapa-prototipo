package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.domain.exception.SaneaMapaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SaneaMapaException.class)
    public ResponseEntity<Map<String, Object>> tratarErroDominio(SaneaMapaException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(Map.of(
                "erro", ex.getMessage(),
                "orientacao", ex.getOrientacao()
        ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tratarArquivoGrande(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of(
                "erro", "O arquivo enviado excede o tamanho máximo permitido (50 MB).",
                "orientacao", "Envie uma foto ou um vídeo mais curto, de até 30 segundos."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarErroValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getDefaultMessage())
                .orElse("Dados inválidos no formulário.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "erro", mensagem,
                "orientacao", "Revise os campos destacados e envie novamente."
        ));
    }
}

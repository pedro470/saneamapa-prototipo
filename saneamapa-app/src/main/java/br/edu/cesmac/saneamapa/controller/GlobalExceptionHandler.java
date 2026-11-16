package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.domain.exception.SaneaMapaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "erro", "Os dados enviados estão em formato inválido.",
                "orientacao", "Recarregue a página e tente de novo."
        ));
    }

    /** Violação de constraint ou exceção levantada por trigger do PostgreSQL. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> tratarErroBanco(DataAccessException ex) {
        log.warn("Operação recusada pelo banco de dados", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "erro", "O banco de dados recusou a operação porque ela conflita com uma regra de integridade.",
                "orientacao", "Confira os dados (por exemplo, registros duplicados ou o limite diário de relatos) e tente de novo."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(Exception ex) throws Exception {
        // Exceções de segurança seguem para o Spring Security (401/403 em SecurityConfig).
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        // Erros que o Spring já sabe classificar (404 de recurso, método não suportado, parâmetro faltando...).
        if (ex instanceof ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode()).body(Map.of(
                    "erro", "Requisição não atendida (" + er.getStatusCode().value() + ").",
                    "orientacao", "Confira o endereço e os dados enviados."
            ));
        }
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "erro", "Ocorreu um erro inesperado no servidor.",
                "orientacao", "Tente de novo em instantes. Se continuar, avise a equipe responsável."
        ));
    }
}

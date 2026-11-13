package br.edu.cesmac.saneamapa.controller;

import br.edu.cesmac.saneamapa.domain.enums.OrigemEvidencia;
import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.service.MidiaStorageService;
import br.edu.cesmac.saneamapa.service.MidiaStorageService.ArquivoCarregado;
import br.edu.cesmac.saneamapa.service.MidiaStorageService.ArquivoSalvo;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
public class MidiaController {

    private final MidiaStorageService storageService;

    public MidiaController(MidiaStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(value = "/api/morador/midias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArquivoSalvo uploadMidiaMorador(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "tipo", required = false) TipoEvidencia tipo,
            @RequestParam(value = "duracaoSegundos", required = false) Integer duracaoSegundos) {
        return storageService.salvar(arquivo, tipo, duracaoSegundos);
    }

    @PostMapping(value = "/api/fiscal/midias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArquivoSalvo uploadMidiaFiscal(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "origem", defaultValue = "CAMERA") OrigemEvidencia origem,
            @RequestParam(value = "duracaoSegundos", required = false) Integer duracaoSegundos) {
        if (origem != OrigemEvidencia.CAMERA) {
            throw new RegraNegocioException(
                    "Na vistoria em campo, envie fotos ou vídeos capturados diretamente pela câmera.",
                    "Use o botão de captura pela câmera no aplicativo."
            );
        }
        return storageService.salvar(arquivo, null, duracaoSegundos);
    }

    @GetMapping("/api/midias/{nomeArquivo:.+}")
    public ResponseEntity<Resource> baixarMidia(@PathVariable String nomeArquivo) {
        ArquivoCarregado carregado = storageService.carregar(nomeArquivo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(carregado.mimeType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(carregado.resource());
    }
}

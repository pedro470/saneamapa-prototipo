package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEvidencia;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaAudio;
import br.edu.cesmac.saneamapa.domain.model.EvidenciaVideo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MidiaStorageService {

    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of(
            "jpg", "jpeg", "png", "webp", "gif",
            "mp4", "webm", "mov", "m4v", "3gp",
            "ogg", "opus", "mp3", "m4a", "wav"
    );

    private static final Map<String, String> MIME_POR_EXTENSAO = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("webm", "video/webm"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("m4v", "video/mp4"),
            Map.entry("3gp", "video/3gpp"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("opus", "audio/opus"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("wav", "audio/wav")
    );

    private final Path uploadDir;

    public MidiaStorageService(@Value("${saneamapa.storage.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível inicializar o diretório de mídias.", ex);
        }
    }

    public ArquivoSalvo salvar(MultipartFile arquivo, TipoEvidencia tipoInformado, Integer duracaoSegundos) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Nenhum arquivo enviado.", "Selecione uma foto, vídeo ou áudio válido.");
        }

        String contentType = arquivo.getContentType() != null
                ? arquivo.getContentType().toLowerCase(Locale.ROOT)
                : "";
        TipoEvidencia tipoDetectado = detectarTipo(tipoInformado, contentType, arquivo.getOriginalFilename());

        validarTipoEDuracao(tipoDetectado, duracaoSegundos);

        String extensao = extrairExtensaoSegura(arquivo.getOriginalFilename(), contentType, tipoDetectado);
        String nomeSalvo = "ev_" + UUID.randomUUID().toString().replace("-", "") + "." + extensao;
        Path destino = uploadDir.resolve(nomeSalvo).normalize();
        if (!destino.startsWith(uploadDir)) {
            throw new RegraNegocioException("Nome de arquivo inválido.", "Renomeie o arquivo e tente novamente.");
        }

        try (InputStream in = arquivo.getInputStream()) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RegraNegocioException("Falha ao gravar mídia no servidor.", "Tente enviar o arquivo novamente.");
        }

        String mimeFinal = MIME_POR_EXTENSAO.getOrDefault(extensao,
                contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType);
        return new ArquivoSalvo(
                nomeSalvo,
                "/api/midias/" + nomeSalvo,
                tipoDetectado,
                mimeFinal,
                arquivo.getSize()
        );
    }

    public ArquivoCarregado carregar(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()
                || nomeArquivo.contains("..") || nomeArquivo.contains("/") || nomeArquivo.contains("\\")) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado.", "Verifique o identificador da mídia.");
        }
        Path caminho = uploadDir.resolve(nomeArquivo).normalize();
        if (!caminho.startsWith(uploadDir) || !Files.exists(caminho) || !Files.isRegularFile(caminho)) {
            throw new RecursoNaoEncontradoException("Arquivo de mídia não localizado.", "A mídia pode ter sido removida.");
        }
        try {
            Resource resource = new UrlResource(caminho.toUri());
            String ext = "";
            int dot = nomeArquivo.lastIndexOf('.');
            if (dot >= 0 && dot < nomeArquivo.length() - 1) {
                ext = nomeArquivo.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
            String mime = MIME_POR_EXTENSAO.get(ext);
            if (mime == null) {
                mime = Files.probeContentType(caminho);
            }
            if (mime == null || mime.isBlank()) {
                mime = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return new ArquivoCarregado(resource, mime);
        } catch (MalformedURLException ex) {
            throw new RecursoNaoEncontradoException("Arquivo não encontrado.", "Verifique o caminho solicitado.");
        } catch (IOException ex) {
            throw new RecursoNaoEncontradoException("Não foi possível abrir o arquivo.", "Tente novamente.");
        }
    }

    private TipoEvidencia detectarTipo(TipoEvidencia tipoInformado, String contentType, String nomeOriginal) {
        if (contentType.startsWith("image/")) return TipoEvidencia.FOTO;
        if (contentType.startsWith("video/")) return TipoEvidencia.VIDEO;
        if (contentType.startsWith("audio/")) return TipoEvidencia.AUDIO;

        if (nomeOriginal != null) {
            String lower = nomeOriginal.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif")) {
                return TipoEvidencia.FOTO;
            }
            if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".m4v") || lower.endsWith(".3gp")) {
                return TipoEvidencia.VIDEO;
            }
            if (lower.endsWith(".ogg") || lower.endsWith(".opus") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav")) {
                return TipoEvidencia.AUDIO;
            }
        }
        if (tipoInformado != null) return tipoInformado;
        throw new RegraNegocioException(
                "Formato de arquivo não suportado.",
                "Envie uma imagem (JPG/PNG/WebP), vídeo (MP4/WebM) ou áudio (MP3/OGG/WebM)."
        );
    }

    private void validarTipoEDuracao(TipoEvidencia tipo, Integer duracaoSegundos) {
        if (tipo == TipoEvidencia.VIDEO && duracaoSegundos != null
                && (duracaoSegundos <= 0 || duracaoSegundos > EvidenciaVideo.DURACAO_MAXIMA_SEGUNDOS)) {
            throw new RegraNegocioException(
                    String.format("Vídeo não aceito: o arquivo tem %d segundos e o limite é %d segundos.",
                            duracaoSegundos, EvidenciaVideo.DURACAO_MAXIMA_SEGUNDOS),
                    "Grave ou selecione um trecho de até 30 segundos."
            );
        }
        if (tipo == TipoEvidencia.AUDIO && duracaoSegundos != null
                && (duracaoSegundos <= 0 || duracaoSegundos > EvidenciaAudio.DURACAO_MAXIMA_SEGUNDOS)) {
            throw new RegraNegocioException(
                    String.format("Áudio não aceito: a gravação tem %d segundos e o limite é %d segundos.",
                            duracaoSegundos, EvidenciaAudio.DURACAO_MAXIMA_SEGUNDOS),
                    "Grave um áudio mais curto, de até 60 segundos."
            );
        }
    }

    private String extrairExtensaoSegura(String nomeOriginal, String contentType, TipoEvidencia tipo) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            String ext = nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
            if (EXTENSOES_PERMITIDAS.contains(ext)) {
                return ext;
            }
        }
        if (contentType.contains("png")) return "png";
        if (contentType.contains("webp")) return "webp";
        if (contentType.contains("gif")) return "gif";
        if (contentType.contains("mp4")) return tipo == TipoEvidencia.AUDIO ? "m4a" : "mp4";
        if (contentType.contains("webm")) return "webm";
        if (contentType.contains("ogg")) return "ogg";
        if (contentType.contains("mpeg")) return "mp3";
        if (contentType.contains("wav")) return "wav";
        return switch (tipo) {
            case FOTO -> "jpg";
            case VIDEO -> "mp4";
            case AUDIO -> "webm";
        };
    }

    public record ArquivoSalvo(
            String nomeArquivo,
            String arquivoCaminho,
            TipoEvidencia tipo,
            String mimeType,
            long tamanhoBytes
    ) {}

    public record ArquivoCarregado(
            Resource resource,
            String mimeType
    ) {}
}

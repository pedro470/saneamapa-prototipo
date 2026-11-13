package br.edu.cesmac.saneamapa.repository;

import br.edu.cesmac.saneamapa.domain.enums.StatusPonto;
import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PontoRepository extends JpaRepository<Ponto, Long> {
    Optional<Ponto> findByCodigoExibicao(String codigoExibicao);
    List<Ponto> findByTipoOcorrencia(TipoOcorrencia tipoOcorrencia);
    List<Ponto> findByStatusIn(List<StatusPonto> statuses);

    @Query("SELECT COUNT(p) FROM Ponto p WHERE p.bairro = :bairro AND p.status IN ('AGUARDANDO_VISTORIA', 'EM_VISTORIA')")
    long contarNaFilaPorBairro(@Param("bairro") String bairro);
}

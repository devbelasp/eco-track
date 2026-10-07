package br.com.fiap.eco_track_ms.service;

import br.com.fiap.eco_track_ms.dto.ColetaCadastroDto;
import br.com.fiap.eco_track_ms.dto.ColetaExibicaoDto;
import br.com.fiap.eco_track_ms.exception.ColetaNaoEncontradaException;
import br.com.fiap.eco_track_ms.model.Coleta;
import br.com.fiap.eco_track_ms.model.StatusDestinacao;
import br.com.fiap.eco_track_ms.repository.ColetaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColetaService {

    static final String MSG_NAO_ENCONTRADA = "Agendamento de coleta não existe no banco de dados!";

    private final ColetaRepository repository;

    public ColetaService(ColetaRepository repository) {
        this.repository = repository;
    }

    // 1. CADASTRAR / AGENDAR COLETA (POST)
    @Transactional
    public ColetaExibicaoDto agendar(ColetaCadastroDto dto) {
        Coleta coleta = new Coleta();
        preencher(coleta, dto);
        coleta.setStatusDestinacao(StatusDestinacao.AGENDADO);

        return new ColetaExibicaoDto(repository.save(coleta));
    }

    // 2. BUSCAR UMA COLETA ESPECÍFICA POR ID (GET por ID)
    @Transactional(readOnly = true)
    public ColetaExibicaoDto buscarPorId(Long id) {
        return new ColetaExibicaoDto(buscarEntidade(id));
    }

    // 3. LISTAR TODAS AS COLETAS COM PAGINAÇÃO (GET)
    @Transactional(readOnly = true)
    public Page<ColetaExibicaoDto> listarTodas(Pageable paginacao) {
        return repository
                .findAll(paginacao)
                .map(ColetaExibicaoDto::new);
    }

    // 4. ATUALIZAR UMA COLETA EXISTENTE (PUT) - o status de destinação é preservado
    @Transactional
    public ColetaExibicaoDto atualizar(Long id, ColetaCadastroDto dto) {
        Coleta coleta = buscarEntidade(id);
        preencher(coleta, dto);

        return new ColetaExibicaoDto(repository.save(coleta));
    }

    // 5. DELETAR / CANCELAR UM AGENDAMENTO (DELETE)
    @Transactional
    public void deletar(Long id) {
        repository.delete(buscarEntidade(id));
    }

    private Coleta buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ColetaNaoEncontradaException(MSG_NAO_ENCONTRADA));
    }

    private void preencher(Coleta coleta, ColetaCadastroDto dto) {
        coleta.setEmpresaGeradora(dto.empresaGeradora());
        coleta.setCategoriaResiduo(dto.categoriaResiduo());
        coleta.setPesoKg(dto.pesoKg());
        coleta.setDataAgendamento(dto.dataAgendamento());
    }
}

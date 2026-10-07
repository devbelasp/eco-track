package br.com.fiap.eco_track_ms.controller;

import br.com.fiap.eco_track_ms.dto.ColetaCadastroDto;
import br.com.fiap.eco_track_ms.dto.ColetaExibicaoDto;
import br.com.fiap.eco_track_ms.service.ColetaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/residuos")
public class ColetaController {

    private final ColetaService service;

    public ColetaController(ColetaService service) {
        this.service = service;
    }

    // 1. CADASTRAR / AGENDAR COLETA (POST)
    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public ColetaExibicaoDto agendar(@RequestBody @Valid ColetaCadastroDto dto) {
        return service.agendar(dto);
    }

    // 2. LISTAR TODAS AS COLETAS COM PAGINAÇÃO (GET)
    // Exemplo de URL: http://localhost:8080/api/residuos/agendamentos?size=5&page=0
    @GetMapping("/agendamentos")
    @ResponseStatus(HttpStatus.OK)
    public Page<ColetaExibicaoDto> listarTodas(
            @PageableDefault(size = 20, page = 0) Pageable paginacao
    ) {
        return service.listarTodas(paginacao);
    }

    // 3. BUSCAR UMA COLETA ESPECÍFICA POR ID (GET)
    @GetMapping("/agendamentos/{id}")
    public ResponseEntity<ColetaExibicaoDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // 4. ATUALIZAR UMA COLETA (PUT)
    @PutMapping("/agendamentos/{id}")
    public ResponseEntity<ColetaExibicaoDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ColetaCadastroDto dto
    ) {
        return ResponseEntity.ok(service.atualizar(id, dto));
    }

    // 5. DELETAR / CANCELAR UM AGENDAMENTO (DELETE)
    @DeleteMapping("/agendamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        service.deletar(id);
    }
}

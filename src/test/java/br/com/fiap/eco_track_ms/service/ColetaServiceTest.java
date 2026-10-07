package br.com.fiap.eco_track_ms.service;

import br.com.fiap.eco_track_ms.dto.ColetaCadastroDto;
import br.com.fiap.eco_track_ms.dto.ColetaExibicaoDto;
import br.com.fiap.eco_track_ms.exception.ColetaNaoEncontradaException;
import br.com.fiap.eco_track_ms.model.CategoriaResiduo;
import br.com.fiap.eco_track_ms.model.Coleta;
import br.com.fiap.eco_track_ms.model.StatusDestinacao;
import br.com.fiap.eco_track_ms.repository.ColetaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testes unitários (base da pirâmide): o repositório é simulado com Mockito. */
@ExtendWith(MockitoExtension.class)
class ColetaServiceTest {

    @Mock
    private ColetaRepository repository;

    @InjectMocks
    private ColetaService service;

    private ColetaCadastroDto dtoValido() {
        return new ColetaCadastroDto(
                "Empresa Verde Ltda",
                CategoriaResiduo.PLASTICO,
                new BigDecimal("12.50"),
                LocalDate.now().plusDays(5));
    }

    private Coleta coletaExistente(Long id) {
        Coleta coleta = new Coleta();
        coleta.setId(id);
        coleta.setEmpresaGeradora("Empresa Antiga S/A");
        coleta.setCategoriaResiduo(CategoriaResiduo.VIDRO);
        coleta.setPesoKg(new BigDecimal("3.00"));
        coleta.setDataAgendamento(LocalDate.now().plusDays(1));
        coleta.setStatusDestinacao(StatusDestinacao.EM_TRANSPORTE);
        return coleta;
    }

    @Test
    void agendar_deveSalvarColetaComStatusAgendado() {
        when(repository.save(any(Coleta.class))).thenAnswer(invocation -> {
            Coleta salva = invocation.getArgument(0);
            salva.setId(1L);
            return salva;
        });

        ColetaExibicaoDto resultado = service.agendar(dtoValido());

        assertEquals(1L, resultado.id());
        assertEquals("Empresa Verde Ltda", resultado.empresaGeradora());
        assertEquals("PLASTICO", resultado.categoriaResiduo());
        assertEquals(new BigDecimal("12.50"), resultado.pesoKg());
        assertEquals("AGENDADO", resultado.statusDestinacao());

        ArgumentCaptor<Coleta> captor = ArgumentCaptor.forClass(Coleta.class);
        verify(repository).save(captor.capture());
        assertEquals(StatusDestinacao.AGENDADO, captor.getValue().getStatusDestinacao());
    }

    @Test
    void buscarPorId_quandoExiste_deveRetornarDto() {
        when(repository.findById(7L)).thenReturn(Optional.of(coletaExistente(7L)));

        ColetaExibicaoDto resultado = service.buscarPorId(7L);

        assertEquals(7L, resultado.id());
        assertEquals("VIDRO", resultado.categoriaResiduo());
    }

    @Test
    void buscarPorId_quandoNaoExiste_deveLancarExcecao() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ColetaNaoEncontradaException.class, () -> service.buscarPorId(99L));
    }

    @Test
    void listarTodas_deveConverterParaDto() {
        Pageable paginacao = PageRequest.of(0, 5);
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(coletaExistente(1L)), paginacao, 1));

        Page<ColetaExibicaoDto> resultado = service.listarTodas(paginacao);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1L, resultado.getContent().get(0).id());
    }

    @Test
    void atualizar_deveAlterarDadosEPreservarStatus() {
        when(repository.findById(7L)).thenReturn(Optional.of(coletaExistente(7L)));
        when(repository.save(any(Coleta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ColetaExibicaoDto resultado = service.atualizar(7L, dtoValido());

        assertEquals("Empresa Verde Ltda", resultado.empresaGeradora());
        assertEquals("PLASTICO", resultado.categoriaResiduo());
        assertEquals(new BigDecimal("12.50"), resultado.pesoKg());
        assertEquals("EM_TRANSPORTE", resultado.statusDestinacao());
    }

    @Test
    void atualizar_quandoNaoExiste_naoDeveSalvar() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ColetaNaoEncontradaException.class, () -> service.atualizar(99L, dtoValido()));
        verify(repository, never()).save(any(Coleta.class));
    }

    @Test
    void deletar_quandoExiste_deveRemover() {
        Coleta coleta = coletaExistente(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(coleta));

        service.deletar(7L);

        verify(repository).delete(coleta);
    }

    @Test
    void deletar_quandoNaoExiste_naoDeveRemover() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ColetaNaoEncontradaException.class, () -> service.deletar(99L));
        verify(repository, never()).delete(any(Coleta.class));
    }
}

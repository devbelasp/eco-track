package br.com.fiap.eco_track_ms.dto;

import br.com.fiap.eco_track_ms.model.Coleta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ColetaExibicaoDto(
        Long id,
        String empresaGeradora,
        String categoriaResiduo,
        BigDecimal pesoKg,
        LocalDate dataAgendamento,
        String statusDestinacao
) {
    public ColetaExibicaoDto(Coleta coleta) {
        this(
                coleta.getId(),
                coleta.getEmpresaGeradora(),
                coleta.getCategoriaResiduo().name(),
                coleta.getPesoKg(),
                coleta.getDataAgendamento(),
                coleta.getStatusDestinacao().name()
        );
    }
}

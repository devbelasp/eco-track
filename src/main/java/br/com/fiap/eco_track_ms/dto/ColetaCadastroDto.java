package br.com.fiap.eco_track_ms.dto;

import br.com.fiap.eco_track_ms.model.CategoriaResiduo;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ColetaCadastroDto(
        @NotBlank(message = "O nome da empresa geradora é obrigatório.")
        @Size(max = 150, message = "O nome da empresa deve ter no máximo 150 caracteres.")
        String empresaGeradora,

        @NotNull(message = "A categoria do resíduo é obrigatória.")
        CategoriaResiduo categoriaResiduo,

        @NotNull(message = "O peso em kg é obrigatório.")
        @Positive(message = "O peso deve ser um valor maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O peso aceita até 8 dígitos inteiros e 2 casas decimais.")
        BigDecimal pesoKg,

        @NotNull(message = "A data de agendamento é obrigatória.")
        @FutureOrPresent(message = "A data de agendamento não pode estar no passado.")
        LocalDate dataAgendamento
) {}

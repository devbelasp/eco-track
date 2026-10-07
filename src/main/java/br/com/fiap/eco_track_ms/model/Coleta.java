package br.com.fiap.eco_track_ms.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_coletas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Coleta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "coleta_sequence_generator")
    @SequenceGenerator(name = "coleta_sequence_generator", sequenceName = "seq_coletas", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "empresa_geradora", nullable = false, length = 150)
    private String empresaGeradora;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_residuo", nullable = false, length = 50)
    private CategoriaResiduo categoriaResiduo;

    @Column(name = "peso_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "data_agendamento", nullable = false)
    private LocalDate dataAgendamento;

    @Column(name = "data_cadastro", insertable = false, updatable = false, nullable = false)
    private java.time.LocalDateTime dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_destinacao", nullable = false, length = 30)
    private StatusDestinacao statusDestinacao = StatusDestinacao.AGENDADO;
}
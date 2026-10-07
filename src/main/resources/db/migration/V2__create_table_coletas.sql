-- Tabela de agendamento de coletas de residuos corporativos
CREATE TABLE tb_coletas (
    id                BIGINT         NOT NULL,
    empresa_geradora  VARCHAR(150)   NOT NULL,
    categoria_residuo VARCHAR(50)    NOT NULL,
    peso_kg           NUMERIC(10, 2) NOT NULL,
    data_agendamento  DATE           NOT NULL,
    data_cadastro     TIMESTAMP      DEFAULT CURRENT_TIMESTAMP NOT NULL,
    status_destinacao VARCHAR(30)    DEFAULT 'AGENDADO' NOT NULL,
    CONSTRAINT pk_tb_coletas PRIMARY KEY (id)
);

COMMENT ON TABLE tb_coletas IS 'Agendamentos de coletas de residuos corporativos.';
COMMENT ON COLUMN tb_coletas.categoria_residuo IS 'LIXO_ELETRONICO, PLASTICO, PAPEL_PAPELAO, METAIS ou VIDRO';

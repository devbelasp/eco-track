package br.com.fiap.eco_track_ms.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: sobem a aplicação inteira (controller, service, JPA e as
 * migrations do Flyway) contra um H2 em memória. Cada teste é desfeito ao final.
 */
@SpringBootTest
@Transactional
class ColetaControllerIntegrationTest {

    private static final String URL = "/api/residuos/agendamentos";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private String json(String empresa, String categoria, String peso, LocalDate data) {
        return """
                {"empresaGeradora":"%s","categoriaResiduo":"%s","pesoKg":%s,"dataAgendamento":"%s"}
                """.formatted(empresa, categoria, peso, data);
    }

    private long criarColeta() throws Exception {
        String resposta = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Empresa Verde Ltda", "PLASTICO", "12.5", LocalDate.now().plusDays(7))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");
        return id.longValue();
    }

    @Test
    void post_comDadosValidos_retorna201() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Empresa Verde Ltda", "METAIS", "40", LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.categoriaResiduo").value("METAIS"))
                .andExpect(jsonPath("$.statusDestinacao").value("AGENDADO"));
    }

    @Test
    void post_comCategoriaInvalida_retorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Empresa Verde Ltda", "MADEIRA", "10", LocalDate.now().plusDays(3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void post_semCamposObrigatorios_retorna400ComDetalhes() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.empresaGeradora").exists())
                .andExpect(jsonPath("$.campos.categoriaResiduo").exists())
                .andExpect(jsonPath("$.campos.pesoKg").exists())
                .andExpect(jsonPath("$.campos.dataAgendamento").exists());
    }

    @Test
    void post_comPesoNegativo_retorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Empresa Verde Ltda", "VIDRO", "-5", LocalDate.now().plusDays(3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.pesoKg").exists());
    }

    @Test
    void post_comDataNoPassado_retorna400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Empresa Verde Ltda", "VIDRO", "5", LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.dataAgendamento").exists());
    }

    @Test
    void get_porId_retornaColetaCriada() throws Exception {
        long id = criarColeta();

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.empresaGeradora").value("Empresa Verde Ltda"))
                .andExpect(jsonPath("$.pesoKg").value(12.5));
    }

    @Test
    void get_porIdInexistente_retorna404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void get_comIdNaoNumerico_retorna400() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void get_listagemPaginada_retorna200() throws Exception {
        criarColeta();

        mockMvc.perform(get(URL).param("size", "5").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void put_atualizaDadosDaColeta() throws Exception {
        long id = criarColeta();

        mockMvc.perform(put(URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nova Empresa S/A", "PAPEL_PAPELAO", "99.9", LocalDate.now().plusDays(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresaGeradora").value("Nova Empresa S/A"))
                .andExpect(jsonPath("$.categoriaResiduo").value("PAPEL_PAPELAO"))
                .andExpect(jsonPath("$.statusDestinacao").value("AGENDADO"));
    }

    @Test
    void delete_removeColeta() throws Exception {
        long id = criarColeta();

        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
    }
}

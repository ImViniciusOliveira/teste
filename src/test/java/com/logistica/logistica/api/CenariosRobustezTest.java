package com.logistica.logistica.api;

import com.logistica.logistica.api.domain.ProcessamentoResult;
import com.logistica.logistica.api.repository.CobrancaRepository;
import com.logistica.logistica.api.service.ProcessamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CenariosRobustezTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    private ProcessamentoService processamentoService;

    @BeforeEach
    void setUp() {
        processamentoService = new ProcessamentoService(cobrancaRepository);
    }

    @Test
    @DisplayName("Cenário 1: Deve processar com sucesso o padrão real original do cliente (43 linhas)")
    void deveProcessarCenario1PadraoCliente() throws Exception {
        executarEValidarCenario("/cenarios/cenario1_padrao_cliente/remessa.rel",
                "/cenarios/cenario1_padrao_cliente/esperado.txt", 43);
    }

    @Test
    @DisplayName("Cenário 2: Deve processar com sucesso quando colunas possuem múltiplos espaços extras")
    void deveProcessarCenario2EspacamentoDinamico() throws Exception {
        executarEValidarCenario("/cenarios/cenario2_espacamento_dinamico/remessa.rel",
                "/cenarios/cenario2_espacamento_dinamico/esperado.txt", 3);
    }

    @Test
    @DisplayName("Cenário 3: Deve normalizar protocolos variados (com espaços ou zeros a menos/mais)")
    void deveProcessarCenario3ProtocoloVariado() throws Exception {
        executarEValidarCenario("/cenarios/cenario3_protocolo_variado/remessa.rel",
                "/cenarios/cenario3_protocolo_variado/esperado.txt", 1);
    }

    @Test
    @DisplayName("Cenário 4: Deve higienizar CPF e CNPJ com pontos, traços e barras")
    void deveProcessarCenario4DocumentoFormatado() throws Exception {
        executarEValidarCenario("/cenarios/cenario4_documento_formatado/remessa.rel",
                "/cenarios/cenario4_documento_formatado/esperado.txt", 2);
    }

    @Test
    @DisplayName("Cenário 5: Deve processar linha 999999 se for título legítimo e descartar o rodapé técnico")
    void deveProcessarCenario5Prefixo999999ComoItem() throws Exception {
        executarEValidarCenario("/cenarios/cenario5_prefixo_999999_como_item/remessa.rel",
                "/cenarios/cenario5_prefixo_999999_como_item/esperado.txt", 1);
    }

    @Test
    @DisplayName("Cenário 6: Deve rejeitar com IllegalArgumentException arquivos inválidos sem colunas obrigatórias")
    void deveRejeitarCenario6ArquivoInvalido() {
        InputStream relStream = getClass().getResourceAsStream("/cenarios/cenario6_arquivo_invalido/remessa.rel");
        assertNotNull(relStream);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                processamentoService.processarInputStream(relStream)
        );
        assertTrue(ex.getMessage().contains("Layout inválido") || ex.getMessage().contains("não encontrada"));
    }

    private void executarEValidarCenario(String pathRel, String pathEsperado, int totalEsperado) throws Exception {
        InputStream relStream = getClass().getResourceAsStream(pathRel);
        assertNotNull(relStream, "Arquivo " + pathRel + " não encontrado!");

        InputStream gabaritoStream = getClass().getResourceAsStream(pathEsperado);
        assertNotNull(gabaritoStream, "Arquivo " + pathEsperado + " não encontrado!");

        List<String> linhasEsperadas;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gabaritoStream, StandardCharsets.ISO_8859_1))) {
            linhasEsperadas = reader.lines().filter(l -> !l.isBlank()).toList();
        }

        ProcessamentoResult result = processamentoService.processarInputStream(relStream);

        assertEquals(totalEsperado, result.totalRegistrosProcessados(), "Total de registros divergente");
        assertEquals(linhasEsperadas.size(), result.itens().size(), "Tamanho de itens divergente");

        List<String> linhasGeradas = List.of(result.conteudoArquivoC().split("\n"));

        for (int i = 0; i < linhasEsperadas.size(); i++) {
            assertEquals(linhasEsperadas.get(i), linhasGeradas.get(i), "Divergência na linha " + (i + 1));
        }
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class ProcessamentoE2ETest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    private ProcessamentoService processamentoService;

    @BeforeEach
    void setUp() {
        processamentoService = new ProcessamentoService(cobrancaRepository);
    }

    @Test
    @DisplayName("Deve ler o arquivo real .rel da pasta originais e produzir o Arquivo C idêntico ao gabarito real")
    void deveProcessarAmostraRealE2E() throws Exception {
        InputStream relStream = getClass().getResourceAsStream("/originais/CART051_020926_01.rel");
        assertNotNull(relStream, "Arquivo CART051_020926_01.rel não encontrado em /originais!");

        InputStream gabaritoStream = getClass().getResourceAsStream("/originais/C05120260902.txt");
        assertNotNull(gabaritoStream, "Arquivo C05120260902.txt não encontrado em /originais!");

        List<String> linhasEsperadas;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gabaritoStream, StandardCharsets.ISO_8859_1))) {
            linhasEsperadas = reader.lines().filter(l -> !l.isBlank()).toList();
        }

        ProcessamentoResult result = processamentoService.processarInputStream(relStream);

        assertEquals(43, result.totalRegistrosProcessados());
        assertEquals(linhasEsperadas.size(), result.itens().size());

        List<String> linhasGeradas = List.of(result.conteudoArquivoC().split("\n"));

        for (int i = 0; i < linhasEsperadas.size(); i++) {
            assertEquals(linhasEsperadas.get(i), linhasGeradas.get(i), "Divergência na linha " + (i + 1));
        }
    }
}

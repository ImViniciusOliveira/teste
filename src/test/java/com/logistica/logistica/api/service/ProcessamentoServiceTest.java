package com.logistica.logistica.api.service;

import com.logistica.logistica.api.domain.ProcessamentoResult;
import com.logistica.logistica.api.repository.CobrancaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoServiceTest {

    @Mock
    private CobrancaRepository cobrancaRepository;

    private ProcessamentoService processamentoService;

    @BeforeEach
    void setUp() {
        processamentoService = new ProcessamentoService(cobrancaRepository);
    }

    @Test
    @DisplayName("Deve processar linha válida do .REL e extrair campos corretamente")
    void deveProcessarLinhaValida() {
        // Monta o cabeçalho e a linha de dados com as posições dinâmicas:
        // pos 127: DTPROTOC (Data: 8 chars)
        // pos 138: PROTOC (Protocolo: 7 chars)
        // pos 616: DEVDOCTO (Documento: 14 chars)
        StringBuilder cabecalho = new StringBuilder();
        cabecalho.append("000000");
        while (cabecalho.length() < 127) cabecalho.append(" ");
        cabecalho.append("DTPROTOC");
        while (cabecalho.length() < 138) cabecalho.append(" ");
        cabecalho.append("PROTOC");
        while (cabecalho.length() < 616) cabecalho.append(" ");
        cabecalho.append("DEVDOCTO");
        while (cabecalho.length() < 650) cabecalho.append(" ");

        StringBuilder linhaDado = new StringBuilder();
        linhaDado.append("000001");
        while (linhaDado.length() < 127) linhaDado.append(" ");
        linhaDado.append("02092026"); // 127..134
        while (linhaDado.length() < 138) linhaDado.append(" ");
        linhaDado.append("0024508"); // 138..144
        while (linhaDado.length() < 616) linhaDado.append(" ");
        linhaDado.append("20382813000109"); // 616..629
        while (linhaDado.length() < 650) linhaDado.append(" ");

        String conteudo = cabecalho + "\n" + linhaDado + "\n999999 TOTAL DE TITULOS";
        ByteArrayInputStream is = new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.ISO_8859_1));

        ProcessamentoResult result = processamentoService.processarInputStream(is);

        assertEquals(3, result.totalLinhasLidas());
        assertEquals(1, result.totalRegistrosProcessados());
        assertEquals(2, result.totalLinhasIgnoradas());
        assertEquals(1, result.itens().size());

        var item = result.itens().get(0);
        assertEquals("AD000821411", item.idAd());
        assertEquals("0024508", item.protocolo());
        assertEquals("02/09/2026", item.dataFormatada());
        assertEquals("20382813000109", item.documentoDevedor());

        verify(cobrancaRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando arquivo for nulo ou vazio")
    void deveLancarExcecaoArquivoVazio() {
        MockMultipartFile arquivoVazio = new MockMultipartFile("arquivo", "teste.rel", "text/plain", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> processamentoService.processarArquivoRel(arquivoVazio));
        assertThrows(IllegalArgumentException.class, () -> processamentoService.processarArquivoRel(null));
    }

    @Test
    @DisplayName("Deve extrair campos com segurança de limites")
    void deveExtrairCamposComSeguranca() {
        assertEquals("", processamentoService.extrairCampo(null, 0, 10));
        assertEquals("", processamentoService.extrairCampo("teste", -1, 3));
        assertEquals("", processamentoService.extrairCampo("teste", 10, 5));
        assertEquals("", processamentoService.extrairCampo("teste", 0, 0));
        assertEquals("tes", processamentoService.extrairCampo("teste", 0, 3));
        assertEquals("ste", processamentoService.extrairCampo("teste", 2, 10));
    }
}

package com.logistica.logistica.api.controller;

import com.logistica.logistica.api.domain.CobrancaItem;
import com.logistica.logistica.api.domain.ProcessamentoResult;
import com.logistica.logistica.api.exception.GlobalExceptionHandler;
import com.logistica.logistica.api.service.ProcessamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcessamentoController.class)
@Import(GlobalExceptionHandler.class)
class ProcessamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcessamentoService processamentoService;

    @Test
    @DisplayName("Deve processar upload do arquivo .REL e retornar o Arquivo C para download")
    void deveProcessarUploadComSucesso() throws Exception {
        String linhaEsperada = "AD000821411    002450802/09/202612345678901";
        CobrancaItem item = new CobrancaItem("AD000821411", "0024508", "02092026", "12345678901", "linha original");
        ProcessamentoResult result = new ProcessamentoResult(1, 1, 0, linhaEsperada, List.of(item));

        Mockito.when(processamentoService.processarArquivoRel(any())).thenReturn(result);

        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "cart051_02092026_01.rel.txt",
                "text/plain",
                "000001 dados".getBytes()
        );

        mockMvc.perform(multipart("/api/logistica/processar-rel").file(arquivo))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"C051_GERADO.txt\""))
                .andExpect(header().string("X-Total-Registros", "1"))
                .andExpect(header().string("X-Total-Linhas-Lidas", "1"))
                .andExpect(content().string(linhaEsperada));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando o arquivo for vazio")
    void deveRetornarBadRequestQuandoArquivoVazio() throws Exception {
        Mockito.when(processamentoService.processarArquivoRel(any()))
                .thenThrow(new IllegalArgumentException("O arquivo .REL enviado está vazio ou não foi fornecido."));

        MockMultipartFile arquivoVazio = new MockMultipartFile(
                "arquivo",
                "vazio.rel.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/logistica/processar-rel").file(arquivoVazio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O arquivo .REL enviado está vazio ou não foi fornecido."));
    }
}

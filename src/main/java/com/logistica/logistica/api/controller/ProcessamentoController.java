package com.logistica.logistica.api.controller;

import com.logistica.logistica.api.domain.ProcessamentoResult;
import com.logistica.logistica.api.service.ProcessamentoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/logistica")
public class ProcessamentoController {

    private final ProcessamentoService processamentoService;

    public ProcessamentoController(ProcessamentoService processamentoService) {
        this.processamentoService = processamentoService;
    }

    @PostMapping(value = "/processar-rel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> processarRel(@RequestParam("arquivo") MultipartFile arquivo) {
        ProcessamentoResult result = processamentoService.processarArquivoRel(arquivo);

        byte[] bytes = result.conteudoArquivoC().getBytes(StandardCharsets.ISO_8859_1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/plain; charset=ISO-8859-1"));
        headers.setContentDispositionFormData("attachment", "C051_GERADO.txt");
        headers.set("X-Total-Registros", String.valueOf(result.totalRegistrosProcessados()));
        headers.set("X-Total-Linhas-Lidas", String.valueOf(result.totalLinhasLidas()));

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}

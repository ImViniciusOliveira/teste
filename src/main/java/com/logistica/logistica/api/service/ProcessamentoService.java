package com.logistica.logistica.api.service;

import com.logistica.logistica.api.domain.CobrancaItem;
import com.logistica.logistica.api.domain.ProcessamentoResult;
import com.logistica.logistica.api.domain.RelLayoutMap;
import com.logistica.logistica.api.entity.CobrancaImportada;
import com.logistica.logistica.api.repository.CobrancaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProcessamentoService {

    private static final int ID_AD_INICIAL = 821411;

    // Padrões Semânticos para Camada de Resgate
    private static final Pattern PADRAO_DATA_8_DIGITOS = Pattern.compile("^\\d{8}$");
    private static final Pattern REGEX_DATA_LINHA = Pattern.compile("(?<!\\d)(\\d{2}/\\d{2}/\\d{4}|\\d{8})(?!\\d)");
    private static final Pattern REGEX_DOC_FORMATADO = Pattern.compile("(\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{14}|\\d{11})");
    private static final Pattern REGEX_PROTOCOLO_NUMERICO = Pattern.compile("(?<!\\d)(\\d{5,8})(?!\\d)");

    private final CobrancaRepository cobrancaRepository;

    public ProcessamentoService(CobrancaRepository cobrancaRepository) {
        this.cobrancaRepository = cobrancaRepository;
    }

    @Transactional
    public ProcessamentoResult processarArquivoRel(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O arquivo .REL enviado está vazio ou não foi fornecido.");
        }

        try (InputStream inputStream = arquivo.getInputStream()) {
            return processarInputStream(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler o fluxo de dados do arquivo enviado: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ProcessamentoResult processarInputStream(InputStream inputStream) {
        int totalLinhasLidas = 0;
        int proximoIdAd = ID_AD_INICIAL;
        List<CobrancaItem> itens = new ArrayList<>();
        RelLayoutMap layout = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                totalLinhasLidas++;

                if (linha.isBlank()) {
                    continue;
                }

                // Busca flexível do cabeçalho nas primeiras linhas
                if (layout == null) {
                    try {
                        layout = RelLayoutMap.fromHeader(linha);
                        continue; // Cabeçalho mapeado, passa para a próxima linha
                    } catch (IllegalArgumentException e) {
                        // Se não for o cabeçalho ainda (ex: título do cartório), ignora e continua procurando
                        continue;
                    }
                }

                // Processa a linha com o mecanismo de Camada Rápida + Resgate Semântico
                CobrancaItem item = processarLinhaComResgate(linha, layout, proximoIdAd);
                if (item != null) {
                    itens.add(item);
                    proximoIdAd++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro durante o processamento do arquivo .REL: " + e.getMessage(), e);
        }

        if (layout == null) {
            throw new IllegalArgumentException("Layout inválido: Colunas obrigatórias não encontradas no cabeçalho (DTPROTOC, PROTOC, DEVDOCTO).");
        }

        // Persistência em lote no banco PostgreSQL
        salvarNoBanco(itens);

        int totalProcessados = itens.size();
        int totalIgnorados = totalLinhasLidas - totalProcessados;

        String conteudoArquivoC = itens.stream()
                .map(CobrancaItem::toLinhaArquivoC)
                .collect(Collectors.joining("\n"));

        return new ProcessamentoResult(
                totalLinhasLidas,
                totalProcessados,
                totalIgnorados,
                conteudoArquivoC,
                itens
        );
    }

    /**
     * Processa a linha: tenta primeiro por layout posicional; se falhar por desalinhamento,
     * aciona o resgate semântico baseado em padrões da linha.
     */
    private CobrancaItem processarLinhaComResgate(String linha, RelLayoutMap layout, int proximoIdAd) {
        // 1. Camada Rápida (Posicional)
        CobrancaItem item = extrairPorLayout(linha, layout, proximoIdAd);
        if (item != null) {
            return item;
        }

        // 2. Camada de Resgate Semântico (Caso a linha esteja com espaços alterados ou desalinhada)
        return resgatarSemântico(linha, proximoIdAd);
    }

    private CobrancaItem extrairPorLayout(String linha, RelLayoutMap layout, int proximoIdAd) {
        String dataRaw = extrairCampo(linha, layout.posDataProtocolo(), layout.tamDataProtocolo()).trim();
        String dataLimpa = dataRaw.replaceAll("[^0-9]", "");
        if (!PADRAO_DATA_8_DIGITOS.matcher(dataLimpa).matches()) {
            return null;
        }

        String protocoloRaw = extrairCampo(linha, layout.posProtocolo(), layout.tamProtocolo()).trim();
        String protocoloNumerico = protocoloRaw.replaceAll("[^0-9]", "");
        if (protocoloNumerico.isEmpty()) {
            return null;
        }

        String documentoRaw = extrairCampo(linha, layout.posDocumento(), layout.tamDocumento()).trim();
        String documentoLimpo = documentoRaw.replaceAll("[^0-9]", "");
        if (documentoLimpo.length() < 11) {
            return null;
        }

        String protocoloFinal = normalizarProtocolo(protocoloNumerico);
        String idAd = String.format("AD%09d", proximoIdAd);

        return new CobrancaItem(idAd, protocoloFinal, dataLimpa, documentoLimpo, linha);
    }

    private CobrancaItem resgatarSemântico(String linha, int proximoIdAd) {
        // Busca data na linha
        Matcher mData = REGEX_DATA_LINHA.matcher(linha);
        if (!mData.find()) {
            return null; // Sem data -> é cabeçalho ou rodapé descritivo
        }
        String dataEncontrada = mData.group(1).replaceAll("[^0-9]", "");
        if (dataEncontrada.length() != 8) {
            return null;
        }

        // Busca CPF ou CNPJ na linha
        Matcher mDoc = REGEX_DOC_FORMATADO.matcher(linha);
        String docEncontrado = null;
        while (mDoc.find()) {
            String docCandidate = mDoc.group(1).replaceAll("[^0-9]", "");
            if (docCandidate.length() == 11 || docCandidate.length() == 14) {
                docEncontrado = docCandidate;
                break;
            }
        }
        if (docEncontrado == null) {
            return null;
        }

        // Busca protocolo numérico adjacente
        Matcher mProt = REGEX_PROTOCOLO_NUMERICO.matcher(linha);
        String protEncontrado = null;
        while (mProt.find()) {
            String candidato = mProt.group(1);
            // Evita confundir com a própria data ou outros campos
            if (!candidato.equals(dataEncontrada) && !docEncontrado.contains(candidato)) {
                protEncontrado = candidato;
                break;
            }
        }

        if (protEncontrado == null) {
            return null;
        }

        String protocoloFinal = normalizarProtocolo(protEncontrado);
        String idAd = String.format("AD%09d", proximoIdAd);

        return new CobrancaItem(idAd, protocoloFinal, dataEncontrada, docEncontrado, linha);
    }

    private String normalizarProtocolo(String protocoloNumerico) {
        try {
            long protLong = Long.parseLong(protocoloNumerico);
            return String.format("%07d", protLong);
        } catch (NumberFormatException e) {
            if (protocoloNumerico.length() >= 7) {
                return protocoloNumerico.substring(protocoloNumerico.length() - 7);
            }
            return String.format("%7s", protocoloNumerico).replace(' ', '0');
        }
    }

    String extrairCampo(String linha, int inicio, int tamanho) {
        if (linha == null || inicio < 0 || tamanho <= 0 || inicio >= linha.length()) {
            return "";
        }
        int fim = Math.min(inicio + tamanho, linha.length());
        return linha.substring(inicio, fim);
    }

    private void salvarNoBanco(List<CobrancaItem> itens) {
        if (itens.isEmpty()) {
            return;
        }

        List<CobrancaImportada> entidades = itens.stream()
                .map(item -> new CobrancaImportada(
                        item.idAd(),
                        item.protocolo(),
                        item.dataFormatada(),
                        item.documentoDevedor().trim(),
                        item.linhaOriginal()
                ))
                .toList();

        cobrancaRepository.saveAll(entidades);
    }
}

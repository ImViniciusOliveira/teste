package com.logistica.logistica.api.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Record que armazena os índices posicionais mapeados dinamicamente a partir do cabeçalho do arquivo .REL.
 */
public record RelLayoutMap(
        int posDataProtocolo,
        int tamDataProtocolo,
        int posProtocolo,
        int tamProtocolo,
        int posDocumento,
        int tamDocumento
) {
    public static final String COLUNA_DATA = "DTPROTOC";
    public static final String COLUNA_PROTOCOLO = "PROTOC";
    public static final String COLUNA_DOCUMENTO = "DEVDOCTO";

    public static final int TAMANHO_PADRAO_DATA = 8;
    public static final int TAMANHO_PADRAO_PROTOCOLO = 10;
    public static final int TAMANHO_PADRAO_DOCUMENTO = 20;

    /**
     * Analisa o cabeçalho e calcula as posições dinamicamente usando limites de palavra (\b).
     */
    public static RelLayoutMap fromHeader(String linhaCabecalho) {
        if (linhaCabecalho == null || linhaCabecalho.isBlank()) {
            throw new IllegalArgumentException("O cabeçalho do arquivo .REL está vazio ou não foi fornecido.");
        }

        int idxData = encontrarIndiceColuna(linhaCabecalho, COLUNA_DATA);
        if (idxData < 0) {
            throw new IllegalArgumentException("Layout inválido: Coluna obrigatória '" + COLUNA_DATA + "' não encontrada no cabeçalho.");
        }

        int idxProtocolo = encontrarIndiceColuna(linhaCabecalho, COLUNA_PROTOCOLO);
        if (idxProtocolo < 0) {
            throw new IllegalArgumentException("Layout inválido: Coluna obrigatória '" + COLUNA_PROTOCOLO + "' não encontrada no cabeçalho.");
        }

        int idxDocumento = encontrarIndiceColuna(linhaCabecalho, COLUNA_DOCUMENTO);
        if (idxDocumento < 0) {
            throw new IllegalArgumentException("Layout inválido: Coluna obrigatória '" + COLUNA_DOCUMENTO + "' não encontrada no cabeçalho.");
        }

        return new RelLayoutMap(
                idxData,
                TAMANHO_PADRAO_DATA,
                idxProtocolo,
                TAMANHO_PADRAO_PROTOCOLO,
                idxDocumento,
                TAMANHO_PADRAO_DOCUMENTO
        );
    }

    private static int encontrarIndiceColuna(String cabecalho, String nomeColuna) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(nomeColuna) + "\\b").matcher(cabecalho);
        return matcher.find() ? matcher.start() : -1;
    }
}

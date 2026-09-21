package com.logistica.logistica.api.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resultado do processamento de um arquivo .REL contendo o Arquivo C gerado e métricas de importação.
 */
public record ProcessamentoResult(
        int totalLinhasLidas,
        int totalRegistrosProcessados,
        int totalLinhasIgnoradas,
        String conteudoArquivoC,
        List<CobrancaItem> itens
) {
    public ProcessamentoResult {
        conteudoArquivoC = Objects.requireNonNullElse(conteudoArquivoC, "");
        itens = itens != null ? List.copyOf(itens) : List.of();
    }
}

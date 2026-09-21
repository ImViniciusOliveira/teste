package com.logistica.logistica.api.domain;

import java.util.Objects;

/**
 * Record que representa os dados extraídos de uma linha de cobrança do arquivo .REL.
 */
public record CobrancaItem(
        String idAd,
        String protocolo,
        String dataProtocolo,
        String documentoDevedor,
        String linhaOriginal
) {
    public CobrancaItem {
        Objects.requireNonNull(idAd, "idAd não pode ser nulo");
        Objects.requireNonNull(protocolo, "protocolo não pode ser nulo");
        Objects.requireNonNull(dataProtocolo, "dataProtocolo não pode ser nulo");
        Objects.requireNonNull(documentoDevedor, "documentoDevedor não pode ser nulo");
    }

    /**
     * Converte data no formato DDMMAAAA para DD/MM/AAAA.
     */
    public String dataFormatada() {
        if (dataProtocolo.length() == 8) {
            return dataProtocolo.substring(0, 2) + "/" +
                   dataProtocolo.substring(2, 4) + "/" +
                   dataProtocolo.substring(4, 8);
        }
        return dataProtocolo;
    }

    /**
     * Gera a linha no padrão do Arquivo C com 44 posições exatas:
     * [ID AD (13)] + [Protocolo (7)] + [Data (10)] + [Documento (14)]
     */
    public String toLinhaArquivoC() {
        return String.format("%-13s%7s%10s%-14s", idAd, protocolo, dataFormatada(), documentoDevedor);
    }
}

package com.logistica.logistica.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelLayoutMapTest {

    @Test
    @DisplayName("Deve identificar as posições corretas a partir de um cabeçalho padrão")
    void deveMapearCabecalhoPadrao() {
        String cabecalho = "000000  ESPTIT  NUMERO  EMISSAO  DTVENC  VALOR  DTPROTOC  PROTOC  MOTIVO  DEVDOCTO  NROPRIV";
        RelLayoutMap layout = RelLayoutMap.fromHeader(cabecalho);

        assertNotNull(layout);
        assertEquals(cabecalho.indexOf("DTPROTOC"), layout.posDataProtocolo());
        assertEquals(cabecalho.indexOf(" PROTOC") + 1, layout.posProtocolo());
        assertEquals(cabecalho.indexOf("DEVDOCTO"), layout.posDocumento());
    }

    @Test
    @DisplayName("Deve identificar as posições mesmo com múltiplos espaços extras entre as colunas")
    void deveMapearCabecalhoComEspacamentoExtra() {
        String cabecalho = "000000       ESPTIT                 DTPROTOC                    PROTOC                         DEVDOCTO";
        RelLayoutMap layout = RelLayoutMap.fromHeader(cabecalho);

        assertNotNull(layout);
        assertEquals(cabecalho.indexOf("DTPROTOC"), layout.posDataProtocolo());
        assertEquals(cabecalho.indexOf(" PROTOC") + 1, layout.posProtocolo());
        assertEquals(cabecalho.indexOf("DEVDOCTO"), layout.posDocumento());
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando coluna obrigatória estiver ausente")
    void deveRejeitarCabecalhoSemColunaObrigatoria() {
        String cabecalhoSemData = "000000  ESPTIT  PROTOC  DEVDOCTO";
        assertThrows(IllegalArgumentException.class, () -> RelLayoutMap.fromHeader(cabecalhoSemData));

        String cabecalhoSemProtocolo = "000000  ESPTIT  DTPROTOC  DEVDOCTO";
        assertThrows(IllegalArgumentException.class, () -> RelLayoutMap.fromHeader(cabecalhoSemProtocolo));

        String cabecalhoSemDocumento = "000000  ESPTIT  DTPROTOC  PROTOC";
        assertThrows(IllegalArgumentException.class, () -> RelLayoutMap.fromHeader(cabecalhoSemDocumento));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando cabeçalho for nulo ou vazio")
    void deveRejeitarCabecalhoVazio() {
        assertThrows(IllegalArgumentException.class, () -> RelLayoutMap.fromHeader(null));
        assertThrows(IllegalArgumentException.class, () -> RelLayoutMap.fromHeader("   "));
    }
}

package com.logistica.logistica.api.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CobrancaItemTest {

    @Test
    @DisplayName("Deve formatar a data corretamente de DDMMAAAA para DD/MM/AAAA")
    void deveFormatarDataCorretamente() {
        CobrancaItem item = new CobrancaItem("AD000821411", "0024508", "02092026", "20382813000109", "linha de teste");
        assertEquals("02/09/2026", item.dataFormatada());
    }

    @Test
    @DisplayName("Deve gerar a linha do Arquivo C no padrão exato com CNPJ")
    void deveGerarLinhaArquivoCComCnpj() {
        CobrancaItem item = new CobrancaItem("AD000821411", "0024508", "02092026", "20382813000109", "linha de teste");
        String linhaGerada = item.toLinhaArquivoC();

        assertEquals("AD000821411  002450802/09/202620382813000109", linhaGerada);
        assertEquals(44, linhaGerada.length());
    }

    @Test
    @DisplayName("Deve gerar a linha do Arquivo C no padrão exato com CPF (11 dígitos + 3 espaços)")
    void deveGerarLinhaArquivoCComCpf() {
        CobrancaItem item = new CobrancaItem("AD000821414", "0024511", "02092026", "15289495797", "linha de teste");
        String linhaGerada = item.toLinhaArquivoC();

        assertEquals("AD000821414  002451102/09/202615289495797   ", linhaGerada);
        assertEquals(44, linhaGerada.length());
    }

    @Test
    @DisplayName("Deve lançar NullPointerException se algum campo obrigatório for nulo")
    void deveValidarCamposNulos() {
        assertThrows(NullPointerException.class, () ->
                new CobrancaItem(null, "0024508", "02092026", "20382813000109", "linha")
        );
    }
}

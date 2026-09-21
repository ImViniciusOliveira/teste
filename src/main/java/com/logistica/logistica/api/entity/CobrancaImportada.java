package com.logistica.logistica.api.entity;

import com.logistica.logistica.api.domain.CobrancaItem;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cobrancas_importadas", indexes = {
        @Index(name = "idx_cobrancas_id_ad", columnList = "id_ad"),
        @Index(name = "idx_cobrancas_protocolo", columnList = "protocolo")
})
public class CobrancaImportada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_ad", nullable = false, length = 50)
    private String idAd;

    @Column(name = "protocolo", nullable = false, length = 50)
    private String protocolo;

    @Column(name = "data_protocolo", nullable = false, length = 20)
    private String dataProtocolo;

    @Column(name = "documento_devedor", nullable = false, length = 255)
    private String documentoDevedor;

    @Column(name = "linha_original", columnDefinition = "TEXT")
    private String linhaOriginal;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    public CobrancaImportada() {
    }

    public CobrancaImportada(String idAd, String protocolo, String dataProtocolo, String documentoDevedor, String linhaOriginal) {
        this.idAd = idAd;
        this.protocolo = protocolo;
        this.dataProtocolo = dataProtocolo;
        this.documentoDevedor = documentoDevedor;
        this.linhaOriginal = linhaOriginal;
    }

    public static CobrancaImportada fromItem(CobrancaItem item) {
        return new CobrancaImportada(
                item.idAd(),
                item.protocolo(),
                item.dataFormatada(),
                item.documentoDevedor(),
                item.linhaOriginal()
        );
    }

    @PrePersist
    protected void onCreate() {
        if (this.criadoEm == null) {
            this.criadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdAd() {
        return idAd;
    }

    public void setIdAd(String idAd) {
        this.idAd = idAd;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public String getDataProtocolo() {
        return dataProtocolo;
    }

    public void setDataProtocolo(String dataProtocolo) {
        this.dataProtocolo = dataProtocolo;
    }

    public String getDocumentoDevedor() {
        return documentoDevedor;
    }

    public void setDocumentoDevedor(String documentoDevedor) {
        this.documentoDevedor = documentoDevedor;
    }

    public String getLinhaOriginal() {
        return linhaOriginal;
    }

    public void setLinhaOriginal(String linhaOriginal) {
        this.linhaOriginal = linhaOriginal;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CobrancaImportada that = (CobrancaImportada) o;
        return Objects.equals(id, that.id) && Objects.equals(idAd, that.idAd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idAd);
    }
}

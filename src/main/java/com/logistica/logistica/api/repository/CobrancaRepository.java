package com.logistica.logistica.api.repository;

import com.logistica.logistica.api.entity.CobrancaImportada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<CobrancaImportada, Long> {

    Optional<CobrancaImportada> findByIdAd(String idAd);

    List<CobrancaImportada> findByProtocolo(String protocolo);

    boolean existsByIdAd(String idAd);
}

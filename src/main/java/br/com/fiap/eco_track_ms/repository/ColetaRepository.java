package br.com.fiap.eco_track_ms.repository;

import br.com.fiap.eco_track_ms.model.Coleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColetaRepository extends JpaRepository<Coleta, Long> {
}
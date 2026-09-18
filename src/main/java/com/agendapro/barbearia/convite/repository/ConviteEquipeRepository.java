package com.agendapro.barbearia.convite.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agendapro.barbearia.convite.entity.ConviteEquipe;
import com.agendapro.barbearia.convite.entity.StatusConviteEquipe;

import jakarta.persistence.LockModeType;

public interface ConviteEquipeRepository extends JpaRepository<ConviteEquipe, Long> {

	@EntityGraph(attributePaths = {"barbearia", "criadoPor"})
	List<ConviteEquipe> findAllByBarbeariaIdOrderByCriadoEmDesc(Long barbeariaId);

	@EntityGraph(attributePaths = {"barbearia", "criadoPor"})
	List<ConviteEquipe> findAllByEmailIgnoreCaseOrderByCriadoEmDesc(String email);

	@EntityGraph(attributePaths = {"barbearia", "criadoPor"})
	Optional<ConviteEquipe> findByBarbeariaIdAndEmailIgnoreCaseAndStatus(
			Long barbeariaId,
			String email,
			StatusConviteEquipe status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from ConviteEquipe c join fetch c.barbearia join fetch c.criadoPor where c.id = :id")
	Optional<ConviteEquipe> buscarPorIdParaAtualizacao(@Param("id") Long id);
}

package com.agendapro.profissionalservico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.agendapro.profissionalservico.entity.ProfissionalServico;

public interface ProfissionalServicoRepository
		extends JpaRepository<ProfissionalServico, Long> {

	boolean existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(Long id, Long usuarioId);

	@Override
	@EntityGraph(attributePaths = {"profissional", "servico"})
	Optional<ProfissionalServico> findById(Long id);

	@EntityGraph(attributePaths = {"profissional", "servico"})
	Optional<ProfissionalServico> findByProfissionalIdAndServicoId(
			Long profissionalId,
			Long servicoId
	);

	boolean existsByProfissionalIdAndServicoIdAndAtivoTrue(
			Long profissionalId,
			Long servicoId
	);

	@EntityGraph(attributePaths = {"profissional", "servico"})
	List<ProfissionalServico> findAllByProfissionalIdAndAtivoTrue(
			Long profissionalId
	);
}

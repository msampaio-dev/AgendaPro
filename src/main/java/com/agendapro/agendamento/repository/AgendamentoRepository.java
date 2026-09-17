package com.agendapro.agendamento.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import com.agendapro.agendamento.entity.Agendamento;
import com.agendapro.agendamento.entity.StatusAgendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

	@Override
	@EntityGraph(attributePaths = {"cliente", "profissional", "profissional.usuario", "barbearia", "servico", "servicoAdicional"})
	Optional<Agendamento> findById(Long id);

	@Override
	@EntityGraph(attributePaths = {"cliente", "profissional", "profissional.usuario", "barbearia", "servico", "servicoAdicional"})
	Page<Agendamento> findAll(Specification<Agendamento> specification, Pageable pageable);

	boolean existsByIdAndClienteId(Long id, Long clienteId);

	boolean existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(Long id, Long usuarioId);

	boolean existsByIdAndBarbeariaProprietarioUsuarioIdAndBarbeariaAtivoTrue(Long id, Long usuarioId);

	boolean existsByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThan(
			Long profissionalId,
			Collection<StatusAgendamento> status,
			Instant novoFim,
			Instant novoInicio
	);

	boolean existsByProfissionalIdAndStatusInAndInicioAfter(
			Long profissionalId,
			Collection<StatusAgendamento> status,
			Instant inicio
	);

	boolean existsByBarbeariaIdAndStatusInAndInicioAfter(
			Long barbeariaId,
			Collection<StatusAgendamento> status,
			Instant inicio
	);

	List<Agendamento> findAllByBarbeariaIdAndStatusInAndInicioAfterOrderByInicio(
			Long barbeariaId,
			Collection<StatusAgendamento> status,
			Instant inicio
	);

	List<Agendamento> findAllByProfissionalIdAndStatusInAndInicioLessThanAndFimGreaterThanOrderByInicio(
			Long profissionalId,
			Collection<StatusAgendamento> status,
			Instant fimDoPeriodo,
			Instant inicioDoPeriodo
	);

	List<Agendamento> findAllByProfissionalIdAndInicioGreaterThanEqualAndInicioLessThanOrderByInicio(
			Long profissionalId,
			Instant inicioDoDia,
			Instant inicioDoProximoDia
	);

}

package com.agendapro.disponibilidade.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.disponibilidade.entity.HorarioAtendimento;

public interface HorarioAtendimentoRepository
		extends JpaRepository<HorarioAtendimento, Long> {

	boolean existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(Long id, Long usuarioId);

	@Override
	@EntityGraph(attributePaths = "profissional")
	Optional<HorarioAtendimento> findById(Long id);

	boolean existsByProfissionalIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
			Long profissionalId,
			DayOfWeek diaSemana,
			LocalTime novoHorarioFim,
			LocalTime novoHorarioInicio
	);

	@EntityGraph(attributePaths = "profissional")
	List<HorarioAtendimento> findAllByProfissionalIdAndAtivoTrue(
			Long profissionalId
	);

	List<HorarioAtendimento> findAllByProfissionalIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
			Long profissionalId,
			DayOfWeek diaSemana
	);
}

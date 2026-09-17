package com.agendapro.barbearia.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.barbearia.entity.HorarioFuncionamentoBarbearia;

public interface HorarioFuncionamentoBarbeariaRepository
		extends JpaRepository<HorarioFuncionamentoBarbearia, Long> {

	@Override
	@EntityGraph(attributePaths = "barbearia")
	Optional<HorarioFuncionamentoBarbearia> findById(Long id);

	List<HorarioFuncionamentoBarbearia> findAllByBarbeariaIdAndAtivoTrueOrderByDiaSemanaAscHorarioInicioAsc(
			Long barbeariaId
	);

	List<HorarioFuncionamentoBarbearia> findAllByBarbeariaIdAndDiaSemanaAndAtivoTrueOrderByHorarioInicio(
			Long barbeariaId,
			DayOfWeek diaSemana
	);

	boolean existsByBarbeariaIdAndDiaSemanaAndAtivoTrueAndHorarioInicioLessThanAndHorarioFimGreaterThan(
			Long barbeariaId,
			DayOfWeek diaSemana,
			LocalTime novoFim,
			LocalTime novoInicio
	);

	boolean existsByIdAndBarbeariaProprietarioUsuarioIdAndAtivoTrue(Long id, Long usuarioId);
}

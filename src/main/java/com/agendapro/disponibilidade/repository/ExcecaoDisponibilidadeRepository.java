package com.agendapro.disponibilidade.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.disponibilidade.entity.ExcecaoDisponibilidade;
import com.agendapro.disponibilidade.entity.TipoExcecaoDisponibilidade;

public interface ExcecaoDisponibilidadeRepository
		extends JpaRepository<ExcecaoDisponibilidade, Long> {

	boolean existsByIdAndProfissionalUsuarioIdAndProfissionalAtivoTrue(Long id, Long usuarioId);

	@Override
	@EntityGraph(attributePaths = "profissional")
	Optional<ExcecaoDisponibilidade> findById(Long id);

	boolean existsByProfissionalIdAndDataAndTipoAndHorarioInicioAndHorarioFimAndAtivoTrue(
			Long profissionalId,
			LocalDate data,
			TipoExcecaoDisponibilidade tipo,
			LocalTime horarioInicio,
			LocalTime horarioFim
	);

	@EntityGraph(attributePaths = "profissional")
	List<ExcecaoDisponibilidade> findAllByProfissionalIdAndDataAndAtivoTrueOrderByHorarioInicio(
			Long profissionalId,
			LocalDate data
	);
}

package com.agendapro.profissional.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.profissional.entity.Profissional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

	@Override
	@EntityGraph(attributePaths = {"usuario", "barbearia"})
	Optional<Profissional> findById(Long id);

	@Override
	@EntityGraph(attributePaths = {"usuario", "barbearia"})
	List<Profissional> findAll();

	boolean existsByUsuarioId(Long usuarioId);

	Optional<Profissional> findByUsuarioId(Long usuarioId);

	@EntityGraph(attributePaths = {"usuario", "barbearia"})
	Optional<Profissional> findByUsuarioIdAndAtivoTrue(Long usuarioId);

	boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

	boolean existsByIdAndUsuarioIdAndAtivoTrue(Long id, Long usuarioId);

	boolean existsByIdAndBarbeariaProprietarioUsuarioIdAndAtivoTrue(Long id, Long usuarioId);

	@EntityGraph(attributePaths = {"usuario", "barbearia"})
	List<Profissional> findAllByBarbeariaId(Long barbeariaId);

	/** Catalogo da sugestao por IA: o limite vai para o SQL, nao para a memoria. */
	@EntityGraph(attributePaths = {"usuario"})
	List<Profissional> findTop30ByBarbeariaIdAndAtivoTrueAndUsuarioAtivoTrueOrderByIdAsc(Long barbeariaId);

	boolean existsByBarbeariaIdAndAtivoTrue(Long barbeariaId);

	boolean existsByBarbeariaIdAndUsuarioIdAndAtivoTrue(Long barbeariaId, Long usuarioId);
}

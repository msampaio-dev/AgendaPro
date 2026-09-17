package com.agendapro.barbearia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.barbearia.entity.Barbearia;

public interface BarbeariaRepository extends JpaRepository<Barbearia, Long> {
	boolean existsByNomeIgnoreCase(String nome);

	@Override
	@EntityGraph(attributePaths = {"proprietario", "proprietario.usuario"})
	Optional<Barbearia> findById(Long id);

	@Override
	@EntityGraph(attributePaths = {"proprietario", "proprietario.usuario"})
	List<Barbearia> findAll();

	@EntityGraph(attributePaths = {"proprietario", "proprietario.usuario"})
	List<Barbearia> findAllByAtivoTrueAndProprietarioIsNotNullOrderByNomeAsc();

	@EntityGraph(attributePaths = {"proprietario", "proprietario.usuario"})
	List<Barbearia> findAllByProprietarioUsuarioId(Long usuarioId);

	boolean existsByIdAndProprietarioUsuarioIdAndAtivoTrue(Long id, Long usuarioId);
	boolean existsByProprietarioIdAndAtivoTrue(Long profissionalId);
	boolean existsByProprietarioIdAndAtivoTrueAndIdNot(Long profissionalId, Long id);
}

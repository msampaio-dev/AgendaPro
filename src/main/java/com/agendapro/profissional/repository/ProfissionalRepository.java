package com.agendapro.profissional.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.profissional.entity.Profissional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

	@Override
	@EntityGraph(attributePaths = "usuario")
	Optional<Profissional> findById(Long id);

	@Override
	@EntityGraph(attributePaths = "usuario")
	List<Profissional> findAll();

	boolean existsByUsuarioId(Long usuarioId);

	boolean existsByIdAndUsuarioId(Long id, Long usuarioId);

	boolean existsByIdAndUsuarioIdAndAtivoTrue(Long id, Long usuarioId);
}

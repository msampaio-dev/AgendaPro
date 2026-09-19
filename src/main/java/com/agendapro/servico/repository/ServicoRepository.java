package com.agendapro.servico.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.servico.entity.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

	boolean existsByBarbeariaIdAndNomeIgnoreCase(Long barbeariaId, String nome);

	List<Servico> findAllByBarbeariaIdOrderByNomeAsc(Long barbeariaId);

	List<Servico> findAllByBarbeariaIdAndAtivoTrueOrderByNomeAsc(Long barbeariaId);

	List<Servico> findAllByOrderByNomeAsc();
}

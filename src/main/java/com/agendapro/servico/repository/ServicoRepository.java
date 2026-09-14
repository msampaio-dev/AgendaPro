package com.agendapro.servico.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.agendapro.servico.entity.Servico;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

	boolean existsByNomeIgnoreCase(String nome);
}

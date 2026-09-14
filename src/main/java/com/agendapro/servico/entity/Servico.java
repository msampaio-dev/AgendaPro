package com.agendapro.servico.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "servicos")
public class Servico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(length = 500)
	private String descricao;

	@Column(name = "duracao_minutos", nullable = false)
	private Integer duracaoMinutos;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal preco;

	@Column(nullable = false)
	private boolean ativo = true;

	protected Servico() {
	}

	public Servico(
			String nome,
			String descricao,
			Integer duracaoMinutos,
			BigDecimal preco
	) {
		this.nome = nome;
		this.descricao = descricao;
		this.duracaoMinutos = duracaoMinutos;
		this.preco = preco;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public Integer getDuracaoMinutos() {
		return duracaoMinutos;
	}

	public BigDecimal getPreco() {
		return preco;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void atualizarDados(
			String nome,
			String descricao,
			Integer duracaoMinutos,
			BigDecimal preco
	) {
		this.nome = nome;
		this.descricao = descricao;
		this.duracaoMinutos = duracaoMinutos;
		this.preco = preco;
	}

	public void desativar() {
		this.ativo = false;
	}
}

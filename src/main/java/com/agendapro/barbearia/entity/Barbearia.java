package com.agendapro.barbearia.entity;

import java.time.ZoneId;

import com.agendapro.profissional.entity.Profissional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "barbearias")
public class Barbearia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proprietario_profissional_id")
	private Profissional proprietario;

	@Embedded
	private EnderecoBarbearia endereco;

	@Column(name = "fuso_horario", nullable = false, length = 50)
	private ZoneId fusoHorario = ZoneId.of("America/Sao_Paulo");

	protected Barbearia() {
	}

	public Barbearia(String nome) {
		this.nome = nome;
	}

	public Barbearia(
			String nome,
			Profissional proprietario,
			EnderecoBarbearia endereco,
			ZoneId fusoHorario
	) {
		this.nome = nome;
		this.proprietario = proprietario;
		this.endereco = endereco;
		this.fusoHorario = fusoHorario;
	}

	public Long getId() { return id; }
	public String getNome() { return nome; }
	public boolean isAtivo() { return ativo; }
	public String getFotoUrl() { return fotoUrl; }
	public Profissional getProprietario() { return proprietario; }
	public EnderecoBarbearia getEndereco() { return endereco; }
	public ZoneId getFusoHorario() { return fusoHorario; }

	public void atualizarNome(String nome) { this.nome = nome; }
	public void atualizarDados(String nome, EnderecoBarbearia endereco, ZoneId fusoHorario) {
		this.nome = nome;
		this.endereco = endereco;
		this.fusoHorario = fusoHorario;
	}
	public void atualizarFoto(String fotoUrl) { this.fotoUrl = fotoUrl; }
	public void transferirPropriedadePara(Profissional profissional) { this.proprietario = profissional; }
	public void desativar() { this.ativo = false; }
}

package com.agendapro.profissional.entity;

import java.time.ZoneId;

import com.agendapro.usuario.entity.Usuario;
import com.agendapro.barbearia.entity.Barbearia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "profissionais")
public class Profissional {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, unique = true)
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "barbearia_id", nullable = false)
	private Barbearia barbearia;

	@Column(nullable = false)
	private boolean ativo = true;

	@Column(name = "fuso_horario", nullable = false, length = 50)
	private ZoneId fusoHorario = ZoneId.of("America/Sao_Paulo");

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	protected Profissional() {
	}

	public Profissional(Usuario usuario) {
		this.usuario = usuario;
	}

	public Profissional(Usuario usuario, Barbearia barbearia) {
		this.usuario = usuario;
		this.barbearia = barbearia;
	}

	public Profissional(Usuario usuario, ZoneId fusoHorario) {
		this.usuario = usuario;
		this.fusoHorario = fusoHorario;
	}

	public Profissional(Usuario usuario, Barbearia barbearia, ZoneId fusoHorario) {
		this.usuario = usuario;
		this.barbearia = barbearia;
		this.fusoHorario = fusoHorario;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public Barbearia getBarbearia() { return barbearia; }

	public ZoneId getFusoHorario() {
		return fusoHorario;
	}

	public String getFotoUrl() {
		return fotoUrl;
	}

	public void atualizarFoto(String fotoUrl) {
		this.fotoUrl = fotoUrl;
	}

	public void transferirPara(Barbearia barbearia) {
		this.barbearia = barbearia;
	}

	public void desativar() {
		this.ativo = false;
	}
}

package com.agendapro.barbearia.convite.entity;

import java.time.Instant;

import com.agendapro.barbearia.entity.Barbearia;
import com.agendapro.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "convites_equipe")
public class ConviteEquipe {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "barbearia_id", nullable = false)
	private Barbearia barbearia;

	@Column(nullable = false, length = 254)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private StatusConviteEquipe status = StatusConviteEquipe.PENDENTE;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false)
	private Usuario criadoPor;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "expira_em", nullable = false)
	private Instant expiraEm;

	@Column(name = "respondido_em")
	private Instant respondidoEm;

	protected ConviteEquipe() {
	}

	public ConviteEquipe(
			Barbearia barbearia,
			String email,
			Usuario criadoPor,
			Instant criadoEm,
			Instant expiraEm
	) {
		this.barbearia = barbearia;
		this.email = email;
		this.criadoPor = criadoPor;
		this.criadoEm = criadoEm;
		this.expiraEm = expiraEm;
	}

	public Long getId() { return id; }
	public Barbearia getBarbearia() { return barbearia; }
	public String getEmail() { return email; }
	public StatusConviteEquipe getStatus() { return status; }
	public Usuario getCriadoPor() { return criadoPor; }
	public Instant getCriadoEm() { return criadoEm; }
	public Instant getExpiraEm() { return expiraEm; }
	public Instant getRespondidoEm() { return respondidoEm; }

	public boolean estaPendente() { return status == StatusConviteEquipe.PENDENTE; }
	public boolean expirou(Instant agora) { return estaPendente() && !agora.isBefore(expiraEm); }
	public void aceitar(Instant agora) { status = StatusConviteEquipe.ACEITO; respondidoEm = agora; }
	public void recusar(Instant agora) { status = StatusConviteEquipe.RECUSADO; respondidoEm = agora; }
	public void cancelar(Instant agora) { status = StatusConviteEquipe.CANCELADO; respondidoEm = agora; }
	public void expirar(Instant agora) { status = StatusConviteEquipe.EXPIRADO; respondidoEm = agora; }
}

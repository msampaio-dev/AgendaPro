package com.agendapro.usuario.entity;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 100)
	private String senhaHash;

	@Column(nullable = false)
	private boolean ativo = true;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "usuarios_perfis", joinColumns = @JoinColumn(name = "usuario_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "perfil", nullable = false, length = 20)
	private Set<PerfilUsuario> perfis = EnumSet.of(PerfilUsuario.CLIENTE);

	protected Usuario() {
	}

	public Usuario(String nome, String email) {
		this.nome = nome;
		this.email = email;
	}

	public Usuario(String nome, String email, String senhaHash) {
		this(nome, email);
		this.senhaHash = senhaHash;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public Set<PerfilUsuario> getPerfis() {
		return Set.copyOf(perfis);
	}

	public void adicionarPerfil(PerfilUsuario perfil) {
		perfis.add(perfil);
	}

	public void removerPerfil(PerfilUsuario perfil) {
		if (perfil != PerfilUsuario.CLIENTE) {
			perfis.remove(perfil);
		}
	}

	public void atualizarDados(String nome, String email) {
		this.nome = nome;
		this.email = email;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void desativar() {
		this.ativo = false;
	}
}

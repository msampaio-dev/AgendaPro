package com.agendapro.shared.imagem;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Uma imagem enviada, guardada no banco em vez do disco.
 *
 * O binario fica em BYTEA e nao em large object: sao imagens pequenas, com
 * limite de 5 MB, e o BYTEA vem e vai numa unica linha, sem o ciclo de vida
 * separado que o oid exigiria.
 */
@Entity
@Table(name = "imagens")
public class Imagem {

	@Id
	@Column(length = 120)
	private String nome;

	@Column(name = "tipo_conteudo", nullable = false, length = 60)
	private String tipoConteudo;

	@Column(nullable = false)
	private byte[] conteudo;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	protected Imagem() {
	}

	public Imagem(String nome, String tipoConteudo, byte[] conteudo, Instant criadoEm) {
		this.nome = nome;
		this.tipoConteudo = tipoConteudo;
		this.conteudo = conteudo;
		this.criadoEm = criadoEm;
	}

	public String getNome() { return nome; }
	public String getTipoConteudo() { return tipoConteudo; }
	public byte[] getConteudo() { return conteudo; }
	public Instant getCriadoEm() { return criadoEm; }
}

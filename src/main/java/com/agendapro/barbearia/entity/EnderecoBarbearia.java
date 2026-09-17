package com.agendapro.barbearia.entity;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EnderecoBarbearia {
	@Column(length = 8)
	private String cep;

	@Column(length = 160)
	private String logradouro;

	@Column(length = 20)
	private String numero;

	@Column(length = 80)
	private String complemento;

	@Column(length = 100)
	private String bairro;

	@Column(length = 100)
	private String cidade;

	@Column(length = 2)
	private String estado;

	protected EnderecoBarbearia() {
	}

	public EnderecoBarbearia(
			String cep,
			String logradouro,
			String numero,
			String complemento,
			String bairro,
			String cidade,
			String estado
	) {
		this.cep = cep.trim();
		this.logradouro = logradouro.trim();
		this.numero = numero.trim();
		this.complemento = normalizarOpcional(complemento);
		this.bairro = bairro.trim();
		this.cidade = cidade.trim();
		this.estado = estado.trim().toUpperCase(Locale.ROOT);
	}

	public String getCep() { return cep; }
	public String getLogradouro() { return logradouro; }
	public String getNumero() { return numero; }
	public String getComplemento() { return complemento; }
	public String getBairro() { return bairro; }
	public String getCidade() { return cidade; }
	public String getEstado() { return estado; }

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}
}

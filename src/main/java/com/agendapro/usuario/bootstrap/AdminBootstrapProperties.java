package com.agendapro.usuario.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record AdminBootstrapProperties(
		boolean enabled,
		String nome,
		String email,
		String senha
) {
}

package com.agendapro.usuario.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapConfig.class);

	@Bean
	@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
	ApplicationRunner primeiroAdminApplicationRunner(
			PrimeiroAdminService primeiroAdminService,
			AdminBootstrapProperties properties
	) {
		return arguments -> {
			boolean criado = primeiroAdminService.criarSeNecessario(properties);

			if (criado) {
				LOGGER.info("Administrador inicial criado com sucesso");
			} else {
				LOGGER.info("Criação do administrador inicial ignorada: já existe um ADMIN");
			}
		};
	}
}

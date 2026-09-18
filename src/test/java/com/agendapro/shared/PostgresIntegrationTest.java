package com.agendapro.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração com PostgreSQL real.
 *
 * O container é um singleton iniciado uma única vez por JVM e nunca parado
 * explicitamente: o Ryuk do Testcontainers o remove ao fim da execução. Não use
 * {@code @Testcontainers}/{@code @Container} aqui — a extensão do JUnit encerra o
 * container ao fim de cada classe de teste e o recria na seguinte, numa porta
 * nova, enquanto o Spring reaproveita o ApplicationContext em cache com o pool de
 * conexões apontando para o container antigo.
 *
 * Cada configuração de contexto recebe um banco próprio dentro desse container.
 * O isolamento é necessário porque as classes de teste partem de um banco vazio e
 * porque nem todas usam o mesmo conjunto de migrations: quem inclui
 * {@code db/devdata} aplica a carga de demonstração, que os demais contextos
 * rejeitariam na validação do Flyway.
 */
@ActiveProfiles("test")
@EnabledIf("com.agendapro.shared.PostgresIntegrationTest#dockerDisponivel")
public abstract class PostgresIntegrationTest {

	protected static final PostgreSQLContainer<?> POSTGRES = iniciarContainer();

	private static final AtomicInteger SEQUENCIA = new AtomicInteger();

	@DynamicPropertySource
	static void bancoIsoladoPorContexto(DynamicPropertyRegistry registry) {
		String banco = criarBanco();
		registry.add("spring.datasource.url", () -> urlJdbc(banco));
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	private static PostgreSQLContainer<?> iniciarContainer() {
		PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16.15-alpine");
		if (dockerDisponivel()) {
			container.start();
		}
		return container;
	}

	private static String criarBanco() {
		String banco = "agendapro_test_" + SEQUENCIA.incrementAndGet();
		try (Connection conexao = DriverManager.getConnection(
				POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
				Statement statement = conexao.createStatement()) {
			statement.execute("CREATE DATABASE " + banco);
		} catch (SQLException e) {
			throw new IllegalStateException("Não foi possível criar o banco de teste " + banco, e);
		}
		return banco;
	}

	private static String urlJdbc(String banco) {
		return "jdbc:postgresql://%s:%d/%s".formatted(
				POSTGRES.getHost(), POSTGRES.getFirstMappedPort(), banco);
	}

	public static boolean dockerDisponivel() {
		return DockerClientFactory.instance().isDockerAvailable();
	}
}

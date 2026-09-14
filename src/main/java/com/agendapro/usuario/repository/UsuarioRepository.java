package com.agendapro.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.agendapro.usuario.entity.PerfilUsuario;
import com.agendapro.usuario.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	boolean existsByEmailIgnoreCase(String email);

	Optional<Usuario> findByEmailIgnoreCase(String email);

	@Query("select (count(u) > 0) from Usuario u join u.perfis perfil where perfil = :perfil")
	boolean existsByPerfil(@Param("perfil") PerfilUsuario perfil);
}

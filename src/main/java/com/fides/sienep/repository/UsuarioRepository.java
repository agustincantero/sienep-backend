package com.fides.sienep.repository;

import com.fides.sienep.model.PaisDocumento;
import com.fides.sienep.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    // Los chequeos de unicidad de altas y ediciones van contra usuarios (estudiantes y
    // funcionarios juntos), porque ahí viven uk_documento_pais y uk_email. Chequeando solo
    // una subclase, el choque con la otra llega hasta la base y vuelve como el 409 genérico
    // de GlobalExceptionHandler, sin decir qué campo falló.
    boolean existsByDocumentoAndPaisDocumento(String documento, PaisDocumento paisDocumento);

    // IgnoreCase: el trigger de BD guarda el email en minúsculas, así que "Ana@X.com" y
    // "ana@x.com" son el mismo email.
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdUsuarioNot(String email, int idUsuario);
}

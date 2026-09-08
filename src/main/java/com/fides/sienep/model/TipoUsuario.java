package com.fides.sienep.model;

// Discrimina los dos subtipos de Usuario (herencia JOINED: funcionarios / estudiantes).
// Lo usa GET /auth/me para que el frontend sepa qué shell pintar sin inferirlo del rol.
public enum TipoUsuario {
    FUNCIONARIO, ESTUDIANTE
}

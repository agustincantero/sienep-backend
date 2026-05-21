-- =====================================================
-- CREAR SCHEMA PROYECTO
-- =====================================================

CREATE SCHEMA proyecto;
SET search_path TO proyecto;


-- =====================================================
-- ENUMS Y DOMINIOS BASE
-- =====================================================

-- Estado lógico general para todas las tablas
CREATE TYPE proyecto.estado_logico AS ENUM ('ACTIVO', 'INACTIVO', 'ELIMINADO', 'PENDIENTE_DE_ACTIVACION');

-- Estado operativo de recordatorios
CREATE TYPE proyecto.estado_operativo AS ENUM('HOY', 'FUTURO', 'VENCIDO');

-- Tipo de acción para auditoría
CREATE TYPE proyecto.tipo_accion AS ENUM ('INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT');

-- Frecuencia para recordatorios
CREATE TYPE proyecto.tipo_frecuencia AS ENUM ('diaria', 'semanal', 'mensual', 'personalizada');

-- Días de la semana
CREATE TYPE proyecto.dia_semana AS ENUM ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo');

-- Tipo de recordatorio
CREATE TYPE proyecto.tipo_recordatorio AS ENUM ('llamada', 'reunion', 'tarea administrativa', 'otro');

-- País de emisión de documento (ISO 3166-1 alpha-3)
CREATE TYPE proyecto.pais_documento AS ENUM (
  'AFG','ALB','DZA','AND','AGO','ATG','ARG','ARM','AUS','AUT','AZE',
  'BHS','BHR','BGD','BRB','BLR','BEL','BLZ','BEN','BTN','BOL','BIH','BWA','BRA','BRN','BGR','BFA','BDI',
  'KHM','CMR','CAN','CPV','CAF','TCD','CHL','CHN','COL','COM','COG','CRI','CIV','HRV','CUB','CYP','CZE',
  'DNK','DJI','DMA','DOM','ECU','EGY','SLV','GNQ','ERI','EST','SWZ','ETH','FJI','FIN','FRA','GAB','GMB',
  'GEO','DEU','GHA','GRC','GRD','GTM','GIN','GNB','GUY','HTI','HND','HUN','ISL','IND','IDN','IRN','IRQ',
  'IRL','ISR','ITA','JAM','JPN','JOR','KAZ','KEN','KIR','PRK','KOR','KWT','KGZ','LAO','LVA','LBN','LSO',
  'LBR','LBY','LIE','LTU','LUX','MDG','MWI','MYS','MDV','MLI','MLT','MHL','MRT','MUS','MEX','FSM','MDA',
  'MCO','MNG','MNE','MAR','MOZ','MMR','NAM','NRU','NPL','NLD','NZL','NIC','NER','NGA','MKD','NOR','OMN',
  'PAK','PLW','PAN','PNG','PRY','PER','PHL','POL','PRT','QAT','ROU','RUS','RWA','KNA','LCA','VCT','WSM',
  'SMR','STP','SAU','SEN','SRB','SYC','SLE','SGP','SVK','SVN','SLB','SOM','ZAF','SSD','ESP','LKA','SDN',
  'SUR','SWE','CHE','SYR','TWN','TJK','TZA','THA','TLS','TGO','TON','TTO','TUN','TUR','TKM','TUV','UGA',
  'UKR','ARE','GBR','USA','URY','UZB','VUT','VAT','VEN','VNM','YEM','ZMB','ZWE'
);

-- =====================================================
-- DOMINIOS
-- =====================================================

-- Correo electrónico, máximo 50 caracteres, validación básica de formato
CREATE DOMAIN proyecto.dom_email AS VARCHAR(50)
    CHECK (VALUE ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$');

-- Teléfono
CREATE DOMAIN proyecto.dom_telefono AS VARCHAR(15)
    CHECK (VALUE ~ '^[0-9]{8,12}$');

-- =====================================================
-- FUNCIONES AUXILIARES
-- =====================================================

-- Actualiza el campo updated_at automáticamente
CREATE OR REPLACE FUNCTION proyecto.actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Normaliza correos electrónicos a minúsculas
CREATE OR REPLACE FUNCTION proyecto.normalizar_email()
RETURNS TRIGGER AS $$
BEGIN
    NEW.email := LOWER(NEW.email);
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Actualiza estado operativo de recordatorio según fecha
CREATE OR REPLACE FUNCTION proyecto.actualizar_estado_recordatorio()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.fec_hora::date < CURRENT_DATE THEN
        NEW.estado_operativo := 'VENCIDO';
    ELSIF NEW.fec_hora::date = CURRENT_DATE THEN
        NEW.estado_operativo := 'HOY';
ELSE
        NEW.estado_operativo := 'FUTURO';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- =====================================================
-- TABLA: usuarios
-- =====================================================

CREATE TABLE proyecto.usuarios (
                                   id_usuario SERIAL PRIMARY KEY,

                                   nombre VARCHAR(50) NOT NULL,
                                   apellido VARCHAR(50) NOT NULL,
                                   documento VARCHAR(15) NOT NULL,
                                   pais_documento pais_documento NOT NULL DEFAULT 'URY',

                                   email dom_email NOT NULL,
                                   contrasenia VARCHAR(255) NOT NULL, -- se almacena el resultado cifrado (Cifrado César a nivel aplicación)

                                   estado estado_logico NOT NULL DEFAULT 'PENDIENTE_DE_ACTIVACION',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- RESTRICCIONES DE NEGOCIO Y VALIDACIONES
    -- ==============================================

    -- No permitir espacios vacíos en nombre o apellido
                                   CONSTRAINT chk_nombre_apellido_not_empty CHECK (
                                       TRIM(nombre) <> '' AND TRIM(apellido) <> ''
                                       ),

    -- Validar nombre y apellido alfanuméricos (sin símbolos)
                                   CONSTRAINT chk_nombre_apellido_valido CHECK (
                                       nombre ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$' AND
                                       apellido ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$'
),

    -- Documento válido según país
    CONSTRAINT chk_documento_valido CHECK (
        (pais_documento = 'URY' AND documento ~ '^[0-9]{6,8}$') OR
        (pais_documento <> 'URY' AND documento ~ '^[A-Za-z0-9]{1,15}$')
    ),


    -- Unicidad combinada
    CONSTRAINT uk_documento_pais UNIQUE (documento, pais_documento),
    CONSTRAINT uk_email UNIQUE (email)
);

-- =====================================================
-- TRIGGERS
-- =====================================================

-- Normalizar email (a minúsculas)
CREATE TRIGGER trg_normalizar_email
    BEFORE INSERT OR UPDATE ON usuarios
                         FOR EACH ROW
                         EXECUTE FUNCTION normalizar_email();

-- Actualizar campo updated_at
CREATE TRIGGER trg_actualizar_usuarios
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA CARRERAS
-- =====================================================
CREATE TABLE proyecto.carreras (
                                   id_carrera SERIAL PRIMARY KEY,
                                   nom_carrera VARCHAR(80) NOT NULL UNIQUE,  -- ampliado a 80 para nombres largos
                                   estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                   CONSTRAINT chk_nom_carrera_valido CHECK (TRIM(nom_carrera) <> ''),
                                   CONSTRAINT uk_nombre_carrera UNIQUE (nom_carrera)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_carreras
    BEFORE UPDATE ON carreras
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA ROLES
-- =====================================================
CREATE TABLE proyecto.roles (
                                id_rol SERIAL PRIMARY KEY,
                                nom_rol VARCHAR(50) NOT NULL UNIQUE,
                                rol_descripcion VARCHAR(100),
                                estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uk_nombre_rol UNIQUE (nom_rol)

);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_roles
    BEFORE UPDATE ON roles
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();


-- =====================================================
-- TABLA ITRs
-- =====================================================
CREATE TABLE proyecto.itrs (
                               id_itr SERIAL PRIMARY KEY,
                               nom_itr VARCHAR(80) NOT NULL UNIQUE,  -- ampliado a 80 para nombres largos
                               estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT uk_nom_itr UNIQUE (nom_itr)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_itrs
    BEFORE UPDATE ON itrs
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA PERMISOS
-- =====================================================
CREATE TABLE permisos (
                          id_permiso SERIAL PRIMARY KEY,
                          nom_permiso VARCHAR(50) NOT NULL UNIQUE,
                          per_descripcion VARCHAR(100) NOT NULL,
                          estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                          CONSTRAINT chk_per_descripcion_valida CHECK (TRIM(per_descripcion) <> ''),
                          CONSTRAINT uk_nom_permiso UNIQUE (nom_permiso)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_permisos
    BEFORE UPDATE ON permisos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA FRECUENCIAS
-- =====================================================
CREATE TABLE proyecto.frecuencias (
                                      id_frecuencia SERIAL PRIMARY KEY,
                                      descripcion VARCHAR(50) NOT NULL,
                                      tipo tipo_frecuencia NOT NULL,
                                      int_dias INT CHECK (int_dias >= 1 AND int_dias <= 31),
                                      dia_semana dia_semana,
                                      estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACION DE COHERENCIA
    -- ==============================================
                                      CONSTRAINT chk_coherencia_frecuencia CHECK (
                                          (tipo = 'diaria' AND int_dias IS NULL AND dia_semana IS NULL) OR
                                          (tipo = 'semanal' AND int_dias IS NULL AND dia_semana IS NOT NULL) OR
                                          (tipo = 'mensual' AND int_dias IS NOT NULL AND dia_semana IS NULL) OR
                                          (tipo = 'personalizada')
                                          )
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_frecuencias
    BEFORE UPDATE ON frecuencias
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA CAT_INSTANCIAS
-- =====================================================
CREATE TABLE cat_instancias (
                                id_categoria SERIAL PRIMARY KEY,
                                nom_cate_instancias VARCHAR(50) NOT NULL UNIQUE,
                                descripcion TEXT,
                                estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                CONSTRAINT chk_nom_categoria_valido CHECK (TRIM(nom_cate_instancias) <> ''),
                                CONSTRAINT uk_nom_cate_instancia UNIQUE (nom_cate_instancias)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_categorias
    BEFORE UPDATE ON cat_instancias
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();


-- =====================================================
-- TABLA CAT_RECORDATORIOS
-- =====================================================
CREATE TABLE proyecto.cat_recordatorios (
                                            id_categoria SERIAL PRIMARY KEY,
                                            nom_cate_recordatorios VARCHAR(50) NOT NULL UNIQUE,
                                            descripcion TEXT,
                                            estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                            CONSTRAINT chk_nom_categoria_valido CHECK (TRIM(nom_cate_recordatorios) <> ''),
                                            CONSTRAINT uk_nom_cate_recordatorio UNIQUE (nom_cate_recordatorios)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_cat_recordatorios
    BEFORE UPDATE ON cat_recordatorios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA: estudiantes
-- =====================================================

CREATE TABLE proyecto.estudiantes (
                                      id_usuario INT PRIMARY KEY,
                                      ciudad VARCHAR(50),
                                      departamento VARCHAR(20),
                                      calle VARCHAR(160),
                                      nro_puerta INT,
                                      inf_esta_salud TEXT,
                                      sis_salud VARCHAR(80),
                                      fec_nacimiento DATE NOT NULL,
                                      motivo TEXT,
                                      foto VARCHAR(255),

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- RESTRICCIONES DE NEGOCIO Y VALIDACIONES
    -- ==============================================

    -- Ciudad alfanumérica
                                      CONSTRAINT chk_ciudad_valida CHECK (
                                          ciudad IS NULL OR ciudad ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$'
),

    -- Departamento alfanumérico
    CONSTRAINT chk_departamento_valido CHECK (
        departamento IS NULL OR departamento ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,20}$'
    ),

    -- Calle alfanumérica (permite espacios)
    CONSTRAINT chk_calle_valida CHECK (
        calle IS NULL OR calle ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,160}$'
    ),

    -- Número de puerta entre 1 y 9999
    CONSTRAINT chk_nro_puerta CHECK (
        nro_puerta IS NULL OR (nro_puerta >= 1 AND nro_puerta <= 9999)
    ),

    -- Fecha de nacimiento: mayor de edad y menor de 100 años
    CONSTRAINT chk_edad_rango CHECK (
        fec_nacimiento <= CURRENT_DATE - INTERVAL '18 years'
        AND fec_nacimiento >= CURRENT_DATE - INTERVAL '100 years'
    ),

    -- Foto: solo formatos JPG, JPEG o PNG (case-insensitive)
    CONSTRAINT chk_foto_extension CHECK (
    foto IS NULL OR foto ~* '\.(jpg|jpeg|png)$'),

    -- Salud y sistema de salud validados
CONSTRAINT chk_sis_salud_valido CHECK (
    sis_salud IS NULL OR sis_salud ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,80}$'
),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================

CREATE TRIGGER trg_actualizar_estudiantes
    BEFORE UPDATE ON estudiantes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA FUNCIONARIOS
-- =====================================================
CREATE TABLE proyecto.funcionarios (
                                       id_usuario INT PRIMARY KEY,
                                       id_rol INT NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT fk_funcionario_roles FOREIGN KEY (id_rol)
                                           REFERENCES proyecto.roles(id_rol)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                       CONSTRAINT fk_funcionario_usuario FOREIGN KEY (id_usuario)
                                           REFERENCES proyecto.usuarios(id_usuario)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_funcionarios
    BEFORE UPDATE ON funcionarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA GRUPOS
-- =====================================================
CREATE TABLE proyecto.grupos (
                                 id_grupo SERIAL PRIMARY KEY,
                                 id_carrera INT NOT NULL,
                                 nom_grupo VARCHAR(60) NOT NULL,
                                 generacion INT NOT NULL,
                                 estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                 CONSTRAINT chk_nom_grupo_valido CHECK (TRIM(nom_grupo) <> ''),
                                 CONSTRAINT chk_generacion_valida CHECK (generacion > 1900 AND generacion <= EXTRACT(YEAR FROM CURRENT_DATE)),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                 CONSTRAINT fk_grupo_carrera FOREIGN KEY (id_carrera)
                                     REFERENCES proyecto.carreras(id_carrera)
                                     ON UPDATE CASCADE
                                     ON DELETE RESTRICT,

                                 CONSTRAINT uk_grupo_carrera_grupo_generacion UNIQUE (id_carrera, nom_grupo, generacion)
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_grupos
    BEFORE UPDATE ON grupos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA ITR_CARRERA
-- =====================================================
CREATE TABLE proyecto.itr_carrera (
                                      id_itr INT NOT NULL,
                                      id_carrera INT NOT NULL,
                                      estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (id_itr, id_carrera),
                                      CONSTRAINT fk_itr_carrera_itr FOREIGN KEY (id_itr)
                                          REFERENCES proyecto.itrs(id_itr)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT,
                                      CONSTRAINT fk_itr_carrera_carrera FOREIGN KEY (id_carrera)
                                          REFERENCES proyecto.carreras(id_carrera)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_itr_carrera
    BEFORE UPDATE ON itr_carrera
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA ROL_PERMISOS
-- =====================================================
CREATE TABLE proyecto.rol_permisos (
                                       id_rol INT NOT NULL,
                                       id_permiso INT NOT NULL,
                                       estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (id_rol, id_permiso),

                                       CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol)
                                           REFERENCES proyecto.roles(id_rol)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                       CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso)
                                           REFERENCES proyecto.permisos(id_permiso)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_rol_permisos
    BEFORE UPDATE ON rol_permisos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA INSTANCIAS
-- =====================================================
CREATE TABLE proyecto.instancias (
                                     cod_instancia SERIAL PRIMARY KEY,
                                     titulo VARCHAR(150) NOT NULL,
                                     fec_hora TIMESTAMP NOT NULL,
                                     id_estudiante INT NOT NULL,
                                     id_neg_instancia VARCHAR(16),
                                     id_funcionario INT NOT NULL,
                                     canal VARCHAR(50),
                                     estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- RESTRICCIONES
    -- ==============================================
                                     CONSTRAINT chk_titulo_valido CHECK (TRIM(titulo) <> ''),
                                     CONSTRAINT chk_id_neg_instancia_valido CHECK (id_neg_instancia ~ '^[A-Za-z0-9-]{1,20}$'),
    CONSTRAINT uk_id_neg_instancia UNIQUE (id_neg_instancia),

    CONSTRAINT fk_instancia_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_instancia_funcionario FOREIGN KEY (id_funcionario)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_instancias
    BEFORE UPDATE ON instancias
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA INCIDENCIAS
-- =====================================================
CREATE TABLE proyecto.incidencias (
                                      cod_instancia INT PRIMARY KEY,
                                      lugar VARCHAR(50) NOT NULL,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                      CONSTRAINT chk_lugar_valido CHECK (TRIM(lugar) <> ''),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                      CONSTRAINT fk_incidencia_instancia FOREIGN KEY (cod_instancia)
                                          REFERENCES proyecto.instancias(cod_instancia)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_incidencias
    BEFORE UPDATE ON incidencias
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA RECORDATORIOS
-- =====================================================
CREATE TABLE proyecto.recordatorios (
                                        cod_recordatorio SERIAL PRIMARY KEY,
                                        id_categoria INT NOT NULL, -- NUEVO CAMPO
                                        id_neg_recordatorio VARCHAR(16),
                                        titulo VARCHAR(150) NOT NULL,
                                        tipo tipo_recordatorio NOT NULL,
                                        fec_hora TIMESTAMP NOT NULL,
                                        cod_instancia INT NOT NULL,
                                        id_frecuencia INT,
                                        fec_fin DATE,
                                        nro_ocurrencias INT CHECK (nro_ocurrencias BETWEEN 0 AND 5),
                                        responsable INT NOT NULL,
                                        estado_operativo estado_operativo NOT NULL DEFAULT 'FUTURO',
                                        estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                        CONSTRAINT chk_titulo_valido CHECK (TRIM(titulo) <> ''),
                                        CONSTRAINT chk_fec_hora_futura CHECK (fec_hora > CURRENT_TIMESTAMP),
                                        CONSTRAINT chk_recurrencia_fec_fin CHECK (
                                            (id_frecuencia IS NOT NULL AND (fec_fin IS NOT NULL OR nro_ocurrencias IS NOT NULL))
                                                OR id_frecuencia IS NULL
                                            ),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                        CONSTRAINT fk_recordatorio_categoria FOREIGN KEY (id_categoria)
                                            REFERENCES proyecto.cat_recordatorios(id_categoria)
                                            ON UPDATE CASCADE
                                            ON DELETE RESTRICT,

                                        CONSTRAINT fk_recordatorio_instancia FOREIGN KEY (cod_instancia)
                                            REFERENCES proyecto.instancias(cod_instancia)
                                            ON UPDATE CASCADE
                                            ON DELETE RESTRICT,

                                        CONSTRAINT fk_recordatorio_frecuencia FOREIGN KEY (id_frecuencia)
                                            REFERENCES proyecto.frecuencias(id_frecuencia)
                                            ON UPDATE CASCADE
                                            ON DELETE RESTRICT,

                                        CONSTRAINT fk_recordatorio_responsable FOREIGN KEY (responsable)
                                            REFERENCES proyecto.funcionarios(id_usuario)
                                            ON UPDATE CASCADE
                                            ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGERS
-- =====================================================
CREATE TRIGGER trg_actualizar_recordatorios
    BEFORE UPDATE ON recordatorios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

CREATE TRIGGER trg_actualizar_estado_recordatorio
    BEFORE INSERT OR UPDATE ON recordatorios
                         FOR EACH ROW
                         EXECUTE FUNCTION actualizar_estado_recordatorio();

-- =====================================================
-- TABLA ESTU_GRUPOS
-- =====================================================
CREATE TABLE proyecto.estu_grupos (
                                      id_usuario INT NOT NULL,
                                      id_grupo INT NOT NULL,
                                      estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (id_usuario, id_grupo),
                                      CONSTRAINT fk_estu_grupo_estudiante FOREIGN KEY (id_usuario)
                                          REFERENCES proyecto.estudiantes(id_usuario)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT,
                                      CONSTRAINT fk_estu_grupo_grupo FOREIGN KEY (id_grupo)
                                          REFERENCES proyecto.grupos(id_grupo)
                                          ON UPDATE CASCADE
                                          ON DELETE RESTRICT
);

-- Trigger para actualizar updated_at automáticamente
CREATE TRIGGER trg_actualizar_estu_grupos
    BEFORE UPDATE ON estu_grupos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA: estu_informesadjs
-- =====================================================

CREATE TABLE proyecto.estu_informesadjs (
                                            id_informe SERIAL PRIMARY KEY,
                                            id_usuario INT NOT NULL,

                                            url_archivo VARCHAR(255) NOT NULL,
                                            ruta VARCHAR(255),
                                            nom_archivo VARCHAR(70) NOT NULL,  -- antes era 30 → ahora 70

                                            estado estado_logico NOT NULL DEFAULT 'ACTIVO',

                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- RESTRICCIONES DE NEGOCIO Y VALIDACIONES
    -- ==============================================

    -- URL más permisiva (alfanumérico + caracteres comunes de URLs)
                                            CONSTRAINT chk_url_archivo_valida CHECK (
                                                url_archivo ~ '^[A-Za-z0-9_./:%?&=-]{1,255}$'
),
    CONSTRAINT chk_ruta_valida CHECK (
        ruta IS NULL OR ruta ~ '^[A-Za-z0-9_./:-]{1,255}$'
    ),

    -- Validar nombre del archivo alfanumérico y sin espacios vacíos
    CONSTRAINT chk_nombre_valido CHECK (
        TRIM(nom_archivo ) <> '' AND nom_archivo  ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 _-]{1,50}$'
    ),

    -- Limitar formato del archivo adjunto a PDF, JPG, JPEG o PNG (case-insensitive)
    CONSTRAINT chk_extension_valida CHECK (
        url_archivo ~* '\.(pdf|jpg|jpeg|png)$'
    ),

    -- ==============================================
    -- RELACIONES
    -- ==============================================

    CONSTRAINT fk_informe_estudiante FOREIGN KEY (id_usuario)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT uk_informe_nombre UNIQUE (id_usuario, nom_archivo)
);

-- =====================================================
-- TRIGGER
-- =====================================================

CREATE TRIGGER trg_actualizar_informes
    BEFORE UPDATE ON estu_informesadjs
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA ESTU_TELEFONOS
-- =====================================================
CREATE TABLE proyecto.estu_telefonos (
                                         id_usuario INT NOT NULL,
                                         telefono dom_telefono NOT NULL,
                                         estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         PRIMARY KEY (id_usuario, telefono),
                                         CONSTRAINT fk_telefono_estudiante FOREIGN KEY (id_usuario)
                                             REFERENCES proyecto.estudiantes(id_usuario)
                                             ON UPDATE CASCADE
                                             ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estu_telefonos
    BEFORE UPDATE ON estu_telefonos
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA ESTU_OBS_COMENTARIOS
-- =====================================================
CREATE TABLE proyecto.estu_obs_comentarios (
                                               id_usuario INT NOT NULL,
                                               id_comentario SERIAL NOT NULL,
                                               comentario TEXT NOT NULL,
                                               estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               id_autor INT NOT NULL,

                                               PRIMARY KEY (id_comentario, id_usuario),

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                               CONSTRAINT chk_comentario_no_vacio CHECK (TRIM(comentario) <> ''),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                               CONSTRAINT fk_estu_obs_comentarios_estudiante FOREIGN KEY (id_usuario)
                                                   REFERENCES proyecto.estudiantes(id_usuario)
                                                   ON UPDATE CASCADE
                                                   ON DELETE restrict,
                                               CONSTRAINT fk_estu_obs_comentarios_autor
                                                   FOREIGN KEY (id_autor)
                                                       REFERENCES proyecto.funcionarios(id_usuario)
                                                       ON UPDATE CASCADE
                                                       ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_estu_obs_comentarios
    BEFORE UPDATE ON estu_obs_comentarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();


-- =====================================================
-- TABLA ESTU_OBS_CONFIDENCIALES
-- =====================================================
CREATE TABLE proyecto.estu_obs_confidenciales (
                                                  id_estudiante INT NOT NULL,
                                                  id_obs_confidencial SERIAL NOT NULL,
                                                  obs_confidencial TEXT NOT NULL,
                                                  estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  id_autor INT NOT null,

                                                  PRIMARY KEY (id_obs_confidencial, id_estudiante),

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                                  CONSTRAINT chk_obs_confidencial_no_vacio CHECK (TRIM(obs_confidencial) <> ''),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                                  CONSTRAINT fk_estu_obs_confidenciales_estudiante FOREIGN KEY (id_estudiante)
                                                      REFERENCES proyecto.estudiantes(id_usuario)
                                                      ON UPDATE CASCADE
                                                      ON DELETE cascade,
                                                  CONSTRAINT fk_estu_obs_confidenciales_autor
                                                      FOREIGN KEY (id_autor)
                                                          REFERENCES proyecto.funcionarios(id_usuario)
                                                          ON UPDATE CASCADE
                                                          ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_estu_obs_confidenciales
    BEFORE UPDATE ON estu_obs_confidenciales
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();

-- =====================================================
-- TABLA INVOLUCRADOS
-- =====================================================
CREATE TABLE proyecto.involucrados (
                                       cod_instancia INT NOT NULL,
                                       nom_involucrado VARCHAR(30) NOT NULL,
                                       estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       PRIMARY KEY (cod_instancia, nom_involucrado),

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                       CONSTRAINT chk_nom_involucrado_valido CHECK (TRIM(nom_involucrado) <> ''),

                                       CONSTRAINT fk_involucrado_instancia FOREIGN KEY (cod_instancia)
                                           REFERENCES proyecto.instancias(cod_instancia)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_involucrados
    BEFORE UPDATE ON involucrados
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();



-- =====================================================
-- TABLA INST_COMUNES
-- =====================================================
CREATE TABLE proyecto.inst_comunes (
                                       cod_instancia INT NOT NULL,
                                       id_categoria INT NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                       PRIMARY KEY (cod_instancia, id_categoria),

                                       CONSTRAINT fk_comunes_instancia FOREIGN KEY (cod_instancia)
                                           REFERENCES proyecto.instancias(cod_instancia)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT,

                                       CONSTRAINT fk_comunes_categoria FOREIGN KEY (id_categoria)
                                           REFERENCES proyecto.cat_instancias(id_categoria)
                                           ON UPDATE CASCADE
                                           ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_inst_comunes
    BEFORE UPDATE ON inst_comunes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();


-- =====================================================
-- TABLA INST_COMENTARIOS
-- =====================================================
CREATE TABLE proyecto.inst_comentarios (
                                           id_comentario SERIAL PRIMARY KEY,
                                           cod_instancia INT NOT NULL,
                                           comentario TEXT NOT NULL,
                                           estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           id_autor INT not null ,

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                           CONSTRAINT chk_comentario_valido CHECK (TRIM(comentario) <> ''),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                           CONSTRAINT fk_comentario_instancia FOREIGN KEY (cod_instancia)
                                               REFERENCES proyecto.instancias(cod_instancia)
                                               ON UPDATE CASCADE
                                               ON DELETE restrict,
                                           CONSTRAINT fk_id_autor FOREIGN KEY (id_autor)
                                               REFERENCES proyecto.funcionarios(id_usuario)
                                               ON UPDATE CASCADE
                                               ON DELETE CASCADE
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_ins_comentarios
    BEFORE UPDATE ON inst_comentarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();


-- =====================================================
-- TABLA INST_COM_CONFIDENCIALES
-- =====================================================
CREATE TABLE proyecto.inst_com_confidenciales (
                                                  cod_instancia INT NOT NULL,
                                                  id_com_confidencial SERIAL NOT NULL,
                                                  comentario TEXT NOT NULL,
                                                  estado estado_logico NOT NULL DEFAULT 'ACTIVO',
                                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                  PRIMARY KEY (id_com_confidencial, cod_instancia),

    -- ==============================================
    -- VALIDACIONES
    -- ==============================================
                                                  CONSTRAINT chk_comentario_valido CHECK (TRIM(comentario) <> ''),

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                                  CONSTRAINT fk_inst_com_confidencial_instancia FOREIGN KEY (cod_instancia)
                                                      REFERENCES proyecto.instancias(cod_instancia)
                                                      ON UPDATE CASCADE
                                                      ON DELETE RESTRICT
);

-- =====================================================
-- TRIGGER
-- =====================================================
CREATE TRIGGER trg_actualizar_inst_com_confidenciales
    BEFORE UPDATE ON inst_com_confidenciales
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_updated_at();



-- =====================================================
-- TABLA LOG_INTENTOS
-- =====================================================
CREATE TABLE proyecto.log_intentos (
                                       id_login SERIAL PRIMARY KEY,
                                       id_usuario INT,
                                       fue_exitoso BOOLEAN NOT NULL,
                                       fec_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                       CONSTRAINT fk_log_intento_usuario FOREIGN KEY (id_usuario)
                                           REFERENCES proyecto.usuarios(id_usuario)
                                           ON UPDATE CASCADE
                                           ON DELETE SET NULL
);

-- ==============================================
-- INDICES
-- ==============================================
-- Índice para consultas rápidas de intentos recientes por usuario
CREATE INDEX idx_login_intentos_usuario_fecha ON proyecto.log_intentos(id_usuario, fec_hora DESC);


-- =====================================================
-- TABLA AUDITORIA
-- =====================================================
CREATE TABLE proyecto.auditoria (
                                    id_auditoria SERIAL PRIMARY KEY,
                                    entidad VARCHAR(100) NOT NULL,           -- nombre de la tabla o entidad afectada
                                    accion tipo_accion NOT NULL,             -- enum previamente creado
                                    id_usuario INT NOT NULL,                 -- FK al usuario que realiza la acción
                                    fec_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    detalle TEXT,                            -- información adicional o before/after

    -- ==============================================
    -- RELACIONES
    -- ==============================================
                                    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
                                        REFERENCES proyecto.usuarios(id_usuario)
                                        ON UPDATE CASCADE
                                        ON DELETE CASCADE
);

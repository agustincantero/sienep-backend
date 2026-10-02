CREATE SCHEMA IF NOT EXISTS proyecto;
SET search_path TO proyecto;

-- =====================================================
-- TIPOS ENUM
-- =====================================================
CREATE TYPE proyecto.estado_logico AS ENUM (
    'ACTIVO', 'INACTIVO', 'ELIMINADO', 'PENDIENTE_DE_ACTIVACION'
);

CREATE TYPE proyecto.estado_operativo AS ENUM ('HOY', 'FUTURO', 'VENCIDO');
CREATE TYPE proyecto.tipo_accion AS ENUM ('INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT');
CREATE TYPE proyecto.tipo_frecuencia AS ENUM ('DIARIA', 'SEMANAL', 'MENSUAL', 'PERSONALIZADA');
CREATE TYPE proyecto.dia_semana AS ENUM ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO');
CREATE TYPE proyecto.tipo_recordatorio AS ENUM ('LLAMADA', 'REUNION', 'TAREA_ADMINISTRATIVA', 'OTRO');

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
CREATE DOMAIN proyecto.dom_email AS VARCHAR(50)
    CHECK (VALUE ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$');

CREATE DOMAIN proyecto.dom_telefono AS VARCHAR(15)
    CHECK (VALUE ~ '^[0-9]{8,12}$');

-- =====================================================
-- FUNCIONES (todas con prefijo proyecto.)
-- =====================================================
CREATE OR REPLACE FUNCTION proyecto.actualizar_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION proyecto.normalizar_email()
RETURNS TRIGGER AS $$
BEGIN
    NEW.email := LOWER(NEW.email);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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
    id_usuario     SERIAL                   PRIMARY KEY,
    nombre         VARCHAR(50)              NOT NULL,
    apellido       VARCHAR(50)              NOT NULL,
    documento      VARCHAR(15)              NOT NULL,
    pais_documento proyecto.pais_documento  NOT NULL DEFAULT 'URY',
    email          proyecto.dom_email       NOT NULL,
    contrasenia    VARCHAR(255)             NOT NULL,
    estado         proyecto.estado_logico   NOT NULL DEFAULT 'PENDIENTE_DE_ACTIVACION',
    bloqueado      BOOLEAN                  NOT NULL DEFAULT FALSE,
    credenciales_vigentes_desde TIMESTAMP,
    created_at     TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_nombre_apellido_not_empty
        CHECK (TRIM(nombre) <> '' AND TRIM(apellido) <> ''),
    CONSTRAINT chk_nombre_apellido_valido CHECK (
        nombre   ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$' AND
        apellido ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$'
    ),
    CONSTRAINT chk_documento_valido CHECK (
        (pais_documento = 'URY' AND documento ~ '^[0-9]{6,8}$') OR
        (pais_documento <> 'URY' AND documento ~ '^[A-Za-z0-9]{1,15}$')
    ),
    CONSTRAINT uk_documento_pais UNIQUE (documento, pais_documento),
    CONSTRAINT uk_email          UNIQUE (email)
);

CREATE TRIGGER trg_normalizar_email
    BEFORE INSERT OR UPDATE ON proyecto.usuarios
    FOR EACH ROW EXECUTE FUNCTION proyecto.normalizar_email();

CREATE TRIGGER trg_actualizar_usuarios
    BEFORE UPDATE ON proyecto.usuarios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: carreras
-- =====================================================
CREATE TABLE proyecto.carreras (
    id_carrera  SERIAL                 PRIMARY KEY,
    nom_carrera VARCHAR(80)            NOT NULL,
    estado      proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at  TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nom_carrera_valido CHECK (TRIM(nom_carrera) <> ''),
    CONSTRAINT uk_nombre_carrera UNIQUE (nom_carrera)
);

CREATE TRIGGER trg_actualizar_carreras
    BEFORE UPDATE ON proyecto.carreras
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: roles
-- =====================================================
CREATE TABLE proyecto.roles (
    id_rol          SERIAL                 PRIMARY KEY,
    nom_rol         VARCHAR(50)            NOT NULL,
    rol_descripcion VARCHAR(100),
    estado          proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_nombre_rol UNIQUE (nom_rol)
);

CREATE TRIGGER trg_actualizar_roles
    BEFORE UPDATE ON proyecto.roles
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: itrs
-- =====================================================
CREATE TABLE proyecto.itrs (
    id_itr     SERIAL                 PRIMARY KEY,
    nom_itr    VARCHAR(80)            NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_nom_itr UNIQUE (nom_itr)
);

CREATE TRIGGER trg_actualizar_itrs
    BEFORE UPDATE ON proyecto.itrs
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: permisos  ← CORREGIDO: antes sin prefijo proyecto.
-- =====================================================
CREATE TABLE proyecto.permisos (
    id_permiso      SERIAL                 PRIMARY KEY,
    nom_permiso     VARCHAR(50)            NOT NULL,
    per_descripcion VARCHAR(100)           NOT NULL,
    estado          proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_per_descripcion_valida CHECK (TRIM(per_descripcion) <> ''),
    CONSTRAINT uk_nom_permiso UNIQUE (nom_permiso)
);

CREATE TRIGGER trg_actualizar_permisos
    BEFORE UPDATE ON proyecto.permisos
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: frecuencias
-- =====================================================
CREATE TABLE proyecto.frecuencias (
    id_frecuencia SERIAL                    PRIMARY KEY,
    descripcion   VARCHAR(50)               NOT NULL,
    tipo          proyecto.tipo_frecuencia  NOT NULL,
    int_dias      INT CHECK (int_dias >= 1 AND int_dias <= 31),
    dia_semana    proyecto.dia_semana,
    estado        proyecto.estado_logico    NOT NULL DEFAULT 'ACTIVO',
    created_at    TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_coherencia_frecuencia CHECK (
        (tipo = 'DIARIA'       AND int_dias IS NULL     AND dia_semana IS NULL) OR
        (tipo = 'SEMANAL'      AND int_dias IS NULL     AND dia_semana IS NOT NULL) OR
        (tipo = 'MENSUAL'      AND int_dias IS NOT NULL AND dia_semana IS NULL) OR
        (tipo = 'PERSONALIZADA')
    )
);

CREATE TRIGGER trg_actualizar_frecuencias
    BEFORE UPDATE ON proyecto.frecuencias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: cat_instancias  ← CORREGIDO: antes sin prefijo proyecto.
-- =====================================================
CREATE TABLE proyecto.cat_instancias (
    id_categoria        SERIAL                 PRIMARY KEY,
    nom_cate_instancias VARCHAR(50)            NOT NULL,
    descripcion         TEXT,
    estado              proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nom_categoria_valido CHECK (TRIM(nom_cate_instancias) <> ''),
    CONSTRAINT uk_nom_cate_instancia    UNIQUE (nom_cate_instancias)
);

CREATE TRIGGER trg_actualizar_categorias
    BEFORE UPDATE ON proyecto.cat_instancias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: cat_recordatorios
-- =====================================================
CREATE TABLE proyecto.cat_recordatorios (
    id_categoria           SERIAL                 PRIMARY KEY,
    nom_cate_recordatorios VARCHAR(50)            NOT NULL,
    descripcion            TEXT,
    estado                 proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at             TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nom_cat_rec_valido   CHECK (TRIM(nom_cate_recordatorios) <> ''),
    CONSTRAINT uk_nom_cate_recordatorio UNIQUE (nom_cate_recordatorios)
);

CREATE TRIGGER trg_actualizar_cat_recordatorios
    BEFORE UPDATE ON proyecto.cat_recordatorios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: estudiantes
-- =====================================================
CREATE TABLE proyecto.estudiantes (
    id_usuario     INT       PRIMARY KEY,
    id_rol         INT       NOT NULL,
    ciudad         VARCHAR(50),
    departamento   VARCHAR(20),
    calle          VARCHAR(160),
    nro_puerta     INT,
    inf_esta_salud TEXT,
    sis_salud      VARCHAR(80),
    fec_nacimiento DATE      NOT NULL,
    motivo         TEXT,
    foto           VARCHAR(255),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ciudad_valida CHECK (
        ciudad IS NULL OR ciudad ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,50}$'
    ),
    CONSTRAINT chk_departamento_valido CHECK (
        departamento IS NULL OR departamento ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,20}$'
    ),
    CONSTRAINT chk_calle_valida CHECK (
        calle IS NULL OR calle ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,160}$'
    ),
    CONSTRAINT chk_nro_puerta CHECK (
        nro_puerta IS NULL OR (nro_puerta >= 1 AND nro_puerta <= 9999)
    ),
    CONSTRAINT chk_edad_rango CHECK (
        fec_nacimiento <= CURRENT_DATE - INTERVAL '18 years' AND
        fec_nacimiento >= CURRENT_DATE - INTERVAL '100 years'
    ),
    CONSTRAINT chk_foto_extension CHECK (
        foto IS NULL OR foto ~* '\.(jpg|jpeg|png)$'
    ),
    CONSTRAINT chk_sis_salud_valido CHECK (
        sis_salud IS NULL OR sis_salud ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 ]{1,80}$'
    ),
    CONSTRAINT fk_estudiante_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_estudiante_roles FOREIGN KEY (id_rol)
        REFERENCES proyecto.roles(id_rol)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estudiantes
    BEFORE UPDATE ON proyecto.estudiantes
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: funcionarios
-- =====================================================
CREATE TABLE proyecto.funcionarios (
    id_usuario INT       PRIMARY KEY,
    id_rol     INT       NOT NULL,
    foto       VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Mismo criterio que estudiantes.foto (chk_foto_extension).
    CONSTRAINT chk_funcionario_foto_extension CHECK (
        foto IS NULL OR foto ~* '\.(jpg|jpeg|png)$'
    ),
    CONSTRAINT fk_funcionario_roles FOREIGN KEY (id_rol)
        REFERENCES proyecto.roles(id_rol)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_funcionario_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_funcionarios
    BEFORE UPDATE ON proyecto.funcionarios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: grupos
-- =====================================================
CREATE TABLE proyecto.grupos (
    id_grupo   SERIAL                 PRIMARY KEY,
    id_carrera INT                    NOT NULL,
    nom_grupo  VARCHAR(60)            NOT NULL,
    generacion INT                    NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_nom_grupo_valido  CHECK (TRIM(nom_grupo) <> ''),
    CONSTRAINT chk_generacion_valida CHECK (
        generacion > 1900 AND generacion <= EXTRACT(YEAR FROM CURRENT_DATE)
    ),
    CONSTRAINT fk_grupo_carrera FOREIGN KEY (id_carrera)
        REFERENCES proyecto.carreras(id_carrera)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uk_grupo_carrera_grupo_generacion UNIQUE (id_carrera, nom_grupo, generacion)
);

CREATE TRIGGER trg_actualizar_grupos
    BEFORE UPDATE ON proyecto.grupos
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

CREATE INDEX idx_grupos_carrera ON proyecto.grupos(id_carrera);

-- =====================================================
-- TABLA: itr_carrera
-- =====================================================
CREATE TABLE proyecto.itr_carrera (
    id_itr     INT                    NOT NULL,
    id_carrera INT                    NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_itr, id_carrera),
    CONSTRAINT fk_itr_carrera_itr FOREIGN KEY (id_itr)
        REFERENCES proyecto.itrs(id_itr)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_itr_carrera_carrera FOREIGN KEY (id_carrera)
        REFERENCES proyecto.carreras(id_carrera)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_itr_carrera
    BEFORE UPDATE ON proyecto.itr_carrera
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: rol_permisos
-- =====================================================
CREATE TABLE proyecto.rol_permisos (
    id_rol     INT                    NOT NULL,
    id_permiso INT                    NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol)
        REFERENCES proyecto.roles(id_rol)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso)
        REFERENCES proyecto.permisos(id_permiso)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_rol_permisos
    BEFORE UPDATE ON proyecto.rol_permisos
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: instancias
-- =====================================================

-- Contador anual para InstanciaIdGenerator/RecordatorioIdGenerator: reemplaza
-- a las viejas SEQUENCE globales seq_instancia/seq_recordatorio, que no
-- reseteaban por año pese a que el formato del id (INST-AAAA-NNNNNN) aparenta
-- un contador anual. Con este contador particionado por (tipo, año), cada año
-- arranca en 1 de verdad — ver Javadoc de InstanciaIdGenerator.
CREATE TABLE proyecto.contador_id_negocio (
    tipo         VARCHAR(20) NOT NULL,
    anio         INT         NOT NULL,
    ultimo_valor INT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_contador_id_negocio PRIMARY KEY (tipo, anio)
);

CREATE TABLE proyecto.instancias (
    cod_instancia    SERIAL                 PRIMARY KEY,
    titulo           VARCHAR(150)           NOT NULL,
    fec_hora         TIMESTAMP              NOT NULL,
    id_estudiante    INT                    NOT NULL,
    id_neg_instancia VARCHAR(16),
    id_funcionario   INT                    NOT NULL,
    canal            VARCHAR(50),
    estado           proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at       TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_titulo_valido CHECK (TRIM(titulo) <> ''),
    CONSTRAINT chk_id_neg_instancia_valido
        CHECK (id_neg_instancia IS NULL OR id_neg_instancia ~ '^[A-Za-z0-9-]{1,20}$'),
    CONSTRAINT uk_id_neg_instancia UNIQUE (id_neg_instancia),
    CONSTRAINT fk_instancia_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_instancia_funcionario FOREIGN KEY (id_funcionario)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_instancias
    BEFORE UPDATE ON proyecto.instancias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

CREATE INDEX idx_instancias_estudiante  ON proyecto.instancias(id_estudiante);
CREATE INDEX idx_instancias_funcionario ON proyecto.instancias(id_funcionario);

-- =====================================================
-- TABLA: incidencias
-- =====================================================
CREATE TABLE proyecto.incidencias (
    cod_instancia INT         PRIMARY KEY,
    lugar         VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_lugar_valido CHECK (TRIM(lugar) <> ''),
    CONSTRAINT fk_incidencia_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_incidencias
    BEFORE UPDATE ON proyecto.incidencias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: recordatorios
-- =====================================================
CREATE TABLE proyecto.recordatorios (
    cod_recordatorio      SERIAL                     PRIMARY KEY,
    id_categoria          INT                        NOT NULL,
    id_neg_recordatorio   VARCHAR(16),
    titulo                VARCHAR(150)               NOT NULL,
    tipo                  proyecto.tipo_recordatorio NOT NULL,
    fec_hora              TIMESTAMP                  NOT NULL,
    cod_instancia         INT                        NOT NULL,
    id_frecuencia         INT,
    fec_fin               DATE,
    nro_ocurrencias       INT CHECK (nro_ocurrencias BETWEEN 1 AND 5),
    responsable           INT                        NOT NULL,
    estado_operativo      proyecto.estado_operativo  NOT NULL DEFAULT 'FUTURO',
    estado                proyecto.estado_logico     NOT NULL DEFAULT 'ACTIVO',
    created_at            TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    event_id_google       VARCHAR(1024),
    notificado_proximidad BOOLEAN                    NOT NULL DEFAULT FALSE,
    cumplido              BOOLEAN                    NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_rec_titulo_valido   CHECK (TRIM(titulo) <> ''),
    CONSTRAINT chk_fec_hora_futura     CHECK (fec_hora > CURRENT_TIMESTAMP),
    CONSTRAINT chk_recurrencia_fec_fin CHECK (
        (id_frecuencia IS NOT NULL AND (fec_fin IS NOT NULL OR nro_ocurrencias IS NOT NULL))
            OR id_frecuencia IS NULL
    ),
    CONSTRAINT fk_recordatorio_categoria FOREIGN KEY (id_categoria)
        REFERENCES proyecto.cat_recordatorios(id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_recordatorio_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_recordatorio_frecuencia FOREIGN KEY (id_frecuencia)
        REFERENCES proyecto.frecuencias(id_frecuencia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_recordatorio_responsable FOREIGN KEY (responsable)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_recordatorios
    BEFORE UPDATE ON proyecto.recordatorios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

CREATE TRIGGER trg_actualizar_estado_recordatorio
    BEFORE INSERT OR UPDATE ON proyecto.recordatorios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_estado_recordatorio();

CREATE INDEX idx_recordatorios_responsable ON proyecto.recordatorios(responsable);
CREATE INDEX idx_recordatorios_instancia   ON proyecto.recordatorios(cod_instancia);
CREATE INDEX idx_recordatorios_categoria   ON proyecto.recordatorios(id_categoria);

-- =====================================================
-- TABLA: estu_grupos
-- =====================================================
CREATE TABLE proyecto.estu_grupos (
    id_usuario INT                    NOT NULL,
    id_grupo   INT                    NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, id_grupo),
    CONSTRAINT fk_estu_grupo_estudiante FOREIGN KEY (id_usuario)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_estu_grupo_grupo FOREIGN KEY (id_grupo)
        REFERENCES proyecto.grupos(id_grupo)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estu_grupos
    BEFORE UPDATE ON proyecto.estu_grupos
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- La PK (id_usuario, id_grupo) ya cubre "grupos de un estudiante"; este índice
-- cubre la dirección inversa, "estudiantes de un grupo" (filtro ?grupo=).
CREATE INDEX idx_estu_grupos_grupo ON proyecto.estu_grupos(id_grupo);

-- =====================================================
-- TABLA: estu_informesadjs
-- =====================================================
CREATE TABLE proyecto.estu_informesadjs (
    id_informe  SERIAL                 PRIMARY KEY,
    id_usuario  INT                    NOT NULL,
    url_archivo VARCHAR(255)           NOT NULL,
    ruta        VARCHAR(255),
    nom_archivo VARCHAR(70)            NOT NULL,
    estado      proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at  TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_url_archivo_valida CHECK (
        url_archivo ~ '^[A-Za-z0-9_./:%?&=-]{1,255}$'
    ),
    CONSTRAINT chk_ruta_valida CHECK (
        ruta IS NULL OR ruta ~ '^[A-Za-z0-9_./:-]{1,255}$'
    ),
    CONSTRAINT chk_nombre_valido CHECK (
        TRIM(nom_archivo) <> '' AND
        nom_archivo ~ '^[A-Za-zÁÉÍÓÚáéíóúÑñÜü0-9 _-]{1,50}$'
    ),
    CONSTRAINT chk_extension_valida CHECK (
        url_archivo ~* '\.(pdf|jpg|jpeg|png)$'
    ),
    CONSTRAINT fk_informe_estudiante FOREIGN KEY (id_usuario)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uk_informe_nombre UNIQUE (id_usuario, nom_archivo)
);

CREATE TRIGGER trg_actualizar_informes
    BEFORE UPDATE ON proyecto.estu_informesadjs
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: estu_telefonos
-- =====================================================
CREATE TABLE proyecto.estu_telefonos (
    id_usuario INT                    NOT NULL,
    telefono   proyecto.dom_telefono  NOT NULL,
    estado     proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, telefono),
    CONSTRAINT fk_telefono_estudiante FOREIGN KEY (id_usuario)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estu_telefonos
    BEFORE UPDATE ON proyecto.estu_telefonos
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: estu_obs_comentarios
-- =====================================================
CREATE TABLE proyecto.estu_obs_comentarios (
    id_usuario    INT                    NOT NULL,
    id_comentario SERIAL                 NOT NULL,
    comentario    TEXT                   NOT NULL,
    estado        proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at    TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_autor      INT                    NOT NULL,
    PRIMARY KEY (id_comentario, id_usuario),
    CONSTRAINT chk_comentario_no_vacio CHECK (TRIM(comentario) <> ''),
    CONSTRAINT fk_estu_obs_comentarios_estudiante FOREIGN KEY (id_usuario)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_estu_obs_comentarios_autor FOREIGN KEY (id_autor)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estu_obs_comentarios
    BEFORE UPDATE ON proyecto.estu_obs_comentarios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- La PK (id_comentario, id_usuario) no cubre "comentarios de este estudiante"
-- (id_usuario no es la columna líder) — esta es la consulta real más usada.
CREATE INDEX idx_estu_obs_comentarios_usuario ON proyecto.estu_obs_comentarios(id_usuario);

-- =====================================================
-- TABLA: estu_obs_confidenciales
-- =====================================================
CREATE TABLE proyecto.estu_obs_confidenciales (
    id_estudiante       INT                    NOT NULL,
    id_obs_confidencial SERIAL                 NOT NULL,
    obs_confidencial    TEXT                   NOT NULL,
    estado              proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_autor            INT                    NOT NULL,
    PRIMARY KEY (id_obs_confidencial, id_estudiante),
    CONSTRAINT chk_obs_confidencial_no_vacio CHECK (TRIM(obs_confidencial) <> ''),
    CONSTRAINT fk_estu_obs_confidenciales_estudiante FOREIGN KEY (id_estudiante)
        REFERENCES proyecto.estudiantes(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_estu_obs_confidenciales_autor FOREIGN KEY (id_autor)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_estu_obs_confidenciales
    BEFORE UPDATE ON proyecto.estu_obs_confidenciales
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- Mismo motivo que estu_obs_comentarios: la PK (id_obs_confidencial, id_estudiante)
-- no cubre "comentarios confidenciales de este estudiante".
CREATE INDEX idx_estu_obs_confidenciales_estudiante ON proyecto.estu_obs_confidenciales(id_estudiante);

-- =====================================================
-- TABLA: involucrados
-- =====================================================
CREATE TABLE proyecto.involucrados (
    cod_instancia   INT                    NOT NULL,
    nom_involucrado VARCHAR(30)            NOT NULL,
    estado          proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cod_instancia, nom_involucrado),
    CONSTRAINT chk_nom_involucrado_valido CHECK (TRIM(nom_involucrado) <> ''),
    CONSTRAINT fk_involucrado_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_involucrados
    BEFORE UPDATE ON proyecto.involucrados
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: inst_comunes
-- =====================================================
CREATE TABLE proyecto.inst_comunes (
    cod_instancia   INT           NOT NULL,
    id_categoria    INT           NOT NULL,
    event_id_google VARCHAR(1024),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cod_instancia),
    CONSTRAINT fk_inst_comun_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_inst_comun_categoria FOREIGN KEY (id_categoria)
        REFERENCES proyecto.cat_instancias(id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_inst_comunes
    BEFORE UPDATE ON proyecto.inst_comunes
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

CREATE INDEX idx_inst_comunes_categoria ON proyecto.inst_comunes(id_categoria);

-- =====================================================
-- TABLA: inst_comentarios
-- =====================================================
CREATE TABLE proyecto.inst_comentarios (
    id_comentario SERIAL                 PRIMARY KEY,
    cod_instancia INT                    NOT NULL,
    comentario    TEXT                   NOT NULL,
    estado        proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at    TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_autor      INT                    NOT NULL,
    CONSTRAINT chk_inst_comentario_valido CHECK (TRIM(comentario) <> ''),
    CONSTRAINT fk_comentario_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_inst_id_autor FOREIGN KEY (id_autor)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TRIGGER trg_actualizar_ins_comentarios
    BEFORE UPDATE ON proyecto.inst_comentarios
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

CREATE INDEX idx_inst_comentarios_instancia ON proyecto.inst_comentarios(cod_instancia);

-- =====================================================
-- TABLA: inst_com_confidenciales
-- =====================================================
CREATE TABLE proyecto.inst_com_confidenciales (
    cod_instancia       INT                    NOT NULL,
    id_com_confidencial SERIAL                 NOT NULL,
    comentario          TEXT                   NOT NULL,
    estado              proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
    created_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_autor            INT                    NOT NULL,
    PRIMARY KEY (id_com_confidencial, cod_instancia),
    CONSTRAINT chk_inst_com_conf_valido CHECK (TRIM(comentario) <> ''),
    CONSTRAINT fk_inst_com_confidencial_instancia FOREIGN KEY (cod_instancia)
        REFERENCES proyecto.instancias(cod_instancia)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_inst_com_conf_autor FOREIGN KEY (id_autor)
        REFERENCES proyecto.funcionarios(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TRIGGER trg_actualizar_inst_com_confidenciales
    BEFORE UPDATE ON proyecto.inst_com_confidenciales
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- La PK (id_com_confidencial, cod_instancia) no cubre "comentarios
-- confidenciales de esta instancia", que es la consulta real.
CREATE INDEX idx_inst_com_confidenciales_instancia ON proyecto.inst_com_confidenciales(cod_instancia);

-- =====================================================
-- TABLA: log_intentos
-- =====================================================
CREATE TABLE proyecto.log_intentos (
    id_login    SERIAL    PRIMARY KEY,
    id_usuario  INT,
    fue_exitoso BOOLEAN   NOT NULL,
    fec_hora    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_intento_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_login_intentos_usuario_fecha
    ON proyecto.log_intentos(id_usuario, fec_hora DESC);

-- =====================================================
-- TABLA: token_restablecimiento
-- =====================================================
-- Tokens de un solo uso para el flujo "olvidé mi contraseña": se guarda el
-- hash SHA-256 del token (no el token en claro), nunca la contraseña en sí.
-- La contraseña solo se aplica cuando se confirma con un token válido, no
-- en el momento de la solicitud — así un pedido no autenticado no alcanza
-- para tumbarle la contraseña a otro usuario.
CREATE TABLE proyecto.token_restablecimiento (
    id_token       SERIAL       PRIMARY KEY,
    id_usuario     INT          NOT NULL,
    token_hash     VARCHAR(255) NOT NULL UNIQUE,
    fec_expiracion TIMESTAMP    NOT NULL,
    usado          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_token_restablecimiento_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_token_restablecimiento_usuario ON proyecto.token_restablecimiento(id_usuario);

-- =====================================================
-- TABLA: reportes_generados
-- =====================================================
-- Registro de verificación de reportes PDF (RF30/RF31): guarda el hash
-- SHA-256 de cada PDF exportado para poder confirmar después, ante la
-- carga pública de un archivo, si ese PDF fue realmente generado por SIENEP
-- y no fue alterado. No se guarda metadata como "generado por" o "filtros
-- aplicados": si el hash coincide, el archivo es idéntico byte a byte al
-- original, y esos datos ya están impresos en el propio PDF. Write-once,
-- nunca se actualiza ni se da de baja lógicamente — mismo criterio que
-- log_intentos/token_restablecimiento (sin updated_at, sin trigger de auditoría).
CREATE TABLE proyecto.reportes_generados (
    id_reporte  SERIAL       PRIMARY KEY,
    hash_sha256 VARCHAR(64)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_hash_sha256_valido CHECK (hash_sha256 ~ '^[a-f0-9]{64}$')
);

-- =====================================================
-- TABLA: auditoria
-- =====================================================
CREATE TABLE proyecto.auditoria (
    id_auditoria   SERIAL               PRIMARY KEY,
    entidad        VARCHAR(100)         NOT NULL,
    accion         proyecto.tipo_accion NOT NULL,
    id_usuario     INT,
    usuario_bd     VARCHAR(63),
    fec_hora       TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip             VARCHAR(45),
    valor_anterior JSONB,
    valor_nuevo    JSONB,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
        REFERENCES proyecto.usuarios(id_usuario)
        ON UPDATE CASCADE ON DELETE SET NULL
);

-- =====================================================
-- FUNCION GENÉRICA DE AUDITORIA
-- =====================================================
CREATE OR REPLACE FUNCTION proyecto.trg_auditoria_general()
RETURNS TRIGGER AS $$
DECLARE
    v_usuario_id     INT;
    v_ip             VARCHAR(45);
    v_valor_anterior JSONB;
    v_valor_nuevo    JSONB;
    v_accion         proyecto.tipo_accion;
BEGIN
    -- Obtener usuario e IP de las variables de sesión de Postgres
    BEGIN
        v_usuario_id := NULLIF(current_setting('sienep.usuario_actual', true), '')::INT;
        v_ip := NULLIF(current_setting('sienep.ip_actual', true), '');
    EXCEPTION WHEN OTHERS THEN
        v_usuario_id := NULL;
        v_ip := NULL;
    END;

    -- Preparar los JSON quitando la contraseña por seguridad
    IF TG_OP = 'INSERT' THEN
        v_accion := 'INSERT';
        v_valor_anterior := NULL;
        v_valor_nuevo := to_jsonb(NEW) - 'contrasenia';
    ELSIF TG_OP = 'UPDATE' THEN
        v_accion := 'UPDATE';
        v_valor_anterior := to_jsonb(OLD) - 'contrasenia';
        v_valor_nuevo := to_jsonb(NEW) - 'contrasenia';
    ELSIF TG_OP = 'DELETE' THEN
        v_accion := 'DELETE';
        v_valor_anterior := to_jsonb(OLD) - 'contrasenia';
        v_valor_nuevo := NULL;
    END IF;

    -- Las observaciones/comentarios confidenciales no deben quedar legibles en la auditoría
    -- para quien solo tiene VER_AUDITORIA (sin los permisos de confidencialidad específicos)
    IF TG_TABLE_NAME = 'estu_obs_confidenciales' THEN
        IF v_valor_anterior IS NOT NULL THEN
            v_valor_anterior := v_valor_anterior - 'obs_confidencial';
        END IF;
        IF v_valor_nuevo IS NOT NULL THEN
            v_valor_nuevo := v_valor_nuevo - 'obs_confidencial';
        END IF;
    ELSIF TG_TABLE_NAME = 'inst_com_confidenciales' THEN
        IF v_valor_anterior IS NOT NULL THEN
            v_valor_anterior := v_valor_anterior - 'comentario';
        END IF;
        IF v_valor_nuevo IS NOT NULL THEN
            v_valor_nuevo := v_valor_nuevo - 'comentario';
        END IF;
    END IF;

    -- Insertar en la tabla de auditoría (registrando también el usuario_bd)
    INSERT INTO proyecto.auditoria (entidad, accion, id_usuario, ip, usuario_bd, valor_anterior, valor_nuevo)
    VALUES (UPPER(TG_TABLE_NAME), v_accion, v_usuario_id, v_ip, CURRENT_USER, v_valor_anterior, v_valor_nuevo);

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Asignar triggers a las tablas principales y secundarias
CREATE TRIGGER trg_audit_usuarios AFTER INSERT OR UPDATE OR DELETE ON proyecto.usuarios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estudiantes AFTER INSERT OR UPDATE OR DELETE ON proyecto.estudiantes FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_funcionarios AFTER INSERT OR UPDATE OR DELETE ON proyecto.funcionarios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_instancias AFTER INSERT OR UPDATE OR DELETE ON proyecto.instancias FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_recordatorios AFTER INSERT OR UPDATE OR DELETE ON proyecto.recordatorios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_carreras AFTER INSERT OR UPDATE OR DELETE ON proyecto.carreras FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_itrs AFTER INSERT OR UPDATE OR DELETE ON proyecto.itrs FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_roles AFTER INSERT OR UPDATE OR DELETE ON proyecto.roles FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_permisos AFTER INSERT OR UPDATE OR DELETE ON proyecto.permisos FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_frecuencias AFTER INSERT OR UPDATE OR DELETE ON proyecto.frecuencias FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_cat_instancias AFTER INSERT OR UPDATE OR DELETE ON proyecto.cat_instancias FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_cat_recordatorios AFTER INSERT OR UPDATE OR DELETE ON proyecto.cat_recordatorios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_grupos AFTER INSERT OR UPDATE OR DELETE ON proyecto.grupos FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_itr_carrera AFTER INSERT OR UPDATE OR DELETE ON proyecto.itr_carrera FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_rol_permisos AFTER INSERT OR UPDATE OR DELETE ON proyecto.rol_permisos FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_incidencias AFTER INSERT OR UPDATE OR DELETE ON proyecto.incidencias FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estu_grupos AFTER INSERT OR UPDATE OR DELETE ON proyecto.estu_grupos FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estu_informesadjs AFTER INSERT OR UPDATE OR DELETE ON proyecto.estu_informesadjs FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estu_telefonos AFTER INSERT OR UPDATE OR DELETE ON proyecto.estu_telefonos FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estu_obs_comentarios AFTER INSERT OR UPDATE OR DELETE ON proyecto.estu_obs_comentarios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_estu_obs_confidenciales AFTER INSERT OR UPDATE OR DELETE ON proyecto.estu_obs_confidenciales FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_involucrados AFTER INSERT OR UPDATE OR DELETE ON proyecto.involucrados FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_inst_comunes AFTER INSERT OR UPDATE OR DELETE ON proyecto.inst_comunes FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_inst_comentarios AFTER INSERT OR UPDATE OR DELETE ON proyecto.inst_comentarios FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();
CREATE TRIGGER trg_audit_inst_com_confidenciales AFTER INSERT OR UPDATE OR DELETE ON proyecto.inst_com_confidenciales FOR EACH ROW EXECUTE FUNCTION proyecto.trg_auditoria_general();


SET search_path TO proyecto;

-- =============================================
-- ROLES
-- =============================================
INSERT INTO proyecto.roles (nom_rol) VALUES
    ('ADMINISTRADOR'),
    ('PSICOPEDAGOGO'),
    ('TUTOR'),
    ('DOCENTE'),
    ('LEGALES'),
    ('ESTUDIANTE')
ON CONFLICT (nom_rol) DO NOTHING;

-- =============================================
-- CATEGORIAS DE INSTANCIA
-- =============================================
INSERT INTO proyecto.cat_instancias (nom_cate_instancias) VALUES
    ('REUNIÓN'),
    ('LLAMADA'),
    ('COORDINACIÓN'),
    ('INFORME')
ON CONFLICT (nom_cate_instancias) DO NOTHING;

-- =============================================
-- CATEGORIAS DE RECORDATORIO
-- =============================================
INSERT INTO proyecto.cat_recordatorios (nom_cate_recordatorios, descripcion) VALUES
    ('ADMINISTRATIVO', 'Tareas administrativas del sistema'),
    ('ACADÉMICO',      'Recordatorios relacionados a actividades académicas'),
    ('SEGUIMIENTO',    'Acciones de seguimiento a estudiantes'),
    ('URGENTE',        'Recordatorios que requieren atención inmediata')
ON CONFLICT (nom_cate_recordatorios) DO NOTHING;

-- =============================================
-- PERMISOS
-- =============================================
INSERT INTO proyecto.permisos (nom_permiso, per_descripcion) VALUES
    -- Funcionarios
    ('VER_FUNCIONARIOS',       'Permite ver la lista de funcionarios del sistema'),
    ('CREAR_FUNCIONARIO',      'Permite registrar un nuevo funcionario'),
    ('EDITAR_FUNCIONARIO',     'Permite editar los datos de un funcionario'),
    ('DESACTIVAR_FUNCIONARIO', 'Permite desactivar a un funcionario'),
    ('REACTIVAR_FUNCIONARIO',  'Permite reactivar un funcionario dado de baja'),
    -- Roles
    ('VER_ROLES',        'Permite ver la lista de roles del sistema'),
    ('CREAR_ROL',         'Permite crear nuevos roles'),
    ('EDITAR_ROL',        'Permite editar la información de un rol'),
    ('DESACTIVAR_ROL',    'Permite desactivar un rol existente'),
    ('REACTIVAR_ROL',    'Permite reactivar un rol dado de baja'),
    -- Estudiantes
    ('VER_ESTUDIANTE',          'Permite visualizar los datos de los estudiantes'),
    ('VER_ESTUDIANTE_RESUMIDO', 'Permite visualizar datos resumidos de estudiantes'),
    ('CREAR_ESTUDIANTE',        'Permite registrar nuevos estudiantes'),
    ('EDITAR_ESTUDIANTE',       'Permite modificar datos de un estudiante existente'),
    ('DESACTIVAR_ESTUDIANTE',   'Permite desactivar a un estudiante'),
    ('BUSCAR_ESTUDIANTE',       'Permite buscar estudiantes por criterios específicos'),
    ('REACTIVAR_ESTUDIANTE',    'Permite reactivar estudiantes dados de baja'),
    -- Adjuntos
    ('VER_INFORME',      'Permite ver los archivos adjuntos de un estudiante o instancia'),
    ('ADJUNTAR_INFORME', 'Permite adjuntar un informe u otro archivo al sistema'),
    ('ELIMINAR_INFORME', 'Permite eliminar un informe.'),
    -- Instancias
    ('VER_INSTANCIAS',          'Permite visualizar las instancias registradas'),
    ('CREAR_INSTANCIA',         'Permite crear nuevas instancias'),
    ('EDITAR_INSTANCIA',        'Permite modificar una instancia existente'),
    ('DESACTIVAR_INSTANCIA',    'Permite desactivar una instancia'),
    ('REACTIVAR_INSTANCIA',     'Permite reactivar una instancia dada de baja'),
    ('VER_BLOQUE_CONFIDENCIAL', 'Permite ver el bloque confidencial de una instancia'),
    -- Recordatorios
    ('VER_RECORDATORIOS',      'Permite ver los recordatorios existentes'),
    ('CREAR_RECORDATORIO',     'Permite crear nuevos recordatorios'),
    ('EDITAR_RECORDATORIO',    'Permite editar recordatorios existentes'),
    ('DESACTIVAR_RECORDATORIO','Permite desactivar recordatorios'),
    ('REACTIVAR_RECORDATORIO', 'Permite reactivar recordatorios y frecuencias dados de baja'),
    -- Incidencias
    ('CREAR_INCIDENCIA',     'Permite registrar nuevas incidencias'),
    ('VER_INCIDENCIAS',      'Permite visualizar las incidencias registradas'),
    ('EDITAR_INCIDENCIA',    'Permite editar incidencias existentes'),
    ('DESACTIVAR_INCIDENCIA','Permite desactivar incidencias existentes'),
    ('REACTIVAR_INCIDENCIA', 'Permite reactivar una incidencia dada de baja'),
    -- Reportes
    ('VER_REPORTES',      'Permite acceder al módulo de reportes'),
    ('EXPORTAR_REPORTES', 'Permite exportar los reportes generados'),
    -- Categorías
    ('VER_CATEGORIAS_INSTANCIA',        'Permite visualizar categorías de instancias'),
    ('CREAR_CATEGORIA_INSTANCIA',       'Permite crear categorías de instancias'),
    ('EDITAR_CATEGORIA_INSTANCIA',      'Permite editar categorías de instancias'),
    ('DESACTIVAR_CATEGORIA_INSTANCIA',  'Permite desactivar categorías de instancias'),
    ('REACTIVAR_CATEGORIA_INSTANCIA',   'Permite reactivar categorías de instancias dadas de baja'),
    ('VER_CATEGORIAS_RECORDATORIO',     'Permite visualizar categorías de recordatorios'),
    ('CREAR_CATEGORIA_RECORDATORIO',    'Permite crear categorías de recordatorios'),
    ('EDITAR_CATEGORIA_RECORDATORIO',   'Permite editar categorías de recordatorios'),
    ('DESACTIVAR_CATEGORIA_RECORDATORIO','Permite desactivar categorías de recordatorios'),
    ('REACTIVAR_CATEGORIA_RECORDATORIO', 'Permite reactivar categorías de recordatorios dadas de baja'),
    -- Permisos y perfil
    ('VER_PERMISOS',   'Permite visualizar la lista de permisos del sistema'),
    ('EDITAR_PERMISO', 'Permite editar la descripción de los permisos'),
    ('VER_AUDITORIA',  'Permite consultar el historial de eventos y auditoría del sistema'),
    -- Comentarios de estudiante
    ('VER_COMENTARIO_NORMAL_ESTUDIANTE',            'Permite ver comentarios normales de un estudiante'),
    ('VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',      'Permite ver comentarios confidenciales de estudiantes'),
    ('CREAR_COMENTARIO_NORMAL_ESTUDIANTE',          'Permite crear comentarios normales sobre un estudiante'),
    ('CREAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',    'Permite crear comentarios confidenciales sobre un estudiante'),
    ('EDITAR_COMENTARIO_NORMAL_ESTUDIANTE',         'Permite editar comentarios normales de un estudiante'),
    ('EDITAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',   'Permite editar comentarios confidenciales de un estudiante'),
    ('ELIMINAR_COMENTARIO_NORMAL_ESTUDIANTE',       'Permite eliminar comentarios normales de un estudiante'),
    ('ELIMINAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE', 'Permite eliminar comentarios confidenciales de un estudiante'),
    -- Comentarios de instancia
    ('VER_COMENTARIO_NORMAL_INSTANCIA',             'Permite ver comentarios normales de instancia'),
    ('VER_COMENTARIO_CONFIDENCIAL_INSTANCIA',       'Permite ver comentarios confidenciales de instancia'),
    ('CREAR_COMENTARIO_NORMAL_INSTANCIA',           'Permite crear comentarios normales de instancia'),
    ('CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA',     'Permite crear comentarios confidenciales de instancia'),
    ('EDITAR_COMENTARIO_NORMAL_INSTANCIA',          'Permite editar comentarios normales de instancia'),
    ('EDITAR_COMENTARIO_CONFIDENCIAL_INSTANCIA',    'Permite editar comentarios confidenciales de instancia'),
    ('ELIMINAR_COMENTARIO_NORMAL_INSTANCIA',        'Permite eliminar comentarios normales de instancia'),
    ('ELIMINAR_COMENTARIO_CONFIDENCIAL_INSTANCIA',  'Permite eliminar comentarios confidenciales de instancia'),
    -- Grupos
    ('CREAR_GRUPO',      'Permite crear nuevos grupos'),
    ('VER_GRUPOS',       'Permite visualizar grupos existentes'),
    ('EDITAR_GRUPO',     'Permite editar grupos existentes'),
    ('DESACTIVAR_GRUPO', 'Permite desactivar grupos'),
    ('REACTIVAR_GRUPO',  'Permite reactivar grupos'),
    -- Carreras
    ('VER_CARRERAS',       'Permite ver la lista de carreras del sistema'),
    ('CREAR_CARRERA',      'Permite registrar una nueva carrera'),
    ('EDITAR_CARRERA',     'Permite editar los datos de una carrera'),
    ('DESACTIVAR_CARRERA', 'Permite desactivar una carrera'),
    ('REACTIVAR_CARRERA',  'Permite reactivar una carrera dada de baja'),
    -- ITRs
    ('VER_ITRS',       'Permite ver la lista de ITRs del sistema'),
    ('CREAR_ITR',      'Permite registrar un nuevo ITR'),
    ('EDITAR_ITR',     'Permite editar los datos de un ITR'),
    ('DESACTIVAR_ITR', 'Permite desactivar un ITR'),
    ('REACTIVAR_ITR',  'Permite reactivar un ITR dado de baja')
ON CONFLICT (nom_permiso) DO NOTHING;

-- =============================================
-- PERMISOS POR ROL
-- =============================================

-- ADMINISTRADOR: todos los permisos
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r, proyecto.permisos p
WHERE r.nom_rol = 'ADMINISTRADOR'
ON CONFLICT DO NOTHING;

-- PSICOPEDAGOGO
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r
    JOIN proyecto.permisos p ON p.nom_permiso IN (
        'VER_ESTUDIANTE_RESUMIDO', 'VER_ESTUDIANTE', 'BUSCAR_ESTUDIANTE',
        'CREAR_ESTUDIANTE', 'EDITAR_ESTUDIANTE', 'REACTIVAR_ESTUDIANTE',
        'VER_INFORME', 'ADJUNTAR_INFORME',
        'VER_INSTANCIAS', 'CREAR_INSTANCIA', 'EDITAR_INSTANCIA',
        'DESACTIVAR_INSTANCIA', 'REACTIVAR_INSTANCIA', 'VER_BLOQUE_CONFIDENCIAL',
        'VER_RECORDATORIOS', 'CREAR_RECORDATORIO', 'EDITAR_RECORDATORIO',
        'DESACTIVAR_RECORDATORIO', 'REACTIVAR_RECORDATORIO',
        'CREAR_INCIDENCIA', 'VER_INCIDENCIAS',
        'EDITAR_INCIDENCIA', 'DESACTIVAR_INCIDENCIA', 'REACTIVAR_INCIDENCIA',
        'VER_COMENTARIO_NORMAL_ESTUDIANTE', 'CREAR_COMENTARIO_NORMAL_ESTUDIANTE',
        'EDITAR_COMENTARIO_NORMAL_ESTUDIANTE', 'ELIMINAR_COMENTARIO_NORMAL_ESTUDIANTE',
        'VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE', 'CREAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',
        'EDITAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE', 'ELIMINAR_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',
        'VER_COMENTARIO_NORMAL_INSTANCIA', 'CREAR_COMENTARIO_NORMAL_INSTANCIA',
        'EDITAR_COMENTARIO_NORMAL_INSTANCIA', 'ELIMINAR_COMENTARIO_NORMAL_INSTANCIA',
        'VER_COMENTARIO_CONFIDENCIAL_INSTANCIA', 'CREAR_COMENTARIO_CONFIDENCIAL_INSTANCIA',
        'EDITAR_COMENTARIO_CONFIDENCIAL_INSTANCIA', 'ELIMINAR_COMENTARIO_CONFIDENCIAL_INSTANCIA',
        'VER_REPORTES', 'EXPORTAR_REPORTES'
    )
WHERE r.nom_rol = 'PSICOPEDAGOGO'
ON CONFLICT DO NOTHING;

-- TUTOR
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r
    JOIN proyecto.permisos p ON p.nom_permiso IN (
        'VER_ESTUDIANTE_RESUMIDO', 'BUSCAR_ESTUDIANTE',
        'VER_INFORME',
        'VER_INSTANCIAS', 'CREAR_INSTANCIA', 'EDITAR_INSTANCIA',
        'VER_RECORDATORIOS', 'CREAR_RECORDATORIO', 'EDITAR_RECORDATORIO', 'REACTIVAR_RECORDATORIO',
        'VER_COMENTARIO_NORMAL_ESTUDIANTE', 'CREAR_COMENTARIO_NORMAL_ESTUDIANTE',
        'VER_COMENTARIO_NORMAL_INSTANCIA', 'CREAR_COMENTARIO_NORMAL_INSTANCIA'
    )
WHERE r.nom_rol = 'TUTOR'
ON CONFLICT DO NOTHING;

-- DOCENTE
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r
    JOIN proyecto.permisos p ON p.nom_permiso IN (
        'VER_ESTUDIANTE_RESUMIDO',
        'VER_INSTANCIAS',
        'VER_RECORDATORIOS', 'CREAR_RECORDATORIO',
        'VER_COMENTARIO_NORMAL_ESTUDIANTE',
        'VER_COMENTARIO_NORMAL_INSTANCIA'
    )
WHERE r.nom_rol = 'DOCENTE'
ON CONFLICT DO NOTHING;

-- LEGALES
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r
    JOIN proyecto.permisos p ON p.nom_permiso IN (
        'VER_ESTUDIANTE_RESUMIDO', 'VER_ESTUDIANTE', 'BUSCAR_ESTUDIANTE',
        'VER_INFORME',
        'VER_INSTANCIAS', 'VER_BLOQUE_CONFIDENCIAL',
        'VER_INCIDENCIAS',
        'VER_COMENTARIO_CONFIDENCIAL_ESTUDIANTE',
        'VER_COMENTARIO_CONFIDENCIAL_INSTANCIA',
        'VER_COMENTARIO_NORMAL_ESTUDIANTE',
        'VER_COMENTARIO_NORMAL_INSTANCIA',
        'VER_REPORTES', 'EXPORTAR_REPORTES'
    )
WHERE r.nom_rol = 'LEGALES'
ON CONFLICT DO NOTHING;

-- ESTUDIANTE
INSERT INTO proyecto.rol_permisos (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM proyecto.roles r
    JOIN proyecto.permisos p ON p.nom_permiso IN (
        'VER_COMENTARIO_NORMAL_ESTUDIANTE'
    )
WHERE r.nom_rol = 'ESTUDIANTE'
ON CONFLICT DO NOTHING;


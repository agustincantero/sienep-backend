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
CREATE TYPE proyecto.tipo_frecuencia AS ENUM ('diaria', 'semanal', 'mensual', 'personalizada');
CREATE TYPE proyecto.dia_semana AS ENUM ('lunes', 'martes', 'miercoles', 'jueves', 'viernes', 'sabado', 'domingo');
CREATE TYPE proyecto.tipo_recordatorio AS ENUM ('llamada', 'reunion', 'tarea administrativa', 'otro');

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
                                   id_usuario  SERIAL PRIMARY KEY,
                                   nombre      VARCHAR(50)              NOT NULL,
                                   apellido    VARCHAR(50)              NOT NULL,
                                   documento   VARCHAR(15)              NOT NULL,
                                   pais_documento proyecto.pais_documento NOT NULL DEFAULT 'URY',
                                   email       proyecto.dom_email       NOT NULL,
                                   contrasenia VARCHAR(255)             NOT NULL,
                                   estado      proyecto.estado_logico   NOT NULL DEFAULT 'PENDIENTE_DE_ACTIVACION',
                                   created_at  TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at  TIMESTAMP                NOT NULL DEFAULT CURRENT_TIMESTAMP,

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
                                   id_carrera  SERIAL PRIMARY KEY,
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
                                id_rol          SERIAL PRIMARY KEY,
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
                               id_itr     SERIAL PRIMARY KEY,
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
                                   id_permiso      SERIAL PRIMARY KEY,
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
                                      id_frecuencia SERIAL PRIMARY KEY,
                                      descripcion   VARCHAR(50)               NOT NULL,
                                      tipo          proyecto.tipo_frecuencia  NOT NULL,
                                      int_dias      INT CHECK (int_dias >= 1 AND int_dias <= 31),
                                      dia_semana    proyecto.dia_semana,
                                      estado        proyecto.estado_logico    NOT NULL DEFAULT 'ACTIVO',
                                      created_at    TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at    TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT chk_coherencia_frecuencia CHECK (
                                          (tipo = 'diaria'       AND int_dias IS NULL     AND dia_semana IS NULL) OR
                                          (tipo = 'semanal'      AND int_dias IS NULL     AND dia_semana IS NOT NULL) OR
                                          (tipo = 'mensual'      AND int_dias IS NOT NULL AND dia_semana IS NULL) OR
                                          (tipo = 'personalizada')
                                          )
);

CREATE TRIGGER trg_actualizar_frecuencias
    BEFORE UPDATE ON proyecto.frecuencias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: cat_instancias  ← CORREGIDO: antes sin prefijo proyecto.
-- =====================================================
CREATE TABLE proyecto.cat_instancias (
                                         id_categoria        SERIAL PRIMARY KEY,
                                         nom_cate_instancias VARCHAR(50)            NOT NULL,
                                         descripcion         TEXT,
                                         estado              proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
                                         created_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at          TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         CONSTRAINT chk_nom_categoria_valido  CHECK (TRIM(nom_cate_instancias) <> ''),
                                         CONSTRAINT uk_nom_cate_instancia     UNIQUE (nom_cate_instancias)
);

CREATE TRIGGER trg_actualizar_categorias
    BEFORE UPDATE ON proyecto.cat_instancias
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: cat_recordatorios
-- =====================================================
CREATE TABLE proyecto.cat_recordatorios (
                                            id_categoria           SERIAL PRIMARY KEY,
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
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
                                 id_grupo   SERIAL PRIMARY KEY,
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
CREATE TABLE proyecto.instancias (
                                     cod_instancia    SERIAL       PRIMARY KEY,
                                     titulo           VARCHAR(150) NOT NULL,
                                     fec_hora         TIMESTAMP    NOT NULL,
                                     id_estudiante    INT          NOT NULL,
                                     id_neg_instancia VARCHAR(16),
                                     id_funcionario   INT          NOT NULL,
                                     canal            VARCHAR(50),
                                     estado           proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
                                     created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
                                        cod_recordatorio    SERIAL PRIMARY KEY,
                                        id_categoria        INT                        NOT NULL,
                                        id_neg_recordatorio VARCHAR(16),
                                        titulo              VARCHAR(150)               NOT NULL,
                                        tipo                proyecto.tipo_recordatorio NOT NULL,
                                        fec_hora            TIMESTAMP                  NOT NULL,
                                        cod_instancia       INT                        NOT NULL,
                                        id_frecuencia       INT,
                                        fec_fin             DATE,
                                        nro_ocurrencias     INT CHECK (nro_ocurrencias BETWEEN 0 AND 5),
                                        responsable         INT                        NOT NULL,
                                        estado_operativo    proyecto.estado_operativo  NOT NULL DEFAULT 'FUTURO',
                                        estado              proyecto.estado_logico     NOT NULL DEFAULT 'ACTIVO',
                                        created_at          TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at          TIMESTAMP                  NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        CONSTRAINT chk_rec_titulo_valido CHECK (TRIM(titulo) <> ''),
                                        CONSTRAINT chk_fec_hora_futura   CHECK (fec_hora > CURRENT_TIMESTAMP),
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

-- =====================================================
-- TABLA: estu_informesadjs
-- =====================================================
CREATE TABLE proyecto.estu_informesadjs (
                                            id_informe   SERIAL       PRIMARY KEY,
                                            id_usuario   INT          NOT NULL,
                                            url_archivo  VARCHAR(255) NOT NULL,
                                            ruta         VARCHAR(255),
                                            nom_archivo  VARCHAR(70)  NOT NULL,
                                            estado       proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
                                            created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
                                         id_usuario INT                       NOT NULL,
                                         telefono   proyecto.dom_telefono     NOT NULL,
                                         estado     proyecto.estado_logico    NOT NULL DEFAULT 'ACTIVO',
                                         created_at TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
                                       cod_instancia INT       NOT NULL,
                                       id_categoria  INT       NOT NULL,
                                       created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (cod_instancia, id_categoria),
                                       CONSTRAINT fk_comunes_instancia FOREIGN KEY (cod_instancia)
                                           REFERENCES proyecto.instancias(cod_instancia)
                                           ON UPDATE CASCADE ON DELETE RESTRICT,
                                       CONSTRAINT fk_comunes_categoria FOREIGN KEY (id_categoria)
                                           REFERENCES proyecto.cat_instancias(id_categoria)
                                           ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_inst_comunes
    BEFORE UPDATE ON proyecto.inst_comunes
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

-- =====================================================
-- TABLA: inst_comentarios
-- =====================================================
CREATE TABLE proyecto.inst_comentarios (
                                           id_comentario SERIAL       PRIMARY KEY,
                                           cod_instancia INT          NOT NULL,
                                           comentario    TEXT         NOT NULL,
                                           estado        proyecto.estado_logico NOT NULL DEFAULT 'ACTIVO',
                                           created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           id_autor      INT          NOT NULL,
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
                                                  PRIMARY KEY (id_com_confidencial, cod_instancia),
                                                  CONSTRAINT chk_inst_com_conf_valido CHECK (TRIM(comentario) <> ''),
                                                  CONSTRAINT fk_inst_com_confidencial_instancia FOREIGN KEY (cod_instancia)
                                                      REFERENCES proyecto.instancias(cod_instancia)
                                                      ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TRIGGER trg_actualizar_inst_com_confidenciales
    BEFORE UPDATE ON proyecto.inst_com_confidenciales
    FOR EACH ROW EXECUTE FUNCTION proyecto.actualizar_updated_at();

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
-- TABLA: auditoria
-- =====================================================
CREATE TABLE proyecto.auditoria (
                                    id_auditoria SERIAL                 PRIMARY KEY,
                                    entidad      VARCHAR(100)           NOT NULL,
                                    accion       proyecto.tipo_accion   NOT NULL,
                                    id_usuario   INT                    NOT NULL,
                                    fec_hora     TIMESTAMP              NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    detalle      TEXT,
                                    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (id_usuario)
                                        REFERENCES proyecto.usuarios(id_usuario)
                                        ON UPDATE CASCADE ON DELETE CASCADE
);
package com.fides.sienep.service;

import com.fides.sienep.model.Auditoria;
import com.fides.sienep.model.TipoAccion;
import com.fides.sienep.model.Usuario;
import com.fides.sienep.repository.AuditoriaRepository;
import com.fides.sienep.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrarEventoManual(String entidad, TipoAccion accion, Integer idUsuario, String ip) {
        Usuario autor = usuarioRepository.findById(idUsuario).orElse(null);
        if (autor == null) return;

        Auditoria auditoria = new Auditoria();
        auditoria.setEntidad(entidad.toUpperCase());
        auditoria.setAccion(accion);
        auditoria.setAutor(autor);
        auditoria.setIp(ip);
        auditoria.setValorAnterior(null);
        auditoria.setValorNuevo(null);

        auditoriaRepository.save(auditoria);
    }
}

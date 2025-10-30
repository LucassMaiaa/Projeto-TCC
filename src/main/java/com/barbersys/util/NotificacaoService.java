package com.barbersys.util;

import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Notificacao;
import com.barbersys.model.Usuario;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ManagedBean(name = "notificacaoService", eager = true)
@ApplicationScoped
public class NotificacaoService {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, List<Notificacao>> notificacoesPorUsuario = new ConcurrentHashMap<>();

    public void enviarParaPerfis(Notificacao notificacao, List<String> nomesDosPerfis) {
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        List<Usuario> usuariosAlvo = usuarioDAO.buscarUsuariosPorPerfis(nomesDosPerfis);

        notificacao.setId(idGenerator.getAndIncrement());

        for (Usuario usuario : usuariosAlvo) {
            notificacoesPorUsuario.computeIfAbsent(usuario.getId(), k -> Collections.synchronizedList(new ArrayList<>())).add(notificacao);
        }
    }

    public List<Notificacao> getNotificacoes(Long usuarioId) {
        return notificacoesPorUsuario.getOrDefault(usuarioId, Collections.emptyList());
    }

    public void remover(Long usuarioId, Notificacao notificacao) {
        if (notificacoesPorUsuario.containsKey(usuarioId)) {
            notificacoesPorUsuario.get(usuarioId).remove(notificacao);
        }
    }
}

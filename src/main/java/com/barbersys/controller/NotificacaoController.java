package com.barbersys.controller;

import com.barbersys.dao.NotificacaoDAO;
import com.barbersys.model.Notificacao;
import com.barbersys.model.Usuario;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@ViewScoped
public class NotificacaoController implements Serializable {

    private NotificacaoDAO notificacaoDAO;
    private List<Notificacao> notificacoes;

    @PostConstruct
    public void init() {
        this.notificacaoDAO = new NotificacaoDAO();
        this.notificacoes = new java.util.ArrayList<>(); // Inicia com lista vazia por padrão

        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        
        if (usuarioLogado != null && usuarioLogado.getPerfil() != null) {
            Long perfilId = usuarioLogado.getPerfil().getId();
            
            // Se for Admin (1) ou Funcionário (2), busca as notificações globais de agendamentos
            if (perfilId.equals(1L) || perfilId.equals(2L)) {
                this.notificacoes = notificacaoDAO.buscarAtivas();
            } 
            // Se for Cliente (3), busca apenas as notificações específicas dele
            else if (perfilId.equals(3L) && usuarioLogado.getClienteAssociado() != null) {
                Long clienteId = usuarioLogado.getClienteAssociado().getId();
                this.notificacoes = notificacaoDAO.buscarAtivasPorCliente(clienteId);
            }
        }
    }

    public void remover(Notificacao notificacao) {
        try {
            notificacaoDAO.marcarComoInativa(notificacao);
            this.notificacoes.remove(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Getters e Setters
    public List<Notificacao> getNotificacoes() {
        return notificacoes;
    }
}

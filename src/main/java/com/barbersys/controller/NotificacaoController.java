package com.barbersys.controller;

import com.barbersys.dao.NotificacaoDAO;
import com.barbersys.model.Notificacao;
import com.barbersys.model.Usuario;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;
import java.io.Serializable;
import java.util.List;

@ManagedBean
@SessionScoped
public class NotificacaoController implements Serializable {

    private NotificacaoDAO notificacaoDAO;
    private List<Notificacao> notificacoes;
    private Notificacao notificacaoSelecionada;

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

    private void exibirAlerta(String icon, String title) {
        String script = String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                icon, title);
        PrimeFaces.current().executeScript(script);
    }

    public void selecionarNotificacao(Notificacao notificacao) {
        this.notificacaoSelecionada = notificacao;
    }

    public void confirmarRemocao() {
        try {
            if (notificacaoSelecionada != null) {
                notificacaoDAO.marcarComoInativa(notificacaoSelecionada);
                this.notificacoes.remove(notificacaoSelecionada);
                this.notificacaoSelecionada = null;
                
                exibirAlerta("success", "Notificação removida com sucesso!");
                PrimeFaces.current().executeScript("PF('dlgConfirmNotificacao').hide();");
            }
        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao remover notificação!");
        }
    }

    public void marcarTodasComoLidas() {
        try {
            List<Long> idsNaoLidas = new java.util.ArrayList<>();
            for (Notificacao n : notificacoes) {
                if ("N".equals(n.getLida())) {
                    idsNaoLidas.add(n.getId());
                    n.setLida("S"); // Atualiza em memória também
                }
            }
            
            if (!idsNaoLidas.isEmpty()) {
                notificacaoDAO.marcarTodasComoLidas(idsNaoLidas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getTotalNaoLidas() {
        if (notificacoes == null) {
            return 0;
        }
        return (int) notificacoes.stream()
                .filter(n -> "N".equals(n.getLida()))
                .count();
    }

    public void limparTodas() {
        try {
            List<Long> ids = new java.util.ArrayList<>();
            for (Notificacao n : notificacoes) {
                ids.add(n.getId());
            }
            
            if (!ids.isEmpty()) {
                // Marca todas como inativas no banco
                for (Long id : ids) {
                    Notificacao n = new Notificacao();
                    n.setId(id);
                    notificacaoDAO.marcarComoInativa(n);
                }
                
                // Limpa a lista em memória
                this.notificacoes.clear();
                
                exibirAlerta("success", "Todas as notificações foram removidas!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao limpar notificações!");
        }
    }

    public void atualizarNotificacoes() {
        try {
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("usuarioLogado");
            
            if (usuarioLogado != null && usuarioLogado.getPerfil() != null) {
                Long perfilId = usuarioLogado.getPerfil().getId();
                
                if (perfilId.equals(1L) || perfilId.equals(2L)) {
                    this.notificacoes = notificacaoDAO.buscarAtivas();
                } else if (perfilId.equals(3L) && usuarioLogado.getClienteAssociado() != null) {
                    Long clienteId = usuarioLogado.getClienteAssociado().getId();
                    this.notificacoes = notificacaoDAO.buscarAtivasPorCliente(clienteId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public Notificacao getNotificacaoSelecionada() {
        return notificacaoSelecionada;
    }

    public void setNotificacaoSelecionada(Notificacao notificacaoSelecionada) {
        this.notificacaoSelecionada = notificacaoSelecionada;
    }
}

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
        this.notificacoes = new java.util.ArrayList<>();

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
    }

    // Exibe alerta via SweetAlert2
    private void exibirAlerta(String icon, String title) {
        String script = String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                icon, title);
        PrimeFaces.current().executeScript(script);
    }

    // Seleciona notificação para remoção
    public void selecionarNotificacao(Notificacao notificacao) {
        this.notificacaoSelecionada = notificacao;
    }

    // Confirma remoção da notificação selecionada
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
            exibirAlerta("error", "Erro ao remover notificação!");
        }
    }

    // Marca todas as notificações como lidas
    public void marcarTodasComoLidas() {
        try {
            List<Long> idsNaoLidas = new java.util.ArrayList<>();
            for (Notificacao n : notificacoes) {
                if ("N".equals(n.getLida())) {
                    idsNaoLidas.add(n.getId());
                    n.setLida("S");
                }
            }
            
            if (!idsNaoLidas.isEmpty()) {
                notificacaoDAO.marcarTodasComoLidas(idsNaoLidas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Retorna total de notificações não lidas
    public int getTotalNaoLidas() {
        if (notificacoes == null) {
            return 0;
        }
        return (int) notificacoes.stream()
                .filter(n -> "N".equals(n.getLida()))
                .count();
    }

    // Remove todas as notificações
    public void limparTodas() {
        try {
            List<Long> ids = new java.util.ArrayList<>();
            for (Notificacao n : notificacoes) {
                ids.add(n.getId());
            }
            
            if (!ids.isEmpty()) {
                for (Long id : ids) {
                    Notificacao n = new Notificacao();
                    n.setId(id);
                    notificacaoDAO.marcarComoInativa(n);
                }
                
                this.notificacoes.clear();
                exibirAlerta("success", "Todas as notificações foram removidas!");
            }
        } catch (Exception e) {
            exibirAlerta("error", "Erro ao limpar notificações!");
        }
    }

    // Atualiza lista de notificações do banco
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

    // Remove uma notificação específica
    public void remover(Notificacao notificacao) {
        try {
            notificacaoDAO.marcarComoInativa(notificacao);
            this.notificacoes.remove(notificacao);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

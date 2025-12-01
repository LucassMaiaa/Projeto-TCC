package com.barbersys.controller;

import com.barbersys.dao.AvaliacaoDAO;
import com.barbersys.dao.NotificacaoDAO;
import com.barbersys.model.Agendamento;
import com.barbersys.model.Avaliacao;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Notificacao;
import com.barbersys.model.Usuario;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

@Getter
@Setter
@ManagedBean
@javax.faces.bean.SessionScoped
public class AvaliacaoController implements Serializable {

    private Avaliacao avaliacaoModel;
    private Notificacao notificacaoSelecionada;
    private Integer notaSelecionada = 0;
    private String comentario;
    private String nomeFuncionario;
    private Long agendamentoId;
    private Long funcionarioId;

    @PostConstruct
    public void init() {
        this.avaliacaoModel = new Avaliacao();
    }

    // Prepara o modal de avaliação com dados da notificação
    public void abrirModalAvaliacao(Notificacao notificacao) {
        this.notificacaoSelecionada = notificacao;
        this.notaSelecionada = 0;
        this.comentario = "";
        
        if (notificacao.getAgendamento() != null) {
            this.agendamentoId = notificacao.getAgendamento().getId();
            String mensagem = notificacao.getMensagem();
            
            if (mensagem != null && mensagem.contains("Seu atendimento com")) {
                try {
                    int inicioNome = mensagem.indexOf("com ") + 4;
                    int fimNome = mensagem.indexOf(" foi finalizado");
                    
                    if (inicioNome > 3 && fimNome > inicioNome) {
                        this.nomeFuncionario = mensagem.substring(inicioNome, fimNome);
                    } else {
                        this.nomeFuncionario = "Funcionário";
                    }
                } catch (Exception e) {
                    this.nomeFuncionario = "Funcionário";
                }
            } else {
                this.nomeFuncionario = "Funcionário";
            }
        } else {
            this.nomeFuncionario = "Funcionário";
        }
    }

    // Salva a avaliação do cliente e remove a notificação
    public void salvarAvaliacao() {
        try {
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("usuarioLogado");
            
            if (usuarioLogado == null || usuarioLogado.getClienteAssociado() == null) {
                exibirAlerta("error", "Usuário não identificado!");
                return;
            }

            if (notaSelecionada == null || notaSelecionada < 1 || notaSelecionada > 5) {
                exibirAlerta("warning", "Por favor, selecione uma nota de 1 a 5 estrelas!");
                return;
            }

            Long clienteId = usuarioLogado.getClienteAssociado().getId();
            
            if (AvaliacaoDAO.verificarSeJaAvaliou(agendamentoId, clienteId)) {
                exibirAlerta("warning", "Você já avaliou este atendimento!");
                return;
            }

            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setNota(notaSelecionada);
            avaliacao.setComentario(comentario);
            avaliacao.setDataCriacao(new Date());
            
            Agendamento agendamento = new Agendamento();
            agendamento.setId(agendamentoId);
            avaliacao.setAgendamento(agendamento);
            
            Cliente cliente = new Cliente();
            cliente.setId(clienteId);
            avaliacao.setCliente(cliente);
            
            Funcionario funcionario = new Funcionario();
            funcionario.setId(getFuncionarioIdDoAgendamento(agendamentoId));
            avaliacao.setFuncionario(funcionario);

            AvaliacaoDAO.salvar(avaliacao);

            if (notificacaoSelecionada != null) {
                NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
                notificacaoDAO.marcarComoInativa(notificacaoSelecionada);
                
                NotificacaoController notificacaoController = (NotificacaoController) 
                    FacesContext.getCurrentInstance().getExternalContext()
                    .getSessionMap().get("notificacaoController");
                
                if (notificacaoController != null) {
                    notificacaoController.atualizarNotificacoes();
                }
            }

            exibirAlerta("success", "Avaliação registrada com sucesso!");
            PrimeFaces.current().ajax().update("form:painelNotificacoes");

        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao salvar avaliação!");
        }
    }

    // Busca o ID do funcionário associado ao agendamento
    private Long getFuncionarioIdDoAgendamento(Long agendamentoId) {
        String sql = "SELECT fun_codigo FROM agendamento WHERE age_codigo = ?";
        try (java.sql.Connection conn = com.barbersys.util.DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, agendamentoId);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("fun_codigo");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void exibirAlerta(String icon, String title) {
        String script = String.format(
            "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
            icon, title);
        PrimeFaces.current().executeScript(script);
    }

    public String getEstrelaClass(int estrela) {
        if (notaSelecionada != null && estrela <= notaSelecionada) {
            return "fas fa-star estrela-selecionada";
        }
        return "far fa-star estrela-vazia";
    }

    public void selecionarNota(int nota) {
        this.notaSelecionada = nota;
    }
}

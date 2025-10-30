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

    public void abrirModalAvaliacao(Notificacao notificacao) {
        this.notificacaoSelecionada = notificacao;
        this.notaSelecionada = 0;
        this.comentario = "";
        
        // Buscar informações do agendamento pela notificação
        if (notificacao.getAgendamento() != null) {
            this.agendamentoId = notificacao.getAgendamento().getId();
            
            // Extrai o nome do funcionário da mensagem
            // Mensagem: "Seu atendimento com [Nome] foi finalizado! Avalie o serviço prestado"
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
            
            // Verifica se já avaliou este agendamento
            if (AvaliacaoDAO.verificarSeJaAvaliou(agendamentoId, clienteId)) {
                exibirAlerta("warning", "Você já avaliou este atendimento!");
                return;
            }

            // Cria a avaliação
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

            // Marca a notificação como inativa
            if (notificacaoSelecionada != null) {
                NotificacaoDAO notificacaoDAO = new NotificacaoDAO();
                notificacaoDAO.marcarComoInativa(notificacaoSelecionada);
            }

            exibirAlerta("success", "Avaliação registrada com sucesso!");
            PrimeFaces.current().executeScript("PF('dlgAvaliacao').hide();");
            
            // Atualiza o painel de notificações
            PrimeFaces.current().ajax().update("form:painelNotificacoes");

        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao salvar avaliação!");
        }
    }

    private Long getFuncionarioIdDoAgendamento(Long agendamentoId) {
        // Buscar o funcionarioId do agendamento no banco
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

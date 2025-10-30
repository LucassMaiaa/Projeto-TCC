package com.barbersys.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;

import com.barbersys.dao.AgendamentoDAO;
import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.ServicosDAO;
import com.barbersys.model.Agendamento;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.model.Servicos;
import com.barbersys.model.Usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class AgendamentoClienteController implements Serializable {



    private static final long serialVersionUID = 1L;

    // --- Campos do Formulário ---
    private String nomeClienteLogado;
    private Date dataSelecionada;
    private Long funcionarioId;
    private String horaSelecionada;
    private List<Long> servicosSelecionadosIds = new ArrayList<>();

    // --- Fontes de Dados para a Tela ---
    private List<Funcionario> funcionariosDisponiveis;
    private List<Servicos> servicosDisponiveis;
    private List<String> horariosDisponiveis = new ArrayList<>();
    private List<Agendamento> meusAgendamentos = new ArrayList<>();

    // --- Controle de Lógica e UI ---
    private String tipoAgendamento = "proprio"; // 'proprio' ou 'outro'
    private String nomeClienteOutro;
    private double valorTotal = 0.0;
    private Date today = new Date();

    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

    @PostConstruct
    public void init() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        
        // Verifica se é um cliente logado
        if (usuarioLogado != null) {
            // Tenta buscar o cliente associado ao usuário
            if (usuarioLogado.getClienteAssociado() == null && usuarioLogado.getId() != null) {
                // Se não estiver carregado na sessão, busca no banco
                Cliente clienteAssociado = ClienteDAO.buscarClientePorUsuarioId(usuarioLogado.getId());
                usuarioLogado.setClienteAssociado(clienteAssociado);
            }
            
            // Agora valida se realmente é um cliente
            if (usuarioLogado.getClienteAssociado() != null) {
                this.nomeClienteLogado = usuarioLogado.getClienteAssociado().getNome(); 
                popularMeusAgendamentos();
            } else {
                // Não é um cliente, não pode usar esta tela
                this.nomeClienteLogado = "Acesso restrito";
                // Redireciona para o dashboard
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            this.nomeClienteLogado = "Visitante";
        }
        
        this.funcionariosDisponiveis = FuncionarioDAO.buscarTodosFuncionarios();
        this.servicosDisponiveis = ServicosDAO.buscarTodos();
    }

    // --- Ações e Lógica ---
    
    public void agendar() {
        Agendamento novoAgendamento = new Agendamento();
        novoAgendamento.setDataCriado(dataSelecionada);
        novoAgendamento.setHoraSelecionada(LocalTime.parse(horaSelecionada));
        novoAgendamento.setStatus("A");
        novoAgendamento.setPago("N");

        Funcionario func = new Funcionario();
        func.setId(funcionarioId);
        novoAgendamento.setFuncionario(func);

        if ("proprio".equals(tipoAgendamento)) {
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
            if (usuarioLogado != null) {
                Cliente clienteLogado = ClienteDAO.buscarClientePorUsuarioId(usuarioLogado.getId());
                novoAgendamento.setCliente(clienteLogado);
                novoAgendamento.setTipoCadastro("A");
            } else {
                exibirAlerta("error", "Você precisa estar logado para agendar!");
                return;
            }
        } else {
            if (nomeClienteOutro == null || nomeClienteOutro.trim().isEmpty()) {
                 exibirAlerta("error", "O nome do cliente é obrigatório.");
                 return;
            }
            novoAgendamento.setTipoCadastro("I");
            novoAgendamento.setNomeClienteAvulso(nomeClienteOutro);
        }

        try {
            AgendamentoDAO.salvar(novoAgendamento, servicosSelecionadosIds);

            // INÍCIO DA LÓGICA DE NOTIFICAÇÃO NO BANCO
            String nomeCliente = "proprio".equals(tipoAgendamento) ? nomeClienteLogado : nomeClienteOutro;
            String nomeFuncionario = "";
            if (funcionariosDisponiveis != null) {
                for (com.barbersys.model.Funcionario f : funcionariosDisponiveis) {
                    if (f.getId().equals(funcionarioId)) {
                        nomeFuncionario = f.getNome();
                        break;
                    }
                }
            }

            String mensagem = "Agendamento com " + nomeCliente + " às " + horaSelecionada + " pelo funcionário " + nomeFuncionario;
            
            com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
            notificacao.setMensagem(mensagem);
            notificacao.setDataEnvio(new java.util.Date());
            notificacao.setAgendamento(novoAgendamento); // Link com o agendamento
            notificacao.setCliente(null); // NULL = notificação para funcionários/admins

            com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
            notificacaoDAO.salvar(notificacao);
            // FIM DA LÓGICA DE NOTIFICAÇÃO

            
            // Atualiza o backend
            popularMeusAgendamentos();
            limparFormulario();
            
            // Comandos para o frontend (separados, como em outras telas)
            PrimeFaces.current().ajax().update("@form");
            exibirAlerta("success", "Agendamento realizado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Ocorreu um erro ao salvar o agendamento.");
        }
    }

    private void limparFormulario() {
        this.dataSelecionada = null;
        this.funcionarioId = null;
        this.horaSelecionada = null;
        this.servicosSelecionadosIds.clear();
        this.horariosDisponiveis.clear();
        this.valorTotal = 0.0;
        this.nomeClienteOutro = null;
    }

    private void popularMeusAgendamentos() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        // Garante que existe um usuário logado e um cliente associado a ele
        if (usuarioLogado != null && usuarioLogado.getClienteAssociado() != null) {
            // Chama o novo método do DAO, passando o ID do cliente para uma busca precisa
            this.meusAgendamentos = AgendamentoDAO.buscarAgendamentosPorClienteId(usuarioLogado.getClienteAssociado().getId(), "A", 0, 10);
        } else {
            // Se não houver um cliente válido na sessão, limpa a lista
            this.meusAgendamentos.clear();
        }
    }

    public void cancelarMeuAgendamento(Long agendamentoId) {
        try {
            // Busca o agendamento completo antes de cancelar para obter os dados
            Agendamento agendamentoCancelado = null;
            for (Agendamento ag : meusAgendamentos) {
                if (ag.getId().equals(agendamentoId)) {
                    agendamentoCancelado = ag;
                    break;
                }
            }

            // Cancela o agendamento no banco
            AgendamentoDAO.cancelarAgendamento(agendamentoId);

            // Cria notificação de cancelamento para os funcionários
            if (agendamentoCancelado != null) {
                String nomeCliente = agendamentoCancelado.getCliente() != null 
                    ? agendamentoCancelado.getCliente().getNome() 
                    : agendamentoCancelado.getNomeClienteAvulso();
                
                String nomeFuncionario = agendamentoCancelado.getFuncionario() != null 
                    ? agendamentoCancelado.getFuncionario().getNome() 
                    : "Funcionário";

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String horaFormatada = agendamentoCancelado.getHoraSelecionada().format(timeFormatter);

                String mensagem = "Agendamento CANCELADO: " + nomeCliente + " às " + horaFormatada 
                    + " com " + nomeFuncionario;

                com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
                notificacao.setMensagem(mensagem);
                notificacao.setDataEnvio(new java.util.Date());
                notificacao.setAgendamento(agendamentoCancelado);
                notificacao.setCliente(null); // NULL = notificação para funcionários/admins

                com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
                notificacaoDAO.salvar(notificacao);
            }

            popularMeusAgendamentos(); // Refresh the list
            exibirAlerta("success", "Agendamento cancelado com sucesso!");
            PrimeFaces.current().ajax().update("@form");
        } catch (Exception e) {
            exibirAlerta("error", "Não foi possível cancelar o agendamento.");
            e.printStackTrace();
        }
    }

    public void aoMudarTipoAgendamento() {
        if ("proprio".equals(tipoAgendamento)) {
            nomeClienteOutro = null;
        }
    }

    public void aoSelecionarData() {
        this.funcionarioId = null;
        this.horaSelecionada = null;
        this.horariosDisponiveis.clear();
        aoSelecionarServico();
    }

    public void aoSelecionarFuncionario() {
        gerarHorariosDisponiveis();
        aoSelecionarServico();
    }

    public void aoSelecionarServico() {
        this.valorTotal = 0.0;
        if (servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty()) {
            for (Long servicoId : servicosSelecionadosIds) {
                for (Servicos servico : servicosDisponiveis) {
                    if (servico.getId().equals(servicoId)) {
                        this.valorTotal += servico.getPreco();
                        break;
                    }
                }
            }
        }
    }

    public boolean isDataDesabilitada() {
        return "outro".equals(tipoAgendamento) && (nomeClienteOutro == null || nomeClienteOutro.trim().isEmpty());
    }

    public boolean isFuncionarioDesabilitado() {
        return dataSelecionada == null;
    }

    public boolean isAgendamentoDesabilitado() {
        return funcionarioId == null;
    }

    public void gerarHorariosDisponiveis() {
		horariosDisponiveis.clear();

		if (funcionarioId == null || dataSelecionada == null) {
			return;
		}

		int totalMinutos = 0;
		if (servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty()) {
			for (Long servicoId : servicosSelecionadosIds) {
				Servicos servico = ServicosDAO.buscarPorId(servicoId);
				if (servico != null) {
					totalMinutos += servico.getMinutos();
				}
			}
		}
		if (totalMinutos == 0) {
			totalMinutos = 30;
		}
		int numeroDeSlotsNecessarios = (totalMinutos + 29) / 30;
		if (numeroDeSlotsNecessarios == 0) {
			numeroDeSlotsNecessarios = 1;
		}

		Funcionario funcionarioSelecionado = new Funcionario();
        funcionarioSelecionado.setId(funcionarioId);

		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return;
		}

		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioId, dataSelecionada, null);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalDateTime agora = LocalDateTime.now();
		LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();

		for (Horario periodo : horariosFuncionario) {
			LocalTime horaInicialPeriodo = periodo.getHoraInicial();
			LocalTime horaFinalPeriodo = periodo.getHoraFinal();

			long duracaoTotalServico = (long) numeroDeSlotsNecessarios * 30;
			LocalTime ultimoHorarioPossivel = horaFinalPeriodo.minusMinutes(duracaoTotalServico);

			LocalTime horaAtual = horaInicialPeriodo;

			while (!horaAtual.isAfter(ultimoHorarioPossivel)) {
				
				boolean isHoje = dataSelecionadaLocalDate.isEqual(LocalDate.now());
				boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(agora.toLocalTime());

				if (isHorarioFuturo) {
					boolean todosSlotsLivres = true;
					for (int i = 0; i < numeroDeSlotsNecessarios; i++) {
						LocalTime slotParaVerificar = horaAtual.plusMinutes((long) i * 30);
						if (horariosOcupados.contains(slotParaVerificar)) {
							todosSlotsLivres = false;
							break;
						}
					}

					if (todosSlotsLivres) {
						String horaFormatadaLoop = horaAtual.format(formatter);
						if (!horariosDisponiveis.contains(horaFormatadaLoop)) {
							horariosDisponiveis.add(horaFormatadaLoop);
						}
					}
				}
				horaAtual = horaAtual.plusMinutes(30);
			}
		}
		java.util.Collections.sort(horariosDisponiveis);
	}
}

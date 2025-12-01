package com.barbersys.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIInput;
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
    private Map<Long, Boolean> servicosSelecionadosMap = new HashMap<>();

    // --- Fontes de Dados para a Tela ---
    private List<Funcionario> funcionariosDisponiveis;
    private List<Servicos> servicosDisponiveis;
    private List<String> horariosDisponiveis = new ArrayList<>();
    private List<Agendamento> meusAgendamentos = new ArrayList<>();

    // --- Controle de Lógica e UI ---
    private Date today;
    private Long agendamentoIdParaCancelar;
    private List<LocalDate> datasDesabilitadas = new ArrayList<>();
    private List<Date> datasDesabilitadasDate = new ArrayList<>();
    
    // --- Controle de Steps ---
    private boolean agendamentoIniciado = false;
    private int activeIndex = 0;
    private String observacoes;
    
    // --- Filtro e exibição de serviços ---
    private String filtroServico = "";
    private List<Servicos> servicosFiltrados = new ArrayList<>();
    
    // --- Total de tempo dos serviços selecionados ---
    private int tempoTotalServicos = 0;


    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

    @PostConstruct
    public void init() {
        try {
            // Define a data de hoje para o datepicker
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            this.today = cal.getTime();

            // Carrega dados essenciais da página
            this.funcionariosDisponiveis = FuncionarioDAO.buscarTodosFuncionarios();
            this.servicosDisponiveis = ServicosDAO.buscarTodos();
            this.servicosFiltrados = new ArrayList<>(this.servicosDisponiveis);

            // Carrega dados específicos do cliente logado
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
            
            if (usuarioLogado != null && usuarioLogado.getId() != null) {
                // Busca o cliente no banco para garantir dados atualizados
                Cliente cliente = ClienteDAO.buscarClientePorUsuarioId(usuarioLogado.getId());
                
                if (cliente != null) {
                    // Se encontrou um cliente, popula os dados na tela
                    usuarioLogado.setClienteAssociado(cliente); // Garante que a sessão está atualizada
                    this.nomeClienteLogado = cliente.getNome();
                    popularMeusAgendamentos(); // Ponto central do fix: carrega os agendamentos
                } else {
                    // Usuário logado não é um cliente
                    this.nomeClienteLogado = "Acesso Restrito";
                    this.meusAgendamentos.clear();
                }
            } else {
                // Nenhum usuário logado
                this.nomeClienteLogado = "Visitante";
                this.meusAgendamentos.clear();
            }
        } catch (Exception e) {
            System.err.println("ERRO crítico no init() do AgendamentoClienteController: " + e.getMessage());
            e.printStackTrace();
            this.meusAgendamentos = new ArrayList<>(); // Garante que a lista não seja nula em caso de erro
        }
    }

    // --- Ações e Lógica ---
    
    public void agendar() {
        // VALIDAÇÃO: Não permite agendar para datas passadas
        LocalDate dataAgendamento = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();
        LocalDate hoje = LocalDate.now();
        
        if (dataAgendamento.isBefore(hoje)) {
            exibirAlerta("error", "Não é possível agendar para datas passadas!");
            return;
        }
        
        // Se for hoje, valida se o horário já passou
        if (dataAgendamento.isEqual(hoje)) {
            LocalTime horarioAgendamento = LocalTime.parse(horaSelecionada);
            LocalTime horarioAtual = LocalTime.now();
            
            if (horarioAgendamento.isBefore(horarioAtual) || horarioAgendamento.equals(horarioAtual)) {
                exibirAlerta("error", "Não é possível agendar para horários que já passaram!");
                return;
            }
        }
        
        Agendamento novoAgendamento = new Agendamento();
        novoAgendamento.setDataCriado(dataSelecionada);
        novoAgendamento.setHoraSelecionada(LocalTime.parse(horaSelecionada));
        novoAgendamento.setStatus("A");
        novoAgendamento.setPago("N");
        
        // Define observações (prioriza observação do agendamento, senão usa do cliente)
        if (observacoes != null && !observacoes.trim().isEmpty()) {
            novoAgendamento.setObservacoes(observacoes);
        } else {
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
            if (usuarioLogado != null && usuarioLogado.getClienteAssociado() != null) {
                Cliente clienteLogado = usuarioLogado.getClienteAssociado();
                if (clienteLogado.getObservacoes() != null && !clienteLogado.getObservacoes().trim().isEmpty()) {
                    novoAgendamento.setObservacoes(clienteLogado.getObservacoes());
                }
            }
        }

        Funcionario func = new Funcionario();
        func.setId(funcionarioId);
        novoAgendamento.setFuncionario(func);

        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        if (usuarioLogado != null) {
            Cliente clienteLogado = ClienteDAO.buscarClientePorUsuarioId(usuarioLogado.getId());
            novoAgendamento.setCliente(clienteLogado);
            novoAgendamento.setTipoCadastro("A");
        } else {
            exibirAlerta("error", "Você precisa estar logado para agendar!");
            return;
        }

        try {
            AgendamentoDAO.salvar(novoAgendamento, servicosSelecionadosIds);

            // INÍCIO DA LÓGICA DE NOTIFICAÇÃO NO BANCO
            String nomeCliente = nomeClienteLogado;
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
            
            // A atualização do formulário é feita pelo update= do p:commandButton no XHTML
            exibirAlerta("success", "Agendamento realizado com sucesso!");

        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Ocorreu um erro ao salvar o agendamento.");
        }
    }

    public void iniciarAgendamento() {
        try {
            this.agendamentoIniciado = true;
            this.activeIndex = 0;
            
            // Força atualização do componente steps
            PrimeFaces.current().ajax().update("form:bookingAreaPanel");
        } catch (Exception e) {
            System.err.println("ERRO em iniciarAgendamento(): " + e.getMessage());
            e.printStackTrace();
            exibirAlerta("error", "Erro ao iniciar agendamento");
        }
    }
    
    public void proximoPasso() {
        try {
            // Debug: Mostra estado atual antes de validar
            System.out.println("=== AVANÇANDO DO PASSO " + activeIndex + " ===");
            System.out.println("funcionarioId: " + funcionarioId);
            System.out.println("dataSelecionada: " + dataSelecionada);
            System.out.println("servicosSelecionadosIds size: " + (servicosSelecionadosIds != null ? servicosSelecionadosIds.size() : "null"));
            
            // Validações antes de avançar
            if (activeIndex == 0) {
                // Validar Passo 1: Cliente e Funcionário
                
                if (funcionarioId == null) {
                    exibirAlerta("warning", "Por favor, selecione um funcionário.");
                    return;
                }
                // Calcula datas desabilitadas quando sair do passo 1 (otimizado)
                calcularDatasDesabilitadas();
                
            } else if (activeIndex == 1) {
                // Validar Passo 2: Data e Serviços
                
                if (dataSelecionada == null) {
                    exibirAlerta("warning", "Por favor, selecione uma data.");
                    return;
                }
                if (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty()) {
                    exibirAlerta("warning", "Por favor, selecione ao menos um serviço.");
                    return;
                }
                // Carregar horários disponíveis para o próximo passo
                gerarHorariosDisponiveis();
                
                // Verifica se há horários disponíveis
                if (horariosDisponiveis.isEmpty()) {
                    exibirAlerta("warning", "Não há horários disponíveis para esta data com os serviços selecionados.");
                    return;
                }
            }
            
            if (activeIndex < 2) {
                activeIndex++;
                System.out.println("=== AVANÇOU PARA PASSO " + activeIndex + " ===");
                // NÃO FAZ UPDATE AQUI - o update é feito pelo botão no XHTML
            }
        } catch (Exception e) {
            System.err.println("ERRO em proximoPasso(): " + e.getMessage());
            e.printStackTrace();
            exibirAlerta("error", "Erro ao avançar para o próximo passo");
        }
    }
    
    public void passoAnterior() {
        try {
            if (activeIndex > 0) {
                activeIndex--;
                
                // Debug: Mostra estado das variáveis ao voltar
                System.out.println("=== VOLTANDO PARA PASSO " + activeIndex + " ===");
                System.out.println("funcionarioId: " + funcionarioId);
                System.out.println("dataSelecionada: " + dataSelecionada);
                System.out.println("servicosSelecionadosIds size: " + (servicosSelecionadosIds != null ? servicosSelecionadosIds.size() : "null"));
                System.out.println("horaSelecionada: " + horaSelecionada);
                
                // NÃO FAZ UPDATE AQUI - o update é feito pelo botão no XHTML
            }
        } catch (Exception e) {
            System.err.println("ERRO em passoAnterior(): " + e.getMessage());
            e.printStackTrace();
            exibirAlerta("error", "Erro ao voltar ao passo anterior");
        }
    }
    
    /**
     * Verifica se pode avançar para o próximo passo
     */
    public boolean isPodeAvancarPasso() {
        try {
            if (activeIndex == 0) {
                // Passo 1: Sempre precisa ter funcionário selecionado
                return funcionarioId != null;
            } else if (activeIndex == 1) {
                // Passo 2: Deve ter data e serviços selecionados
                return dataSelecionada != null && servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty();
            }
            return false;
        } catch (Exception e) {
            System.err.println("ERRO em isPodeAvancarPasso(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica se pode finalizar o agendamento
     */
    public boolean isPodeAgendar() {
        try {
            if (activeIndex != 2) {
                return false;
            }
            // Deve ter funcionário, data, serviços e horário
            return funcionarioId != null 
                && dataSelecionada != null 
                && servicosSelecionadosIds != null
                && !servicosSelecionadosIds.isEmpty() 
                && horaSelecionada != null 
                && !horaSelecionada.trim().isEmpty();
        } catch (Exception e) {
            System.err.println("ERRO em isPodeAgendar(): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void limparFormulario() {
        this.dataSelecionada = null;
        this.funcionarioId = null;
        this.observacoes = null;
        this.agendamentoIniciado = false;
        this.activeIndex = 0;
        this.horaSelecionada = null;
        this.servicosSelecionadosIds.clear();
        this.servicosSelecionadosMap.clear();
        this.horariosDisponiveis.clear();
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
            System.out.println("=== CANCELAMENTO DE AGENDAMENTO (CLIENTE) ===");
            System.out.println("Agendamento ID: " + agendamentoId);
            
            // Busca o agendamento completo antes de cancelar
            Agendamento agendamentoCancelado = null;
            for (Agendamento ag : meusAgendamentos) {
                if (ag.getId().equals(agendamentoId)) {
                    agendamentoCancelado = ag;
                    break;
                }
            }

            if (agendamentoCancelado == null) {
                exibirAlerta("error", "Agendamento não encontrado!");
                return;
            }

            // Verifica se o agendamento foi pago
            boolean agendamentoPago = "S".equals(agendamentoCancelado.getPago());
            System.out.println("Agendamento Pago: " + (agendamentoPago ? "SIM" : "NÃO"));

            if (agendamentoPago) {
                // CASO 1: Agendamento já foi pago → ESTORNAR
                System.out.println("🔄 Iniciando processo de ESTORNO...");
                
                // Verifica se o pagamento integra com o caixa
                boolean registrarNoCaixa = false;
                if (agendamentoCancelado.getPagamento() != null && agendamentoCancelado.getPagamento().getIntegraCaixa()) {
                    registrarNoCaixa = true;
                    System.out.println("✅ Pagamento integra com caixa - será registrado estorno");
                }

                if (registrarNoCaixa) {
                    // Verifica se o caixa está aberto
                    Date dataAtual = Date.from(java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                    List<com.barbersys.model.CaixaData> checkData = com.barbersys.dao.CaixaDataDAO.verificaExisteData(dataAtual);

                    if (checkData.isEmpty() || "I".equals(checkData.get(0).getStatus())) {
                        System.out.println("❌ Caixa fechado - não é possível estornar agora");
                        exibirAlerta("error", "O caixa do dia precisa estar aberto para cancelar um agendamento pago. Entre em contato com a barbearia.");
                        return;
                    }

                    // Registra estorno no caixa
                    com.barbersys.model.CaixaData caixaDataModel = checkData.get(0);
                    String horaAtualFormatada = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    
                    // Calcula valor total dos serviços
                    double totalGastoServicos = 0.0;
                    if (agendamentoCancelado.getServicos() != null) {
                        for (Servicos servico : agendamentoCancelado.getServicos()) {
                            totalGastoServicos += servico.getPreco();
                        }
                    }
                    System.out.println("💰 Valor do estorno: R$ " + totalGastoServicos);

                    // Cria movimentação de estorno no caixa
                    com.barbersys.model.ControleCaixa estornoCaixa = new com.barbersys.model.ControleCaixa();
                    estornoCaixa.setCaixaData(caixaDataModel);
                    estornoCaixa.setHoraAtual(horaAtualFormatada);
                    estornoCaixa.setData(dataAtual);
                    estornoCaixa.setMovimentacao("Saída de estorno");
                    estornoCaixa.setValor(-totalGastoServicos);
                    com.barbersys.dao.ControleCaixaDAO.salvar(estornoCaixa);
                    System.out.println("✅ Estorno registrado no caixa");
                }

                // Atualiza agendamento para ESTORNADO
                AgendamentoDAO.cancelarAgendamento(agendamentoId);
                AgendamentoDAO.atualizarInformacoesPagamento(
                    agendamentoId, 
                    "E",  // E = Estornado
                    agendamentoCancelado.getPagamento() != null ? agendamentoCancelado.getPagamento().getId() : null
                );
                System.out.println("✅ Agendamento marcado como ESTORNADO");
                
                exibirAlerta("success", "Agendamento cancelado e valor estornado com sucesso!");

            } else {
                // CASO 2: Agendamento NÃO foi pago → Cancelamento normal
                System.out.println("✅ Cancelamento normal (sem pagamento)");
                AgendamentoDAO.cancelarAgendamento(agendamentoId);
                exibirAlerta("success", "Agendamento cancelado com sucesso!");
            }

            // Cria notificação de cancelamento para funcionários/admin
            String nomeCliente = agendamentoCancelado.getCliente() != null 
                ? agendamentoCancelado.getCliente().getNome() 
                : agendamentoCancelado.getNomeClienteAvulso();
            
            String nomeFuncionario = agendamentoCancelado.getFuncionario() != null 
                ? agendamentoCancelado.getFuncionario().getNome() 
                : "Funcionário";

            java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String horaFormatada = agendamentoCancelado.getHoraSelecionada().format(timeFormatter);

            String tipoMensagem = agendamentoPago ? "CANCELADO (ESTORNADO)" : "CANCELADO";
            String mensagem = "Agendamento " + tipoMensagem + ": " + nomeCliente + " às " + horaFormatada 
                + " com " + nomeFuncionario;

            com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
            notificacao.setMensagem(mensagem);
            notificacao.setDataEnvio(new java.util.Date());
            notificacao.setAgendamento(agendamentoCancelado);
            notificacao.setCliente(null); // NULL = notificação para funcionários/admins

            com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
            notificacaoDAO.salvar(notificacao);
            System.out.println("✅ Notificação enviada para admin/funcionários");

            popularMeusAgendamentos(); // Atualiza a lista
            System.out.println("=== CANCELAMENTO CONCLUÍDO ===");

        } catch (Exception e) {
            System.out.println("❌ ERRO ao cancelar agendamento: " + e.getMessage());
            e.printStackTrace();
            exibirAlerta("error", "Não foi possível cancelar o agendamento. Entre em contato com a barbearia.");
        }
    }

    public void aoSelecionarData() {
        try {
            // No fluxo de steps, não limpamos mais os serviços ao selecionar a data
            // Os serviços são selecionados no mesmo passo que a data
            this.horaSelecionada = null;
            
            // Não precisa gerar horários aqui, pois será feito no próximo passo
            // quando os serviços forem selecionados
        } catch (Exception e) {
            System.err.println("ERRO em aoSelecionarData(): " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void aoSelecionarFuncionario() {
        try {
            this.dataSelecionada = null;
            this.horaSelecionada = null;
            this.horariosDisponiveis.clear();
            this.servicosSelecionadosIds.clear();
            this.servicosSelecionadosMap.clear();
            
            // Não calcula datas desabilitadas aqui mais, será feito ao avançar do passo 1
            // calcularDatasDesabilitadas();
        } catch (Exception e) {
            System.err.println("ERRO em aoSelecionarFuncionario(): " + e.getMessage());
            e.printStackTrace();
        }
    }



    public void aoSelecionarHorario() {
        // Os serviços devem ser habilitados automaticamente pelo getter isAgendamentoDesabilitado
    }
    
    public void aoSelecionarServico() {
        try {
            // Sincroniza o Map com a lista de IDs
            servicosSelecionadosIds.clear();
            
            for (Map.Entry<Long, Boolean> entry : servicosSelecionadosMap.entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) {
                    servicosSelecionadosIds.add(entry.getKey());
                }
            }
            
            // No fluxo de steps, não recalculamos datas ou horários aqui
            // Os horários serão calculados apenas quando avançar para o passo 3
        } catch (Exception e) {
            System.err.println("ERRO em aoSelecionarServico(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Filtra serviços conforme o texto digitado
     */
    public void filtrarServicos() {
        try {
            if (filtroServico == null || filtroServico.trim().isEmpty()) {
                servicosFiltrados = new ArrayList<>(servicosDisponiveis);
            } else {
                servicosFiltrados.clear();
                String filtroLower = filtroServico.toLowerCase().trim();
                for (Servicos servico : servicosDisponiveis) {
                    if (servico.getNome().toLowerCase().contains(filtroLower)) {
                        servicosFiltrados.add(servico);
                    }
                }
            }
            PrimeFaces.current().ajax().update("form:servicosWrapper");
        } catch (Exception e) {
            System.err.println("ERRO em filtrarServicos(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Retorna o tempo total dos serviços selecionados em minutos
     */
    public int getTempoTotalMinutos() {
        int total = 0;
        if (servicosDisponiveis != null && servicosSelecionadosIds != null) {
            for (Servicos servico : servicosDisponiveis) {
                if (servicosSelecionadosIds.contains(servico.getId())) {
                    if (servico.getMinutos() != null) {
                        total += servico.getMinutos();
                    }
                }
            }
        }
        return total;
    }
    
    /**
     * Retorna o tempo total formatado (ex: "1h 30min" ou "45min")
     */
    public String getTempoTotalFormatado() {
        int totalMinutos = getTempoTotalMinutos();
        if (totalMinutos == 0) {
            return "0min";
        }
        
        int horas = totalMinutos / 60;
        int minutos = totalMinutos % 60;
        
        if (horas > 0 && minutos > 0) {
            return horas + "h " + minutos + "min";
        } else if (horas > 0) {
            return horas + "h";
        } else {
            return minutos + "min";
        }
    }
    
    /**
     * Formata a duração de um serviço individual
     */
    public String formatarDuracaoServico(Integer minutos) {
        if (minutos == null || minutos == 0) {
            return "";
        }
        
        int horas = minutos / 60;
        int min = minutos % 60;
        
        if (horas > 0 && min > 0) {
            return horas + "h " + min + "min";
        } else if (horas > 0) {
            return horas + "h";
        } else {
            return min + "min";
        }
    }
    
    /**
     * Lista de serviços para exibição (filtrados ou todos)
     */
    public List<Servicos> getServicosParaExibir() {
        if (filtroServico != null && !filtroServico.trim().isEmpty()) {
            return servicosFiltrados;
        }
        return servicosDisponiveis;
    }
    
    /**
	 * Verifica se uma data tem horários disponíveis (sem considerar serviços)
	 * Otimizado para evitar loop infinito
	 */
	private boolean temHorariosDisponiveisNaData(Date data, Funcionario funcionario) {
		// Busca os horários de trabalho do funcionário
		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionario);
		
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return false;
		}
		
		// Verifica qual dia da semana
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(data);
		int diaSemana = cal.get(java.util.Calendar.DAY_OF_WEEK);
		
		// Filtra horários válidos para este dia
		List<Horario> horariosValidosParaDia = new ArrayList<>();
		for (Horario h : horariosFuncionario) {
			boolean trabalhaNesteDay = false;
			
			switch (diaSemana) {
				case java.util.Calendar.SUNDAY:
					trabalhaNesteDay = h.getDomingo() != null && h.getDomingo();
					break;
				case java.util.Calendar.MONDAY:
					trabalhaNesteDay = h.getSegunda() != null && h.getSegunda();
					break;
				case java.util.Calendar.TUESDAY:
					trabalhaNesteDay = h.getTerca() != null && h.getTerca();
					break;
				case java.util.Calendar.WEDNESDAY:
					trabalhaNesteDay = h.getQuarta() != null && h.getQuarta();
					break;
				case java.util.Calendar.THURSDAY:
					trabalhaNesteDay = h.getQuinta() != null && h.getQuinta();
					break;
				case java.util.Calendar.FRIDAY:
					trabalhaNesteDay = h.getSexta() != null && h.getSexta();
					break;
				case java.util.Calendar.SATURDAY:
					trabalhaNesteDay = h.getSabado() != null && h.getSabado();
					break;
			}
			
			if (trabalhaNesteDay) {
				horariosValidosParaDia.add(h);
			}
		}
		
		if (horariosValidosParaDia.isEmpty()) {
			return false;
		}
		
		// Busca horários ocupados
		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionario.getId(), data, null);
		
		LocalDate dataLocal = new java.sql.Date(data.getTime()).toLocalDate();
		LocalDateTime agora = LocalDateTime.now();
		
		// Otimização: Verifica se há pelo menos UM slot de 30min livre
		// Não precisa considerar a duração dos serviços neste momento
		for (Horario periodo : horariosValidosParaDia) {
			LocalTime horaAtual = periodo.getHoraInicial();
			LocalTime horaFinal = periodo.getHoraFinal().minusMinutes(30);
			
			// Limita iteração para evitar loop infinito (máximo 100 slots = 50 horas)
			int maxIteracoes = 100;
			int iteracao = 0;
			
			// Proteção contra overflow de horário
			while ((horaAtual.isBefore(horaFinal) || horaAtual.equals(horaFinal)) 
					&& iteracao < maxIteracoes) {
				iteracao++;
				
				boolean isHoje = dataLocal.isEqual(LocalDate.now());
				boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(agora.toLocalTime());
				
				if (isHorarioFuturo && !horariosOcupados.contains(horaAtual)) {
					// Encontrou pelo menos 1 horário disponível
					return true;
				}
				
				LocalTime proximaHora = horaAtual.plusMinutes(30);
				
				// Se deu volta (passou de 23:59 para 00:00), para o loop
				if (proximaHora.compareTo(horaAtual) < 0) {
					break;
				}
				
				horaAtual = proximaHora;
			}
		}
		
		return false; // Nenhum horário disponível
	}
	
	/**
	 * Calcula as datas que devem ser desabilitadas no datepicker
	 * baseado no funcionário selecionado (SEM considerar serviços)
	 * Calcula para os próximos 3 anos (rápido e suficiente)
	 */
	public void calcularDatasDesabilitadas() {
		datasDesabilitadas.clear();
		datasDesabilitadasDate.clear();
		
		if (funcionarioId == null) {
			return;
		}
		
		// Busca o funcionário selecionado
		Funcionario funcionarioSelecionado = null;
		if (funcionariosDisponiveis != null) {
			for (Funcionario f : funcionariosDisponiveis) {
				if (f.getId().equals(funcionarioId)) {
					funcionarioSelecionado = f;
					break;
				}
			}
		}
		
		if (funcionarioSelecionado == null) {
			return;
		}
		
		// Busca os horários de trabalho do funcionário
		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			// Se não tem horários, todas as datas futuras ficam desabilitadas
			return;
		}
		
		// Calcula para os próximos 3 anos (1095 dias)
		LocalDate hoje = LocalDate.now();
		for (int i = 0; i <= 1095; i++) {
			LocalDate dataVerificar = hoje.plusDays(i);
			Date dataUtil = Date.from(dataVerificar.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
			
			// Verifica se está bloqueada por restrição
			if (com.barbersys.dao.RestricaoDataDAO.isDataBloqueada(dataUtil, funcionarioSelecionado.getId())) {
				datasDesabilitadas.add(dataVerificar);
				datasDesabilitadasDate.add(dataUtil);
				continue;
			}
			
			// Verifica se o funcionário trabalha neste dia da semana
			int diaSemana = dataVerificar.getDayOfWeek().getValue(); // 1=Segunda, 7=Domingo
			boolean trabalhaNesteDay = false;
			
			for (Horario h : horariosFuncionario) {
				boolean trabalha = false;
				
				switch (diaSemana) {
					case 7: // Domingo
						trabalha = h.getDomingo() != null && h.getDomingo();
							break;
					case 1: // Segunda
						trabalha = h.getSegunda() != null && h.getSegunda();
							break;
					case 2: // Terça
						trabalha = h.getTerca() != null && h.getTerca();
							break;
					case 3: // Quarta
						trabalha = h.getQuarta() != null && h.getQuarta();
							break;
					case 4: // Quinta
						trabalha = h.getQuinta() != null && h.getQuinta();
							break;
					case 5: // Sexta
						trabalha = h.getSexta() != null && h.getSexta();
							break;
					case 6: // Sábado
						trabalha = h.getSabado() != null && h.getSabado();
							break;
				}
				
				if (trabalha) {
					trabalhaNesteDay = true;
					break;
				}
			}
			
			// Se não trabalha neste dia, desabilita
			if (!trabalhaNesteDay) {
				datasDesabilitadas.add(dataVerificar);
				datasDesabilitadasDate.add(dataUtil);
				continue;
			}
			
			// Verifica se tem horários disponíveis nesta data (otimizado)
			if (!temHorariosDisponiveisNaData(dataUtil, funcionarioSelecionado)) {
				datasDesabilitadas.add(dataVerificar);
				datasDesabilitadasDate.add(dataUtil);
			}
		}
	}
	
	/**
	 * Retorna string JavaScript com array de datas desabilitadas
	 * Formato: "2025-11-13,2025-11-14,2025-11-17"
	 */
	public String getDatasDesabilitadasString() {
		if (datasDesabilitadas.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		
		for (int i = 0; i < datasDesabilitadas.size(); i++) {
			sb.append(datasDesabilitadas.get(i).format(formatter));
			if (i < datasDesabilitadas.size() - 1) {
				sb.append(",");
			}
		}
		
		return sb.toString();
	}
    
    public double getValorTotal() {
        double total = 0.0;
        
        if (servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty() && servicosDisponiveis != null) {
            for (Long servicoId : servicosSelecionadosIds) {
                for (Servicos servico : servicosDisponiveis) {
                    if (servico.getId().equals(servicoId)) {
                        if (servico.getPreco() != null) {
                            total += servico.getPreco();
                        }
                        break;
                    }
                }
            }
        }
        
        return total;
    }
    
    public String getValorTotalFormatado() {
        double total = getValorTotal();
        return String.format("%.2f", total);
    }

    public boolean isDataDesabilitada() {
        // NUNCA desabilitar - o controle é feito pelo activeIndex e validações
        return false;
    }

    public boolean isFuncionarioDesabilitado() {
        // NUNCA desabilitar
        return false;
    }

    public boolean isHorarioDesabilitado() {
        // NUNCA desabilitar - o dropdown mostra "Nenhum horário disponível" quando vazio
        return false;
    }
    
    /**
     * Serviços SEMPRE habilitados
     */
    public boolean isAgendamentoDesabilitado() {
        return false;
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
        
        // Verifica se a data está bloqueada por restrição
        if (com.barbersys.dao.RestricaoDataDAO.isDataBloqueada(dataSelecionada, funcionarioId)) {
            return; // Data bloqueada, não mostra horários
        }

		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return;
		}
		
		// Verifica qual dia da semana é a data selecionada
		java.util.Calendar cal = java.util.Calendar.getInstance();
		cal.setTime(dataSelecionada);
		int diaSemana = cal.get(java.util.Calendar.DAY_OF_WEEK);
		
		// Filtra apenas os horários que trabalham neste dia da semana
		List<Horario> horariosValidosParaDia = new ArrayList<>();
		for (Horario h : horariosFuncionario) {
			boolean trabalhaNesteDay = false;
			
			switch (diaSemana) {
				case java.util.Calendar.SUNDAY:
					trabalhaNesteDay = h.getDomingo() != null && h.getDomingo();
					break;
				case java.util.Calendar.MONDAY:
					trabalhaNesteDay = h.getSegunda() != null && h.getSegunda();
					break;
				case java.util.Calendar.TUESDAY:
					trabalhaNesteDay = h.getTerca() != null && h.getTerca();
					break;
				case java.util.Calendar.WEDNESDAY:
					trabalhaNesteDay = h.getQuarta() != null && h.getQuarta();
					break;
				case java.util.Calendar.THURSDAY:
					trabalhaNesteDay = h.getQuinta() != null && h.getQuinta();
					break;
				case java.util.Calendar.FRIDAY:
					trabalhaNesteDay = h.getSexta() != null && h.getSexta();
					break;
				case java.util.Calendar.SATURDAY:
					trabalhaNesteDay = h.getSabado() != null && h.getSabado();
					break;
			}
			
			if (trabalhaNesteDay) {
				horariosValidosParaDia.add(h);
			}
		}
		
		// Se não trabalha neste dia, retorna vazio
		if (horariosValidosParaDia.isEmpty()) {
			return;
		}

		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioId, dataSelecionada, null);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalDateTime agora = LocalDateTime.now();
		LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();

		// Usa apenas os horários válidos para o dia da semana
		for (Horario periodo : horariosValidosParaDia) {
			LocalTime horaInicialPeriodo = periodo.getHoraInicial();
			LocalTime horaFinalPeriodo = periodo.getHoraFinal();

			LocalTime horaAtual = horaInicialPeriodo;
			
			// Proteção contra loop infinito - máximo 100 iterações por período
			int maxIteracoes = 100;
			int iteracao = 0;

			// CORREÇÃO: Para evitar overflow de horário (passar de 23:59 para 00:00)
			// Usa isBefore e equals ao invés de isAfter
			while ((horaAtual.isBefore(horaFinalPeriodo) || horaAtual.equals(horaFinalPeriodo)) 
					&& iteracao < maxIteracoes 
					&& horaAtual.compareTo(horaInicialPeriodo) >= 0) {
				iteracao++;
				
				boolean isHoje = dataSelecionadaLocalDate.isEqual(LocalDate.now());
				boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(agora.toLocalTime());

				if (isHorarioFuturo) {
					// Calcula quando o serviço terminaria se começasse neste horário
					// CORREÇÃO: Usa totalMinutos ao invés de (slots-1)*30
					LocalTime horarioTermino = horaAtual.plusMinutes(totalMinutos);
					
					// Verifica se o serviço termina dentro do período de trabalho
					// E não ultrapassa o limite (evita overflow para 00:00, 01:00, etc)
					if (!horarioTermino.isAfter(horaFinalPeriodo) 
							&& horarioTermino.compareTo(horaAtual) >= 0) {
						// Verifica se todos os slots necessários estão livres
						boolean todosSlotsLivres = true;
						for (int i = 0; i < numeroDeSlotsNecessarios; i++) {
							LocalTime slotParaVerificar = horaAtual.plusMinutes((long) i * 30);
							
							// Verifica se o slot não deu overflow (passou da meia-noite)
							if (slotParaVerificar.compareTo(horaAtual) < 0) {
								// Slot deu volta (passou de 23:59 para 00:00)
								todosSlotsLivres = false;
								break;
							}
							
							// Verifica se o slot ultrapassa o horário final
							if (slotParaVerificar.isAfter(horaFinalPeriodo)) {
								todosSlotsLivres = false;
								break;
							}
							
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
				}
				
				// Avança 30 minutos
				LocalTime proximaHora = horaAtual.plusMinutes(30);
				
				// Se deu volta (passou de 23:59 para 00:00), para o loop
				if (proximaHora.compareTo(horaAtual) < 0) {
					break;
				}
				
				horaAtual = proximaHora;
			}
		}
		java.util.Collections.sort(horariosDisponiveis);
	}
	
}
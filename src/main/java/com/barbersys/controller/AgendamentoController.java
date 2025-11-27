package com.barbersys.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import com.barbersys.dao.AgendamentoDAO;
import com.barbersys.dao.CaixaDataDAO;
import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.ControleCaixaDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.PagamentoDAO;
import com.barbersys.dao.ServicosDAO;
import com.barbersys.model.Agendamento;
import com.barbersys.model.CaixaData;
import com.barbersys.model.Cliente;
import com.barbersys.model.ControleCaixa;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.model.Pagamento;
import com.barbersys.model.Servicos;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class AgendamentoController implements Serializable {

	private static final long serialVersionUID = 1L;
	private String tipoCadastro = "A";
	private Date today = new Date();
	private String nomeCliente;
	private Long clienteId;
	private String nomeClienteFiltro;
	private String nomeFuncionario;
	private String nomeFuncionarioFiltro;
	private Long idPagamento;
	private Integer passos = 0;
	private Double totalGastoServicos = 0.0;
	private Date dataSelecionada;
	private Date dataFiltro = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
	private String horaSelecionada;
	private String statusSelecionado = "";
	private Agendamento agendamentoModel = new Agendamento(); // sempre inicializado para evitar null em binds como sexo
	private Cliente clienteModel = new Cliente();
	private ControleCaixa controleCaixaModel = new ControleCaixa();
	private LazyDataModel<Agendamento> lstAgendamentos;
	private String editarModel;
	private String verificaPagamento = "A";

	private DateTimeFormatter horaFormatada = DateTimeFormatter.ofPattern("HH:mm");

	private List<Funcionario> lstFuncionario;
	private List<Cliente> lstCliente;
	private List<Servicos> lstServico;
	private List<Pagamento> lstPagamento;
	private List<String> horariosDisponiveis = new ArrayList<>();
	private List<Long> servicosSelecionadosIds;
	private List<Funcionario> lstFuncionarioDisponivel;
	private List<LocalDate> datasDesabilitadas = new ArrayList<>();
	private List<Date> datasDesabilitadasDate = new ArrayList<>();
	
	// Filtro de serviços
	private String filtroServico = "";
	private List<Servicos> servicosFiltrados = new ArrayList<>();
	
	// Propriedades para tela agendamentoCliente
	private String tipoAgendamento = "proprio";
	private String nomeClienteOutro;
	private String nomeClienteLogado;
	private boolean agendamentoIniciado = false;
	private int activeIndex = 0;
	private String observacoes;
	private String sexoSelecionado;
	private Long funcionarioId;
	private List<Funcionario> funcionariosDisponiveis = new ArrayList<>();
	private List<Servicos> servicosDisponiveis = new ArrayList<>();
	private java.util.Map<Long, Boolean> servicosSelecionadosMap = new java.util.HashMap<>();
	private List<Agendamento> meusAgendamentos = new ArrayList<>();

	@PostConstruct
	public void init() {
		// Define today como meia-noite do dia atual para o datepicker funcionar corretamente
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		this.today = cal.getTime();
		
		// Garante que agendamentoModel está sempre inicializado
		if (agendamentoModel == null) {
			agendamentoModel = new Agendamento();
			agendamentoModel.setObservacoes("");
			agendamentoModel.setServicos(new ArrayList<>());
		}
		// Inicializa agendamentoModel para evitar NullPointerException
		if (agendamentoModel == null) {
			agendamentoModel = new Agendamento();
		}
		agendamentoModel.setObservacoes("");
		agendamentoModel.setServicos(new ArrayList<>());
		
		lstAgendamentos = new LazyDataModel<Agendamento>() {
			private static final long serialVersionUID = 1L;
			@Override
			public List<Agendamento> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
				                List<Agendamento> agendamentos = AgendamentoDAO.buscarAgendamentos(nomeClienteFiltro, nomeFuncionarioFiltro, statusSelecionado, dataFiltro, first, pageSize); 				this.setRowCount(count(filterBy));
				return agendamentos;
			}
			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return AgendamentoDAO.agendamentoCount(nomeClienteFiltro, nomeFuncionarioFiltro, statusSelecionado, dataFiltro);
			}
			@Override
			public String getRowKey(Agendamento agendamento) {
				return agendamento.getId() != null ? agendamento.getId().toString() : "";
			}
			@Override
			public Agendamento getRowData(String rowKey) {
				try {
					Long id = Long.parseLong(rowKey);
					List<Agendamento> all = (List<Agendamento>) getWrappedData();
					for (Agendamento ag : all) {
						if (ag.getId().equals(id)) return ag;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};

		lstFuncionario = FuncionarioDAO.buscarTodosFuncionarios();
		lstCliente = ClienteDAO.buscarTodosClientes();
		lstServico = ServicosDAO.buscarTodos();
		servicosFiltrados = new ArrayList<>(lstServico);
		lstPagamento = PagamentoDAO.buscarTodos();
		lstFuncionarioDisponivel = new ArrayList<>(lstFuncionario);
		
		// Inicializa listas para tela agendamentoCliente
		funcionariosDisponiveis = new ArrayList<>(lstFuncionario);
		servicosDisponiveis = new ArrayList<>(lstServico);
		
		// Busca o nome do cliente logado para a tela agendamentoCliente
		try {
			com.barbersys.model.Usuario usuarioLogado = (com.barbersys.model.Usuario) FacesContext
					.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
			
			if (usuarioLogado != null && usuarioLogado.getClienteAssociado() != null) {
				nomeClienteLogado = usuarioLogado.getClienteAssociado().getNome();
			} else if (usuarioLogado != null) {
				nomeClienteLogado = usuarioLogado.getLogin(); // Fallback para o login
			}
		} catch (Exception e) {
			System.err.println("Erro ao buscar cliente logado: " + e.getMessage());
		}
	}

	public void calculaValorServicos() {
		totalGastoServicos = 0.0;
		
		// Se estamos no modal (servicosSelecionadosIds tem dados), usa ele
		if (servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty()) {
			for (Long servicoId : servicosSelecionadosIds) {
				Servicos servico = ServicosDAO.buscarPorId(servicoId);
				if (servico != null) {
					totalGastoServicos += servico.getPreco();
				}
			}
		}
		// Caso contrário, usa o agendamentoModel (para quando já tem agendamento)
		else if (agendamentoModel != null && agendamentoModel.getServicos() != null) {
			for (Servicos item : agendamentoModel.getServicos()) {
				totalGastoServicos += item.getPreco();
			}
		}
	}

	private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

	public void confirmaPagamentoPedido() {
		if (idPagamento == null) {
			exibirAlerta("warning", "Selecione uma forma de pagamento!");
			return;
		}

		Pagamento pagamentoSelecionado = PagamentoDAO.buscarPorId(idPagamento);

		if (pagamentoSelecionado != null && pagamentoSelecionado.getIntegraCaixa()) {
			Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
			List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataAtual);

			if (checkData.isEmpty() || checkData.get(0).getStatus().equals("I")) {
				exibirAlerta("error", "Caixa precisa ser aberto para esta forma de pagamento!");
				return;
			}

			CaixaData caixaDataModel = checkData.get(0);
			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			
			controleCaixaModel = new ControleCaixa();
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataAtual);
			controleCaixaModel.setMovimentacao("Entrada automática");
			controleCaixaModel.setValor(totalGastoServicos);
			ControleCaixaDAO.salvar(controleCaixaModel);
		}

		// Atualiza o status de pagamento e o método de pagamento do agendamento
		AgendamentoDAO.atualizarInformacoesPagamento(agendamentoModel.getId(), "S", idPagamento);
		agendamentoModel.setPago("S");
		agendamentoModel.setPagamento(pagamentoSelecionado);

		exibirAlerta("success", "Pagamento confirmado com sucesso!");
		PrimeFaces.current().ajax().update("form:attCaixa"); 
	}

	public void finalizarPedido() {
		try {
			// Valida se o pagamento foi efetuado antes de finalizar
			if ("N".equals(agendamentoModel.getPago())) {
				exibirAlerta("warning", "Confirme o pagamento antes de finalizar o agendamento!");
				return;
			}

			// Altera o status para Finalizado e atualiza no banco
			agendamentoModel.setStatus("F");
			List<Long> servicosIds = new ArrayList<>();
			for(Servicos servico : agendamentoModel.getServicos()){
				servicosIds.add(servico.getId());
			}
			AgendamentoDAO.atualizar(agendamentoModel, servicosIds);
			
			// Cria notificação para o cliente avaliar o serviço (se houver cliente cadastrado)
			if (agendamentoModel.getCliente() != null && agendamentoModel.getCliente().getId() != null) {
				String nomeFuncionario = agendamentoModel.getFuncionario() != null 
					? agendamentoModel.getFuncionario().getNome() 
					: "Funcionário";

				String mensagem = "Seu atendimento com " + nomeFuncionario 
					+ " foi finalizado! Avalie o serviço prestado";

				com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
				notificacao.setMensagem(mensagem);
				notificacao.setDataEnvio(new java.util.Date());
				notificacao.setAgendamento(agendamentoModel);
				notificacao.setCliente(agendamentoModel.getCliente());

				com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
				notificacaoDAO.salvar(notificacao);
			}
			
			exibirAlerta("success", "Agendamento finalizado com sucesso!");
			PrimeFaces.current().ajax().update("form");

		} catch (Exception e) {
			exibirAlerta("error", "Ocorreu um erro ao finalizar o agendamento!");
			e.printStackTrace();
		}
	}

	public void estornarAgendamento() {
	    try {
	        // 1. Validações
	        if (agendamentoModel == null || !"S".equals(agendamentoModel.getPago())) {
	            exibirAlerta("error", "Este agendamento não pode ser estornado.");
	            return;
	        }

	        // 2. Verifica se o estorno deve impactar o caixa
	        boolean registrarNoCaixa = false;
	        if (agendamentoModel.getPagamento() != null && agendamentoModel.getPagamento().getIntegraCaixa()) {
	            registrarNoCaixa = true;
	        }

	        if (registrarNoCaixa) {
	            // 3. Verifica se o caixa está aberto
	            Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
	            List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataAtual);

	            if (checkData.isEmpty() || checkData.get(0).getStatus().equals("I")) {
	                exibirAlerta("error", "O caixa do dia precisa estar aberto para realizar um estorno!");
	                return;
	            }

	            // 4. Lógica de Estorno no Caixa
	            CaixaData caixaDataModel = checkData.get(0);
	            String horaAtualFormatada = LocalTime.now().format(horaFormatada);
	            calculaValorServicos(); 

	            ControleCaixa estornoCaixa = new ControleCaixa();
	            estornoCaixa.setCaixaData(caixaDataModel);
	            estornoCaixa.setHoraAtual(horaAtualFormatada);
	            estornoCaixa.setData(dataAtual);
	            estornoCaixa.setMovimentacao("Saída de estorno");
	            estornoCaixa.setValor(-totalGastoServicos);
	            ControleCaixaDAO.salvar(estornoCaixa);
	        }

	        // 5. Atualiza o status do agendamento (acontece sempre)
	        AgendamentoDAO.cancelarAgendamento(agendamentoModel.getId());
	        AgendamentoDAO.atualizarInformacoesPagamento(agendamentoModel.getId(), "E", agendamentoModel.getPagamento() != null ? agendamentoModel.getPagamento().getId() : null);

	        // 6. Atualiza o modelo local e exibe sucesso
	        agendamentoModel.setStatus("I");
	        agendamentoModel.setPago("E");
	        exibirAlerta("success", "Estorno realizado com sucesso!");
	        PrimeFaces.current().ajax().update("form");

	    } catch (Exception e) {
	        exibirAlerta("error", "Ocorreu um erro ao processar o estorno!");
	        e.printStackTrace();
	    }
	}

	public void fecharCliente() {
		clienteModel = new Cliente();
		totalGastoServicos = 0.0;
		servicosSelecionadosIds = new ArrayList<>();
		agendamentoModel = new Agendamento();
		idPagamento = null;
		PrimeFaces.current().ajax().update("form");
	}


	public void agendamentoSelecionado() {
		// Busca o cliente completo do banco se não for cliente avulso
		if (agendamentoModel.getCliente() != null && agendamentoModel.getCliente().getId() != null) {
			clienteModel = ClienteDAO.buscarPorId(agendamentoModel.getCliente().getId());
		} else {
			clienteModel = new Cliente();
		}
		
		// Garante que observações não seja null
		if (agendamentoModel.getObservacoes() == null) {
			agendamentoModel.setObservacoes("");
		}
		
		editarModel = "A";
		verificaPagamento = "A";
		passos = 0;
		calculaValorServicos();
	}

	public void editAgendamento() {
	    this.passos = 0;
	    this.editarModel = "A";
	    
	    this.servicosSelecionadosIds = new ArrayList<>();
	    this.servicosSelecionadosMap = new java.util.HashMap<>();
	    
	    for (Servicos item : agendamentoModel.getServicos()) {
	        this.servicosSelecionadosIds.add(item.getId());
	        this.servicosSelecionadosMap.put(item.getId(), true);
	    }

	    		if ("A".equals(agendamentoModel.getTipoCadastro()) && agendamentoModel.getCliente() != null) {
	    			this.clienteId = agendamentoModel.getCliente().getId();
	    			this.nomeCliente = null;
	    		} else {
	    			this.clienteId = null;
	    			this.nomeCliente = agendamentoModel.getNomeClienteAvulso();
	    		}	    this.tipoCadastro = agendamentoModel.getTipoCadastro();
	    this.nomeFuncionario = agendamentoModel.getFuncionario().getNome();
	    this.dataSelecionada = agendamentoModel.getDataCriado();
	    this.horaSelecionada = agendamentoModel.getHoraSelecionada().format(horaFormatada);
	    
	    // Popula a propriedade observacoes do controller
	    this.observacoes = agendamentoModel.getObservacoes() != null ? agendamentoModel.getObservacoes() : "";

	    this.lstFuncionarioDisponivel = new ArrayList<>(this.lstFuncionario);
	    this.horariosDisponiveis.clear(); // Apenas limpa para garantir que não haja dados antigos

	    // Popula os horários disponíveis ao entrar no modo de edição
	    gerarHorariosDisponiveis();
	}


	public void novoAgendamento() {
		editarModel = "I";
		tipoCadastro = "A";
		passos = 0;
		agendamentoModel = new Agendamento();
		agendamentoModel.setServicos(new ArrayList<>());
		agendamentoModel.setObservacoes(""); // Inicializa observações como string vazia
		agendamentoModel.setSexo(null); // Inicializa sexo como null
		servicosSelecionadosIds = new ArrayList<>();
		servicosSelecionadosMap = new java.util.HashMap<>();
		clienteId = null;
		nomeCliente = "";
		nomeFuncionario = "";
		dataSelecionada = null;
		horaSelecionada = "";
		observacoes = "";
		lstFuncionarioDisponivel = new ArrayList<>(lstFuncionario);
	}

	private boolean validarCamposAgendamento() {
		if (("A".equals(tipoCadastro) && clienteId == null) || ("I".equals(tipoCadastro) && (nomeCliente == null || nomeCliente.trim().isEmpty()))) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente é obrigatório", "Erro!"));
			return false;
		}
		if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do funcionário é obrigatório", "Erro!"));
			return false;
		}
		if (dataSelecionada == null) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo de data é obrigatório", "Erro!"));
			return false;
		}
		if (horaSelecionada == null || horaSelecionada.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo horário é obrigatório", "Erro!"));
			return false;
		}
		if (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione pelo menos 1 serviço", "Erro!"));
			return false;
		}
		// Validação do sexo APENAS para agendamentos sem cadastro
		if ("I".equals(tipoCadastro) && (sexoSelecionado == null || sexoSelecionado.trim().isEmpty())) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo sexo é obrigatório", "Erro!"));
			return false;
		}
		return true;
	}

	public void adicionarNovoAgendamento() {
		if (!validarCamposAgendamento()) return;
		
		System.out.println("🔍 Tipo de cadastro: " + tipoCadastro);
		System.out.println("📝 Sexo selecionado (variável): " + sexoSelecionado);
		System.out.println("📝 Sexo no model (ANTES): " + agendamentoModel.getSexo());
		
		// Atribui o sexo selecionado ao model ANTES da validação
		if ("I".equals(tipoCadastro) && sexoSelecionado != null && !sexoSelecionado.trim().isEmpty()) {
			agendamentoModel.setSexo(sexoSelecionado);
			System.out.println("✅ Sexo atribuído ao model: " + agendamentoModel.getSexo());
		}
		
		// Validação do campo sexo para agendamentos sem cadastro
		if ("I".equals(tipoCadastro) && (agendamentoModel.getSexo() == null || agendamentoModel.getSexo().trim().isEmpty())) {
			System.out.println("❌ Validação falhou - sexo vazio!");
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo sexo é obrigatório para agendamentos sem cadastro", "Erro!"));
			return;
		}
		
		System.out.println("✅ Validação de sexo OK!");
		
		Funcionario funcionarioSelecionado = lstFuncionario.stream().filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim())).findFirst().orElse(null);
		Cliente clienteSelecionado = null;
		if ("A".equals(tipoCadastro)) {
			if (this.clienteId != null) {
				clienteSelecionado = lstCliente.stream().filter(c -> c.getId().equals(this.clienteId)).findFirst()
						.orElse(null);
			}
		}

		// Validação de horário de término
		int totalMinutos = 0;
		for (Long servicoId : servicosSelecionadosIds) {
			Servicos servico = ServicosDAO.buscarPorId(servicoId);
			if (servico != null) {
				totalMinutos += servico.getMinutos();
			}
		}

		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		LocalTime horaInicioAgendamento = LocalTime.parse(horaSelecionada);
		LocalTime horaFimAgendamento = horaInicioAgendamento.plusMinutes(totalMinutos);

		// Verifica se o horário de término deu overflow (passou da meia-noite)
		if (horaFimAgendamento.compareTo(horaInicioAgendamento) < 0) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"O horário de término do serviço ultrapassa a meia-noite. Por favor, escolha um horário mais cedo.", "Erro!"));
			return;
		}

		boolean agendamentoDentroDeUmPeriodo = false;
		for (Horario periodo : horariosFuncionario) {
			if (!horaInicioAgendamento.isBefore(periodo.getHoraInicial())
					&& !horaFimAgendamento.isAfter(periodo.getHoraFinal())) {
				agendamentoDentroDeUmPeriodo = true;
				break;
			}
		}

		if (!agendamentoDentroDeUmPeriodo) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Serviços excedem o horário disponível do funcionário ou invadem um intervalo!", "Erro!"));
			return;
		}
		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioSelecionado.getId(),
				dataSelecionada, null);

		if (horariosOcupados.contains(LocalTime.parse(horaSelecionada))) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Este horário acaba de ser agendado. Por favor, escolha outro.", "Erro!"));
			return;
		}

		// Garante que agendamentoModel não está null
		if (agendamentoModel == null) {
			agendamentoModel = new Agendamento();
		}
		
		// NÃO cria novo objeto para manter as observações preenchidas
		agendamentoModel.setStatus("A");
		agendamentoModel.setDataCriado(dataSelecionada);
		agendamentoModel.setFuncionario(funcionarioSelecionado);
		agendamentoModel.setHoraSelecionada(LocalTime.parse(horaSelecionada));
		agendamentoModel.setCliente(clienteSelecionado);
		if ("I".equals(tipoCadastro)) {
			agendamentoModel.setNomeClienteAvulso(this.nomeCliente);
			// Mantém o sexo que foi selecionado no Step 3
		} else {
			// Limpa o sexo se for cliente cadastrado (não deve salvar)
			agendamentoModel.setSexo(null);
		}
		agendamentoModel.setTipoCadastro(tipoCadastro);
		agendamentoModel.setObservacoes(this.observacoes); // Copia observacoes do controller
		AgendamentoDAO.salvar(agendamentoModel, servicosSelecionadosIds);

		List<Servicos> servicosSelecionados = new ArrayList<>();
	    for (Long servicoId : servicosSelecionadosIds) {
	        servicosSelecionados.add(ServicosDAO.buscarPorId(servicoId));
	    }
	    agendamentoModel.setServicos(servicosSelecionados);

	    if (agendamentoModel.getCliente() != null) {
	        clienteModel = agendamentoModel.getCliente();
	    } else {
	        clienteModel = new Cliente();
	    }

	    calculaValorServicos();
	    
		exibirAlerta("success", "Agendamento cadastrado com sucesso!");
		PrimeFaces.current().executeScript("PF('dlgAgendar').hide();");
		PrimeFaces.current().ajax().update("form");
	}

	public void atualizarAgendamento() {
		if (!validarCamposAgendamento()) return;
		
		// Atribui o sexo selecionado ao model ANTES da validação
		if ("I".equals(tipoCadastro) && sexoSelecionado != null && !sexoSelecionado.trim().isEmpty()) {
			agendamentoModel.setSexo(sexoSelecionado);
		}
		
		// Validação do campo sexo para agendamentos sem cadastro
		if ("I".equals(tipoCadastro) && (agendamentoModel.getSexo() == null || agendamentoModel.getSexo().trim().isEmpty())) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo sexo é obrigatório para agendamentos sem cadastro", "Erro!"));
			return;
		}
		
		Funcionario funcionarioSelecionado = lstFuncionario.stream().filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim())).findFirst().orElse(null);
		Cliente clienteSelecionado = null;
		if ("A".equals(tipoCadastro) && this.clienteId != null) {
			clienteSelecionado = lstCliente.stream().filter(c -> c.getId().equals(this.clienteId)).findFirst().orElse(null);
		}
		
        // Validação de horário de término
        int totalMinutos = 0;
        for (Long servicoId : servicosSelecionadosIds) {
            Servicos servico = ServicosDAO.buscarPorId(servicoId);
            if (servico != null) {
                totalMinutos += servico.getMinutos();
            }
        }

        List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
        LocalTime horaInicioAgendamento = LocalTime.parse(horaSelecionada);
        LocalTime horaFimAgendamento = horaInicioAgendamento.plusMinutes(totalMinutos);

		// Verifica se o horário de término deu overflow (passou da meia-noite)
		if (horaFimAgendamento.compareTo(horaInicioAgendamento) < 0) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"O horário de término do serviço ultrapassa a meia-noite. Por favor, escolha um horário mais cedo.", "Erro!"));
			return;
		}

        boolean agendamentoDentroDeUmPeriodo = false;
        for (Horario periodo : horariosFuncionario) {
            if (!horaInicioAgendamento.isBefore(periodo.getHoraInicial()) && !horaFimAgendamento.isAfter(periodo.getHoraFinal())) {
                agendamentoDentroDeUmPeriodo = true;
                break;
            }
        }

		if (!agendamentoDentroDeUmPeriodo) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Serviços excedem o horário disponível do funcionário ou invadem um intervalo!", "Erro!"));
			return;
		}

        

        		// VALIDAÇÃO DE CONFLITO DE HORÁRIO AO EDITAR

        	    int numeroDeSlotsNecessarios = (totalMinutos + 29) / 30;

        	    if (numeroDeSlotsNecessarios == 0) numeroDeSlotsNecessarios = 1;

        

        	    List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioSelecionado.getId(), dataSelecionada, agendamentoModel.getId());

        

        	    for (int i = 0; i < numeroDeSlotsNecessarios; i++) {

        	        LocalTime slotParaVerificar = horaInicioAgendamento.plusMinutes((long) i * 30);

        	        if (horariosOcupados.contains(slotParaVerificar)) {

        	            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 

        	                "A nova duração do serviço invade o horário de outro agendamento.", "Erro de Conflito!"));

        	            return; // Aborta o salvamento

        	        }

        	    }

        	    // FIM DA VALIDAÇÃO

        

        		boolean mudouFuncionario = !agendamentoModel.getFuncionario().getId().equals(funcionarioSelecionado.getId());
		boolean mudouData = !agendamentoModel.getDataCriado().equals(dataSelecionada);
		boolean mudouHora = !agendamentoModel.getHoraSelecionada().equals(LocalTime.parse(horaSelecionada));

		agendamentoModel.setDataCriado(dataSelecionada);
		agendamentoModel.setFuncionario(funcionarioSelecionado);
		agendamentoModel.setCliente(clienteSelecionado);
		agendamentoModel.setHoraSelecionada(LocalTime.parse(horaSelecionada));
		if ("I".equals(tipoCadastro)) {
			agendamentoModel.setNomeClienteAvulso(this.nomeCliente);
			// Mantém o sexo que foi selecionado no Step 3
		} else {
			// Limpa o sexo se for cliente cadastrado (não deve salvar)
			agendamentoModel.setSexo(null);
		}
		agendamentoModel.setTipoCadastro(tipoCadastro);
		agendamentoModel.setObservacoes(this.observacoes); // Copia observacoes do controller
		AgendamentoDAO.atualizar(agendamentoModel, servicosSelecionadosIds);

		// Atualiza a lista de serviços no modelo para refletir na tela
		List<Servicos> servicosAtualizados = new ArrayList<>();
		for (Long servicoId : servicosSelecionadosIds) {
			servicosAtualizados.add(ServicosDAO.buscarPorId(servicoId));
		}
		agendamentoModel.setServicos(servicosAtualizados);
		calculaValorServicos();

		exibirAlerta("success", "Agendamento atualizado com sucesso!");
		PrimeFaces.current().executeScript("PF('dlgAgendar').hide();");
		PrimeFaces.current().executeScript("setTimeout(function() { PrimeFaces.ab({s:'form', u:'form'}); }, 50);");
	}

	public void salvarOuAtualizarAgendamento() {
		if ("A".equals(editarModel)) {
			atualizarAgendamento();
		} else {
			adicionarNovoAgendamento();
		}
	}


	public void cancelarAgendamento() {
		try {
			// Validação: verifica se o agendamento existe
			if (agendamentoModel == null || agendamentoModel.getId() == null) {
				exibirAlerta("error", "Nenhum agendamento selecionado para cancelar!");
				return;
			}
			
			// Cancela o agendamento no banco
			AgendamentoDAO.cancelarAgendamento(agendamentoModel.getId());
			agendamentoModel.setStatus("I"); // Atualiza o modelo na tela

			// Cria notificação para o cliente (se houver cliente cadastrado)
			if (agendamentoModel.getCliente() != null && agendamentoModel.getCliente().getId() != null) {
				String nomeCliente = agendamentoModel.getCliente().getNome();
				String nomeFuncionario = agendamentoModel.getFuncionario() != null 
					? agendamentoModel.getFuncionario().getNome() 
					: "Funcionário";

				DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
				String horaFormatada = agendamentoModel.getHoraSelecionada().format(timeFormatter);
				
				// Converte java.util.Date para LocalDate de forma segura
				java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("dd/MM/yyyy");
				String dataFormatada = dateFormatter.format(agendamentoModel.getDataCriado());

				String mensagem = "Seu agendamento do dia " + dataFormatada + " às " + horaFormatada 
					+ " com " + nomeFuncionario + " foi CANCELADO";

				com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
				notificacao.setMensagem(mensagem);
				notificacao.setDataEnvio(new java.util.Date());
				notificacao.setAgendamento(agendamentoModel);
				notificacao.setCliente(agendamentoModel.getCliente());

				com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
				notificacaoDAO.salvar(notificacao);
			}

			exibirAlerta("success", "Agendamento cancelado com sucesso!");
			PrimeFaces.current().executeScript("PF('dlgAgendar').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (Exception e) {
			exibirAlerta("error", "Ocorreu um erro ao cancelar o agendamento!");
			e.printStackTrace();
		}
	}

	public void gerarHorariosDisponiveis() {
		horariosDisponiveis.clear();

		// Suporta tanto funcionarioId quanto nomeFuncionario
		Funcionario funcionarioSelecionado = null;
		
		if (funcionarioId != null) {
			// Busca por ID (usado na tela agendamentoCliente)
			funcionarioSelecionado = lstFuncionario.stream()
					.filter(f -> f.getId().equals(funcionarioId)).findFirst().orElse(null);
		} else if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
			// Busca por nome (usado na tela agendamento)
			funcionarioSelecionado = lstFuncionario.stream()
					.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim())).findFirst().orElse(null);
		}
		
		if (funcionarioSelecionado == null || dataSelecionada == null) {
			System.out.println("⚠️ Funcionário ou data não selecionado");
			return;
		}
		
		System.out.println("\n========== GERANDO HORÁRIOS DISPONÍVEIS ==========");
		System.out.println("👤 Funcionário: " + funcionarioSelecionado.getNome());
		System.out.println("📅 Data: " + dataSelecionada);
		
		if (com.barbersys.dao.RestricaoDataDAO.isDataBloqueada(dataSelecionada, funcionarioSelecionado.getId())) {
			System.out.println("🚫 Data bloqueada para este funcionário");
			return; // Data bloqueada, não mostra horários
		}

		int totalMinutos = 0;
		if (servicosSelecionadosIds != null && !servicosSelecionadosIds.isEmpty()) {
			for (Long servicoId : servicosSelecionadosIds) {
				Servicos servico = ServicosDAO.buscarPorId(servicoId);
				if (servico != null) {
					totalMinutos += servico.getMinutos();
					System.out.println("✂️ Serviço: " + servico.getNome() + " (" + servico.getMinutos() + " min)");
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
		
		System.out.println("⏱️ Duração total: " + totalMinutos + " minutos (" + numeroDeSlotsNecessarios + " slots de 30min)");

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
		
		if (horariosValidosParaDia.isEmpty()) {
			System.out.println("⚠️ Funcionário não trabalha neste dia da semana");
			return; // Funcionário não trabalha neste dia da semana
		}
		
		System.out.println("🕐 Períodos de trabalho neste dia:");
		for (Horario h : horariosValidosParaDia) {
			System.out.println("  ⏰ " + h.getHoraInicial() + " às " + h.getHoraFinal());
		}

		Long idParaExcluir = null;
		if ("A".equals(editarModel) && agendamentoModel != null && agendamentoModel.getFuncionario() != null
				&& agendamentoModel.getFuncionario().getId().equals(funcionarioSelecionado.getId())) {
			idParaExcluir = agendamentoModel.getId();
			System.out.println("✏️ Editando agendamento ID: " + idParaExcluir + " (será excluído da verificação)");
		}
		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioSelecionado.getId(),
				dataSelecionada, idParaExcluir);
		
		System.out.println("\n📍 Horários OCUPADOS (slots de 30min):");
		if (horariosOcupados.isEmpty()) {
			System.out.println("  ✅ Nenhum horário ocupado");
		} else {
			for (LocalTime h : horariosOcupados) {
				System.out.println("  ❌ " + h);
			}
		}

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalDateTime agora = LocalDateTime.now();
		LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();
		LocalTime horaAtualSistema = agora.toLocalTime();
		
		boolean isHojeDia = dataSelecionadaLocalDate.isEqual(LocalDate.now());
		
		System.out.println("\n⏰ VERIFICAÇÃO DE DATA/HORA:");
		System.out.println("  📅 Data selecionada: " + dataSelecionadaLocalDate);
		System.out.println("  📅 Data de hoje: " + LocalDate.now());
		System.out.println("  ⏰ Hora atual do sistema: " + horaAtualSistema);
		System.out.println("  🔍 É hoje? " + (isHojeDia ? "SIM" : "NÃO"));

		if ("A".equals(editarModel) && horaSelecionada != null && !horaSelecionada.isEmpty()) {
			horariosDisponiveis.add(horaSelecionada);
		}
		
		System.out.println("\n🔎 TESTANDO HORÁRIOS:");

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
				// CORREÇÃO: Para hoje, só aceita horários que ainda não passaram
				// Margem de segurança: precisa ter pelo menos 15 minutos de antecedência
				// Exemplo: Se são 20:00, só aceita 20:15 em diante
				boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(horaAtualSistema.plusMinutes(15));
				
				// Log detalhado para DEBUG
				if (isHoje && iteracao <= 5) {
					String status = isHorarioFuturo ? "✅ ACEITO" : "❌ BLOQUEADO";
					System.out.println("  " + status + " " + horaAtual.format(formatter) + 
						" (hora atual: " + horaAtualSistema.format(formatter) + 
						", limite: " + horaAtualSistema.plusMinutes(15).format(formatter) + ")");
				}
				
				if (!isHorarioFuturo) {
					horaAtual = horaAtual.plusMinutes(30);
					continue; // Pula este horário
				}

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
						String motivoRejeicao = "";
						
						for (int i = 0; i < numeroDeSlotsNecessarios; i++) {
							LocalTime slotParaVerificar = horaAtual.plusMinutes((long) i * 30);
							
							// Verifica se o slot não deu overflow (passou da meia-noite)
							if (slotParaVerificar.compareTo(horaAtual) < 0) {
								// Slot deu volta (passou de 23:59 para 00:00)
								todosSlotsLivres = false;
								motivoRejeicao = "overflow (passou da meia-noite)";
								break;
							}
							
							// Verifica se o slot ultrapassa o horário final
							if (slotParaVerificar.isAfter(horaFinalPeriodo)) {
								todosSlotsLivres = false;
								motivoRejeicao = "ultrapassa horário final (" + horaFinalPeriodo + ")";
								break;
							}
							
							if (horariosOcupados.contains(slotParaVerificar)) {
								todosSlotsLivres = false;
								motivoRejeicao = "slot " + slotParaVerificar + " ocupado";
								break;
							}
						}

						if (todosSlotsLivres) {
							String horaFormatadaLoop = horaAtual.format(formatter);
							if (!horariosDisponiveis.contains(horaFormatadaLoop)) {
								horariosDisponiveis.add(horaFormatadaLoop);
								System.out.println("  ✅ " + horaFormatadaLoop + " → DISPONÍVEL");
							}
						} else {
							System.out.println("  ❌ " + horaAtual.format(formatter) + " → REJEITADO (" + motivoRejeicao + ")");
						}
					} else {
						System.out.println("  ⚠️ " + horaAtual.format(formatter) + " → Termina fora do período (termina às " + horarioTermino + ")");
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
		
		System.out.println("\n✨ RESULTADO FINAL:");
		System.out.println("📊 Total de horários disponíveis: " + horariosDisponiveis.size());
		if (!horariosDisponiveis.isEmpty()) {
			System.out.println("🕐 Horários: " + String.join(", ", horariosDisponiveis));
		}
		System.out.println("==================================================\n");
	}

	public void funcionarioAlterado() {
	    this.horaSelecionada = "";
	    gerarHorariosDisponiveis();
	}
	
	public void addPassosAgendamento() {
		try {
			if (this.passos < 2) {
				// VALIDAÇÃO PASSO 0 → 1: Cliente e Funcionário
				if (this.passos == 0) {
					// Valida Nome do Cliente
					if (tipoCadastro.equals("A")) {
						// Com cadastro: valida se cliente foi selecionado
						if (clienteId == null) {
							FacesContext.getCurrentInstance().addMessage(null,
									new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione um cliente", "Erro!"));
							return;
						}
					} else {
						// Sem cadastro: valida se nome foi preenchido
						if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
							FacesContext.getCurrentInstance().addMessage(null,
									new FacesMessage(FacesMessage.SEVERITY_ERROR, "Informe o nome do cliente", "Erro!"));
							return;
						}
					}
					
					// Valida Nome do Funcionário
					if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) {
						FacesContext.getCurrentInstance().addMessage(null,
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione um funcionário", "Erro!"));
						return;
					}
				}
				
				// VALIDAÇÃO PASSO 1 → 2: Verifica se há horários disponíveis para os serviços selecionados
				if (this.passos == 1) {
					// Valida se data foi selecionada
					if (dataSelecionada == null) {
						FacesContext.getCurrentInstance().addMessage(null,
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione uma data antes de continuar", "Erro!"));
						return;
					}
					
					// Valida se pelo menos 1 serviço foi selecionado
					if (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty()) {
						FacesContext.getCurrentInstance().addMessage(null,
								new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione pelo menos 1 serviço", "Erro!"));
						return;
					}
					
					// Gera horários antes de avançar para verificar se há disponibilidade
					gerarHorariosDisponiveis();
					
					// Verifica se há horários disponíveis
					if (horariosDisponiveis == null || horariosDisponiveis.isEmpty()) {
						FacesContext.getCurrentInstance().addMessage(null,
								new FacesMessage(FacesMessage.SEVERITY_WARN, 
										"Não há horários disponíveis para os serviços selecionados nesta data. " +
										"O funcionário não possui disponibilidade suficiente ou todos os horários estão ocupados.", 
										"Atenção!"));
						return;
					}
				}
				
				this.passos++;
				if (this.passos == 1) {
					// Calcula datas desabilitadas quando entra no passo de seleção de data
					// Mesmo sem serviços selecionados, calcula para mostrar dias que funcionário não trabalha
					calcularDatasDesabilitadas();
				}
				if (this.passos == 2) {
					// Gera horários quando avança para passo 2 (já validado antes)
					gerarHorariosDisponiveis();
					
					// Garante que agendamentoModel e observacoes estão inicializados
					if (agendamentoModel == null) {
						agendamentoModel = new Agendamento();
					}
					if (agendamentoModel.getObservacoes() == null) {
						agendamentoModel.setObservacoes("");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao avançar: " + e.getMessage(), "Erro!"));
			// Volta ao passo anterior em caso de erro
			if (this.passos > 0) {
				this.passos--;
			}
		}
	}
	
	/**
	 * Listener chamado quando o tipo de cadastro é alterado no Step 1
	 * Garante que agendamentoModel esteja sempre inicializado
	 */
	public void onTipoCadastroChange() {
		// Garante que agendamentoModel não está null
		if (agendamentoModel == null) {
			agendamentoModel = new Agendamento();
			agendamentoModel.setServicos(new ArrayList<>());
			agendamentoModel.setObservacoes("");
		}
		
		// Se mudou para "Com cadastro", limpa o sexo
		if ("A".equals(tipoCadastro)) {
			agendamentoModel.setSexo(null);
		}
	}

	public void voltaPassosAgendamento() {
		if (this.passos > 0) {
			this.passos--;
		}
	}
	
	/**
	 * Listener chamado quando o funcionário é alterado no passo 0
	 * NÃO calcula datas aqui para não travar - será calculado ao avançar
	 */
	public void onFuncionarioChange() {
		// Não calcula datas desabilitadas aqui (seria muito lento com 15 anos)
		// O cálculo será feito quando avançar para o passo 1
	}
	

	/**
	 * Valida quando os serviços são alterados no passo 1
	 * Agora não recalcula datas porque elas já estão desabilitadas desde o início
	 */
	public void validarServicosAlterados() {
		// Apenas atualiza componentes visuais
		// As datas já foram desabilitadas quando o funcionário foi selecionado
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
		
		// Suporta tanto funcionarioId quanto nomeFuncionario
		Funcionario funcionarioSelecionado = null;
		
		if (funcionarioId != null) {
			// Busca por ID (usado na tela agendamentoCliente)
			funcionarioSelecionado = lstFuncionario.stream()
					.filter(f -> f.getId().equals(funcionarioId))
					.findFirst()
					.orElse(null);
		} else if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
			// Busca por nome (usado na tela agendamento)
			funcionarioSelecionado = lstFuncionario.stream()
					.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim()))
					.findFirst()
					.orElse(null);
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
		// Inclui o dia de hoje para verificar se há horários disponíveis
		LocalDate hoje = LocalDate.now();
		for (int i = 0; i <= 1095; i++) {  // ← Volta para 0 (inclui hoje)
			LocalDate dataVerificar = hoje.plusDays(i);
			Date dataUtil = Date.from(dataVerificar.atStartOfDay(ZoneId.systemDefault()).toInstant());
			
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

	public void filtrarFuncionariosPorData() {
	    horariosDisponiveis.clear();
	    horaSelecionada = "";
	    // NÃO limpa o nomeFuncionario pois ele já foi selecionado no passo anterior

	    if (dataSelecionada == null) {
	        lstFuncionarioDisponivel = new ArrayList<>(lstFuncionario);
	        return;
	    }

	    lstFuncionarioDisponivel = new ArrayList<>();
	    LocalDateTime agora = LocalDateTime.now();
	    LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();
	    
	    // Descobre qual dia da semana é a data selecionada
	    java.util.Calendar cal = java.util.Calendar.getInstance();
	    cal.setTime(dataSelecionada);
	    int diaSemana = cal.get(java.util.Calendar.DAY_OF_WEEK);

	    for (Funcionario func : lstFuncionario) {
	        // Verifica se a data está bloqueada para este funcionário
	        if (com.barbersys.dao.RestricaoDataDAO.isDataBloqueada(dataSelecionada, func.getId())) {
	            continue; // Data bloqueada, pula este funcionário
	        }
	        
	        List<Horario> horariosTrabalho = FuncionarioDAO.buscarHorarioPorFuncionario(func);
	        if (horariosTrabalho == null || horariosTrabalho.isEmpty()) {
	            continue;
	        }
	        
	        // Filtra apenas os horários que trabalham no dia da semana selecionado
	        List<Horario> horariosValidosParaDia = new ArrayList<>();
	        for (Horario h : horariosTrabalho) {
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
	            continue; // Funcionário não trabalha neste dia da semana
	        }

	        Long idParaExcluir = null;
	        if ("A".equals(editarModel) && agendamentoModel != null && agendamentoModel.getFuncionario() != null && agendamentoModel.getFuncionario().getId().equals(func.getId())) {
	            idParaExcluir = agendamentoModel.getId();
	        }
	        List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(func.getId(), dataSelecionada, idParaExcluir);

	        boolean temHorarioDisponivel = false;
	        for (Horario periodo : horariosValidosParaDia) {
	            LocalTime horaAtual = periodo.getHoraInicial();
	            LocalTime horaFinal = periodo.getHoraFinal().minusMinutes(30);
	            
	            // Proteção contra overflow de horário
	            int maxIteracoes = 100;
	            int iteracao = 0;
	            
	            while ((horaAtual.isBefore(horaFinal) || horaAtual.equals(horaFinal)) 
	                    && iteracao < maxIteracoes) {
	                iteracao++;
	                
	                boolean isHoje = dataSelecionadaLocalDate.isEqual(LocalDate.now());
	                boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(agora.toLocalTime());
	                
	                if (isHorarioFuturo && !horariosOcupados.contains(horaAtual)) {
	                    temHorarioDisponivel = true;
	                    break; 
	                }
	                
	                LocalTime proximaHora = horaAtual.plusMinutes(30);
	                
	                // Se deu volta (passou de 23:59 para 00:00), para o loop
	                if (proximaHora.compareTo(horaAtual) < 0) {
	                    break;
	                }
	                
	                horaAtual = proximaHora;
	            }
	            if (temHorarioDisponivel) {
	                break;
	            }
	        }

	        if (temHorarioDisponivel) {
	            lstFuncionarioDisponivel.add(func);
	        }
	    }
	}
	
	public boolean isAgendamentoPassado() {
	    if (agendamentoModel == null || agendamentoModel.getDataCriado() == null || agendamentoModel.getHoraSelecionada() == null) {
	        return false;
	    }
	    LocalDateTime agendamentoDateTime = LocalDateTime.of(
	        java.time.Instant.ofEpochMilli(agendamentoModel.getDataCriado().getTime()).atZone(ZoneId.systemDefault()).toLocalDate(),
	        agendamentoModel.getHoraSelecionada()
	    );
	    return agendamentoDateTime.isBefore(LocalDateTime.now());
	}
	
	/**
	 * Verifica se o agendamento pode ser finalizado hoje
	 * Só permite finalizar no dia do agendamento ou depois
	 */
	public boolean isPodeFinalizarHoje() {
	    if (agendamentoModel == null || agendamentoModel.getDataCriado() == null) {
	        return false;
	    }
	    
	    // Converte a data do agendamento para LocalDate
	    LocalDate dataAgendamento = java.time.Instant
	        .ofEpochMilli(agendamentoModel.getDataCriado().getTime())
	        .atZone(ZoneId.systemDefault())
	        .toLocalDate();
	    
	    LocalDate hoje = LocalDate.now();
	    
	    // Só pode finalizar se for hoje ou depois da data do agendamento
	    return !dataAgendamento.isAfter(hoje);
	}

	public boolean isProximoDisabled() {
	    if (passos == 0) {
	        // Passo 0: Cliente + Funcionário
	        boolean clienteInvalido = ("A".equals(tipoCadastro) && clienteId == null) || 
	                                  ("I".equals(tipoCadastro) && (nomeCliente == null || nomeCliente.trim().isEmpty()));
	        return clienteInvalido || (nomeFuncionario == null || nomeFuncionario.trim().isEmpty());
	    }
	    if (passos == 1) {
	        // Passo 1: Data + Serviços
	        return dataSelecionada == null || 
	               (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty());
	    }
	    return false; // No "Próximo" button on step 2
	}
	
	// ===== MÉTODOS PARA CARDS DE SERVIÇOS =====
	
	/**
	 * Retorna o valor total dos serviços selecionados
	 */
	public Double getValorTotalSelecionado() {
	    double total = 0.0;
	    if (servicosSelecionadosMap != null && !servicosSelecionadosMap.isEmpty()) {
	        for (Map.Entry<Long, Boolean> entry : servicosSelecionadosMap.entrySet()) {
	            if (entry.getValue() != null && entry.getValue()) {
	                Servicos s = lstServico.stream()
	                    .filter(servico -> servico.getId().equals(entry.getKey()))
	                    .findFirst()
	                    .orElse(null);
	                if (s != null) {
	                    total += s.getPreco();
	                }
	            }
	        }
	    }
	    return total;
	}
	
	/**
	 * Retorna o tempo total em minutos dos serviços selecionados
	 */
	public Integer getTempoTotalSelecionado() {
	    int total = 0;
	    if (servicosSelecionadosMap != null && !servicosSelecionadosMap.isEmpty()) {
	        for (Map.Entry<Long, Boolean> entry : servicosSelecionadosMap.entrySet()) {
	            if (entry.getValue() != null && entry.getValue()) {
	                Servicos s = lstServico.stream()
	                    .filter(servico -> servico.getId().equals(entry.getKey()))
	                    .findFirst()
	                    .orElse(null);
	                if (s != null) {
	                    total += s.getMinutos();
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
	    int totalMinutos = getTempoTotalSelecionado();
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
	 * Calcula o tempo total de um agendamento (soma dos minutos de todos os serviços)
	 */
	public Integer calcularTempoTotalAgendamento(Agendamento agendamento) {
	    int total = 0;
	    if (agendamento != null && agendamento.getServicos() != null && !agendamento.getServicos().isEmpty()) {
	        for (Servicos servico : agendamento.getServicos()) {
	            if (servico != null && servico.getMinutos() != null) {
	                total += servico.getMinutos();
	            }
	        }
	    }
	    return total;
	}
	
	/**
	 * Retorna lista de serviços filtrados pela pesquisa
	 */
	public List<Servicos> getServicosFiltrados() {
	    if (filtroServico == null || filtroServico.trim().isEmpty()) {
	        return lstServico != null ? lstServico : new ArrayList<>();
	    }
	    if (lstServico == null) {
	        return new ArrayList<>();
	    }
	    return lstServico.stream()
	        .filter(s -> s.getNome().toLowerCase().contains(filtroServico.toLowerCase()))
	        .collect(java.util.stream.Collectors.toList());
	}
	
	/**
	 * Inicializa o map de serviços selecionados baseado na lista de IDs
	 */
	public java.util.Map<Long, Boolean> getServicosSelecionadosMap() {
	    // Sincroniza o map com a lista de IDs
	    if (servicosSelecionadosMap == null) {
	        servicosSelecionadosMap = new java.util.HashMap<>();
	    }
	    
	    // Atualiza o map baseado nos IDs selecionados
	    if (lstServico != null) {
	        for (Servicos s : lstServico) {
	            boolean selecionado = servicosSelecionadosIds != null && servicosSelecionadosIds.contains(s.getId());
	            servicosSelecionadosMap.put(s.getId(), selecionado);
	        }
	    }
	    
	    return servicosSelecionadosMap;
	}
	
	public void setServicosSelecionadosMap(java.util.Map<Long, Boolean> servicosSelecionadosMap) {
	    this.servicosSelecionadosMap = servicosSelecionadosMap;
	}
	
	/**
	 * Gera lista de datas desabilitadas para o datePicker em formato JSON
	 * Inclui: datas com restrições gerais, datas onde funcionário não trabalha
	 */
	public String getDisabledDatesJson() {
		if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) {
			return "[]";
		}
		
		List<String> datasDesabilitadas = new ArrayList<>();
		
		// Busca o funcionário selecionado
		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim()))
				.findFirst()
				.orElse(null);
		
		if (funcionarioSelecionado == null) {
			return "[]";
		}
		
		// Busca os horários do funcionário
		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return "[]";
		}
		
		// Gera array de dias da semana que o funcionário trabalha
		boolean[] trabalhaNodia = new boolean[7]; // 0=Dom, 1=Seg, 2=Ter, 3=Qua, 4=Qui, 5=Sex, 6=Sáb
		
		for (Horario h : horariosFuncionario) {
			if (h.getDomingo() != null && h.getDomingo()) trabalhaNodia[0] = true;
			if (h.getSegunda() != null && h.getSegunda()) trabalhaNodia[1] = true;
			if (h.getTerca() != null && h.getTerca()) trabalhaNodia[2] = true;
			if (h.getQuarta() != null && h.getQuarta()) trabalhaNodia[3] = true;
			if (h.getQuinta() != null && h.getQuinta()) trabalhaNodia[4] = true;
			if (h.getSexta() != null && h.getSexta()) trabalhaNodia[5] = true;
			if (h.getSabado() != null && h.getSabado()) trabalhaNodia[6] = true;
		}
		
		// Gera lista de datas desabilitadas (próximos 3 anos)
		LocalDate hoje = LocalDate.now();
		for (int i = 0; i < 1095; i++) {
			LocalDate data = hoje.plusDays(i);
			int diaSemana = data.getDayOfWeek().getValue() % 7; // 0=Dom, 1=Seg, ..., 6=Sáb
			
			// Verifica se o funcionário trabalha neste dia da semana
			if (!trabalhaNodia[diaSemana]) {
				datasDesabilitadas.add(String.format("%d-%02d-%02d", data.getYear(), data.getMonthValue(), data.getDayOfMonth()));
			}
			
			// Verifica se existe restrição para esta data
			Date dataUtil = Date.from(data.atStartOfDay(ZoneId.systemDefault()).toInstant());
			if (com.barbersys.dao.RestricaoDataDAO.isDataBloqueada(dataUtil, funcionarioSelecionado.getId())) {
				String dataStr = String.format("%d-%02d-%02d", data.getYear(), data.getMonthValue(), data.getDayOfMonth());
				if (!datasDesabilitadas.contains(dataStr)) {
					datasDesabilitadas.add(dataStr);
				}
			}
		}
		
		// Retorna no formato JSON array: ["2025-01-15", "2025-01-22", ...]
		if (datasDesabilitadas.isEmpty()) {
			return "[]";
		}
		
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < datasDesabilitadas.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append("\"").append(datasDesabilitadas.get(i)).append("\"");
		}
		sb.append("]");
		
		return sb.toString();
	}
	
	/**
	 * Retorna array de dias da semana desabilitados (0-6)
	 */
	public String getDisabledDaysOfWeek() {
		if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) {
			return "[]";
		}
		
		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim()))
				.findFirst()
				.orElse(null);
		
		if (funcionarioSelecionado == null) {
			return "[]";
		}
		
		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return "[]";
		}
		
		boolean[] trabalhaNodia = new boolean[7];
		
		for (Horario h : horariosFuncionario) {
			if (h.getDomingo() != null && h.getDomingo()) trabalhaNodia[0] = true;
			if (h.getSegunda() != null && h.getSegunda()) trabalhaNodia[1] = true;
			if (h.getTerca() != null && h.getTerca()) trabalhaNodia[2] = true;
			if (h.getQuarta() != null && h.getQuarta()) trabalhaNodia[3] = true;
			if (h.getQuinta() != null && h.getQuinta()) trabalhaNodia[4] = true;
			if (h.getSexta() != null && h.getSexta()) trabalhaNodia[5] = true;
			if (h.getSabado() != null && h.getSabado()) trabalhaNodia[6] = true;
		}
		
		// Retorna dias que NÃO trabalha
		List<Integer> diasDesabilitados = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			if (!trabalhaNodia[i]) {
				diasDesabilitados.add(i);
			}
		}
		
		if (diasDesabilitados.isEmpty()) {
			return "[]";
		}
		
		StringBuilder sb = new StringBuilder("[");
		for (int i = 0; i < diasDesabilitados.size(); i++) {
			if (i > 0) sb.append(",");
			sb.append(diasDesabilitados.get(i));
		}
		sb.append("]");
		
		return sb.toString();
	}
	
	// ===============================================
	// MÉTODOS PARA TELA AGENDAMENTO CLIENTE
	// ===============================================
	
	/**
	 * Método chamado quando o tipo de agendamento é alterado (próprio/outro)
	 */
	public void aoMudarTipoAgendamento() {
		try {
			System.out.println("=== aoMudarTipoAgendamento() chamado ===");
			System.out.println("tipoAgendamento alterado para: " + tipoAgendamento);
			
			// Limpa campos relacionados
			this.nomeClienteOutro = null;
			
			// Atualiza componentes
			PrimeFaces.current().ajax().update("form:stepsContainer", "form:msgs");
			System.out.println("=== aoMudarTipoAgendamento() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em aoMudarTipoAgendamento(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Inicia o processo de agendamento na tela do cliente
	 */
	public void iniciarAgendamento() {
		try {
			System.out.println("=== iniciarAgendamento() chamado ===");
			this.agendamentoIniciado = true;
			this.activeIndex = 0;
			
			System.out.println("agendamentoIniciado=" + agendamentoIniciado);
			System.out.println("activeIndex=" + activeIndex);
			
			PrimeFaces.current().ajax().update("form:bookingAreaPanel");
			System.out.println("=== iniciarAgendamento() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em iniciarAgendamento(): " + e.getMessage());
			e.printStackTrace();
			exibirAlerta("error", "Erro ao iniciar agendamento");
		}
	}
	
	/**
	 * Avança para o próximo passo no wizard de steps
	 */
	public void proximoPasso() {
		try {
			System.out.println("=== proximoPasso() chamado. activeIndex=" + activeIndex + " ===");
			
			// Validações antes de avançar
			if (activeIndex == 0) {
				// Validar Passo 1: Cliente e Funcionário
				System.out.println("Validando Passo 1...");
				System.out.println("tipoAgendamento=" + tipoAgendamento);
				System.out.println("nomeClienteOutro=" + nomeClienteOutro);
				System.out.println("funcionarioId=" + funcionarioId);
				
				if ("outro".equals(tipoAgendamento) && (nomeClienteOutro == null || nomeClienteOutro.trim().isEmpty())) {
					exibirAlerta("warning", "Por favor, informe o nome do cliente.");
					return;
				}
				if (funcionarioId == null) {
					exibirAlerta("warning", "Por favor, selecione um funcionário.");
					return;
				}
				// Calcula datas desabilitadas quando sair do passo 1
				System.out.println("Calculando datas desabilitadas...");
				calcularDatasDesabilitadas();
				System.out.println("Datas desabilitadas calculadas: " + datasDesabilitadas.size());
				
			} else if (activeIndex == 1) {
				// Validar Passo 2: Data e Serviços
				System.out.println("Validando Passo 2...");
				System.out.println("dataSelecionada=" + dataSelecionada);
				System.out.println("servicosSelecionadosIds.size=" + (servicosSelecionadosIds != null ? servicosSelecionadosIds.size() : 0));
				
				if (dataSelecionada == null) {
					exibirAlerta("warning", "Por favor, selecione uma data.");
					return;
				}
				if (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty()) {
					exibirAlerta("warning", "Por favor, selecione ao menos um serviço.");
					return;
				}
				// Carregar horários disponíveis para o próximo passo
				System.out.println("Gerando horários disponíveis...");
				gerarHorariosDisponiveis();
				System.out.println("Horários disponíveis gerados: " + horariosDisponiveis.size());
				
				// Verifica se há horários disponíveis
				if (horariosDisponiveis.isEmpty()) {
					exibirAlerta("warning", "Não há horários disponíveis para esta data com os serviços selecionados.");
					return;
				}
			}
			
			if (activeIndex < 2) {
				activeIndex++;
				System.out.println("Avançando para passo " + activeIndex);
				PrimeFaces.current().ajax().update("form:stepsContainer", "form:msgs");
			}
			System.out.println("=== proximoPasso() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em proximoPasso(): " + e.getMessage());
			e.printStackTrace();
			exibirAlerta("error", "Erro ao avançar para o próximo passo");
		}
	}
	
	/**
	 * Volta para o passo anterior no wizard
	 */
	public void passoAnterior() {
		if (activeIndex > 0) {
			activeIndex--;
			PrimeFaces.current().ajax().update("form:stepsComponent", "form:bookingAreaPanel");
		}
	}
	
	/**
	 * Verifica se pode avançar para o próximo passo
	 */
	public boolean isPodeAvancarPasso() {
		try {
			if (activeIndex == 0) {
				// Passo 1: Validações
				// Se for "outro", precisa ter nome preenchido
				if ("outro".equals(tipoAgendamento)) {
					if (nomeClienteOutro == null || nomeClienteOutro.trim().isEmpty()) {
						return false;
					}
				}
				// Sempre precisa ter funcionário selecionado
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
			return (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty() || funcionarioId != null)
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
	
	/**
	 * Listener quando o funcionário é selecionado na tela do cliente
	 */
	public void aoSelecionarFuncionario() {
		try {
			System.out.println("=== aoSelecionarFuncionario() chamado ===");
			System.out.println("funcionarioId selecionado: " + funcionarioId);
			
			// Limpa campos dependentes
			this.dataSelecionada = null;
			this.horaSelecionada = null;
			this.horariosDisponiveis.clear();
			if (this.servicosSelecionadosIds != null) {
				this.servicosSelecionadosIds.clear();
			}
			
			System.out.println("=== aoSelecionarFuncionario() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em aoSelecionarFuncionario(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Listener quando a data é selecionada na tela do cliente
	 */
	public void aoSelecionarData() {
		try {
			System.out.println("=== aoSelecionarData() chamado ===");
			System.out.println("Data selecionada: " + dataSelecionada);
			
			// No fluxo de steps, não limpamos os serviços
			this.horaSelecionada = null;
			
			System.out.println("=== aoSelecionarData() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em aoSelecionarData(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Verifica se o campo de funcionário está desabilitado
	 */
	public boolean isFuncionarioDesabilitado() {
		// Campo de funcionário sempre habilitado
		return false;
	}
	
	/**
	 * Verifica se o campo de data está desabilitado
	 */
	public boolean isDataDesabilitada() {
		return funcionarioId == null;
	}
	
	/**
	 * Verifica se o campo de horário está desabilitado
	 */
	public boolean isHorarioDesabilitado() {
		return funcionarioId == null || dataSelecionada == null;
	}
	
	/**
	 * Listener quando um serviço é selecionado/desselecionado
	 */
	public void aoSelecionarServico() {
		try {
			System.out.println("=== aoSelecionarServico() chamado ===");
			
			// Inicializa a lista se necessário
			if (servicosSelecionadosIds == null) {
				servicosSelecionadosIds = new ArrayList<>();
			}
			
			// Sincroniza o Map com a lista de IDs
			servicosSelecionadosIds.clear();
			
			for (java.util.Map.Entry<Long, Boolean> entry : servicosSelecionadosMap.entrySet()) {
				if (Boolean.TRUE.equals(entry.getValue())) {
					servicosSelecionadosIds.add(entry.getKey());
				}
			}
			
			System.out.println("Serviços selecionados: " + servicosSelecionadosIds.size());
			System.out.println("=== aoSelecionarServico() concluído ===");
		} catch (Exception e) {
			System.err.println("ERRO em aoSelecionarServico(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Calcula o valor total dos serviços selecionados
	 */
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
	
	/**
	 * Retorna o valor total formatado
	 */
	public String getValorTotalFormatado() {
		double total = getValorTotal();
		return String.format("%.2f", total);
	}
	
	/**
	 * Método para agendar na tela do cliente
	 */
	public void agendar() {
		try {
			System.out.println("=== agendar() chamado ===");
			
			// Validação: Data no passado
			LocalDate dataAgendamento = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();
			LocalDate hoje = LocalDate.now();
			
			if (dataAgendamento.isBefore(hoje)) {
				exibirAlerta("error", "Não é possível agendar para datas passadas!");
				return;
			}
			
			// Validação: Horário no passado (se for hoje)
			if (dataAgendamento.isEqual(hoje)) {
				LocalTime horarioAgendamento = LocalTime.parse(horaSelecionada);
				LocalTime horarioAtual = LocalTime.now();
				
				if (horarioAgendamento.isBefore(horarioAtual) || horarioAgendamento.equals(horarioAtual)) {
					exibirAlerta("error", "Não é possível agendar para horários que já passaram!");
					return;
				}
			}
			
			// Busca o funcionário selecionado
			Funcionario funcionarioSelecionado = lstFuncionario.stream()
					.filter(f -> f.getId().equals(funcionarioId))
					.findFirst()
					.orElse(null);
			
			if (funcionarioSelecionado == null) {
				exibirAlerta("error", "Funcionário não encontrado!");
				return;
			}
			
			// Cria o novo agendamento
			Agendamento novoAgendamento = new Agendamento();
			novoAgendamento.setDataCriado(dataSelecionada);
			novoAgendamento.setHoraSelecionada(LocalTime.parse(horaSelecionada));
			novoAgendamento.setStatus("A");
			novoAgendamento.setPago("N");
			novoAgendamento.setFuncionario(funcionarioSelecionado);
			novoAgendamento.setObservacoes(observacoes);
			
			// Define o cliente
			if ("proprio".equals(tipoAgendamento)) {
				// Busca o cliente logado
				com.barbersys.model.Usuario usuarioLogado = (com.barbersys.model.Usuario) FacesContext
						.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
				
				if (usuarioLogado != null && usuarioLogado.getClienteAssociado() != null) {
					novoAgendamento.setCliente(usuarioLogado.getClienteAssociado());
					novoAgendamento.setTipoCadastro("A");
					novoAgendamento.setNomeClienteAvulso(null);
				} else {
					exibirAlerta("error", "Cliente não encontrado na sessão!");
					return;
				}
			} else {
				// Cliente avulso
				novoAgendamento.setCliente(null);
				novoAgendamento.setTipoCadastro("I");
				novoAgendamento.setNomeClienteAvulso(nomeClienteOutro);
			}
			
			// Adiciona os serviços
			List<Servicos> servicosSelecionados = new ArrayList<>();
			for (Long servicoId : servicosSelecionadosIds) {
				Servicos servico = ServicosDAO.buscarPorId(servicoId);
				if (servico != null) {
					servicosSelecionados.add(servico);
				}
			}
			novoAgendamento.setServicos(servicosSelecionados);
			
			// Salva no banco
			AgendamentoDAO.salvar(novoAgendamento, servicosSelecionadosIds);
			
			// Cria notificação
			String nomeCliente = "proprio".equals(tipoAgendamento) 
					? novoAgendamento.getCliente().getNome() 
					: nomeClienteOutro;
			String mensagem = "Agendamento com " + nomeCliente + " às " + horaSelecionada 
					+ " pelo funcionário " + funcionarioSelecionado.getNome();
			
			com.barbersys.model.Notificacao notificacao = new com.barbersys.model.Notificacao();
			notificacao.setMensagem(mensagem);
			notificacao.setDataEnvio(new java.util.Date());
			notificacao.setAgendamento(novoAgendamento);
			notificacao.setCliente(null); // NULL = notificação para funcionários/admins
			
			com.barbersys.dao.NotificacaoDAO notificacaoDAO = new com.barbersys.dao.NotificacaoDAO();
			notificacaoDAO.salvar(notificacao);
			
			// Limpa o formulário
			limparFormularioCliente();
			
			// Atualiza a interface
			exibirAlerta("success", "Agendamento realizado com sucesso!");
			PrimeFaces.current().ajax().update("form");
			
			System.out.println("=== agendar() concluído com sucesso ===");
			
		} catch (Exception e) {
			System.err.println("ERRO em agendar(): " + e.getMessage());
			e.printStackTrace();
			exibirAlerta("error", "Ocorreu um erro ao salvar o agendamento.");
		}
	}
	
	/**
	 * Limpa o formulário do cliente após agendar
	 */
	private void limparFormularioCliente() {
		this.dataSelecionada = null;
		this.funcionarioId = null;
		this.observacoes = null;
		this.agendamentoIniciado = false;
		this.activeIndex = 0;
		this.horaSelecionada = null;
		if (this.servicosSelecionadosIds != null) {
			this.servicosSelecionadosIds.clear();
		}
		if (this.servicosSelecionadosMap != null) {
			this.servicosSelecionadosMap.clear();
		}
		if (this.horariosDisponiveis != null) {
			this.horariosDisponiveis.clear();
		}
		this.nomeClienteOutro = null;
	}
	
	// ===============================================
	// MÉTODOS PARA O MODAL DE AGENDAMENTO COM GRID VISUAL
	// ===============================================
	
	/**
	 * Método chamado quando um serviço é selecionado no modal (grid visual de cards)
	 */
	public void aoSelecionarServicoModal() {
		try {
			// Atualiza a lista servicosSelecionadosIds com base no Map
			if (servicosSelecionadosIds == null) {
				servicosSelecionadosIds = new ArrayList<>();
			} else {
				servicosSelecionadosIds.clear();
			}
			
			if (servicosSelecionadosMap != null) {
				for (Map.Entry<Long, Boolean> entry : servicosSelecionadosMap.entrySet()) {
					if (entry.getValue() != null && entry.getValue()) {
						servicosSelecionadosIds.add(entry.getKey());
					}
				}
			}
			
			// Recalcula o valor total
			calculaValorServicos();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retorna o tempo total dos serviços selecionados formatado (ex: "45min", "1h 30min")
	 */
	public String getTempoTotalServicosFormatado() {
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
			return "0min";
		}
		
		if (totalMinutos < 60) {
			return totalMinutos + "min";
		}
		
		int horas = totalMinutos / 60;
		int minutos = totalMinutos % 60;
		
		if (minutos == 0) {
			return horas + "h";
		}
		
		return horas + "h " + minutos + "min";
	}
	
	/**
	 * Formata a duração de um serviço individual (para exibir no card)
	 */
	public String formatarDuracaoServico(Integer minutos) {
		if (minutos == null || minutos == 0) {
			return "0min";
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
	 * Filtra serviços conforme o texto digitado
	 */
	public void filtrarServicos() {
		try {
			if (filtroServico == null || filtroServico.trim().isEmpty()) {
				servicosFiltrados = new ArrayList<>(lstServico);
			} else {
				servicosFiltrados.clear();
				String filtroLower = filtroServico.toLowerCase().trim();
				
				for (Servicos servico : lstServico) {
					if (servico.getNome().toLowerCase().contains(filtroLower)) {
						servicosFiltrados.add(servico);
					}
				}
			}
		} catch (Exception e) {
			System.err.println("ERRO em filtrarServicos(): " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Lista de serviços para exibição (filtrados ou todos)
	 */
	public List<Servicos> getServicosParaExibir() {
		// Se tem filtro e lista filtrada não está vazia, retorna filtrados
		if (filtroServico != null && !filtroServico.trim().isEmpty() && servicosFiltrados != null && !servicosFiltrados.isEmpty()) {
			return servicosFiltrados;
		}
		// Se tem filtro mas não encontrou nada, retorna lista vazia (para mostrar empty message)
		if (filtroServico != null && !filtroServico.trim().isEmpty()) {
			return servicosFiltrados != null ? servicosFiltrados : new ArrayList<>();
		}
		// Sem filtro, retorna todos
		return lstServico != null ? lstServico : new ArrayList<>();
	}
}

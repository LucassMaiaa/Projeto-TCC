package com.barbersys.controller;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
	private Agendamento agendamentoModel = new Agendamento();
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

	@PostConstruct
	public void init() {
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
		lstPagamento = PagamentoDAO.buscarTodos();
		lstFuncionarioDisponivel = new ArrayList<>(lstFuncionario);
	}

	public void calculaValorServicos() {
		totalGastoServicos = 0.0;
		if (agendamentoModel != null && agendamentoModel.getServicos() != null) {
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
		clienteModel = agendamentoModel.getCliente();
		editarModel = "A";
		verificaPagamento = "A";
		passos = 0;
		calculaValorServicos();
	}

	public void editAgendamento() {
	    this.passos = 0;
	    this.editarModel = "A";
	    
	    this.servicosSelecionadosIds = new ArrayList<>();
	    for (Servicos item : agendamentoModel.getServicos()) {
	        this.servicosSelecionadosIds.add(item.getId());
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
		servicosSelecionadosIds = new ArrayList<>();
		clienteId = null;
		nomeCliente = "";
		nomeFuncionario = "";
		dataSelecionada = null;
		horaSelecionada = "";
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
		return true;
	}

	public void adicionarNovoAgendamento() {
		if (!validarCamposAgendamento()) return;

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
			gerarHorariosDisponiveis();
			return;
		}

		agendamentoModel = new Agendamento();
		agendamentoModel.setStatus("A");
		agendamentoModel.setDataCriado(dataSelecionada);
		agendamentoModel.setFuncionario(funcionarioSelecionado);
		agendamentoModel.setHoraSelecionada(LocalTime.parse(horaSelecionada));
		agendamentoModel.setCliente(clienteSelecionado);
		if ("I".equals(tipoCadastro)) {
			agendamentoModel.setNomeClienteAvulso(this.nomeCliente);
		}
		agendamentoModel.setTipoCadastro(tipoCadastro);
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
		agendamentoModel.setNomeClienteAvulso(nomeCliente);
		agendamentoModel.setTipoCadastro(tipoCadastro);
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


	public void cancelarAgendamento() {
		try {
			AgendamentoDAO.cancelarAgendamento(agendamentoModel.getId());
			agendamentoModel.setStatus("I"); // Atualiza o modelo na tela

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

		if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty() || dataSelecionada == null) {
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

		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.trim())).findFirst().orElse(null);

		if (funcionarioSelecionado == null) {
			return;
		}

		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);
		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return;
		}

		Long idParaExcluir = null;
		if ("A".equals(editarModel) && agendamentoModel != null && agendamentoModel.getFuncionario() != null
				&& agendamentoModel.getFuncionario().getId().equals(funcionarioSelecionado.getId())) {
			idParaExcluir = agendamentoModel.getId();
		}
		List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(funcionarioSelecionado.getId(),
				dataSelecionada, idParaExcluir);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
		LocalDateTime agora = LocalDateTime.now();
		LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();

		if ("A".equals(editarModel) && horaSelecionada != null && !horaSelecionada.isEmpty()) {
			horariosDisponiveis.add(horaSelecionada);
		}

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

	public void funcionarioAlterado() {
	    this.horaSelecionada = "";
	    gerarHorariosDisponiveis();
	}
	
	public void addPassosAgendamento() {
		if (this.passos < 2) {
			this.passos++;
			if (this.passos == 2) {
				gerarHorariosDisponiveis();
			}
		}
	}

	public void voltaPassosAgendamento() {
		if (this.passos > 0) {
			this.passos--;
		}
	}

	public void filtrarFuncionariosPorData() {
	    horariosDisponiveis.clear();
	    horaSelecionada = "";
	    nomeFuncionario = "";

	    if (dataSelecionada == null) {
	        lstFuncionarioDisponivel = new ArrayList<>(lstFuncionario);
	        return;
	    }

	    lstFuncionarioDisponivel = new ArrayList<>();
	    LocalDateTime agora = LocalDateTime.now();
	    LocalDate dataSelecionadaLocalDate = new java.sql.Date(dataSelecionada.getTime()).toLocalDate();

	    for (Funcionario func : lstFuncionario) {
	        List<Horario> horariosTrabalho = FuncionarioDAO.buscarHorarioPorFuncionario(func);
	        if (horariosTrabalho == null || horariosTrabalho.isEmpty()) {
	            continue;
	        }

	        Long idParaExcluir = null;
	        if ("A".equals(editarModel) && agendamentoModel != null && agendamentoModel.getFuncionario() != null && agendamentoModel.getFuncionario().getId().equals(func.getId())) {
	            idParaExcluir = agendamentoModel.getId();
	        }
	        List<LocalTime> horariosOcupados = AgendamentoDAO.getHorariosOcupados(func.getId(), dataSelecionada, idParaExcluir);

	        boolean temHorarioDisponivel = false;
	        for (Horario periodo : horariosTrabalho) {
	            LocalTime horaAtual = periodo.getHoraInicial();
	            while (!horaAtual.isAfter(periodo.getHoraFinal().minusMinutes(30))) {
	                boolean isHoje = dataSelecionadaLocalDate.isEqual(LocalDate.now());
	                				boolean isHorarioFuturo = !isHoje || horaAtual.isAfter(agora.toLocalTime());
	                
	                								if (!horariosOcupados.contains(horaAtual)) {	                    temHorarioDisponivel = true;
	                    break; 
	                }
	                horaAtual = horaAtual.plusMinutes(30);
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

	public boolean isProximoDisabled() {
	    if (passos == 0) {
	        boolean clienteInvalido = ("A".equals(tipoCadastro) && clienteId == null) || 
	                                  ("I".equals(tipoCadastro) && (nomeCliente == null || nomeCliente.trim().isEmpty()));
	        return clienteInvalido || dataSelecionada == null;
	    }
	    if (passos == 1) {
	        return (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) || 
	               (servicosSelecionadosIds == null || servicosSelecionadosIds.isEmpty());
	    }
	    return false; // No "Próximo" button on step 2
	}
}
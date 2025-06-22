package com.barbersys.controller;

import java.time.LocalDate;
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
public class AgendamentoController {

	private String tipoCadastro = "A";
	private String nomeCliente;
	private String nomeClienteFiltro;
	private String nomeFuncionario;
	private String nomeFuncionarioFiltro;
	private String tipoPagamento;
	private Double totalGastoServicos = 0.0;
	private Date dataSelecionada;
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

	@PostConstruct
	public void init() {
		lstAgendamentos = new LazyDataModel<Agendamento>() {
			private static final long serialVersionUID = 1L;

			@Override
			public List<Agendamento> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				List<Agendamento> agendamentos = AgendamentoDAO.buscarAgendamentos(nomeClienteFiltro,
						nomeFuncionarioFiltro, statusSelecionado);
				this.setRowCount(count(filterBy));
				return agendamentos;
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return AgendamentoDAO.agendamentoCount(nomeClienteFiltro, nomeFuncionarioFiltro, statusSelecionado);
			}

			@Override
			public String getRowKey(Agendamento agendamento) {
				Long id = agendamento.getId();
				return id != null ? id.toString() : "";
			}

			@Override
			public Agendamento getRowData(String rowKey) {
				try {
					Long id = Long.parseLong(rowKey);
					List<Agendamento> all = AgendamentoDAO.buscarAgendamentos(nomeClienteFiltro, nomeFuncionarioFiltro,
							statusSelecionado);
					for (Agendamento ag : all) {
						if (ag.getId().equals(id)) {
							return ag;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};

		lstFuncionario = FuncionarioDAO.buscarTodosFuncionarios();
		lstCliente = ClienteDAO.buscarTodosClientes();
		lstServico = ServicosDAO.buscarTodosServico();
		lstPagamento = PagamentoDAO.buscarTodosPagamento();
	}

	public void calculaValorServicos() {
		for (Servicos item : agendamentoModel.getServicos()) {
			totalGastoServicos += item.getPreco();
		}
	}
	
	public void confirmaPagamentoPedido() {
		CaixaData caixaDataModel = new CaixaData();
		Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataAtual);
		if (tipoPagamento == null || tipoPagamento.isEmpty()) {
			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'warning',"
							+ "  title: '<span style=\"font-size: 14px\">Selecione uma forma de pagamento!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			return;
		}
		
		String tipoPagamentoFiltro = tipoPagamento.toLowerCase().trim();


		if (checkData.isEmpty() && tipoPagamentoFiltro.equals("dinheiro")) {
			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'error',"
							+ "  title: '<span style=\"font-size: 14px\">Caixa precisa ser aberto!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			return;
		}
		for (CaixaData item : checkData) {
			if (item.getStatus().equals("I") && tipoPagamentoFiltro.equals("dinheiro")) {
				PrimeFaces.current()
						.executeScript("Swal.fire({" + "  icon: 'error',"
								+ "  title: '<span style=\"font-size: 14px\">Caixa precisa ser aberto!</span>',"
								+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
				return;
			}
		}
		if (tipoPagamentoFiltro.equals("dinheiro")) {
			for (CaixaData item : checkData) {
				caixaDataModel = item;
			}
			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataAtual);
			controleCaixaModel.setMovimentacao("Entrada automática");
			controleCaixaModel.setValor(totalGastoServicos);

			ControleCaixaDAO.salvar(controleCaixaModel);

			controleCaixaModel = new ControleCaixa();
		}
		
		verificaPagamento = "I";

		PrimeFaces.current()
		.executeScript("Swal.fire({" + "  icon: 'success',"
				+ "  title: '<span style=\"font-size: 14px\">Pagamento realizado com sucesso!</span>',"
				+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
	}

	public void finalizarPedido() {
		CaixaData caixaDataModel = new CaixaData();
		Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataAtual);
		if (tipoPagamento == null || tipoPagamento.isEmpty()) {
			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'warning',"
							+ "  title: '<span style=\"font-size: 14px\">Selecione uma forma de pagamento!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			return;
		}
		
		String tipoPagamentoFiltro = tipoPagamento.toLowerCase().trim();


		if (checkData.isEmpty() && tipoPagamentoFiltro.equals("dinheiro")) {
			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'error',"
							+ "  title: '<span style=\"font-size: 14px\">Caixa precisa ser aberto!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			return;
		}
		for (CaixaData item : checkData) {
			if (item.getStatus().equals("I") && tipoPagamentoFiltro.equals("dinheiro")) {
				PrimeFaces.current()
						.executeScript("Swal.fire({" + "  icon: 'error',"
								+ "  title: '<span style=\"font-size: 14px\">Caixa precisa ser aberto!</span>',"
								+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
				return;
			}
		}
		if (tipoPagamentoFiltro.equals("dinheiro")) {
			for (CaixaData item : checkData) {
				caixaDataModel = item;
			}
			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataAtual);
			controleCaixaModel.setMovimentacao("Entrada automática");
			controleCaixaModel.setValor(totalGastoServicos);

			ControleCaixaDAO.salvar(controleCaixaModel);

			controleCaixaModel = new ControleCaixa();
		}

		deletarAgendamento();
	}

	public void fecharCliente() {
		clienteModel = new Cliente();
		totalGastoServicos = 0.0;
		PrimeFaces.current().ajax().update("form");
		servicosSelecionadosIds = new ArrayList<Long>();
		agendamentoModel = new Agendamento();
		tipoPagamento = "";
	}

	public void agendamentoSelecionado() {
		clienteModel = agendamentoModel.getCliente();
		editarModel = "A";
		verificaPagamento = "A";
		totalGastoServicos = 0.0;
		calculaValorServicos();
	}

	public void editAgendamento() {
		servicosSelecionadosIds = new ArrayList<Long>();
		if(agendamentoModel.getTipoCadastro().equals("A")) {
			nomeCliente = agendamentoModel.getCliente().getNome();
		}else {
			nomeCliente = agendamentoModel.getNomeClienteAvulso();
		}	
		tipoCadastro = agendamentoModel.getTipoCadastro();
		nomeFuncionario = agendamentoModel.getFuncionario().getNome();
		dataSelecionada = agendamentoModel.getDataCriado();
		horaSelecionada = agendamentoModel.getHoraSelecionada().toString();
		for (Servicos item : agendamentoModel.getServicos()) {
			servicosSelecionadosIds.add(item.getId());
		}

		gerarHorariosDisponiveis();
	}

	public void novoAgendamento() {
		editarModel = "I";
		tipoCadastro = "A";
		agendamentoModel = new Agendamento();
		agendamentoModel.setServicos(new ArrayList<>());
		servicosSelecionadosIds = new ArrayList<Long>();
		nomeCliente = "";
		nomeFuncionario = "";
		dataSelecionada = null;
		horaSelecionada = "";
	}

	public void adicionarNovoAgendamento() {
		agendamentoModel = new Agendamento();

		if (nomeCliente == null || nomeCliente.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente é obrigatório", "Erro!"));
			return;
		}
		if (nomeFuncionario == null || nomeFuncionario.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do funcionário é obrigatório", "Erro!"));
			return;
		}
		if (dataSelecionada == null) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo de data é obrigatório", "Erro!"));
			return;
		}
		if (horaSelecionada == null || horaSelecionada.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo horário é obrigatório", "Erro!"));
			return;
		}
		if (servicosSelecionadosIds.size() < 1) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione pelo menos 1 serviço", "Erro!"));
			return;
		}

		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.toString().trim())).findFirst().orElse(null);

		Cliente clienteSelecionado = lstCliente.stream()
				.filter(c -> c.getNome().equalsIgnoreCase(nomeCliente.toString().trim())).findFirst().orElse(null);

		agendamentoModel.setStatus("A");
		agendamentoModel.setDataCriado(dataSelecionada);
		agendamentoModel.setFuncionario(funcionarioSelecionado);
		agendamentoModel.setHoraSelecionada(LocalTime.parse(horaSelecionada));
		agendamentoModel.setCliente(clienteSelecionado);
		agendamentoModel.setNomeClienteAvulso(nomeCliente);
		agendamentoModel.setTipoCadastro(tipoCadastro);

		AgendamentoDAO.salvar(agendamentoModel, servicosSelecionadosIds);
		agendamentoModel = new Agendamento();
		tipoPagamento = "";

		PrimeFaces.current().executeScript("PF('dlgAgendar').hide();");
		PrimeFaces.current().ajax().update("form");

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "icon: 'success',"
						+ "title: '<span style=\"font-size: 14px\">Agendamento cadastrado com sucesso!</span>',"
						+ "showConfirmButton: false," + "timer: 2000," + "width: '350px'" + "});");
	}

	public void atualizarAgendamento() {
		if (nomeCliente == null || nomeCliente.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente é obrigatório", "Erro!"));
			return;
		}
		if (nomeFuncionario == null || nomeFuncionario.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do funcionário é obrigatório", "Erro!"));
			return;
		}
		if (dataSelecionada == null || dataSelecionada.toString().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo de data é obrigatório", "Erro!"));
			return;
		}
		if (horaSelecionada == null || horaSelecionada.isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo horário é obrigatório", "Erro!"));
			return;
		}
		if (servicosSelecionadosIds.size() < 1) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione pelo menos 1 serviço", "Erro!"));
			return;
		}

		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.toString().trim())).findFirst().orElse(null);

		Cliente clienteSelecionado = lstCliente.stream()
				.filter(c -> c.getNome().equalsIgnoreCase(nomeCliente.toString().trim())).findFirst().orElse(null);

		agendamentoModel.setStatus("A");
		agendamentoModel.setDataCriado(dataSelecionada);
		agendamentoModel.setFuncionario(funcionarioSelecionado);
		agendamentoModel.setCliente(clienteSelecionado);
		agendamentoModel.setHoraSelecionada(LocalTime.parse(horaSelecionada));
		agendamentoModel.setNomeClienteAvulso(nomeCliente);

		AgendamentoDAO.atualizar(agendamentoModel, servicosSelecionadosIds);

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "icon: 'success',"
						+ "title: '<span style=\\\"font-size: 14px\\\">Agendamento atualizado com sucesso!</span>',"
						+ "showConfirmButton: false," + "timer: 2000," + "width: '350px'" + "});");

		PrimeFaces.current().executeScript("PF('dlgAgendar').hide();");

		PrimeFaces.current()
				.executeScript("setTimeout(function() {" + "PrimeFaces.ab({s:'form', u:'form'});" + "}, 50);");
	}

	public void deletarAgendamento() {
		AgendamentoDAO.deletarAgendamento(agendamentoModel.getId());
		clienteModel = new Cliente();

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "icon: 'success',"
						+ "title: '<span style=\"font-size: 14px\">Agendamento finalizado com sucesso!</span>',"
						+ "showConfirmButton: false," + "timer: 2000," + "width: '350px'" + "});");

		PrimeFaces.current().executeScript("PF('dlgAgendamento').hide();");
		PrimeFaces.current().executeScript("PF('dlgConfirmAgendamento').hide();");
		PrimeFaces.current().ajax().update("form");
	}

	public void gerarHorariosDisponiveis() {
		horariosDisponiveis.clear();

		if (nomeFuncionario == null || nomeFuncionario.trim().isEmpty()) {
			return;
		}

		Funcionario funcionarioSelecionado = lstFuncionario.stream()
				.filter(f -> f.getNome().equalsIgnoreCase(nomeFuncionario.toString().trim())).findFirst().orElse(null);

		if (funcionarioSelecionado == null || funcionarioSelecionado.getId() == null) {
			return;
		}

		List<Horario> horariosFuncionario = FuncionarioDAO.buscarHorarioPorFuncionario(funcionarioSelecionado);

		if (horariosFuncionario == null || horariosFuncionario.isEmpty()) {
			return;
		}

		LocalTime menorHoraInicio = LocalTime.MAX;
		LocalTime maiorHoraFim = LocalTime.MIN;

		for (Horario h : horariosFuncionario) {
			if (h.getHoraInicial().isBefore(menorHoraInicio)) {
				menorHoraInicio = h.getHoraInicial();
			}
			if (h.getHoraFinal().isAfter(maiorHoraFim)) {
				maiorHoraFim = h.getHoraFinal();
			}
		}

		LocalTime atual = menorHoraInicio;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

		while (!atual.isAfter(maiorHoraFim.minusMinutes(30))) {
			horariosDisponiveis.add(atual.format(formatter));
			atual = atual.plusMinutes(30);
		}
	}
}

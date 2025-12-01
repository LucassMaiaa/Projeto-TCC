package com.barbersys.controller;

import com.barbersys.dao.CaixaDataDAO;
import com.barbersys.dao.ControleCaixaDAO;
import com.barbersys.dao.GenericDAO;
import com.barbersys.model.CaixaData;
import com.barbersys.model.ControleCaixa;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

/**
 *
 * @author Lucas
 */

@Getter
@Setter
@ManagedBean(name = "controleCaixaController")
@ViewScoped
public class ControleCaixaController implements Serializable {

	private static final long serialVersionUID = 1L;

	private String statusSelecionado;
	private String statusSelecionadoAnterior; // Guarda o status antes do clique
	private String motivoFinal;
	private String mensagemMotivoFinal = "";
	private Double valorInicial = 0.0;
	private Double valorFinal = 0.0;
	private Double valorInicialTemp;  // Variável temporária para o modal
	private Double valorFinalTemp;    // Variável temporária para o modal
	private Boolean dadosLiberados;
	private Double valorSugerido = 0.0;
	private Boolean chaveCaixa = false;
	private Date dataSelecionada = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
	private LazyDataModel<ControleCaixa> lstControleCaixa;
	private ControleCaixa controleCaixaModel = new ControleCaixa();
	private CaixaData caixaDataModel = new CaixaData();
	private Double totalEntradas;
	private Double totalEntradasMes;
	private Double totalSaidasMes;
	private Double totalSaidas;
	private String tipodeValor = "e";
	private String filtroTipoDeValor = "";
	private DateTimeFormatter horaFormatada = DateTimeFormatter.ofPattern("HH:mm");
	private SimpleDateFormat dataFormatada = new SimpleDateFormat("dd/MM/yyyy");

	@PostConstruct
	public void init() {
		lstControleCaixa = new LazyDataModel<ControleCaixa>() {

			@Override
			public List<ControleCaixa> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				
				// Extrai o campo e a direção da ordenação
				String sortField = "id"; // padrão
				String sortOrder = "DESC"; // padrão
				
				if (sortBy != null && !sortBy.isEmpty()) {
					SortMeta sortMeta = sortBy.values().iterator().next();
					sortField = sortMeta.getField();
					sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
				}
				
				return ControleCaixaDAO.buscarCaixasPaginado(first, pageSize, dataSelecionada, filtroTipoDeValor, sortField, sortOrder);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return ControleCaixaDAO.contarTotalCaixas(dataSelecionada, filtroTipoDeValor);
			}

		};
		getValoresRegistro();
		calcularTotal();
	}

	// Calcula totais de entradas e saídas do dia e do mês
	public void calcularTotal() {
		List<Map<String, Object>> listaPorDia = ControleCaixaDAO.buscarCaixasContagem(dataSelecionada);
		List<Map<String, Object>> listaPorMes = ControleCaixaDAO.buscarCaixasContagemPorMes(dataSelecionada);

		for (Map<String, Object> caixa : listaPorDia) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradas = tipoValorEntrada != null ? tipoValorEntrada : 0.0;
			this.totalSaidas = tipoValorSaida != null ? tipoValorSaida : 0.0;
		}
		
		if (listaPorDia.isEmpty()) {
			this.totalEntradas = 0.0;
			this.totalSaidas = 0.0;
		}

		for (Map<String, Object> caixa : listaPorMes) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradasMes = tipoValorEntrada != null ? tipoValorEntrada : 0.0;
			this.totalSaidasMes = tipoValorSaida != null ? tipoValorSaida : 0.0;
		}
		
		if (listaPorMes.isEmpty()) {
			this.totalEntradasMes = 0.0;
			this.totalSaidasMes = 0.0;
		}
	}

	// Prepara modal de abertura/fechamento do caixa
	public void prepararModal() {
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
		String statusAtualBanco = "I";
		if (!checkData.isEmpty()) {
			statusAtualBanco = checkData.get(0).getStatus();
		}
		
		statusSelecionadoAnterior = statusAtualBanco;
		
		if (statusSelecionado.equals("A")) {
			if (statusAtualBanco.equals("I")) {
				valorInicialTemp = null;
				PrimeFaces.current().ajax().addCallbackParam("aberto", true);
			} else {
				statusSelecionado = statusAtualBanco;
				PrimeFaces.current().ajax().addCallbackParam("aberto", false);
			}
		} else if (statusSelecionado.equals("I")) {
			if (statusAtualBanco.equals("A")) {
				buscaValorSugerido();
				valorFinalTemp = valorSugerido;
				mensagemMotivoFinal = "";
				PrimeFaces.current().ajax().addCallbackParam("aberto", true);
			} else {
				statusSelecionado = statusAtualBanco;
				PrimeFaces.current().ajax().addCallbackParam("aberto", false);
			}
		}
	}

	// Busca o valor sugerido para fechamento do caixa
	public void buscaValorSugerido() {
		Map<String, Double> valoresFechamento = ControleCaixaDAO
				.buscarEntradasESaidasDesdeUltimaAbertura(dataSelecionada);

		Double tipoValorEntrada = valoresFechamento.getOrDefault("entrada", 0.0);
		Double tipoValorSaida = valoresFechamento.getOrDefault("saida", 0.0);

		this.valorSugerido = (tipoValorEntrada - tipoValorSaida) + caixaDataModel.getValorInicial();
	}

	// Verifica se a data é passada e realiza fechamento automático se necessário
	public void verificaData() {
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);

		Map<String, Double> valoresFechamento = ControleCaixaDAO
				.buscarEntradasESaidasDesdeUltimaAbertura(dataSelecionada);

		String dataSelecionadaFormatada = dataFormatada.format(this.dataSelecionada);
		Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
		String dataAtualFormatada = dataFormatada.format(dataAtual);

		Double tipoValorEntrada = valoresFechamento.getOrDefault("entrada", 0.0);
		Double tipoValorSaida = valoresFechamento.getOrDefault("saida", 0.0);

		if (!dataSelecionadaFormatada.equals(dataAtualFormatada)) {
			chaveCaixa = true;

			if (!checkData.isEmpty()) {
				for (CaixaData item : checkData) {
					caixaDataModel.setId(item.getId());
					caixaDataModel.setValorInicial(item.getValorInicial());
					caixaDataModel.setValorFinal(item.getValorInicial() + (tipoValorEntrada - tipoValorSaida));

					if (!"I".equals(item.getStatus())) {
						caixaDataModel.setStatus("I");
						CaixaDataDAO.atualizar(caixaDataModel);
						calcularTotal();

						boolean ultimoRegistroFechamento = ControleCaixaDAO.ultimoRegistroEhFechamento(dataSelecionada);

						if (!ultimoRegistroFechamento) {
							controleCaixaModel.setCaixaData(caixaDataModel);
							controleCaixaModel.setHoraAtual("23:59");
							controleCaixaModel.setData(dataSelecionada);
							controleCaixaModel.setValor(tipoValorEntrada - tipoValorSaida);
							controleCaixaModel.setMovimentacao("Fechamento de Caixa");
							controleCaixaModel.setMotivo("Fechamento automático, valor final de caixa não informado");

							ControleCaixaDAO.salvar(controleCaixaModel);

							controleCaixaModel = new ControleCaixa();
							mensagemMotivoFinal = "";
							calcularTotal();
							dadosLiberados = true;
						}
					}
				}
			}
		} else {
			chaveCaixa = false;
		}

		calcularTotal();
		getValoresRegistro();
	}

	// Carrega valores do caixa do banco de dados
	public void getValoresRegistro() {
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);

		if (!checkData.isEmpty()) {
			for (CaixaData item : checkData) {
				caixaDataModel.setId(item.getId());
				caixaDataModel.setValorInicial(item.getValorInicial());
				caixaDataModel.setValorFinal(item.getValorFinal());
				caixaDataModel.setDataCadastro(item.getDataCadastro());
				caixaDataModel.setStatus(item.getStatus());

				valorInicial = caixaDataModel.getValorInicial();
				valorFinal = caixaDataModel.getValorFinal();
				statusSelecionado = caixaDataModel.getStatus();

				dadosLiberados = statusSelecionado.equals("I");
			}
		} else {
			valorInicial = 0.0;
			valorFinal = 0.0;
			statusSelecionado = "I";
			dadosLiberados = true;
		}
	}

	// Salva o fechamento do caixa
	public void salvarRegistroFinal() {
		valorFinal = (valorFinalTemp != null) ? valorFinalTemp : 0.0;
		
		if (valorFinal == null || valorFinal < 0.0) {
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Erro!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe o valor final do caixa.");
			return;
		}
		
		if (valorFinal < valorSugerido && (mensagemMotivoFinal == null || mensagemMotivoFinal.trim().isEmpty())) {
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Atenção!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe o motivo da diferença de valor.");
			return;
		}
		
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
		controleCaixaModel.setMotivo(mensagemMotivoFinal);

		for (CaixaData item : checkData) {
			caixaDataModel.setId(item.getId());
			caixaDataModel.setValorInicial(item.getValorInicial());
			caixaDataModel.setValorFinal(valorFinal);
			caixaDataModel.setStatus("I");
		}
		
		statusSelecionado = "I";
		motivoFinal = "I";
		CaixaDataDAO.atualizar(caixaDataModel);

		String horaAtualFormatada = LocalTime.now().format(horaFormatada);
		controleCaixaModel.setCaixaData(caixaDataModel);
		controleCaixaModel.setHoraAtual(horaAtualFormatada);
		controleCaixaModel.setData(dataSelecionada);
		controleCaixaModel.setValor(valorFinal);
		controleCaixaModel.setMovimentacao("Fechamento de Caixa");
		ControleCaixaDAO.salvar(controleCaixaModel);

		controleCaixaModel = new ControleCaixa();
		mensagemMotivoFinal = "";
		getValoresRegistro();
		calcularTotal();
		dadosLiberados = true;
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Caixa fechado com sucesso!");
	}
	
	// Formata valor monetário para exibição
	public String formatarValor(Double valor) {
		if (valor == null) return "R$ 0,00";
		return String.format("R$ %,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
	}

	// Salva abertura do caixa
	public void salvarRegistroInicial() {
		valorInicial = (valorInicialTemp != null) ? valorInicialTemp : 0.0;
		
		if (valorInicial == null || valorInicial < 0.0) {
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Erro!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe um valor inicial válido (maior que zero).");
			return;
		}
		
		statusSelecionado = "A";
		
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);

		if (checkData.isEmpty()) {
			caixaDataModel.setValorInicial(valorInicial);
			caixaDataModel.setValorFinal(0.0);
			caixaDataModel.setDataCadastro(new Date());
			caixaDataModel.setStatus("A");
			CaixaDataDAO.salvar(caixaDataModel);

			List<CaixaData> searchData = CaixaDataDAO.verificaExisteData(dataSelecionada);

			for (CaixaData item : searchData) {
				caixaDataModel.setId(item.getId());
			}
			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataSelecionada);
			controleCaixaModel.setValor(valorInicial);
			controleCaixaModel.setMovimentacao("Abertura de Caixa");
			ControleCaixaDAO.salvar(controleCaixaModel);

			controleCaixaModel = new ControleCaixa();

		} else {
			for (CaixaData item : checkData) {
				caixaDataModel.setId(item.getId());
				caixaDataModel.setValorInicial(valorInicial);
				caixaDataModel.setValorFinal(0.0);
				caixaDataModel.setStatus("A");
				CaixaDataDAO.atualizar(caixaDataModel);
			}

			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataSelecionada);
			controleCaixaModel.setValor(valorInicial);
			controleCaixaModel.setMovimentacao("Abertura de Caixa");
			ControleCaixaDAO.salvar(controleCaixaModel);

			valorFinal = caixaDataModel.getValorFinal();

			controleCaixaModel = new ControleCaixa();
		}
		dadosLiberados = false;
		getValoresRegistro();
		calcularTotal();
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Caixa aberto com sucesso!");
	}

	// Registra entrada ou saída manual no caixa
	public void salvarValores() {
		if (controleCaixaModel.getValor() == null || controleCaixaModel.getValor() <= 0) {
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Erro!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe um valor válido (maior que zero).");
			return;
		}

		String horaAtualFormatada = LocalTime.now().format(horaFormatada);
		controleCaixaModel.setCaixaData(caixaDataModel);
		controleCaixaModel.setHoraAtual(horaAtualFormatada);
		controleCaixaModel.setData(dataSelecionada);

		if (this.tipodeValor.equals("e")) {
			controleCaixaModel.setMovimentacao("Entrada");
		} else if (this.tipodeValor.equals("s")) {
			controleCaixaModel.setMovimentacao("Saida");
		}

		ControleCaixaDAO.salvar(controleCaixaModel);

		controleCaixaModel = new ControleCaixa();
		calcularTotal();
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Movimentação registrada com sucesso!");
	}

}
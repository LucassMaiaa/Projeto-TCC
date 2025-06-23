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
import javax.faces.bean.SessionScoped;
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
@ManagedBean
@ViewScoped
public class ControleCaixaController implements Serializable {

	private String statusSelecionado;
	private String motivoFinal;
	private String mensagemMotivoFinal = "";
	private Double valorInicial = 0.0;
	private Double valorFinal = 0.0;
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
				return ControleCaixaDAO.buscarCaixasPaginado(first, pageSize, dataSelecionada, filtroTipoDeValor);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return ControleCaixaDAO.contarTotalCaixas(dataSelecionada, filtroTipoDeValor);
			}

		};
		getValoresRegistro();
		calcularTotal();
	}

	public void calcularTotal() {
		List<Map<String, Object>> listaPorDia = ControleCaixaDAO.buscarCaixasContagem(dataSelecionada);
		List<Map<String, Object>> listaPorMes = ControleCaixaDAO.buscarCaixasContagemPorMes(dataSelecionada);

		for (Map<String, Object> caixa : listaPorDia) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradas = tipoValorEntrada;
			this.totalSaidas = tipoValorSaida;
		}

		for (Map<String, Object> caixa : listaPorMes) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradasMes = tipoValorEntrada;
			this.totalSaidasMes = tipoValorSaida;
		}

	}

	public void verificaTravaCampo() {
		PrimeFaces.current().ajax().addCallbackParam("aberto", true);
		if (statusSelecionado.equals("A")) {
			valorInicial = 0.0;
		} else {
			buscaValorSugerido();
		}
	}

	public void buscaValorSugerido() {
		Map<String, Double> valoresFechamento = ControleCaixaDAO
				.buscarEntradasESaidasDesdeUltimaAbertura(dataSelecionada);

		Double tipoValorEntrada = valoresFechamento.getOrDefault("entrada", 0.0);
		Double tipoValorSaida = valoresFechamento.getOrDefault("saida", 0.0);

		this.valorSugerido = (tipoValorEntrada - tipoValorSaida) + caixaDataModel.getValorInicial();
	}

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

				if (statusSelecionado.equals("A")) {
					dadosLiberados = false;
				} else {
					dadosLiberados = true;
				}
			}
		} else {
			valorInicial = 0.0;
			valorFinal = 0.0;
			statusSelecionado = "I";
			dadosLiberados = true;
		}

	}

	public void salvarRegistroFinal() {
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
		controleCaixaModel.setMotivo(mensagemMotivoFinal);

		for (CaixaData item : checkData) {
			caixaDataModel.setId(item.getId());
			caixaDataModel.setValorInicial(item.getValorInicial());
			caixaDataModel.setValorFinal(valorFinal);
			caixaDataModel.setStatus(statusSelecionado);
		}
		if(valorSugerido < 0 ) {
			if(Math.abs(valorFinal - valorSugerido) > 0.0001 || controleCaixaModel.getMotivo().trim().isEmpty()) {
				motivoFinal = "A";
				PrimeFaces.current().ajax().addCallbackParam("validado", false);
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Valor negativo! informe o valor exato que o valor sugerido informou e o motivo."));
			}else {
				motivoFinal = "I";
				CaixaDataDAO.atualizar(caixaDataModel);

				String horaAtualFormatada = LocalTime.now().format(horaFormatada);
				controleCaixaModel.setCaixaData(caixaDataModel);
				controleCaixaModel.setHoraAtual(horaAtualFormatada);
				controleCaixaModel.setData(dataSelecionada);
				controleCaixaModel.setValor(valorFinal - valorInicial);
				controleCaixaModel.setMovimentacao("Fechamento de Caixa");
				ControleCaixaDAO.salvar(controleCaixaModel);

				controleCaixaModel = new ControleCaixa();
				mensagemMotivoFinal = "";
				calcularTotal();
				dadosLiberados = true;
				PrimeFaces.current().ajax().addCallbackParam("validado", true);
				PrimeFaces.current().ajax().update("form");
			}
		}else if (caixaDataModel.getValorFinal() < caixaDataModel.getValorInicial() && controleCaixaModel.getMotivo().isEmpty()
				|| caixaDataModel.getValorFinal() < valorSugerido && controleCaixaModel.getMotivo().isEmpty()) {
			motivoFinal = "A";
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "O campo motivo é obrigatório."));
		} else {
			motivoFinal = "I";
			CaixaDataDAO.atualizar(caixaDataModel);

			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataSelecionada);
			controleCaixaModel.setValor(valorFinal - valorInicial);
			controleCaixaModel.setMovimentacao("Fechamento de Caixa");
			ControleCaixaDAO.salvar(controleCaixaModel);

			controleCaixaModel = new ControleCaixa();
			mensagemMotivoFinal = "";
			calcularTotal();
			dadosLiberados = true;
			PrimeFaces.current().ajax().addCallbackParam("validado", true);
			PrimeFaces.current().ajax().update("form");
		}

	}

	public void salvarRegistroInicial() {
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);

		if (checkData.isEmpty()) {
			caixaDataModel.setValorInicial(valorInicial);
			caixaDataModel.setValorFinal(0.0);
			caixaDataModel.setDataCadastro(new Date());
			caixaDataModel.setStatus(statusSelecionado);
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
				caixaDataModel.setStatus(statusSelecionado);
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
	}

	public void salvarValores() {

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

	}

}

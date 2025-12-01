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

	public void calcularTotal() {
		System.out.println("📊 ===== CALCULANDO TOTAIS =====");
		System.out.println("📅 Data selecionada: " + dataFormatada.format(dataSelecionada));
		
		List<Map<String, Object>> listaPorDia = ControleCaixaDAO.buscarCaixasContagem(dataSelecionada);
		List<Map<String, Object>> listaPorMes = ControleCaixaDAO.buscarCaixasContagemPorMes(dataSelecionada);

		System.out.println("📋 Registros do DIA: " + listaPorDia.size());
		for (Map<String, Object> caixa : listaPorDia) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradas = tipoValorEntrada != null ? tipoValorEntrada : 0.0;
			this.totalSaidas = tipoValorSaida != null ? tipoValorSaida : 0.0;
			
			System.out.println("💵 Total Entradas DIA: R$ " + String.format("%.2f", this.totalEntradas));
			System.out.println("💸 Total Saídas DIA: R$ " + String.format("%.2f", this.totalSaidas));
		}
		
		if (listaPorDia.isEmpty()) {
			this.totalEntradas = 0.0;
			this.totalSaidas = 0.0;
			System.out.println("⚠️ Nenhum registro encontrado para o dia");
		}

		System.out.println("📋 Registros do MÊS: " + listaPorMes.size());
		for (Map<String, Object> caixa : listaPorMes) {
			Double tipoValorEntrada = (Double) caixa.get("entrada");
			Double tipoValorSaida = (Double) caixa.get("saida");

			this.totalEntradasMes = tipoValorEntrada != null ? tipoValorEntrada : 0.0;
			this.totalSaidasMes = tipoValorSaida != null ? tipoValorSaida : 0.0;
			
			System.out.println("💵 Total Entradas MÊS: R$ " + String.format("%.2f", this.totalEntradasMes));
			System.out.println("💸 Total Saídas MÊS: R$ " + String.format("%.2f", this.totalSaidasMes));
		}
		
		if (listaPorMes.isEmpty()) {
			this.totalEntradasMes = 0.0;
			this.totalSaidasMes = 0.0;
			System.out.println("⚠️ Nenhum registro encontrado para o mês");
		}
		
		System.out.println("📊 ===== FIM DO CÁLCULO =====\n");
	}

	public void prepararModal() {
		System.out.println("========================================");
		System.out.println("🔧 PREPARANDO MODAL - MÉTODO CHAMADO!");
		System.out.println("========================================");
		System.out.println("📊 statusSelecionado (NOVO - clicado): " + statusSelecionado);
		
		// Pega o status ATUAL do banco
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
		String statusAtualBanco = "I";
		if (!checkData.isEmpty()) {
			statusAtualBanco = checkData.get(0).getStatus();
		}
		
		System.out.println("📊 statusAtualBanco (do BD): " + statusAtualBanco);
		
		// Guarda o status anterior (do banco) para restaurar se necessário
		statusSelecionadoAnterior = statusAtualBanco;
		
		// Se clicou em "Aberto"
		if (statusSelecionado.equals("A")) {
			System.out.println("🔹 Usuario clicou em ABERTO");
			// Se banco está "Fechado", pode abrir
			if (statusAtualBanco.equals("I")) {
				valorInicialTemp = null;
				System.out.println("✅ Caixa FECHADO no banco → PODE ABRIR → abrindo modal");
				PrimeFaces.current().ajax().addCallbackParam("aberto", true);
			} 
			// Se banco JÁ está "Aberto", restaura o status e não abre modal
			else {
				System.out.println("❌ Caixa JÁ está ABERTO no banco → NÃO PODE abrir novamente → revertendo");
				statusSelecionado = statusAtualBanco; // Restaura para "A"
				PrimeFaces.current().ajax().addCallbackParam("aberto", false);
			}
		} 
		// Se clicou em "Fechado"
		else if (statusSelecionado.equals("I")) {
			System.out.println("🔹 Usuario clicou em FECHADO");
			// Se banco está "Aberto", pode fechar
			if (statusAtualBanco.equals("A")) {
				buscaValorSugerido();
				valorFinalTemp = valorSugerido;
				mensagemMotivoFinal = "";
				System.out.println("✅ Caixa ABERTO no banco → PODE FECHAR → abrindo modal");
				System.out.println("💰 Valor sugerido calculado: " + valorSugerido);
				PrimeFaces.current().ajax().addCallbackParam("aberto", true);
			}
			// Se banco JÁ está "Fechado", restaura o status e não abre modal
			else {
				System.out.println("❌ Caixa JÁ está FECHADO no banco → NÃO PODE fechar novamente → revertendo");
				statusSelecionado = statusAtualBanco; // Restaura para "I"
				PrimeFaces.current().ajax().addCallbackParam("aberto", false);
			}
		}
		System.out.println("========================================");
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
		System.out.println("⭐ MÉTODO CHAMADO: salvarRegistroFinal");
		System.out.println("📊 valorFinalTemp RECEBIDO: " + valorFinalTemp);
		
		// Transfere o valor temporário para o valor definitivo
		valorFinal = (valorFinalTemp != null) ? valorFinalTemp : 0.0;
		
		System.out.println("📊 valorFinal DEPOIS: " + valorFinal);
		System.out.println("📊 valorSugerido: " + valorSugerido);
		System.out.println("📊 mensagemMotivoFinal: " + mensagemMotivoFinal);
		
		// Validação 1: Valor final deve ser informado
		if (valorFinal == null || valorFinal < 0.0) {
			System.out.println("❌ Valor final não informado");
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Erro!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe o valor final do caixa.");
			return;
		}
		
		// Validação 2: Se valor final < valor sugerido, motivo é obrigatório
		if (valorFinal < valorSugerido && (mensagemMotivoFinal == null || mensagemMotivoFinal.trim().isEmpty())) {
			System.out.println("❌ Valor final menor que sugerido sem motivo");
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Atenção!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe o motivo da diferença de valor.");
			return;
		}
		
		System.out.println("✅ Validações passaram - prosseguindo com fechamento");
		
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
		controleCaixaModel.setMotivo(mensagemMotivoFinal);

		for (CaixaData item : checkData) {
			caixaDataModel.setId(item.getId());
			caixaDataModel.setValorInicial(item.getValorInicial());
			caixaDataModel.setValorFinal(valorFinal);
			caixaDataModel.setStatus("I");
		}
		
		// Altera o status para Fechado
		statusSelecionado = "I";
		motivoFinal = "I";
		CaixaDataDAO.atualizar(caixaDataModel);

		String horaAtualFormatada = LocalTime.now().format(horaFormatada);
		controleCaixaModel.setCaixaData(caixaDataModel);
		controleCaixaModel.setHoraAtual(horaAtualFormatada);
		controleCaixaModel.setData(dataSelecionada);
		controleCaixaModel.setValor(valorFinal); // Valor do fechamento é o valor final
		controleCaixaModel.setMovimentacao("Fechamento de Caixa");
		ControleCaixaDAO.salvar(controleCaixaModel);

		controleCaixaModel = new ControleCaixa();
		mensagemMotivoFinal = "";
		getValoresRegistro(); // Atualiza statusSelecionado do banco
		calcularTotal();
		dadosLiberados = true;
		
		System.out.println("🎉 SUCESSO! Caixa fechado com valor: " + valorFinal);
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Caixa fechado com sucesso!");
	}
	
	public String formatarValor(Double valor) {
		if (valor == null) return "R$ 0,00";
		return String.format("R$ %,.2f", valor).replace(",", "X").replace(".", ",").replace("X", ".");
	}

	public void salvarRegistroInicial() {
		System.out.println("⭐ MÉTODO CHAMADO: salvarRegistroInicial");
		System.out.println("📊 valorInicialTemp RECEBIDO: " + valorInicialTemp);
		System.out.println("📊 valorInicial ANTES: " + valorInicial);
		
		// Transfere o valor temporário para o valor definitivo
		valorInicial = (valorInicialTemp != null) ? valorInicialTemp : 0.0;
		
		System.out.println("📊 valorInicial DEPOIS: " + valorInicial);
		
		// Validação do valor inicial
		if (valorInicial == null || valorInicial < 0.0) {
			System.out.println("❌ Validação FALHOU!");
			PrimeFaces.current().ajax().addCallbackParam("validado", false);
			PrimeFaces.current().ajax().addCallbackParam("titulo", "Erro!");
			PrimeFaces.current().ajax().addCallbackParam("mensagem", "Informe um valor inicial válido (maior que zero).");
			return;
		}

		System.out.println("✅ Valor válido: " + valorInicial + " - Prosseguindo...");
		
		// AGORA SIM altera o status para Aberto
		statusSelecionado = "A";
		
		List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);

		if (checkData.isEmpty()) {
			System.out.println("📝 Criando NOVO registro de caixa com valor: " + valorInicial);
			caixaDataModel.setValorInicial(valorInicial);
			caixaDataModel.setValorFinal(0.0);
			caixaDataModel.setDataCadastro(new Date());
			caixaDataModel.setStatus("A");
			CaixaDataDAO.salvar(caixaDataModel);
			System.out.println("✅ CaixaData SALVO no banco");

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
			System.out.println("✅ ControleCaixa SALVO - Movimentação registrada");

			controleCaixaModel = new ControleCaixa();

		} else {
			System.out.println("📝 ATUALIZANDO registro existente com valor: " + valorInicial);

			for (CaixaData item : checkData) {
				caixaDataModel.setId(item.getId());
				caixaDataModel.setValorInicial(valorInicial);
				caixaDataModel.setValorFinal(0.0);
				caixaDataModel.setStatus("A");
				CaixaDataDAO.atualizar(caixaDataModel);
				System.out.println("✅ CaixaData ATUALIZADO no banco");
			}

			String horaAtualFormatada = LocalTime.now().format(horaFormatada);
			controleCaixaModel.setCaixaData(caixaDataModel);
			controleCaixaModel.setHoraAtual(horaAtualFormatada);
			controleCaixaModel.setData(dataSelecionada);
			controleCaixaModel.setValor(valorInicial);
			controleCaixaModel.setMovimentacao("Abertura de Caixa");
			ControleCaixaDAO.salvar(controleCaixaModel);
			System.out.println("✅ ControleCaixa SALVO - Movimentação registrada");

			valorFinal = caixaDataModel.getValorFinal();

			controleCaixaModel = new ControleCaixa();
		}
		dadosLiberados = false;
		getValoresRegistro(); // Atualiza statusSelecionado do banco
		calcularTotal();
		
		System.out.println("🎉 SUCESSO! Caixa aberto com valor: " + valorInicial);
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Caixa aberto com sucesso!");
	}

	public void salvarValores() {
		System.out.println("💰 SALVANDO ENTRADA/SAÍDA");
		System.out.println("📊 Tipo: " + tipodeValor);
		System.out.println("📊 Valor: " + controleCaixaModel.getValor());
		System.out.println("📊 Motivo: " + controleCaixaModel.getMotivo());
		
		// Validação: Valor obrigatório
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
		
		System.out.println("✅ Movimentação salva com sucesso!");

		controleCaixaModel = new ControleCaixa();

		calcularTotal();
		
		PrimeFaces.current().ajax().addCallbackParam("validado", true);
		PrimeFaces.current().ajax().addCallbackParam("mensagem", "Movimentação registrada com sucesso!");
	}

}
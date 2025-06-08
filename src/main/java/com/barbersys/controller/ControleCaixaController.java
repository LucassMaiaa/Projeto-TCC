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
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
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
@SessionScoped
public class ControleCaixaController implements Serializable {

    private String statusSelecionado;
    private String motivoFinal;
    private String mensagemMotivoFinal = "";
    private Double valorInicial = 0.0;
    private Double valorFinal = 0.0;
    private Boolean dadosLiberados;
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
            public List<ControleCaixa> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
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
        List<Map<String, Object>> listaPorDia = ControleCaixaDAO.buscarCaixasContagem(this.dataSelecionada);
        List<Map<String, Object>> listaPorMes = ControleCaixaDAO.buscarCaixasContagemPorMes(dataSelecionada);
  
        for (Map<String, Object>caixa : listaPorDia) {
            Double tipoValorEntrada = (Double) caixa.get("entrada");
            Double tipoValorSaida = (Double) caixa.get("saida");
            
            this.totalEntradas =  tipoValorEntrada;
            this.totalSaidas = tipoValorSaida;
        }
        
        for(Map<String, Object>caixa : listaPorMes){
            Double tipoValorEntrada = (Double) caixa.get("entrada");
            Double tipoValorSaida = (Double) caixa.get("saida");
            
            this.totalEntradasMes = tipoValorEntrada;
            this.totalSaidasMes = tipoValorSaida;
        }

    }

    public void verificaTravaCampo(){
        PrimeFaces.current().ajax().addCallbackParam("aberto", true);   
    }
    
    public void verificaData() {
        String dataSelecionadaFormatada = dataFormatada.format(this.dataSelecionada);
        Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        String dataAtualFormatada = dataFormatada.format(dataAtual);
        
        if (!dataSelecionadaFormatada.equals(dataAtualFormatada)) {
            chaveCaixa = true;            
        }else {
        	chaveCaixa = false;
        }
        
        calcularTotal();
        getValoresRegistro();
    }
    
    public void getValoresRegistro() {
    	List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
    	
    	if(!checkData.isEmpty()) {
    		for(CaixaData item : checkData) {
    			caixaDataModel.setId(item.getId());
    			caixaDataModel.setValorInicial(item.getValorInicial());
    			caixaDataModel.setValorFinal(item.getValorFinal());
    			caixaDataModel.setDataCadastro(item.getDataCadastro());
    			caixaDataModel.setStatus(item.getStatus());
    			
				valorInicial = caixaDataModel.getValorInicial();
				valorFinal = caixaDataModel.getValorFinal();		
    			statusSelecionado = caixaDataModel.getStatus();
    			
    			if(statusSelecionado.equals("A")) {
    				dadosLiberados = false;
    			}else {
    				dadosLiberados = true;
    			}
    		}
    	}else {
			valorInicial = 0.0;
			valorFinal = 0.0;
			statusSelecionado = "I";
			dadosLiberados = true;
		}
		
    }
    
    public void salvarRegistroFinal() {
    	List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
    	controleCaixaModel.setMotivo(mensagemMotivoFinal);
    	
		for(CaixaData item : checkData) {
			caixaDataModel.setId(item.getId());
			caixaDataModel.setValorInicial(item.getValorInicial());
			caixaDataModel.setValorFinal(valorFinal);
			caixaDataModel.setStatus(statusSelecionado);
		}
		System.out.println(controleCaixaModel.getMotivo());
		if(caixaDataModel.getValorFinal() < caixaDataModel.getValorInicial() && controleCaixaModel.getMotivo().isEmpty()) {
    		motivoFinal = "A";
    		PrimeFaces.current().ajax().addCallbackParam("mostrarMotivo", true);
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
    		calcularTotal();
    		dadosLiberados = true;
    		PrimeFaces.current().ajax().addCallbackParam("mostrarMotivo", false);
    	}
    	
    }
    
    public void salvarRegistroInicial() {
    	List<CaixaData> checkData = CaixaDataDAO.verificaExisteData(dataSelecionada);
    	
    	if(checkData.isEmpty()) {
    		caixaDataModel.setValorInicial(valorInicial);
    		caixaDataModel.setValorFinal(0.0);
    		caixaDataModel.setDataCadastro(new Date());
    		caixaDataModel.setStatus(statusSelecionado);
    		CaixaDataDAO.salvar(caixaDataModel);
    		
    		String horaAtualFormatada = LocalTime.now().format(horaFormatada);
            controleCaixaModel.setCaixaData(caixaDataModel);
            controleCaixaModel.setHoraAtual(horaAtualFormatada);
            controleCaixaModel.setData(dataSelecionada);
            controleCaixaModel.setValor(valorInicial);
            controleCaixaModel.setMovimentacao("Abertura de Caixa");
            ControleCaixaDAO.salvar(controleCaixaModel);
            
            controleCaixaModel = new ControleCaixa();
    		
    	}else {
    		for(CaixaData item : checkData) {
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
    
    public void salvarValores(){
    		
		String horaAtualFormatada = LocalTime.now().format(horaFormatada);
        controleCaixaModel.setCaixaData(caixaDataModel);
        controleCaixaModel.setHoraAtual(horaAtualFormatada);
        controleCaixaModel.setData(dataSelecionada);
        
        if(this.tipodeValor.equals("e")){
            controleCaixaModel.setMovimentacao("Entrada");
        }else if(this.tipodeValor.equals("s")){
            controleCaixaModel.setMovimentacao("Saida");
        }
        
        ControleCaixaDAO.salvar(controleCaixaModel);
        
        controleCaixaModel = new ControleCaixa();
        
        calcularTotal();
    	
    		     
    }
    
}

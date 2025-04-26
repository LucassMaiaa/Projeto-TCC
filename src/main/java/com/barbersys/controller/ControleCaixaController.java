package com.barbersys.controller;

import com.barbersys.model.ControleCaixa;
import com.barbersys.service.CaixaService;
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

    private String statusSelecionado = "a";
    private String verificaCampo = "a";
    private Boolean dadosLiberados = false;
    private Date dataSelecionada = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    private LazyDataModel<ControleCaixa> lstControleCaixa;
    private ControleCaixa controleCaixaModel = new ControleCaixa();
    private Double totalEntradas;
    private Double totalEntradasMes;
    private Double totalSaidasMes;
    private Double totalSaidas;
    private String tipodeValor = "e";
    private String filtroTipoDeValor = "";

  @PostConstruct
    public void init() {
        lstControleCaixa = new LazyDataModel<ControleCaixa>() {

            @Override
            public List<ControleCaixa> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                return CaixaService.buscarCaixasPaginado(first, pageSize, dataSelecionada, filtroTipoDeValor); 
            }
            
              @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return CaixaService.contarTotalCaixas(dataSelecionada, filtroTipoDeValor);
            }
            
        };
        
        calcularTotal();
    }
    
    public void calcularTotal() {
        List<Map<String, Object>> listaPorDia = CaixaService.buscarCaixasContagem(this.dataSelecionada);
        List<Map<String, Object>> listaPorMes = CaixaService.buscarCaixasContagemPorMes(dataSelecionada);
  
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
        SimpleDateFormat dataFormatada = new SimpleDateFormat("dd/MM/yyyy");
        String dataSelecionadaFormatada = dataFormatada.format(this.dataSelecionada);
        Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        String dataAtualFormatada = dataFormatada.format(dataAtual);
        
        if (dataSelecionadaFormatada.equals(dataAtualFormatada) && statusSelecionado.equals("a")) {
            verificaCampo = "a";
            dadosLiberados = false;
        } else if(dataSelecionadaFormatada.equals(dataAtualFormatada) && statusSelecionado.equals("f")) {
            verificaCampo = "f";
            dadosLiberados = true;
        } else if(!dataSelecionadaFormatada.equals(dataAtualFormatada) && statusSelecionado.equals("a")){
            dadosLiberados = false;
        }else if(!dataSelecionadaFormatada.equals(dataAtualFormatada) && statusSelecionado.equals("f")){
            dadosLiberados = true;
        }
        
    }
    
    public void verificaData() {
        SimpleDateFormat dataFormatada = new SimpleDateFormat("dd/MM/yyyy");
        String dataSelecionadaFormatada = dataFormatada.format(this.dataSelecionada);
        Date dataAtual = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        String dataAtualFormatada = dataFormatada.format(dataAtual);
        
        if (dataSelecionadaFormatada.equals(dataAtualFormatada)) {
            statusSelecionado = verificaCampo;
            
            if(verificaCampo.equals("a")){
                dadosLiberados = false;
            }else{
                dadosLiberados = true;
            }
            
        } else if(!dataSelecionadaFormatada.equals(dataAtualFormatada)) {
            statusSelecionado = "f";
            dadosLiberados = true;
        }
        
        calcularTotal();
    }
    
    public void salvarValores(){
        DateTimeFormatter horaFormatada = DateTimeFormatter.ofPattern("HH:mm");
        String horaAtualFormatada = LocalTime.now().format(horaFormatada);
        
        controleCaixaModel.setHoraAtual(horaAtualFormatada);
        controleCaixaModel.setData(dataSelecionada);
        
        if(this.tipodeValor.equals("e")){
            controleCaixaModel.setMovimentacao("Entrada");
        }else if(this.tipodeValor.equals("s")){
            controleCaixaModel.setMovimentacao("Saida");
        }
        
        CaixaService.salvar(controleCaixaModel);
        
        controleCaixaModel = new ControleCaixa();
        
        calcularTotal();
        
    }
    
}

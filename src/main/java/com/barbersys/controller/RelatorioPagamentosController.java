package com.barbersys.controller;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.PagamentoDAO;
import com.barbersys.dao.PagamentoRelatorioDAO;
import com.barbersys.model.Pagamento;
import com.barbersys.model.PagamentoRelatorio;
import com.barbersys.util.RelatorioPagamentosPDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioPagamentosController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Long formaPagamentoFiltro;
    private String statusPagamentoFiltro = "";
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private String nomeClienteFiltro;
    private LazyDataModel<PagamentoRelatorio> lstPagamentos;
    private List<Pagamento> lstFormasPagamento;
    
    private PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private PagamentoRelatorioDAO pagamentoRelatorioDAO = new PagamentoRelatorioDAO();

    @PostConstruct
    public void init() {
        carregarFormasPagamento();
        inicializarLazyModel();
    }
    
    // Inicializa modelo lazy para carregamento paginado
    private void inicializarLazyModel() {
        lstPagamentos = new LazyDataModel<PagamentoRelatorio>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<PagamentoRelatorio> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                String status = obterStatusFiltro();
                
                String sortField = "data";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return pagamentoRelatorioDAO.buscarPagamentosPaginado(
                    dataInicial, dataFinal, nomeClienteFiltro, formaPagamentoFiltro, status, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                String status = obterStatusFiltro();
                return pagamentoRelatorioDAO.contarPagamentos(
                    dataInicial, dataFinal, nomeClienteFiltro, formaPagamentoFiltro, status);
            }
        };
    }
    
    // Retorna status do filtro ou null se vazio
    private String obterStatusFiltro() {
        if (statusPagamentoFiltro != null && !statusPagamentoFiltro.trim().isEmpty()) {
            return statusPagamentoFiltro;
        }
        return null;
    }

    // Carrega formas de pagamento do banco
    private void carregarFormasPagamento() {
        lstFormasPagamento = pagamentoDAO.buscarTodos();
    }

    // Recarrega dados com filtros aplicados
    public void filtrar() {
    }
    
    // Limpa todos os filtros
    public void limparFiltros() {
        formaPagamentoFiltro = null;
        statusPagamentoFiltro = "";
        dataInicial = null;
        dataFinal = null;
        nomeClienteFiltro = null;
    }

    // Exporta relatório em PDF
    public void exportarPDF() {
        try {
            String status = obterStatusFiltro();
            List<PagamentoRelatorio> todosRegistros = pagamentoRelatorioDAO.buscarTodosPagamentos(
                dataInicial, dataFinal, nomeClienteFiltro, formaPagamentoFiltro, status);
            
            if (todosRegistros == null || todosRegistros.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, 
                        "Não há dados para gerar o relatório!", "Aviso"));
                return;
            }
            
            RelatorioPagamentosPDF.gerarPDF(todosRegistros, dataInicial, dataFinal);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "PDF gerado com sucesso!", "Sucesso"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro ao gerar PDF: " + e.getMessage(), "Erro"));
        }
    }
}

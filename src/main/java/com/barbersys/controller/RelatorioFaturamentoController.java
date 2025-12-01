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

import com.barbersys.dao.FaturamentoMensalDAO;
import com.barbersys.dao.ServicosDAO;
import com.barbersys.model.FaturamentoMensal;
import com.barbersys.model.Servicos;
import com.barbersys.util.RelatorioFaturamentoMensalPDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioFaturamentoController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private Long servicoFiltro;
    private LazyDataModel<FaturamentoMensal> lstFaturamento;
    private List<Servicos> lstServicos;
    
    private FaturamentoMensalDAO faturamentoDAO = new FaturamentoMensalDAO();

    @PostConstruct
    public void init() {
        carregarServicos();
        inicializarLazyModel();
    }
    
    // Inicializa o modelo lazy para carregamento paginado dos dados
    private void inicializarLazyModel() {
        lstFaturamento = new LazyDataModel<FaturamentoMensal>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<FaturamentoMensal> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                
                String sortField = "data";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return faturamentoDAO.buscarFaturamentoPaginado(
                    dataInicial, dataFinal, servicoFiltro, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return faturamentoDAO.contarFaturamento(dataInicial, dataFinal, servicoFiltro);
            }
        };
    }
    
    // Carrega lista de serviços para o filtro
    private void carregarServicos() {
        lstServicos = ServicosDAO.buscarTodos();
    }
    
    // Limpa todos os filtros aplicados
    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
        servicoFiltro = null;
    }

    // Gera relatório em PDF com os dados filtrados
    public void gerarPDF() {
        try {
            List<FaturamentoMensal> todosRegistros = faturamentoDAO.buscarTodosFaturamento(
                dataInicial, dataFinal, servicoFiltro);
            
            if (todosRegistros == null || todosRegistros.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_WARN, 
                        "Atenção", "Não há dados para exportar com os filtros aplicados."));
                return;
            }
            
            RelatorioFaturamentoMensalPDF.gerarRelatorio(todosRegistros, dataInicial, dataFinal);
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Erro ao gerar o relatório PDF: " + e.getMessage()));
        }
    }
}

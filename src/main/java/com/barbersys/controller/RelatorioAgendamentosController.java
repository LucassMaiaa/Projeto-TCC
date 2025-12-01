package com.barbersys.controller;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.AgendamentoSinteticoDAO;
import com.barbersys.model.AgendamentoSintetico;
import com.barbersys.util.RelatorioAgendamentosPDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioAgendamentosController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private LazyDataModel<AgendamentoSintetico> lstAgendamentos;

    @PostConstruct
    public void init() {
        inicializarLazyModel();
    }
    
    // Inicializa o modelo lazy para paginação dos agendamentos
    private void inicializarLazyModel() {
        lstAgendamentos = new LazyDataModel<AgendamentoSintetico>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<AgendamentoSintetico> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                
                String sortField = "data";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return AgendamentoSinteticoDAO.buscarAgendamentosSinteticos(
                    dataInicial, dataFinal, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return AgendamentoSinteticoDAO.contarAgendamentosSinteticos(dataInicial, dataFinal);
            }
        };
    }

    // Limpa os filtros de data
    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
    }

    // Busca todos os agendamentos dentro do período filtrado
    private List<AgendamentoSintetico> obterTodosAgendamentosFiltrados() {
        return AgendamentoSinteticoDAO.buscarTodosAgendamentosSinteticos(dataInicial, dataFinal);
    }

    // Gera o PDF do relatório de agendamentos
    public void gerarPDF() {
        try {
            List<AgendamentoSintetico> todosAgendamentos = obterTodosAgendamentosFiltrados();
            
            if (todosAgendamentos == null || todosAgendamentos.isEmpty()) {
                return;
            }
            
            RelatorioAgendamentosPDF.gerar(todosAgendamentos, dataInicial, dataFinal);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

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

import com.barbersys.dao.AvaliacaoDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.model.Avaliacao;
import com.barbersys.model.Funcionario;
import com.barbersys.util.RelatorioFeedbackPDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioFeedbackController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String avaliacaoFiltro = "";
    private Long funcionarioFiltro;
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private LazyDataModel<Avaliacao> lstAvaliacoes;
    private List<Funcionario> lstFuncionario;

    @PostConstruct
    public void init() {
        carregarFuncionarios();
        inicializarLazyModel();
    }
    
    // Inicializa modelo lazy para paginação de avaliações
    private void inicializarLazyModel() {
        lstAvaliacoes = new LazyDataModel<Avaliacao>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<Avaliacao> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                Integer notaFiltro = obterNotaFiltro();
                
                String sortField = "dataCriacao";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return AvaliacaoDAO.buscarAvaliacoesPaginado(notaFiltro, funcionarioFiltro, dataInicial, dataFinal, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                Integer notaFiltro = obterNotaFiltro();
                return AvaliacaoDAO.contarAvaliacoes(notaFiltro, funcionarioFiltro, dataInicial, dataFinal);
            }
        };
    }
    
    // Converte filtro de avaliação para Integer
    private Integer obterNotaFiltro() {
        if (avaliacaoFiltro != null && !avaliacaoFiltro.trim().isEmpty()) {
            return Integer.parseInt(avaliacaoFiltro);
        }
        return null;
    }

    // Carrega lista de funcionários ativos
    private void carregarFuncionarios() {
        lstFuncionario = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    // Limpa todos os filtros aplicados
    public void limparFiltros() {
        avaliacaoFiltro = "";
        funcionarioFiltro = null;
        dataInicial = null;
        dataFinal = null;
    }

    // Busca todas as avaliações filtradas para geração do PDF
    private List<Avaliacao> obterTodasAvaliacoesFiltradas() {
        Integer notaFiltro = obterNotaFiltro();
        return AvaliacaoDAO.buscarTodasAvaliacoes(notaFiltro, funcionarioFiltro, dataInicial, dataFinal);
    }

    // Gera relatório PDF de feedback dos clientes
    public void gerarPDF() {
        try {
            List<Avaliacao> todasAvaliacoes = obterTodasAvaliacoesFiltradas();
            
            if (todasAvaliacoes == null || todasAvaliacoes.isEmpty()) {
                return;
            }
            
            RelatorioFeedbackPDF.gerar(todasAvaliacoes, avaliacaoFiltro, funcionarioFiltro, dataInicial, dataFinal);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

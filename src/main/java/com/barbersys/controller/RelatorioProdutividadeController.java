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

import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.ProdutividadeFuncionarioDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.ProdutividadeFuncionario;
import com.barbersys.util.RelatorioProdutividadePDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioProdutividadeController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private Long funcionarioFiltro;
    private LazyDataModel<ProdutividadeFuncionario> lstProdutividade;
    private List<Funcionario> lstFuncionarios;

    @PostConstruct
    public void init() {
        carregarFuncionarios();
        inicializarLazyModel();
    }
    
    // Inicializa o modelo lazy para carregar dados paginados da produtividade
    private void inicializarLazyModel() {
        lstProdutividade = new LazyDataModel<ProdutividadeFuncionario>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ProdutividadeFuncionario> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                
                String sortField = "data";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return ProdutividadeFuncionarioDAO.buscarProdutividade(
                    dataInicial, dataFinal, funcionarioFiltro, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return ProdutividadeFuncionarioDAO.contarProdutividade(
                    dataInicial, dataFinal, funcionarioFiltro);
            }
        };
    }

    // Carrega lista de funcionários ativos
    private void carregarFuncionarios() {
        lstFuncionarios = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    // Limpa os filtros de busca
    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
        funcionarioFiltro = null;
    }

    // Busca todas as produtividades com filtros aplicados
    private List<ProdutividadeFuncionario> obterTodasProdutividadeFiltradas() {
        return ProdutividadeFuncionarioDAO.buscarTodasProdutividade(
            dataInicial, dataFinal, funcionarioFiltro);
    }

    // Gera o relatório de produtividade em PDF
    public void gerarPDF() {
        try {
            List<ProdutividadeFuncionario> todasProdutividade = obterTodasProdutividadeFiltradas();
            
            if (todasProdutividade == null || todasProdutividade.isEmpty()) {
                return;
            }
            
            RelatorioProdutividadePDF.gerar(todasProdutividade, dataInicial, dataFinal, funcionarioFiltro);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

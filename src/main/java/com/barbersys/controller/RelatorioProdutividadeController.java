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
    
    private void inicializarLazyModel() {
        lstProdutividade = new LazyDataModel<ProdutividadeFuncionario>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<ProdutividadeFuncionario> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                return ProdutividadeFuncionarioDAO.buscarProdutividade(
                    dataInicial, dataFinal, funcionarioFiltro, first, pageSize);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return ProdutividadeFuncionarioDAO.contarProdutividade(
                    dataInicial, dataFinal, funcionarioFiltro);
            }
        };
    }

    private void carregarFuncionarios() {
        lstFuncionarios = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
        funcionarioFiltro = null;
    }

    private List<ProdutividadeFuncionario> obterTodasProdutividadeFiltradas() {
        return ProdutividadeFuncionarioDAO.buscarTodasProdutividade(
            dataInicial, dataFinal, funcionarioFiltro);
    }

    public void gerarPDF() {
        List<ProdutividadeFuncionario> todasProdutividade = null;
        
        try {
            todasProdutividade = obterTodasProdutividadeFiltradas();
            
            if (todasProdutividade == null || todasProdutividade.isEmpty()) {
                System.out.println("[AVISO] Nenhum registro encontrado para gerar PDF");
                return;
            }
            
            System.out.println("[INFO] Gerando PDF com " + todasProdutividade.size() + " registros...");
            RelatorioProdutividadePDF.gerar(todasProdutividade, dataInicial, dataFinal, funcionarioFiltro);
            System.out.println("[SUCESSO] PDF gerado com sucesso!");
            
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao gerar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

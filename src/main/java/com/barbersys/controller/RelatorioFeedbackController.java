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
    
    private void inicializarLazyModel() {
        lstAvaliacoes = new LazyDataModel<Avaliacao>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<Avaliacao> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                Integer notaFiltro = obterNotaFiltro();
                return AvaliacaoDAO.buscarAvaliacoesPaginado(notaFiltro, funcionarioFiltro, dataInicial, dataFinal, first, pageSize);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                Integer notaFiltro = obterNotaFiltro();
                return AvaliacaoDAO.contarAvaliacoes(notaFiltro, funcionarioFiltro, dataInicial, dataFinal);
            }
        };
    }
    
    private Integer obterNotaFiltro() {
        if (avaliacaoFiltro != null && !avaliacaoFiltro.trim().isEmpty()) {
            return Integer.parseInt(avaliacaoFiltro);
        }
        return null;
    }

    private void carregarFuncionarios() {
        lstFuncionario = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    public void limparFiltros() {
        avaliacaoFiltro = "";
        funcionarioFiltro = null;
        dataInicial = null;
        dataFinal = null;
    }

    /**
     * Busca TODAS as avaliações (sem paginação) para gerar o PDF
     */
    private List<Avaliacao> obterTodasAvaliacoesFiltradas() {
        Integer notaFiltro = obterNotaFiltro();
        return AvaliacaoDAO.buscarTodasAvaliacoes(notaFiltro, funcionarioFiltro, dataInicial, dataFinal);
    }

    public void gerarPDF() {
        List<Avaliacao> todasAvaliacoes = null;
        
        try {
            todasAvaliacoes = obterTodasAvaliacoesFiltradas();
            
            if (todasAvaliacoes == null || todasAvaliacoes.isEmpty()) {
                System.out.println("[AVISO] Nenhuma avaliação encontrada para gerar PDF");
                return;
            }
            
            System.out.println("[INFO] Gerando PDF com " + todasAvaliacoes.size() + " avaliações...");
            RelatorioFeedbackPDF.gerar(todasAvaliacoes, avaliacaoFiltro, funcionarioFiltro, dataInicial, dataFinal);
            System.out.println("[SUCESSO] PDF gerado com sucesso!");
            
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao gerar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

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

import com.barbersys.dao.AgendamentoDAO;
import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.model.Agendamento;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.util.RelatorioAnaliticoAgendamentosPDF;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RelatorioAnaliticoAgendamentosController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private java.util.Date dataInicial;
    private java.util.Date dataFinal;
    private String nomeClienteFiltro;
    private Long funcionarioFiltro;
    private String statusFiltro = "";
    private LazyDataModel<Agendamento> lstAgendamentos;
    private List<Cliente> lstClientes;
    private List<Funcionario> lstFuncionarios;

    // Inicializa os dados ao carregar a página
    @PostConstruct
    public void init() {
        carregarClientes();
        carregarFuncionarios();
        inicializarLazyModel();
    }
    
    // Configura o modelo lazy para carregar agendamentos paginados
    private void inicializarLazyModel() {
        lstAgendamentos = new LazyDataModel<Agendamento>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<Agendamento> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                
                String sortField = "dataCriado";
                String sortOrder = "DESC";
                
                if (sortBy != null && !sortBy.isEmpty()) {
                    SortMeta sortMeta = sortBy.values().iterator().next();
                    sortField = sortMeta.getField();
                    sortOrder = sortMeta.getOrder().isAscending() ? "ASC" : "DESC";
                }
                
                return AgendamentoDAO.buscarAgendamentosRelatorioAnalitico(
                    dataInicial, dataFinal, nomeClienteFiltro, funcionarioFiltro, 
                    statusFiltro, first, pageSize, sortField, sortOrder);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return AgendamentoDAO.contarAgendamentosRelatorioAnalitico(
                    dataInicial, dataFinal, nomeClienteFiltro, funcionarioFiltro, statusFiltro);
            }
        };
    }

    // Carrega lista de clientes ativos
    private void carregarClientes() {
        lstClientes = ClienteDAO.buscarCliente(null, "ATIVO", 0, 1000);
    }

    // Carrega lista de funcionários ativos
    private void carregarFuncionarios() {
        lstFuncionarios = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    // Limpa todos os filtros de pesquisa
    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
        nomeClienteFiltro = null;
        funcionarioFiltro = null;
        statusFiltro = "";
    }

    // Busca todos os agendamentos sem paginação para exportação
    private List<Agendamento> obterTodosAgendamentosFiltrados() {
        return AgendamentoDAO.buscarTodosAgendamentosRelatorioAnalitico(
            dataInicial, dataFinal, nomeClienteFiltro, funcionarioFiltro, statusFiltro);
    }

    // Gera relatório PDF com os agendamentos filtrados
    public void gerarPDF() {
        try {
            List<Agendamento> todosAgendamentos = obterTodosAgendamentosFiltrados();
            
            if (todosAgendamentos == null || todosAgendamentos.isEmpty()) {
                return;
            }
            
            RelatorioAnaliticoAgendamentosPDF.gerar(todosAgendamentos, dataInicial, dataFinal, 
                nomeClienteFiltro, funcionarioFiltro, statusFiltro);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Retorna descrição legível do status
    public String getStatusDescricao(String status) {
        if (status == null) return "";
        switch (status) {
            case "F": return "FINALIZADO";
            case "I": return "CANCELADO";
            case "A": return "PENDENTE";
            default: return status;
        }
    }
    
    // Retorna classe CSS do badge baseada no status
    public String getStatusClass(String status) {
        if (status == null) return "";
        switch (status) {
            case "F": return "badge badge-success";
            case "I": return "badge badge-danger";
            case "A": return "badge badge-warning";
            default: return "";
        }
    }
}

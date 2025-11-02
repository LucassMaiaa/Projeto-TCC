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
    private Long clienteFiltro;
    private Long funcionarioFiltro;
    private String statusFiltro = "";
    private LazyDataModel<Agendamento> lstAgendamentos;
    private List<Cliente> lstClientes;
    private List<Funcionario> lstFuncionarios;

    @PostConstruct
    public void init() {
        carregarClientes();
        carregarFuncionarios();
        inicializarLazyModel();
    }
    
    private void inicializarLazyModel() {
        lstAgendamentos = new LazyDataModel<Agendamento>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<Agendamento> load(int first, int pageSize, Map<String, SortMeta> sortBy,
                    Map<String, FilterMeta> filterBy) {
                return AgendamentoDAO.buscarAgendamentosRelatorioAnalitico(
                    dataInicial, dataFinal, clienteFiltro, funcionarioFiltro, 
                    statusFiltro, first, pageSize);
            }

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                return AgendamentoDAO.contarAgendamentosRelatorioAnalitico(
                    dataInicial, dataFinal, clienteFiltro, funcionarioFiltro, statusFiltro);
            }
        };
    }

    private void carregarClientes() {
        lstClientes = ClienteDAO.buscarCliente(null, 0, 1000);
    }

    private void carregarFuncionarios() {
        lstFuncionarios = FuncionarioDAO.buscarFuncionario(null, "A", 0, 1000);
    }

    public void limparFiltros() {
        dataInicial = null;
        dataFinal = null;
        clienteFiltro = null;
        funcionarioFiltro = null;
        statusFiltro = "";
    }

    private List<Agendamento> obterTodosAgendamentosFiltrados() {
        return AgendamentoDAO.buscarTodosAgendamentosRelatorioAnalitico(
            dataInicial, dataFinal, clienteFiltro, funcionarioFiltro, statusFiltro);
    }

    public void gerarPDF() {
        List<Agendamento> todosAgendamentos = null;
        
        try {
            todosAgendamentos = obterTodosAgendamentosFiltrados();
            
            if (todosAgendamentos == null || todosAgendamentos.isEmpty()) {
                System.out.println("[AVISO] Nenhum agendamento encontrado para gerar PDF");
                return;
            }
            
            System.out.println("[INFO] Gerando PDF com " + todosAgendamentos.size() + " agendamentos...");
            RelatorioAnaliticoAgendamentosPDF.gerar(todosAgendamentos, dataInicial, dataFinal, 
                clienteFiltro, funcionarioFiltro, statusFiltro);
            System.out.println("[SUCESSO] PDF gerado com sucesso!");
            
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao gerar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String getStatusDescricao(String status) {
        if (status == null) return "";
        switch (status) {
            case "F": return "FINALIZADO";
            case "I": return "CANCELADO";
            case "A": return "PENDENTE";
            default: return status;
        }
    }
    
    public String getStatusClass(String status) {
        if (status == null) return "";
        switch (status) {
            case "F": return "badge-success";
            case "I": return "badge-danger";
            case "A": return "badge-warning";
            default: return "";
        }
    }
}

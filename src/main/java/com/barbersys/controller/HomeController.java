package com.barbersys.controller;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.barbersys.dao.DashboardDAO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean(name = "homeController")
@ViewScoped
public class HomeController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String dataSelecionada = "m"; // "m" para mês, "a" para ano
    
    // Dados de faturamento
    private double totalFaturado;
    private int totalAgendamentos;
    private int agendamentosFinalizados;
    private int agendamentosCancelados;
    private Map<Integer, Double> faturamentoPorMes;
    private double valorMaximo;
    
    // Dados do top funcionário
    private String topFuncionarioNome;
    private int topFuncionarioAtendimentos;
    private double topFuncionarioAvaliacao;
    
    // Dados de visitas por dia da semana
    private Map<Integer, Integer> visitasPorDia;
    
    // Dados dos top serviços do mês
    private List<Map<String, Object>> topServicosMes;
    
    // Dados do resumo de hoje
    private int agendamentosHoje;
    private double faturamentoHoje;
    private String proximoAgendamento;
    
    // Labels dos meses
    private static final String[] MESES = {
        "jan", "fev", "mar", "abr", "mai", "jun",
        "jul", "ago", "set", "out", "nov", "dez"
    };
    
    @PostConstruct
    public void init() {
        carregarDados();
    }
    
    // Carrega todos os dados do dashboard
    public void carregarDados() {
        Calendar cal = Calendar.getInstance();
        int anoAtual = cal.get(Calendar.YEAR);
        boolean isMesAtual = "m".equals(dataSelecionada);
        
        if (isMesAtual) {
            totalFaturado = DashboardDAO.buscarFaturamentoMesAtual();
            totalAgendamentos = DashboardDAO.buscarTotalAgendamentosMesAtual();
            agendamentosFinalizados = DashboardDAO.buscarAgendamentosFinalizadosMesAtual();
            agendamentosCancelados = DashboardDAO.buscarAgendamentosCanceladosMesAtual();
        } else {
            totalFaturado = DashboardDAO.buscarFaturamentoAnoAtual();
            totalAgendamentos = DashboardDAO.buscarTotalAgendamentosAnoAtual();
            agendamentosFinalizados = DashboardDAO.buscarAgendamentosFinalizadosAnoAtual();
            agendamentosCancelados = DashboardDAO.buscarAgendamentosCanceladosAnoAtual();
        }
        
        faturamentoPorMes = DashboardDAO.buscarFaturamentoPorMes(anoAtual);
        
        Map<String, Object> topFuncionario = DashboardDAO.buscarTopFuncionario(isMesAtual);
        topFuncionarioNome = (String) topFuncionario.get("nome");
        topFuncionarioAtendimentos = (Integer) topFuncionario.get("totalAtendimentos");
        topFuncionarioAvaliacao = (Double) topFuncionario.get("mediaAvaliacao");
        
        visitasPorDia = DashboardDAO.buscarVisitasPorDiaSemana();
        topServicosMes = DashboardDAO.buscarTopServicosMesAtual(5);
        
        agendamentosHoje = DashboardDAO.buscarAgendamentosHoje();
        faturamentoHoje = DashboardDAO.buscarFaturamentoHoje();
        proximoAgendamento = DashboardDAO.buscarProximoAgendamento();
        
        valorMaximo = 0.0;
        for (Double valor : faturamentoPorMes.values()) {
            if (valor > valorMaximo) {
                valorMaximo = valor;
            }
        }
        
        if (valorMaximo == 0.0) {
            valorMaximo = 1.0;
        }
    }
    
    // Retorna a altura da barra do gráfico em porcentagem
    public int getAlturaBarra(int mes) {
        if (faturamentoPorMes == null || !faturamentoPorMes.containsKey(mes)) {
            return 5; // Pontinha para meses sem dados
        }
        
        double valor = faturamentoPorMes.get(mes);
        
        // Se não tem valor, mostra pontinha
        if (valor <= 0) {
            return 5;
        }
        
        // Se o valor máximo é zero (nenhum dado no ano), mostra pontinha
        if (valorMaximo <= 0.0) {
            return 5;
        }
        
        // Calcula a porcentagem proporcional (10% a 100%)
        // Reserva 5% mínimo para visualização, e 95% para proporção
        int alturaMinima = 10;
        int alturaMaxima = 100;
        int range = alturaMaxima - alturaMinima;
        
        double proporcao = valor / valorMaximo;
        int altura = alturaMinima + (int)(proporcao * range);
        
        return Math.min(altura, alturaMaxima);
    }
    
    // Retorna o valor formatado do mês
    public String getValorMes(int mes) {
        if (faturamentoPorMes == null || !faturamentoPorMes.containsKey(mes)) {
            return "R$ 0,00";
        }
        
        double valor = faturamentoPorMes.get(mes);
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        DecimalFormat df = new DecimalFormat("###,###,##0.00", symbols);
        return "R$ " + df.format(valor);
    }
    
    // Verifica se o mês tem dados
    public boolean temDados(int mes) {
        if (faturamentoPorMes == null || !faturamentoPorMes.containsKey(mes)) {
            return false;
        }
        return faturamentoPorMes.get(mes) > 0;
    }
    
    // Verifica se é o mês atual
    public boolean isMesAtual(int mes) {
        Calendar cal = Calendar.getInstance();
        int mesAtual = cal.get(Calendar.MONTH) + 1;
        return mes == mesAtual;
    }
    
    // Retorna o label do mês
    public String getLabelMes(int mes) {
        if (mes >= 1 && mes <= 12) {
            return MESES[mes - 1];
        }
        return "";
    }
    
    // Formata valor em Real
    public String getTotalFaturadoFormatado() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        DecimalFormat df = new DecimalFormat("###,###,##0.00", symbols);
        return "R$ " + df.format(totalFaturado);
    }
    
    // Atualiza dados quando filtro é alterado
    public void onFiltroChange() {
        carregarDados();
    }
    
    // Calcula porcentagem de finalizados (sobre finalizados + cancelados)
    public int getPorcentagemFinalizados() {
        int totalRelevante = agendamentosFinalizados + agendamentosCancelados;
        if (totalRelevante == 0) return 0;
        return (int) Math.round((agendamentosFinalizados * 100.0) / totalRelevante);
    }
    
    // Calcula porcentagem de cancelados (sobre finalizados + cancelados)
    public int getPorcentagemCancelados() {
        int totalRelevante = agendamentosFinalizados + agendamentosCancelados;
        if (totalRelevante == 0) return 0;
        return (int) Math.round((agendamentosCancelados * 100.0) / totalRelevante);
    }
    
    // Formata avaliação do top funcionário
    public String getTopFuncionarioAvaliacaoFormatada() {
        if (topFuncionarioAvaliacao == 0.0) {
            return "0.0";
        }
        return String.format("%.1f", topFuncionarioAvaliacao);
    }
    
    // Retorna total de visitas do dia
    public int getVisitasDia(int diaSemana) {
        if (visitasPorDia == null || !visitasPorDia.containsKey(diaSemana)) {
            return 0;
        }
        return visitasPorDia.get(diaSemana);
    }
    
    // Calcula porcentagem de visitas do dia
    public int getPorcentagemVisitasDia(int diaSemana) {
        if (visitasPorDia == null || visitasPorDia.isEmpty()) {
            return 5; // Mínimo de 5% para visualização
        }
        
        // Encontra o valor máximo
        int maxVisitas = 0;
        for (Integer total : visitasPorDia.values()) {
            if (total > maxVisitas) {
                maxVisitas = total;
            }
        }
        
        // Se não há visitas, retorna mínimo
        if (maxVisitas == 0) {
            return 5;
        }
        
        int visitasDia = getVisitasDia(diaSemana);
        
        // Se o dia não tem visitas, retorna mínimo
        if (visitasDia == 0) {
            return 5;
        }
        
        int porcentagem = (int) Math.round((visitasDia * 100.0) / maxVisitas);
        return Math.max(10, porcentagem);
    }
    
    // Retorna nome do dia da semana
    public String getNomeDia(int diaSemana) {
        String[] dias = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        if (diaSemana >= 1 && diaSemana <= 7) {
            return dias[diaSemana - 1];
        }
        return "";
    }
    
    // Formata data para exibição
    public String formatarData(Date data) {
        if (data == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
        return sdf.format(data);
    }
    
    // Formata faturamento de hoje
    public String getFaturamentoHojeFormatado() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        DecimalFormat df = new DecimalFormat("###,###,##0.00", symbols);
        return "R$ " + df.format(faturamentoHoje);
    }
    
    // Retorna cor do status
    public String getCorStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "#999";
        
        status = status.trim().toUpperCase();
        
        switch (status) {
            case "A":
            case "P":
            case "PENDENTE":
                return "#FFA726"; // Pendente - Laranja
            case "F":
            case "FINALIZADO":
                return "#66BB6A"; // Finalizado - Verde
            case "I":
            case "C":
            case "CANCELADO":
                return "#EF5350"; // Cancelado - Vermelho
            case "CONFIRMADO":
                return "#42A5F5"; // Confirmado - Azul
            default: 
                return "#999";
        }
    }
    
    // Retorna texto do status
    public String getTextoStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "Desconhecido";
        
        status = status.trim().toUpperCase();
        
        switch (status) {
            case "A":
            case "P":
            case "PENDENTE":
                return "Pendente";
            case "F":
            case "FINALIZADO":
                return "Finalizado";
            case "I":
            case "C":
            case "CANCELADO":
                return "Cancelado";
            case "CONFIRMADO":
                return "Confirmado";
            default: 
                return status;
        }
    }
    
    // Retorna próximo agendamento formatado
    public String getProximoAgendamentoFormatado() {
        if (proximoAgendamento == null || proximoAgendamento.trim().isEmpty()) {
            return null;
        }
        return proximoAgendamento;
    }
    
    // Formata preço em Real
    public String formatarPreco(Double preco) {
        if (preco == null) return "R$ 0,00";
        
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        
        DecimalFormat df = new DecimalFormat("###,###,##0.00", symbols);
        return "R$ " + df.format(preco);
    }
}

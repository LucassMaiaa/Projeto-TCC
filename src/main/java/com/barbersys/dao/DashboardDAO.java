package com.barbersys.dao;

import com.barbersys.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class DashboardDAO {
    
    /**
     * Busca o faturamento por mês do ano atual
     */
    public static Map<Integer, Double> buscarFaturamentoPorMes(int ano) {
        Map<Integer, Double> faturamentoPorMes = new LinkedHashMap<>();
        
        // Inicializa todos os meses com 0.0
        for (int i = 1; i <= 12; i++) {
            faturamentoPorMes.put(i, 0.0);
        }
        
        String sql = "SELECT MONTH(a.age_data) as mes, SUM(s.ser_preco) as total " +
                     "FROM agendamento a " +
                     "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "WHERE YEAR(a.age_data) = ? " +
                     "AND a.age_status = 'F' " +
                     "AND a.age_pago = 'S' " +
                     "GROUP BY MONTH(a.age_data) " +
                     "ORDER BY mes";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, ano);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                int mes = rs.getInt("mes");
                double total = rs.getDouble("total");
                faturamentoPorMes.put(mes, total);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return faturamentoPorMes;
    }
    
    /**
     * Busca o faturamento dos últimos 12 meses
     */
    public static Map<String, Double> buscarFaturamentoUltimos12Meses() {
        Map<String, Double> faturamento = new LinkedHashMap<>();
        
        String sql = "SELECT " +
                     "DATE_FORMAT(a.age_data, '%Y-%m') as periodo, " +
                     "SUM(s.ser_preco) as total " +
                     "FROM agendamento a " +
                     "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "WHERE a.age_data >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) " +
                     "AND a.age_status = 'F' " +
                     "AND a.age_pago = 'S' " +
                     "GROUP BY DATE_FORMAT(a.age_data, '%Y-%m') " +
                     "ORDER BY periodo";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String periodo = rs.getString("periodo");
                double total = rs.getDouble("total");
                faturamento.put(periodo, total);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return faturamento;
    }
    
    /**
     * Busca o total faturado no mês atual
     */
    public static double buscarFaturamentoMesAtual() {
        String sql = "SELECT COALESCE(SUM(s.ser_preco), 0) as total " +
                     "FROM agendamento a " +
                     "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "WHERE YEAR(a.age_data) = YEAR(CURDATE()) " +
                     "AND MONTH(a.age_data) = MONTH(CURDATE()) " +
                     "AND a.age_status = 'F' " +
                     "AND a.age_pago = 'S'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    /**
     * Busca o total faturado no ano atual
     */
    public static double buscarFaturamentoAnoAtual() {
        String sql = "SELECT COALESCE(SUM(s.ser_preco), 0) as total " +
                     "FROM agendamento a " +
                     "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "WHERE YEAR(a.age_data) = YEAR(CURDATE()) " +
                     "AND a.age_status = 'F' " +
                     "AND a.age_pago = 'S'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0.0;
    }
    
    /**
     * Busca o total de agendamentos no mês atual
     */
    public static int buscarTotalAgendamentosMesAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE()) " +
                     "AND MONTH(age_data) = MONTH(CURDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de agendamentos no ano atual
     */
    public static int buscarTotalAgendamentosAnoAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE())";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de agendamentos finalizados no mês atual
     */
    public static int buscarAgendamentosFinalizadosMesAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE()) " +
                     "AND MONTH(age_data) = MONTH(CURDATE()) " +
                     "AND age_status = 'F'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de agendamentos finalizados no ano atual
     */
    public static int buscarAgendamentosFinalizadosAnoAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE()) " +
                     "AND age_status = 'F'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de agendamentos cancelados no mês atual
     */
    public static int buscarAgendamentosCanceladosMesAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE()) " +
                     "AND MONTH(age_data) = MONTH(CURDATE()) " +
                     "AND age_status = 'I'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de agendamentos cancelados no ano atual
     */
    public static int buscarAgendamentosCanceladosAnoAtual() {
        String sql = "SELECT COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEAR(age_data) = YEAR(CURDATE()) " +
                     "AND age_status = 'I'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Busca o total de visitas (agendamentos) por dia da semana atual
     * Retorna Map com dia da semana (1=Domingo, 7=Sábado) e total de agendamentos
     */
    public static Map<Integer, Integer> buscarVisitasPorDiaSemana() {
        Map<Integer, Integer> visitasPorDia = new LinkedHashMap<>();
        
        // Inicializa todos os dias com 0
        for (int i = 1; i <= 7; i++) {
            visitasPorDia.put(i, 0);
        }
        
        String sql = "SELECT DAYOFWEEK(age_data) as dia_semana, COUNT(*) as total " +
                     "FROM agendamento " +
                     "WHERE YEARWEEK(age_data, 1) = YEARWEEK(CURDATE(), 1) " +
                     "GROUP BY DAYOFWEEK(age_data) " +
                     "ORDER BY dia_semana";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int diaSemana = rs.getInt("dia_semana");
                int total = rs.getInt("total");
                visitasPorDia.put(diaSemana, total);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return visitasPorDia;
    }
    
    /**
     * Busca o funcionário com mais agendamentos finalizados no mês/ano atual
     * Retorna: [funcionario_id, nome, total_atendimentos, media_avaliacao]
     */
    public static Map<String, Object> buscarTopFuncionario(boolean mesAtual) {
        Map<String, Object> dados = new HashMap<>();
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT f.fun_codigo, f.fun_nome, ");
        sql.append("COUNT(DISTINCT a.age_codigo) as total_atendimentos, ");
        sql.append("COALESCE(AVG(av.ava_nota), 0) as media_avaliacao ");
        sql.append("FROM funcionario f ");
        sql.append("LEFT JOIN agendamento a ON f.fun_codigo = a.fun_codigo ");
        sql.append("AND a.age_status = 'F' ");
        sql.append("AND YEAR(a.age_data) = YEAR(CURDATE()) ");
        
        if (mesAtual) {
            sql.append("AND MONTH(a.age_data) = MONTH(CURDATE()) ");
        }
        
        sql.append("LEFT JOIN avaliacao av ON a.age_codigo = av.age_codigo ");
        sql.append("WHERE f.fun_status = 'A' ");
        sql.append("GROUP BY f.fun_codigo, f.fun_nome ");
        sql.append("ORDER BY total_atendimentos DESC, media_avaliacao DESC ");
        sql.append("LIMIT 1");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                dados.put("id", rs.getLong("fun_codigo"));
                dados.put("nome", rs.getString("fun_nome"));
                dados.put("totalAtendimentos", rs.getInt("total_atendimentos"));
                dados.put("mediaAvaliacao", rs.getDouble("media_avaliacao"));
            } else {
                // Se não houver dados, retorna valores padrão
                dados.put("id", 0L);
                dados.put("nome", "Nenhum");
                dados.put("totalAtendimentos", 0);
                dados.put("mediaAvaliacao", 0.0);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            dados.put("id", 0L);
            dados.put("nome", "Erro");
            dados.put("totalAtendimentos", 0);
            dados.put("mediaAvaliacao", 0.0);
        }
        
        return dados;
    }
}

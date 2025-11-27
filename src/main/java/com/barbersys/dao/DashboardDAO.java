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
     * Busca total de agendamentos de hoje (excluindo cancelados)
     */
    public static int buscarAgendamentosHoje() {
        String sql = "SELECT COUNT(*) as total FROM agendamento " +
                     "WHERE DATE(age_data) = CURDATE() " +
                     "AND age_status != 'I'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("✅ Agendamentos de hoje: " + total);
                return total;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao buscar agendamentos de hoje: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Busca faturamento de hoje (apenas finalizados e pagos)
     */
    public static double buscarFaturamentoHoje() {
        String sql = "SELECT COALESCE(SUM(s.ser_preco), 0) as total " +
                     "FROM agendamento a " +
                     "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "WHERE a.age_data = CURDATE() " +
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
     * Busca o próximo agendamento (mais próximo com base na data e hora)
     * Sempre mostra DATA + HORA + CLIENTE
     */
    public static String buscarProximoAgendamento() {
        // Busca o próximo agendamento (hoje ou futuro)
        String sql = "SELECT " +
                     "CONCAT(" +
                     "  DATE_FORMAT(a.age_data, '%d/%m'), " +
                     "  ' às ', " +
                     "  TIME_FORMAT(a.age_hora, '%H:%i'), " +
                     "  ' - ', " +
                     "  COALESCE(c.cli_nome, a.age_nome_cliente)" +
                     ") as info " +
                     "FROM agendamento a " +
                     "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
                     "WHERE (a.age_data > CURDATE() OR " +
                     "       (a.age_data = CURDATE() AND TIME(a.age_hora) >= TIME(NOW()))) " +
                     "AND a.age_status != 'I' " +
                     "ORDER BY a.age_data ASC, a.age_hora ASC " +
                     "LIMIT 1";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString("info");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao buscar próximo agendamento: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Busca os últimos N agendamentos (para dashboard)
     */
    public static List<Map<String, Object>> buscarUltimosAgendamentos(int limite) {
        List<Map<String, Object>> agendamentos = new ArrayList<>();
        
        String sql = "SELECT " +
                     "a.age_codigo, " +
                     "a.age_data, " +
                     "a.age_hora, " +
                     "a.age_status, " +
                     "COALESCE(c.cli_nome, a.age_nome_cliente) as cliente_nome, " +
                     "f.fun_nome as funcionario_nome, " +
                     "GROUP_CONCAT(s.ser_nome SEPARATOR ', ') as servicos " +
                     "FROM agendamento a " +
                     "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
                     "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " +
                     "LEFT JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
                     "LEFT JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "GROUP BY a.age_codigo, a.age_data, a.age_hora, a.age_status, cliente_nome, f.fun_nome " +
                     "ORDER BY a.age_data DESC, a.age_hora DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> agendamento = new HashMap<>();
                agendamento.put("id", rs.getLong("age_codigo"));
                agendamento.put("data", rs.getDate("age_data"));
                agendamento.put("hora", rs.getString("age_hora"));
                agendamento.put("status", rs.getString("age_status"));
                agendamento.put("clienteNome", rs.getString("cliente_nome"));
                agendamento.put("funcionarioNome", rs.getString("funcionario_nome"));
                agendamento.put("servicos", rs.getString("servicos"));
                agendamentos.add(agendamento);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return agendamentos;
    }
    
    /**
     * Busca o total de visitas (agendamentos) por dia da semana atual
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
                     "AND age_status != 'I' " +
                     "GROUP BY DAYOFWEEK(age_data) " +
                     "ORDER BY dia_semana";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int diaSemana = rs.getInt("dia_semana");
                int total = rs.getInt("total");
                visitasPorDia.put(diaSemana, total);
                System.out.println("✅ Dia " + diaSemana + ": " + total + " agendamento(s)");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Erro ao buscar visitas por dia: " + e.getMessage());
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
    
    /**
     * Busca os 5 serviços mais solicitados do mês atual
     * Retorna: [servico_nome, total_solicitacoes, preco_medio]
     */
    public static List<Map<String, Object>> buscarTopServicosMesAtual(int limite) {
        List<Map<String, Object>> servicos = new ArrayList<>();
        
        String sql = "SELECT " +
                     "s.ser_nome as nome, " +
                     "COUNT(ags.ser_codigo) as total, " +
                     "s.ser_preco as preco " +
                     "FROM agendamento_servico ags " +
                     "INNER JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
                     "INNER JOIN agendamento a ON ags.age_codigo = a.age_codigo " +
                     "WHERE YEAR(a.age_data) = YEAR(CURDATE()) " +
                     "AND MONTH(a.age_data) = MONTH(CURDATE()) " +
                     "AND a.age_status != 'I' " +
                     "GROUP BY s.ser_codigo, s.ser_nome, s.ser_preco " +
                     "ORDER BY total DESC " +
                     "LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, limite);
            ResultSet rs = ps.executeQuery();
            
            int totalGeral = 0;
            List<Map<String, Object>> temp = new ArrayList<>();
            
            // Primeira passagem: coletar dados e calcular total
            while (rs.next()) {
                Map<String, Object> servico = new HashMap<>();
                servico.put("nome", rs.getString("nome"));
                servico.put("total", rs.getInt("total"));
                servico.put("preco", rs.getDouble("preco"));
                temp.add(servico);
                totalGeral += rs.getInt("total");
            }
            
            // Segunda passagem: calcular porcentagens
            for (Map<String, Object> servico : temp) {
                int total = (Integer) servico.get("total");
                int porcentagem = totalGeral > 0 ? (int) Math.round((total * 100.0) / totalGeral) : 0;
                servico.put("porcentagem", porcentagem);
                servicos.add(servico);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return servicos;
    }
}

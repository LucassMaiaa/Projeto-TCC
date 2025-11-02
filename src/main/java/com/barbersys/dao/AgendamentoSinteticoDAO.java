package com.barbersys.dao;

import com.barbersys.model.AgendamentoSintetico;
import com.barbersys.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgendamentoSinteticoDAO {

    public static List<AgendamentoSintetico> buscarAgendamentosSinteticos(
            java.util.Date dataInicial, 
            java.util.Date dataFinal, 
            int first, 
            int pageSize) {
        
        List<AgendamentoSintetico> resultado = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    DATE(a.age_data) as data, " +
            "    COUNT(*) as total_agendamentos, " +
            "    SUM(CASE WHEN a.age_status = 'F' THEN 1 ELSE 0 END) as finalizados, " +
            "    SUM(CASE WHEN a.age_status = 'I' THEN 1 ELSE 0 END) as cancelados " +
            "FROM agendamento a " +
            "WHERE 1=1 "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        
        sql.append("GROUP BY DATE(a.age_data) ");
        sql.append("ORDER BY data DESC ");
        sql.append("LIMIT ? OFFSET ?");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex++, first);
            
            ResultSet rs = stmt.executeQuery();
            Long id = (long) first + 1;
            
            while (rs.next()) {
                AgendamentoSintetico sintetico = new AgendamentoSintetico();
                sintetico.setId(id++);
                sintetico.setData(rs.getDate("data"));
                sintetico.setTotalAgendamentos(rs.getInt("total_agendamentos"));
                sintetico.setFinalizados(rs.getInt("finalizados"));
                sintetico.setCancelados(rs.getInt("cancelados"));
                
                resultado.add(sintetico);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return resultado;
    }

    public static int contarAgendamentosSinteticos(java.util.Date dataInicial, java.util.Date dataFinal) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT DATE(a.age_data)) as total " +
            "FROM agendamento a " +
            "WHERE 1=1 "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }

    public static List<AgendamentoSintetico> buscarTodosAgendamentosSinteticos(
            java.util.Date dataInicial, 
            java.util.Date dataFinal) {
        
        List<AgendamentoSintetico> resultado = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    DATE(a.age_data) as data, " +
            "    COUNT(*) as total_agendamentos, " +
            "    SUM(CASE WHEN a.age_status = 'F' THEN 1 ELSE 0 END) as finalizados, " +
            "    SUM(CASE WHEN a.age_status = 'I' THEN 1 ELSE 0 END) as cancelados " +
            "FROM agendamento a " +
            "WHERE 1=1 "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        
        sql.append("GROUP BY DATE(a.age_data) ");
        sql.append("ORDER BY data DESC");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            Long id = 1L;
            
            while (rs.next()) {
                AgendamentoSintetico sintetico = new AgendamentoSintetico();
                sintetico.setId(id++);
                sintetico.setData(rs.getDate("data"));
                sintetico.setTotalAgendamentos(rs.getInt("total_agendamentos"));
                sintetico.setFinalizados(rs.getInt("finalizados"));
                sintetico.setCancelados(rs.getInt("cancelados"));
                
                resultado.add(sintetico);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return resultado;
    }
}

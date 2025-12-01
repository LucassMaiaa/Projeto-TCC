package com.barbersys.dao;

import com.barbersys.model.PagamentoRelatorio;
import com.barbersys.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PagamentoRelatorioDAO {

    // Busca pagamentos com paginação e filtros
    public static List<PagamentoRelatorio> buscarPagamentosPaginado(
            java.util.Date dataInicial, 
            java.util.Date dataFinal,
            String nomeCliente,
            Long formaPagamentoId,
            String statusPagamento,
            int first, 
            int pageSize,
            String sortField,
            String sortOrder) {
        
        List<PagamentoRelatorio> pagamentos = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    a.age_codigo, " +
            "    COALESCE(c.cli_nome, a.age_nome_cliente) as nome_cliente, " +
            "    p.pag_nome as forma_pagamento, " +
            "    SUM(s.ser_preco) as valor_total, " +
            "    a.age_data, " +
            "    a.age_pago " +
            "FROM agendamento a " +
            "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
            "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "LEFT JOIN pagamento p ON a.pag_codigo = p.pag_codigo " +
            "LEFT JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
            "WHERE a.age_codigo IN ( " +
            "    SELECT DISTINCT age_codigo " +
            "    FROM agendamento_servico " +
            ") " +
            "AND a.age_status != 'I' "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
            sql.append("AND (LOWER(c.cli_nome) LIKE LOWER(?) OR LOWER(a.age_nome_cliente) LIKE LOWER(?)) ");
        }
        if (formaPagamentoId != null) {
            sql.append("AND a.pag_codigo = ? ");
        }
        if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
            sql.append("AND a.age_pago = ? ");
        }
        
        sql.append("GROUP BY a.age_codigo, c.cli_nome, a.age_nome_cliente, p.pag_nome, a.age_data, a.age_pago ");
        
        String colunaBanco = "a.age_data";
        if ("nomeCliente".equals(sortField)) colunaBanco = "nome_cliente";
        else if ("formaPagamento".equals(sortField)) colunaBanco = "forma_pagamento";
        else if ("valor".equals(sortField)) colunaBanco = "valor_total";
        else if ("data".equals(sortField)) colunaBanco = "a.age_data";
        else if ("statusPagamento".equals(sortField)) colunaBanco = "a.age_pago";
        
        String ordem = "DESC";
        if ("1".equals(sortOrder) || "ASC".equalsIgnoreCase(sortOrder)) {
            ordem = "ASC";
        }
        
        sql.append("ORDER BY ").append(colunaBanco).append(" ").append(ordem).append(" ");
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
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                String nomePattern = "%" + nomeCliente + "%";
                stmt.setString(paramIndex++, nomePattern);
                stmt.setString(paramIndex++, nomePattern);
            }
            if (formaPagamentoId != null) {
                stmt.setLong(paramIndex++, formaPagamentoId);
            }
            if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
                stmt.setString(paramIndex++, statusPagamento);
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex++, first);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PagamentoRelatorio pagamento = new PagamentoRelatorio();
                pagamento.setId(rs.getLong("age_codigo"));
                pagamento.setNomeCliente(rs.getString("nome_cliente"));
                pagamento.setFormaPagamento(rs.getString("forma_pagamento"));
                pagamento.setValor(rs.getDouble("valor_total"));
                pagamento.setData(rs.getDate("age_data"));
                pagamento.setStatusPagamento(rs.getString("age_pago"));
                
                pagamentos.add(pagamento);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return pagamentos;
    }

    // Conta total de pagamentos com filtros
    public static int contarPagamentos(
            java.util.Date dataInicial, 
            java.util.Date dataFinal,
            String nomeCliente,
            Long formaPagamentoId,
            String statusPagamento) {
        
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT a.age_codigo) as total " +
            "FROM agendamento a " +
            "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
            "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "WHERE a.age_codigo IN ( " +
            "    SELECT DISTINCT age_codigo " +
            "    FROM agendamento_servico " +
            ") " +
            "AND a.age_status != 'I' "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
            sql.append("AND (LOWER(c.cli_nome) LIKE LOWER(?) OR LOWER(a.age_nome_cliente) LIKE LOWER(?)) ");
        }
        if (formaPagamentoId != null) {
            sql.append("AND a.pag_codigo = ? ");
        }
        if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
            sql.append("AND a.age_pago = ? ");
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
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                String nomePattern = "%" + nomeCliente + "%";
                stmt.setString(paramIndex++, nomePattern);
                stmt.setString(paramIndex++, nomePattern);
            }
            if (formaPagamentoId != null) {
                stmt.setLong(paramIndex++, formaPagamentoId);
            }
            if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
                stmt.setString(paramIndex++, statusPagamento);
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

    // Busca todos os pagamentos (para exportação PDF)
    public static List<PagamentoRelatorio> buscarTodosPagamentos(
            java.util.Date dataInicial, 
            java.util.Date dataFinal,
            String nomeCliente,
            Long formaPagamentoId,
            String statusPagamento) {
        
        List<PagamentoRelatorio> pagamentos = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    a.age_codigo, " +
            "    COALESCE(c.cli_nome, a.age_nome_cliente) as nome_cliente, " +
            "    p.pag_nome as forma_pagamento, " +
            "    SUM(s.ser_preco) as valor_total, " +
            "    a.age_data, " +
            "    a.age_pago " +
            "FROM agendamento a " +
            "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
            "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "LEFT JOIN pagamento p ON a.pag_codigo = p.pag_codigo " +
            "LEFT JOIN servicos s ON ags.ser_codigo = s.ser_codigo " +
            "WHERE a.age_codigo IN ( " +
            "    SELECT DISTINCT age_codigo " +
            "    FROM agendamento_servico " +
            ") " +
            "AND a.age_status != 'I' "
        );
        
        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
            sql.append("AND (LOWER(c.cli_nome) LIKE LOWER(?) OR LOWER(a.age_nome_cliente) LIKE LOWER(?)) ");
        }
        if (formaPagamentoId != null) {
            sql.append("AND a.pag_codigo = ? ");
        }
        if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
            sql.append("AND a.age_pago = ? ");
        }
        
        sql.append("GROUP BY a.age_codigo, c.cli_nome, a.age_nome_cliente, p.pag_nome, a.age_data, a.age_pago ");
        sql.append("ORDER BY a.age_data DESC");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                String nomePattern = "%" + nomeCliente + "%";
                stmt.setString(paramIndex++, nomePattern);
                stmt.setString(paramIndex++, nomePattern);
            }
            if (formaPagamentoId != null) {
                stmt.setLong(paramIndex++, formaPagamentoId);
            }
            if (statusPagamento != null && !statusPagamento.trim().isEmpty()) {
                stmt.setString(paramIndex++, statusPagamento);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PagamentoRelatorio pagamento = new PagamentoRelatorio();
                pagamento.setId(rs.getLong("age_codigo"));
                pagamento.setNomeCliente(rs.getString("nome_cliente"));
                pagamento.setFormaPagamento(rs.getString("forma_pagamento"));
                pagamento.setValor(rs.getDouble("valor_total"));
                pagamento.setData(rs.getDate("age_data"));
                pagamento.setStatusPagamento(rs.getString("age_pago"));
                
                pagamentos.add(pagamento);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return pagamentos;
    }
}

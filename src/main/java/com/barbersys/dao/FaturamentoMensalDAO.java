package com.barbersys.dao;

import com.barbersys.model.FaturamentoMensal;
import com.barbersys.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FaturamentoMensalDAO {

    public List<FaturamentoMensal> buscarFaturamentoPaginado(Date dataInicial, Date dataFinal, 
                                                              Long servicoFiltro, int first, int pageSize,
                                                              String sortField, String sortOrder) {
        List<FaturamentoMensal> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT ")
           .append("s.ser_codigo AS servico_id, ")
           .append("s.ser_nome AS tipo_servico, ")
           .append("DATE(a.age_data) AS data, ")
           .append("COUNT(ags.ser_codigo) AS quantidade_servicos, ")
           .append("s.ser_preco AS valor_unitario, ")
           .append("SUM(s.ser_preco) AS total_faturado ")
           .append("FROM agendamento a ")
           .append("JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo ")
           .append("JOIN servicos s ON ags.ser_codigo = s.ser_codigo ")
           .append("WHERE a.age_status = 'F' ")
           .append("AND a.age_pago = 'S' ");
        
        if (dataInicial != null) {
            sql.append("AND a.age_data >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND a.age_data <= ? ");
        }
        if (servicoFiltro != null) {
            sql.append("AND s.ser_codigo = ? ");
        }
        
        sql.append("GROUP BY s.ser_codigo, s.ser_nome, DATE(a.age_data), s.ser_preco ");
        
        // Mapeamento de campos para ordenação
        String colunaBanco = "data";
        if ("tipoServico".equals(sortField)) colunaBanco = "tipo_servico";
        else if ("data".equals(sortField)) colunaBanco = "data";
        else if ("quantidadeServicos".equals(sortField)) colunaBanco = "quantidade_servicos";
        else if ("valorUnitario".equals(sortField)) colunaBanco = "valor_unitario";
        else if ("totalFaturado".equals(sortField)) colunaBanco = "total_faturado";
        
        String ordem = "DESC";
        if ("1".equals(sortOrder) || "ASC".equalsIgnoreCase(sortOrder)) {
            ordem = "ASC";
        }
        
        sql.append("ORDER BY ").append(colunaBanco).append(" ").append(ordem).append(" ");
        sql.append("LIMIT ? OFFSET ?");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (dataInicial != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            if (servicoFiltro != null) {
                ps.setLong(paramIndex++, servicoFiltro);
            }
            
            ps.setInt(paramIndex++, pageSize);
            ps.setInt(paramIndex++, first);
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                FaturamentoMensal faturamento = new FaturamentoMensal();
                faturamento.setServicoId(rs.getLong("servico_id"));
                faturamento.setTipoServico(rs.getString("tipo_servico"));
                faturamento.setData(rs.getDate("data"));
                faturamento.setQuantidadeServicos(rs.getInt("quantidade_servicos"));
                faturamento.setValorUnitario(rs.getDouble("valor_unitario"));
                faturamento.setTotalFaturado(rs.getDouble("total_faturado"));
                lista.add(faturamento);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return lista;
    }
    
    public int contarFaturamento(Date dataInicial, Date dataFinal, Long servicoFiltro) {
        int total = 0;
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT COUNT(*) AS total FROM ( ")
           .append("SELECT s.ser_codigo ")
           .append("FROM agendamento a ")
           .append("JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo ")
           .append("JOIN servicos s ON ags.ser_codigo = s.ser_codigo ")
           .append("WHERE a.age_status = 'F' ")
           .append("AND a.age_pago = 'S' ");
        
        if (dataInicial != null) {
            sql.append("AND a.age_data >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND a.age_data <= ? ");
        }
        if (servicoFiltro != null) {
            sql.append("AND s.ser_codigo = ? ");
        }
        
        sql.append("GROUP BY s.ser_codigo, DATE(a.age_data) ")
           .append(") AS subquery");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (dataInicial != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            if (servicoFiltro != null) {
                ps.setLong(paramIndex++, servicoFiltro);
            }
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return total;
    }
    
    public List<FaturamentoMensal> buscarTodosFaturamento(Date dataInicial, Date dataFinal, Long servicoFiltro) {
        List<FaturamentoMensal> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        
        sql.append("SELECT ")
           .append("s.ser_codigo AS servico_id, ")
           .append("s.ser_nome AS tipo_servico, ")
           .append("DATE(a.age_data) AS data, ")
           .append("COUNT(ags.ser_codigo) AS quantidade_servicos, ")
           .append("s.ser_preco AS valor_unitario, ")
           .append("SUM(s.ser_preco) AS total_faturado ")
           .append("FROM agendamento a ")
           .append("JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo ")
           .append("JOIN servicos s ON ags.ser_codigo = s.ser_codigo ")
           .append("WHERE a.age_status = 'F' ")
           .append("AND a.age_pago = 'S' ");
        
        if (dataInicial != null) {
            sql.append("AND a.age_data >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND a.age_data <= ? ");
        }
        if (servicoFiltro != null) {
            sql.append("AND s.ser_codigo = ? ");
        }
        
        sql.append("GROUP BY s.ser_codigo, s.ser_nome, DATE(a.age_data), s.ser_preco ")
           .append("ORDER BY data DESC, tipo_servico ASC");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (dataInicial != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                ps.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            if (servicoFiltro != null) {
                ps.setLong(paramIndex++, servicoFiltro);
            }
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                FaturamentoMensal faturamento = new FaturamentoMensal();
                faturamento.setServicoId(rs.getLong("servico_id"));
                faturamento.setTipoServico(rs.getString("tipo_servico"));
                faturamento.setData(rs.getDate("data"));
                faturamento.setQuantidadeServicos(rs.getInt("quantidade_servicos"));
                faturamento.setValorUnitario(rs.getDouble("valor_unitario"));
                faturamento.setTotalFaturado(rs.getDouble("total_faturado"));
                lista.add(faturamento);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return lista;
    }
}

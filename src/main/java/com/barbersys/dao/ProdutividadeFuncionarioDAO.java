package com.barbersys.dao;

import java.sql.*;
import java.util.*;
import com.barbersys.model.ProdutividadeFuncionario;
import com.barbersys.util.DatabaseConnection;

public class ProdutividadeFuncionarioDAO {

    public static List<ProdutividadeFuncionario> buscarProdutividade(
            java.util.Date dataInicial, java.util.Date dataFinal, Long funcionarioId, int first, int pageSize,
            String sortField, String sortOrder) {

        List<ProdutividadeFuncionario> resultado = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    f.fun_codigo, " +
            "    f.fun_nome, " +
            "    a.age_data AS data, " +
            "    COUNT(CASE WHEN a.age_status = 'F' THEN 1 END) AS atendimentos_realizados, " +
            "    ROUND((COUNT(CASE WHEN a.age_status = 'I' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0)), 2) AS taxa_cancelamento, " +
            "    COALESCE(ROUND(AVG(av.ava_nota), 1), 0) AS media_avaliacoes " +
            "FROM funcionario f " +
            "INNER JOIN agendamento a ON f.fun_codigo = a.fun_codigo " +
            "LEFT JOIN avaliacao av ON a.age_codigo = av.age_codigo " +
            "WHERE 1=1 "
        );

        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND f.fun_codigo = ? ");
        }

        sql.append("GROUP BY f.fun_codigo, f.fun_nome, a.age_data ");
        
        // Mapeamento de campos para ordenação
        String colunaBanco = "data";
        if ("funcionarioNome".equals(sortField)) colunaBanco = "f.fun_nome";
        else if ("data".equals(sortField)) colunaBanco = "data";
        else if ("atendimentosRealizados".equals(sortField)) colunaBanco = "atendimentos_realizados";
        else if ("taxaCancelamento".equals(sortField)) colunaBanco = "taxa_cancelamento";
        else if ("mediaAvaliacoes".equals(sortField)) colunaBanco = "media_avaliacoes";
        
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
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex++, first);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProdutividadeFuncionario prod = new ProdutividadeFuncionario();
                prod.setFuncionarioId(rs.getLong("fun_codigo"));
                prod.setFuncionarioNome(rs.getString("fun_nome"));
                prod.setData(rs.getDate("data"));
                prod.setAtendimentosRealizados(rs.getInt("atendimentos_realizados"));
                prod.setTaxaCancelamento(rs.getDouble("taxa_cancelamento"));
                prod.setMediaAvaliacoes(rs.getDouble("media_avaliacoes"));
                resultado.add(prod);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resultado;
    }

    public static int contarProdutividade(java.util.Date dataInicial, java.util.Date dataFinal, Long funcionarioId) {

        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT CONCAT(f.fun_codigo, '_', a.age_data)) as total " +
            "FROM funcionario f " +
            "INNER JOIN agendamento a ON f.fun_codigo = a.fun_codigo " +
            "WHERE 1=1 "
        );

        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND f.fun_codigo = ? ");
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
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
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

    public static List<ProdutividadeFuncionario> buscarTodasProdutividade(
            java.util.Date dataInicial, java.util.Date dataFinal, Long funcionarioId) {

        List<ProdutividadeFuncionario> resultado = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "    f.fun_codigo, " +
            "    f.fun_nome, " +
            "    a.age_data AS data, " +
            "    COUNT(CASE WHEN a.age_status = 'F' THEN 1 END) AS atendimentos_realizados, " +
            "    ROUND((COUNT(CASE WHEN a.age_status = 'I' THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0)), 2) AS taxa_cancelamento, " +
            "    COALESCE(ROUND(AVG(av.ava_nota), 1), 0) AS media_avaliacoes " +
            "FROM funcionario f " +
            "INNER JOIN agendamento a ON f.fun_codigo = a.fun_codigo " +
            "LEFT JOIN avaliacao av ON a.age_codigo = av.age_codigo " +
            "WHERE 1=1 "
        );

        if (dataInicial != null) {
            sql.append("AND DATE(a.age_data) >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.age_data) <= ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND f.fun_codigo = ? ");
        }

        sql.append("GROUP BY f.fun_codigo, f.fun_nome, a.age_data ");
        sql.append("ORDER BY a.age_data DESC, f.fun_nome ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ProdutividadeFuncionario prod = new ProdutividadeFuncionario();
                prod.setFuncionarioId(rs.getLong("fun_codigo"));
                prod.setFuncionarioNome(rs.getString("fun_nome"));
                prod.setData(rs.getDate("data"));
                prod.setAtendimentosRealizados(rs.getInt("atendimentos_realizados"));
                prod.setTaxaCancelamento(rs.getDouble("taxa_cancelamento"));
                prod.setMediaAvaliacoes(rs.getDouble("media_avaliacoes"));
                resultado.add(prod);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resultado;
    }
}

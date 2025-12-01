package com.barbersys.dao;

import com.barbersys.model.Avaliacao;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AvaliacaoDAO {

    // Salva uma nova avaliação no banco
    public static Avaliacao salvar(Avaliacao avaliacao) throws SQLException {
        String sql = "INSERT INTO avaliacao (ava_nota, ava_comentario, ava_data_criacao, age_codigo, cli_codigo, fun_codigo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, avaliacao.getNota());
            stmt.setString(2, avaliacao.getComentario());
            stmt.setTimestamp(3, new java.sql.Timestamp(avaliacao.getDataCriacao().getTime()));
            stmt.setLong(4, avaliacao.getAgendamento().getId());
            stmt.setLong(5, avaliacao.getCliente().getId());
            stmt.setLong(6, avaliacao.getFuncionario().getId());
            
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    avaliacao.setId(generatedKeys.getLong(1));
                }
            }
        }
        return avaliacao;
    }

    // Verifica se o cliente já avaliou determinado agendamento
    public static boolean verificarSeJaAvaliou(Long agendamentoId, Long clienteId) {
        String sql = "SELECT COUNT(*) FROM avaliacao WHERE age_codigo = ? AND cli_codigo = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, agendamentoId);
            stmt.setLong(2, clienteId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Busca avaliações com paginação e filtros
    public static List<Avaliacao> buscarAvaliacoesPaginado(Integer nota, Long funcionarioId, java.util.Date dataInicial, java.util.Date dataFinal, int first, int pageSize, String sortField, String sortOrder) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT a.ava_codigo, a.ava_nota, a.ava_comentario, a.ava_data_criacao, " +
            "f.fun_codigo, f.fun_nome, " +
            "c.cli_codigo, c.cli_nome " +
            "FROM avaliacao a " +
            "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " +
            "JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "WHERE 1=1 "
        );
        
        if (nota != null) {
            sql.append("AND a.ava_nota = ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND a.fun_codigo = ? ");
        }
        if (dataInicial != null) {
            sql.append("AND a.ava_data_criacao >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.ava_data_criacao) <= ? ");
        }
        
        String colunaBanco = "a.ava_data_criacao";
        if ("dataCriacao".equals(sortField)) colunaBanco = "a.ava_data_criacao";
        else if ("funcionario.nome".equals(sortField)) colunaBanco = "f.fun_nome";
        else if ("nota".equals(sortField)) colunaBanco = "a.ava_nota";
        else if ("comentario".equals(sortField)) colunaBanco = "a.ava_comentario";
        
        String ordem = "DESC";
        if ("1".equals(sortOrder) || "ASC".equalsIgnoreCase(sortOrder)) {
            ordem = "ASC";
        }
        
        sql.append("ORDER BY ").append(colunaBanco).append(" ").append(ordem).append(" ");
        sql.append("LIMIT ? OFFSET ?");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (nota != null) {
                stmt.setInt(paramIndex++, nota);
            }
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
            }
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex++, first);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Avaliacao avaliacao = new Avaliacao();
                avaliacao.setId(rs.getLong("ava_codigo"));
                avaliacao.setNota(rs.getInt("ava_nota"));
                avaliacao.setComentario(rs.getString("ava_comentario"));
                avaliacao.setDataCriacao(rs.getDate("ava_data_criacao"));
                
                Funcionario funcionario = new Funcionario();
                funcionario.setId(rs.getLong("fun_codigo"));
                funcionario.setNome(rs.getString("fun_nome"));
                avaliacao.setFuncionario(funcionario);
                
                Cliente cliente = new Cliente();
                cliente.setId(rs.getLong("cli_codigo"));
                cliente.setNome(rs.getString("cli_nome"));
                avaliacao.setCliente(cliente);
                
                avaliacoes.add(avaliacao);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return avaliacoes;
    }

    // Conta total de avaliações com base nos filtros
    public static int contarAvaliacoes(Integer nota, Long funcionarioId, java.util.Date dataInicial, java.util.Date dataFinal) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM avaliacao a WHERE 1=1 "
        );
        
        if (nota != null) {
            sql.append("AND a.ava_nota = ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND a.fun_codigo = ? ");
        }
        if (dataInicial != null) {
            sql.append("AND a.ava_data_criacao >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.ava_data_criacao) <= ? ");
        }
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (nota != null) {
                stmt.setInt(paramIndex++, nota);
            }
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
            }
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }

    // Busca todas as avaliações sem paginação (usado em relatórios)
    public static List<Avaliacao> buscarTodasAvaliacoes(Integer nota, Long funcionarioId, java.util.Date dataInicial, java.util.Date dataFinal) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT a.ava_codigo, a.ava_nota, a.ava_comentario, a.ava_data_criacao, " +
            "f.fun_codigo, f.fun_nome, " +
            "c.cli_codigo, c.cli_nome " +
            "FROM avaliacao a " +
            "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " +
            "JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "WHERE 1=1 "
        );
        
        if (nota != null) {
            sql.append("AND a.ava_nota = ? ");
        }
        if (funcionarioId != null) {
            sql.append("AND a.fun_codigo = ? ");
        }
        if (dataInicial != null) {
            sql.append("AND a.ava_data_criacao >= ? ");
        }
        if (dataFinal != null) {
            sql.append("AND DATE(a.ava_data_criacao) <= ? ");
        }
        
        sql.append("ORDER BY a.ava_data_criacao DESC");
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (nota != null) {
                stmt.setInt(paramIndex++, nota);
            }
            if (funcionarioId != null) {
                stmt.setLong(paramIndex++, funcionarioId);
            }
            if (dataInicial != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
            }
            if (dataFinal != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Avaliacao avaliacao = new Avaliacao();
                avaliacao.setId(rs.getLong("ava_codigo"));
                avaliacao.setNota(rs.getInt("ava_nota"));
                avaliacao.setComentario(rs.getString("ava_comentario"));
                avaliacao.setDataCriacao(rs.getDate("ava_data_criacao"));
                
                Funcionario funcionario = new Funcionario();
                funcionario.setId(rs.getLong("fun_codigo"));
                funcionario.setNome(rs.getString("fun_nome"));
                avaliacao.setFuncionario(funcionario);
                
                Cliente cliente = new Cliente();
                cliente.setId(rs.getLong("cli_codigo"));
                cliente.setNome(rs.getString("cli_nome"));
                avaliacao.setCliente(cliente);
                
                avaliacoes.add(avaliacao);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return avaliacoes;
    }
}

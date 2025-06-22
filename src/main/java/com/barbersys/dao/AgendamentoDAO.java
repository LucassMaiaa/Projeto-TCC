package com.barbersys.dao;

import java.sql.*;
import java.util.*;

import com.barbersys.model.Agendamento;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Servicos;
import com.barbersys.util.DatabaseConnection;

public class AgendamentoDAO {

    public static int agendamentoCount(String nomeCliente, String nomeFuncionario, String status) {
        int total = 0;
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(DISTINCT a.age_codigo) " +
            "FROM agendamento a " +
            "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " +
            "WHERE 1=1 ");

        if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
            sql.append(" AND a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?");
        }
        if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
            sql.append(" AND LOWER(f.fun_nome) LIKE ?");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND a.age_status = ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + nomeCliente.toLowerCase() + "%");
            }
            if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + nomeFuncionario.toLowerCase() + "%");
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(paramIndex++, status);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }

    public static List<Agendamento> buscarAgendamentos(String nomeCliente, String nomeFuncionario, String status) {
        Map<Long, Agendamento> mapaAgendamentos = new LinkedHashMap<>();

        StringBuilder sql = new StringBuilder(
            "SELECT " +
            "a.age_codigo AS agendamento_id, " +
            "a.age_status AS agendamento_status, " +
            "a.age_data AS agendamento_data, " +
            "a.age_hora AS agendamento_hora, " +
            "a.age_tipo_cadastro AS agendamento_tipo, " +
            "s.ser_codigo AS servico_id, " +
            "s.ser_nome AS servico_nome, " +
            "s.ser_preco AS servico_preco, " +
            "c.cli_codigo AS cliente_id, " +
            "c.cli_nome AS cliente_nome, " +
            "a.age_nome_cliente AS nome_cliente_avulso, " +
            "f.fun_codigo AS funcionario_id, " +
            "f.fun_nome AS funcionario_nome " +
            "FROM agendamento a " +
            "JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo " +
            "JOIN servicos s ON s.ser_codigo = ags.ser_codigo " +
            "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " +
            "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " +
            "WHERE 1=1 ");

        if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
            sql.append(" AND ( (a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?) )");
        }
        if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
            sql.append(" AND LOWER(f.fun_nome) LIKE ?");
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND a.age_status = ?");
        }
        sql.append(" ORDER BY a.age_codigo DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
                String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
                ps.setString(paramIndex++, nomeParam);
                ps.setString(paramIndex++, nomeParam); 
            }
            if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + nomeFuncionario.toLowerCase() + "%");
            }
            if (status != null && !status.trim().isEmpty()) {
                ps.setString(paramIndex++, status);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Long agendamentoId = rs.getLong("agendamento_id");
                Agendamento agendamento = mapaAgendamentos.get(agendamentoId);

                if (agendamento == null) {
                    agendamento = new Agendamento();
                    agendamento.setId(agendamentoId);
                    agendamento.setStatus(rs.getString("agendamento_status"));
                    agendamento.setDataCriado(rs.getDate("agendamento_data"));
                    agendamento.setHoraSelecionada(rs.getTime("agendamento_hora").toLocalTime());
                    agendamento.setTipoCadastro(rs.getString("agendamento_tipo"));

                    Long clienteId = rs.getLong("cliente_id");
                    String clienteNome = rs.getString("cliente_nome");
                    String nomeAvulso = rs.getString("nome_cliente_avulso");

                    if (clienteId != 0 && clienteNome != null) {
                        Cliente cliente = new Cliente();
                        cliente.setId(clienteId);
                        cliente.setNome(clienteNome);
                        agendamento.setCliente(cliente);
                    } else if (nomeAvulso != null && !nomeAvulso.trim().isEmpty()) {
                        agendamento.setNomeClienteAvulso(nomeAvulso);
                    }

                    Funcionario funcionario = new Funcionario();
                    funcionario.setId(rs.getLong("funcionario_id"));
                    funcionario.setNome(rs.getString("funcionario_nome"));
                    agendamento.setFuncionario(funcionario);

                    agendamento.setServicos(new ArrayList<>());
                    mapaAgendamentos.put(agendamentoId, agendamento);
                }

                Servicos servico = new Servicos();
                servico.setId(rs.getLong("servico_id"));
                servico.setNome(rs.getString("servico_nome"));
                servico.setPreco(rs.getDouble("servico_preco"));
                agendamento.getServicos().add(servico);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(mapaAgendamentos.values());
    }

    public static void atualizar(Agendamento agendamento, List<Long> servicos) {
        String sql = "UPDATE agendamento SET age_status = ?, age_data = ?, age_hora = ?, fun_codigo = ?, cli_codigo = ?, age_nome_cliente = ? WHERE age_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, agendamento.getStatus());
            stmt.setDate(2, new java.sql.Date(agendamento.getDataCriado().getTime()));
            stmt.setTime(3, java.sql.Time.valueOf(agendamento.getHoraSelecionada()));
            stmt.setLong(4, agendamento.getFuncionario().getId());

            if (agendamento.getCliente() != null) {
                stmt.setLong(5, agendamento.getCliente().getId());
                stmt.setNull(6, java.sql.Types.VARCHAR);
            } else {
                stmt.setNull(5, java.sql.Types.BIGINT);
                stmt.setString(6, agendamento.getNomeClienteAvulso());
            }

            stmt.setLong(7, agendamento.getId());

            stmt.executeUpdate();
            atualizarServicosDoAgendamento(conn, agendamento, servicos);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void atualizarServicosDoAgendamento(Connection conn, Agendamento agendamento, List<Long> servicos) throws SQLException {
        String deleteSql = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setLong(1, agendamento.getId());
            deleteStmt.executeUpdate();
        }

        String insertSql = "INSERT INTO agendamento_servico (age_codigo, ser_codigo) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (Long id : servicos) {
                Servicos servico = ServicosDAO.buscarPorId(id);
                insertStmt.setLong(1, agendamento.getId());
                insertStmt.setLong(2, servico.getId());
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    public static void deletarAgendamento(Long agendamentoId) {
        String sqlDeleteServicos = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
        String sqlDeleteAgendamento = "DELETE FROM agendamento WHERE age_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmtServicos = conn.prepareStatement(sqlDeleteServicos);
                 PreparedStatement stmtAgendamento = conn.prepareStatement(sqlDeleteAgendamento)) {

                stmtServicos.setLong(1, agendamentoId);
                stmtServicos.executeUpdate();

                stmtAgendamento.setLong(1, agendamentoId);
                stmtAgendamento.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void salvar(Agendamento agendamento, List<Long> servicos) {
        String sql = "INSERT INTO agendamento (age_status, age_data, age_hora, fun_codigo, cli_codigo, age_nome_cliente, age_tipo_cadastro) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, agendamento.getStatus());
            stmt.setDate(2, new java.sql.Date(agendamento.getDataCriado().getTime()));
            stmt.setTime(3, java.sql.Time.valueOf(agendamento.getHoraSelecionada()));
            stmt.setLong(4, agendamento.getFuncionario().getId());

            if (agendamento.getCliente() != null) {
                stmt.setLong(5, agendamento.getCliente().getId());
                stmt.setNull(6, java.sql.Types.VARCHAR);
            } else {
                stmt.setNull(5, java.sql.Types.BIGINT);
                stmt.setString(6, agendamento.getNomeClienteAvulso());
            }

            stmt.setString(7, agendamento.getTipoCadastro());

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    agendamento.setId(generatedKeys.getLong(1));
                }
            }

            String sqlInsertServico = "INSERT INTO agendamento_servico (age_codigo, ser_codigo) VALUES (?, ?)";
            try (PreparedStatement psServico = conn.prepareStatement(sqlInsertServico)) {
                for (Long id : servicos) {
                    Servicos servico = ServicosDAO.buscarPorId(id);
                    psServico.setLong(1, agendamento.getId());
                    psServico.setLong(2, servico.getId());
                    psServico.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

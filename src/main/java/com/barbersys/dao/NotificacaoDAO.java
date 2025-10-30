package com.barbersys.dao;

import com.barbersys.model.Agendamento;
import com.barbersys.model.Cliente;
import com.barbersys.model.Notificacao;
import com.barbersys.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacaoDAO {

    public Notificacao salvar(Notificacao notificacao) throws SQLException {
        String sql = "INSERT INTO notificacao (not_mensagem, not_data_envio, not_status, age_codigo, cli_codigo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, notificacao.getMensagem());
            stmt.setTimestamp(2, new Timestamp(notificacao.getDataEnvio().getTime()));
            stmt.setString(3, "A"); // Sempre salva como Ativa
            stmt.setLong(4, notificacao.getAgendamento().getId());
            
            if (notificacao.getCliente() != null) {
                stmt.setLong(5, notificacao.getCliente().getId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notificacao.setId(generatedKeys.getLong(1));
                }
            }
        }
        return notificacao;
    }

    public List<Notificacao> buscarAtivas() {
        List<Notificacao> notificacoes = new ArrayList<>();
        String sql = "SELECT not_codigo, not_mensagem, not_data_envio FROM notificacao " +
                     "WHERE not_status = 'A' AND cli_codigo IS NULL ORDER BY not_data_envio DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Notificacao n = new Notificacao();
                n.setId(rs.getLong("not_codigo"));
                n.setMensagem(rs.getString("not_mensagem"));
                n.setDataEnvio(rs.getTimestamp("not_data_envio"));
                notificacoes.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notificacoes;
    }

    public List<Notificacao> buscarAtivasPorCliente(Long clienteId) {
        List<Notificacao> notificacoes = new ArrayList<>();
        String sql = "SELECT not_codigo, not_mensagem, not_data_envio, age_codigo FROM notificacao " +
                     "WHERE not_status = 'A' AND cli_codigo = ? ORDER BY not_data_envio DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, clienteId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Notificacao n = new Notificacao();
                n.setId(rs.getLong("not_codigo"));
                n.setMensagem(rs.getString("not_mensagem"));
                n.setDataEnvio(rs.getTimestamp("not_data_envio"));
                
                // Adiciona o agendamento
                Long agendamentoId = rs.getLong("age_codigo");
                if (agendamentoId != null && agendamentoId > 0) {
                    Agendamento agendamento = new Agendamento();
                    agendamento.setId(agendamentoId);
                    n.setAgendamento(agendamento);
                }
                
                notificacoes.add(n);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notificacoes;
    }

    public void marcarComoInativa(Notificacao notificacao) throws SQLException {
        String sql = "UPDATE notificacao SET not_status = 'I' WHERE not_codigo = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificacao.getId());
            stmt.executeUpdate();
        }
    }
}

package com.barbersys.dao;

import com.barbersys.model.Avaliacao;
import com.barbersys.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AvaliacaoDAO {

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
}

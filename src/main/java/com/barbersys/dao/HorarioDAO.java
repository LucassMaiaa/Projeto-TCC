package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.Horario;
import com.barbersys.util.DatabaseConnection;

public class HorarioDAO {

    public static List<Horario> buscarHorariosPorFuncionario(Long funcionarioId) {
        List<Horario> horarios = new ArrayList<>();
        String sql = "SELECT id, hora_inicial, hora_final FROM horario WHERE funcionario_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, funcionarioId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Horario horario = new Horario();
                horario.setId(rs.getLong("id"));
                horario.setHoraInicial(rs.getTime("hora_inicial"));
                horario.setHoraFinal(rs.getTime("hora_final"));
                horario.setFuncionarioId(funcionarioId);
                horarios.add(horario);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return horarios;
    }
    
    public static List<Horario> buscarHorariosPorFuncionarioPaginado(Long funcionarioId, int first, int pageSize) {
        List<Horario> horarios = new ArrayList<>();
        String sql = "SELECT id, hora_inicial, hora_final FROM horario WHERE funcionario_id = ? LIMIT ?, ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, funcionarioId);
            ps.setInt(2, first);
            ps.setInt(3, pageSize);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Horario horario = new Horario();
                horario.setId(rs.getLong("id"));
                horario.setHoraInicial(rs.getTime("hora_inicial"));
                horario.setHoraFinal(rs.getTime("hora_final"));
                horario.setFuncionarioId(funcionarioId);
                horarios.add(horario);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return horarios;
    }
    
    public static int countHorariosPorFuncionario(Long funcionarioId) {
        int total = 0;
        String sql = "SELECT COUNT(*) FROM horario WHERE funcionario_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setLong(1, funcionarioId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                total = rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return total;
    }


}

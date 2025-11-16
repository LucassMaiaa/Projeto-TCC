package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.ControleCaixa;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.util.DatabaseConnection;

public class HorarioDAO {

	public static List<Horario> buscarHorariosPorFuncionarioPaginado(Funcionario funcionario, int first, int pageSize) {
		List<Horario> horarios = new ArrayList<>();
		String sql = "SELECT * FROM horario WHERE fun_codigo = ? LIMIT ?, ?";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, funcionario.getId());
			ps.setInt(2, first);
			ps.setInt(3, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Horario horario = new Horario();
				horario.setId(rs.getLong("hor_codigo"));
				horario.setHoraInicial(rs.getTime("hor_hora_inicio").toLocalTime());
				horario.setHoraFinal(rs.getTime("hor_hora_fim").toLocalTime());
				horario.setFuncionario(funcionario);
				horario.setDomingo(rs.getBoolean("hor_domingo"));
				horario.setSegunda(rs.getBoolean("hor_segunda"));
				horario.setTerca(rs.getBoolean("hor_terca"));
				horario.setQuarta(rs.getBoolean("hor_quarta"));
				horario.setQuinta(rs.getBoolean("hor_quinta"));
				horario.setSexta(rs.getBoolean("hor_sexta"));
				horario.setSabado(rs.getBoolean("hor_sabado"));
				horarios.add(horario);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return horarios;
	}

	public static int countHorariosPorFuncionario(Funcionario funcionario) {
		if (funcionario == null || funcionario.getId() == null) {
			return 0;
		}

		int total = 0;
		String sql = "SELECT COUNT(*) FROM horario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, funcionario.getId());
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				total = rs.getInt(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return total;
	}

	public static void deletar(Long horario) {
		String sql = "DELETE FROM horario WHERE hor_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, horario);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void salvar(Horario horario) {
		String sql = "INSERT INTO horario (hor_hora_inicio, hor_hora_fim, fun_codigo, " +
					 "hor_domingo, hor_segunda, hor_terca, hor_quarta, hor_quinta, hor_sexta, hor_sabado) " +
					 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			stmt.setTime(1, java.sql.Time.valueOf(horario.getHoraInicial()));
			stmt.setTime(2, java.sql.Time.valueOf(horario.getHoraFinal()));
			stmt.setLong(3, horario.getFuncionario().getId());
			stmt.setBoolean(4, horario.getDomingo() != null ? horario.getDomingo() : false);
			stmt.setBoolean(5, horario.getSegunda() != null ? horario.getSegunda() : false);
			stmt.setBoolean(6, horario.getTerca() != null ? horario.getTerca() : false);
			stmt.setBoolean(7, horario.getQuarta() != null ? horario.getQuarta() : false);
			stmt.setBoolean(8, horario.getQuinta() != null ? horario.getQuinta() : false);
			stmt.setBoolean(9, horario.getSexta() != null ? horario.getSexta() : false);
			stmt.setBoolean(10, horario.getSabado() != null ? horario.getSabado() : false);
			stmt.executeUpdate();
			
			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					horario.setId(generatedKeys.getLong(1));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
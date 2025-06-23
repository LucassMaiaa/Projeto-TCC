package com.barbersys.dao;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.barbersys.model.CaixaData;
import com.barbersys.model.ControleCaixa;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.util.DatabaseConnection;

public class FuncionarioDAO {
	
	public static int funcionarioCount(String nome, String status) {
	    int total = 0;
	    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM funcionario WHERE 1=1");

	    if (nome != null && !nome.trim().isEmpty()) {
	        sql.append(" AND LOWER(fun_nome) LIKE ?");
	    }

	    if (status != null && !status.equals("")) {
	        sql.append(" AND fun_status = ?");
	    }

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql.toString())) {

	        int paramIndex = 1;

	        if (nome != null && !nome.trim().isEmpty()) {
	            ps.setString(paramIndex++, "%" + nome.toLowerCase() + "%");
	        }

	        if (status != null && !status.equals("")) {
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
	
	public static List<Funcionario> buscarTodosFuncionarios() {
		List<Funcionario> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM funcionario WHERE fun_status = ? ORDER BY fun_codigo DESC");


		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {
			
			ps.setString(1, "A");
			
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Funcionario funcionario = new Funcionario();
				funcionario.setId(rs.getLong("fun_codigo"));
				funcionario.setNome(rs.getString("fun_nome"));
				funcionario.setStatus(rs.getString("fun_status"));
				lista.add(funcionario);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}


	public static List<Funcionario> buscarFuncionario(String nome, String status, int first, int pageSize) {
		List<Funcionario> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM funcionario WHERE 1=1");

		if (nome != null && !nome.trim().isEmpty()) {
			sql.append(" AND LOWER(fun_nome) LIKE ?");
		}

		if (status != null && !status.isEmpty()) {
			sql.append(" AND fun_status = ?");
		}

		sql.append(" ORDER BY fun_codigo DESC LIMIT ?, ?");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;

			if (nome != null && !nome.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nome.toLowerCase() + "%");
			}

			if (status != null && !status.isEmpty()) {
				ps.setString(paramIndex++, status);
			}

			ps.setInt(paramIndex++, first);
			ps.setInt(paramIndex, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Funcionario funcionario = new Funcionario();
				funcionario.setId(rs.getLong("fun_codigo"));
				funcionario.setNome(rs.getString("fun_nome"));
				funcionario.setStatus(rs.getString("fun_status"));
				lista.add(funcionario);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}
	
	public static List<Horario> buscarHorarioPorFuncionario(Funcionario funcionario) {
	    List<Horario> lista = new ArrayList<>();
	    String sql = "SELECT * FROM horario WHERE fun_codigo = ?";

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement stmt = conn.prepareStatement(sql)) {

	        stmt.setLong(1, funcionario.getId());
	        ResultSet rs = stmt.executeQuery();

	        while (rs.next()) {
	            Horario horario = new Horario();
	            horario.setId(rs.getLong("hor_codigo"));
	            horario.setHoraInicial(rs.getTime("hor_hora_inicio").toLocalTime());
	            horario.setHoraFinal(rs.getTime("hor_hora_fim").toLocalTime());

	            lista.add(horario);
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }

	    return lista;
	}

	public static void atualizar(Funcionario funcionario) {
		String sql = "UPDATE funcionario SET fun_nome = ?, fun_status = ? WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
			stmt.setLong(3, funcionario.getId());

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void deletarHorariosPorFuncionario(Funcionario funcionario) {
		String sql = "DELETE FROM horario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, funcionario.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void deletar(Funcionario funcionario) {
		String sql = "DELETE FROM funcionario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			deletarHorariosPorFuncionario(funcionario);

			stmt.setLong(1, funcionario.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void salvar(Funcionario funcionario) {
		String sql = "INSERT INTO funcionario (fun_nome, fun_status) VALUES (?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
			stmt.executeUpdate();

			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					funcionario.setId(generatedKeys.getLong(1));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

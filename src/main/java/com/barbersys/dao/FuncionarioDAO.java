package com.barbersys.dao;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.model.Usuario;
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

	private static Funcionario mapResultSetToFuncionario(ResultSet rs) throws SQLException {
		Funcionario funcionario = new Funcionario();
		funcionario.setId(rs.getLong("fun_codigo"));
		funcionario.setNome(rs.getString("fun_nome"));
		funcionario.setStatus(rs.getString("fun_status"));

		if (rs.getObject("usu_codigo") != null) {
			Usuario usuario = new Usuario();
			usuario.setId(rs.getLong("usu_codigo"));
			usuario.setLogin(rs.getString("usu_login"));
			funcionario.setUsuario(usuario);
		}
		return funcionario;
	}

	public static List<Funcionario> buscarFuncionario(String nome, String status, int first, int pageSize) {
		List<Funcionario> lista = new ArrayList<>();
		String sql = "SELECT * FROM funcionario f LEFT JOIN usuario u ON f.usu_codigo = u.usu_codigo WHERE 1=1";

		if (nome != null && !nome.trim().isEmpty()) {
			sql += " AND LOWER(f.fun_nome) LIKE ?";
		}

		if (status != null && !status.isEmpty()) {
			sql += " AND f.fun_status = ?";
		}

		sql += " ORDER BY f.fun_codigo DESC LIMIT ?, ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

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
				lista.add(mapResultSetToFuncionario(rs));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static List<Funcionario> buscarTodosFuncionarios() {
		List<Funcionario> lista = new ArrayList<>();
		String sql = "SELECT * FROM funcionario WHERE fun_status = 'A' ORDER BY fun_nome";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

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

	public static Funcionario buscarFuncionarioPorUsuarioId(Long usuarioId) {
		Funcionario funcionario = null;
		String sql = "SELECT * FROM funcionario f LEFT JOIN usuario u ON f.usu_codigo = u.usu_codigo WHERE f.usu_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, usuarioId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				funcionario = mapResultSetToFuncionario(rs);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return funcionario;
	}

	public static List<Horario> buscarHorarioPorFuncionario(Funcionario funcionario) {
		List<Horario> lista = new ArrayList<>();
		String sql = "SELECT * FROM horario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, funcionario.getId());
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Horario horario = new Horario();
				horario.setId(rs.getLong("hor_codigo"));
				horario.setHoraInicial(rs.getTime("hor_hora_inicio").toLocalTime());
				horario.setHoraFinal(rs.getTime("hor_hora_fim").toLocalTime());
				lista.add(horario);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static void atualizar(Funcionario funcionario) throws SQLException {
        if (funcionario.getUsuario() != null && funcionario.getUsuario().getId() != null && funcionario.getUsuario().getId() > 0) {
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(funcionario.getUsuario());
        }

		String sql = "UPDATE funcionario SET fun_nome = ?, fun_status = ? WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
			stmt.setLong(3, funcionario.getId());

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
            throw e;
		}
	}

	public static void deletarHorariosPorFuncionario(Funcionario funcionario) {
		String sql = "DELETE FROM horario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, funcionario.getId());
			stmt.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void deletar(Funcionario funcionario) throws SQLException {
		String sql = "DELETE FROM funcionario WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			deletarHorariosPorFuncionario(funcionario);

			stmt.setLong(1, funcionario.getId());
			stmt.executeUpdate();

            if (funcionario.getUsuario() != null && funcionario.getUsuario().getId() != null && funcionario.getUsuario().getId() > 0) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                usuarioDAO.deletar(funcionario.getUsuario());
            }

		} catch (SQLException e) {
			e.printStackTrace();
            throw e;
		}
	}

	public static void salvar(Funcionario funcionario) throws SQLException {
		String sql = "INSERT INTO funcionario (fun_nome, fun_status, usu_codigo) VALUES (?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
            stmt.setLong(3, funcionario.getUsuario().getId());
			stmt.executeUpdate();

			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					funcionario.setId(generatedKeys.getLong(1));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
            throw e;
		}
	}

}
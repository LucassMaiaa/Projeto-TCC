package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.barbersys.model.Funcionario;
import com.barbersys.model.RestricaoData;
import com.barbersys.util.DatabaseConnection;

public class RestricaoDataDAO {

	public static List<RestricaoData> buscarRestricoes(String descricao, int first, int pageSize) {
		List<RestricaoData> lista = new ArrayList<>();
		String sql = "SELECT r.*, f.fun_nome FROM restricao_data r " +
					 "LEFT JOIN funcionario f ON r.fun_codigo = f.fun_codigo " +
					 "WHERE r.res_status = 'A'";

		if (descricao != null && !descricao.trim().isEmpty()) {
			sql += " AND LOWER(r.res_descricao) LIKE ?";
		}

		sql += " ORDER BY r.res_data DESC LIMIT ?, ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			int paramIndex = 1;

			if (descricao != null && !descricao.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + descricao.toLowerCase() + "%");
			}

			ps.setInt(paramIndex++, first);
			ps.setInt(paramIndex, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				RestricaoData restricao = new RestricaoData();
				restricao.setId(rs.getLong("res_codigo"));
				restricao.setData(rs.getDate("res_data"));
				restricao.setDescricao(rs.getString("res_descricao"));
				restricao.setTipo(rs.getString("res_tipo"));
				restricao.setStatus(rs.getString("res_status"));

				if (rs.getObject("fun_codigo") != null) {
					Funcionario func = new Funcionario();
					func.setId(rs.getLong("fun_codigo"));
					func.setNome(rs.getString("fun_nome"));
					restricao.setFuncionario(func);
				}

				lista.add(restricao);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static int restricaoCount(String descricao) {
		int total = 0;
		String sql = "SELECT COUNT(*) FROM restricao_data WHERE res_status = 'A'";

		if (descricao != null && !descricao.trim().isEmpty()) {
			sql += " AND LOWER(res_descricao) LIKE ?";
		}

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			if (descricao != null && !descricao.trim().isEmpty()) {
				ps.setString(1, "%" + descricao.toLowerCase() + "%");
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

	public static void salvar(RestricaoData restricao) throws SQLException {
		String sql = "INSERT INTO restricao_data (res_data, res_descricao, res_tipo, fun_codigo, res_status) " +
					 "VALUES (?, ?, ?, ?, ?)";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			stmt.setDate(1, new java.sql.Date(restricao.getData().getTime()));
			stmt.setString(2, restricao.getDescricao());
			stmt.setString(3, restricao.getTipo());
			
			if (restricao.getFuncionario() != null && restricao.getFuncionario().getId() != null) {
				stmt.setLong(4, restricao.getFuncionario().getId());
			} else {
				stmt.setNull(4, java.sql.Types.BIGINT);
			}
			
			stmt.setString(5, restricao.getStatus());
			stmt.executeUpdate();

			try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					restricao.setId(generatedKeys.getLong(1));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static void atualizar(RestricaoData restricao) throws SQLException {
		String sql = "UPDATE restricao_data SET res_data = ?, res_descricao = ?, res_tipo = ?, fun_codigo = ?, res_status = ? " +
					 "WHERE res_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setDate(1, new java.sql.Date(restricao.getData().getTime()));
			stmt.setString(2, restricao.getDescricao());
			stmt.setString(3, restricao.getTipo());
			
			if (restricao.getFuncionario() != null && restricao.getFuncionario().getId() != null) {
				stmt.setLong(4, restricao.getFuncionario().getId());
			} else {
				stmt.setNull(4, java.sql.Types.BIGINT);
			}
			
			stmt.setString(5, restricao.getStatus());
			stmt.setLong(6, restricao.getId());

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static void deletar(RestricaoData restricao) throws SQLException {
		String sql = "UPDATE restricao_data SET res_status = 'I' WHERE res_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, restricao.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}

	// Verifica se uma data está bloqueada para um funcionário específico
	public static boolean isDataBloqueada(Date data, Long funcionarioId) {
		String sql = "SELECT COUNT(*) FROM restricao_data " +
					 "WHERE res_data = ? AND res_status = 'A' " +
					 "AND (res_tipo = 'G' OR (res_tipo = 'F' AND fun_codigo = ?))";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setDate(1, new java.sql.Date(data.getTime()));
			ps.setLong(2, funcionarioId);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
	// Busca todas as datas bloqueadas para relatórios/visualização
	public static List<Date> buscarDatasBloqueadas() {
		List<Date> datas = new ArrayList<>();
		String sql = "SELECT DISTINCT res_data FROM restricao_data WHERE res_status = 'A' ORDER BY res_data";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				datas.add(rs.getDate("res_data"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return datas;
	}
}

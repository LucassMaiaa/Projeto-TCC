package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.Servicos;
import com.barbersys.util.DatabaseConnection;

public class ServicosDAO {
	
	public static int servicosCount(String nome, String status) {
	    int total = 0;
	    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM servicos WHERE 1=1");

	    if (nome != null && !nome.trim().isEmpty()) {
	        sql.append(" AND LOWER(ser_nome) LIKE ?");
	    }

	    if (status != null && !status.equals("")) {
	        sql.append(" AND ser_status = ?");
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


	public static List<Servicos> buscarServico(String nome, String status, int first, int pageSize) {
		List<Servicos> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM servicos WHERE 1=1");

		if (nome != null && !nome.trim().isEmpty()) {
			sql.append(" AND LOWER(ser_nome) LIKE ?");
		}

		if (status != null && !status.isEmpty()) {
			sql.append(" AND ser_status = ?");
		}

		sql.append(" ORDER BY ser_codigo DESC LIMIT ?, ?");

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
				Servicos servicos = new Servicos();
				servicos.setId(rs.getLong("ser_codigo"));
				servicos.setNome(rs.getString("ser_nome"));
				servicos.setStatus(rs.getString("ser_status"));
				servicos.setMinutos(rs.getInt("ser_minutos"));
				servicos.setPreco(rs.getDouble("ser_preco"));
				lista.add(servicos);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static void atualizar(Servicos servicos) {
		String sql = "UPDATE servicos SET ser_nome = ?, ser_preco = ?, ser_minutos = ?, ser_status = ? WHERE ser_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, servicos.getNome());
			stmt.setDouble(2, servicos.getPreco());
			stmt.setInt(3, servicos.getMinutos());
			stmt.setString(4, servicos.getStatus());
			stmt.setLong(5, servicos.getId());

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	public static void deletar(Servicos servicos) {
		String sql = "DELETE FROM servicos WHERE ser_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, servicos.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void salvar(Servicos servicos) {
		String sql = "INSERT INTO servicos (ser_nome, ser_preco, ser_minutos ,ser_status) VALUES (?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, servicos.getNome());
			stmt.setDouble(2, servicos.getPreco());
			stmt.setInt(3, servicos.getMinutos());
			stmt.setString(4, servicos.getStatus());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}



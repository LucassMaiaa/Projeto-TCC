package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.Pagamento;
import com.barbersys.util.DatabaseConnection;

public class PagamentoDAO {
	
	public static int pagamentoCount(String nome, String status) {
	    int total = 0;
	    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM pagamento WHERE 1=1");

	    if (nome != null && !nome.trim().isEmpty()) {
	        sql.append(" AND LOWER(pag_nome) LIKE ?");
	    }

	    if (status != null && !status.equals("")) {
	        sql.append(" AND pag_status = ?");
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
	
	public static List<Pagamento> buscarTodosPagamento() {
		List<Pagamento> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM pagamento ORDER BY pag_codigo DESC");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Pagamento pagamento = new Pagamento();
				pagamento.setId(rs.getLong("pag_codigo"));
				pagamento.setNome(rs.getString("pag_nome"));
				pagamento.setStatus(rs.getString("pag_status"));
				lista.add(pagamento);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}


	public static List<Pagamento> buscarPagamento(String nome, String status, int first, int pageSize) {
		List<Pagamento> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM pagamento WHERE 1=1");

		if (nome != null && !nome.trim().isEmpty()) {
			sql.append(" AND LOWER(pag_nome) LIKE ?");
		}

		if (status != null && !status.isEmpty()) {
			sql.append(" AND pag_status = ?");
		}

		sql.append(" ORDER BY pag_codigo DESC LIMIT ?, ?");

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
				Pagamento pagamento = new Pagamento();
				pagamento.setId(rs.getLong("pag_codigo"));
				pagamento.setNome(rs.getString("pag_nome"));
				pagamento.setStatus(rs.getString("pag_status"));
				lista.add(pagamento);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static void atualizar(Pagamento pagamento) {
		String sql = "UPDATE pagamento SET pag_nome = ?, pag_status = ? WHERE pag_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, pagamento.getNome());
			stmt.setString(2, pagamento.getStatus());
			stmt.setLong(3, pagamento.getId());

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	public static void deletar(Pagamento pagamento) {
		String sql = "DELETE FROM pagamento WHERE pag_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, pagamento.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void salvar(Pagamento pagamento) {
		String sql = "INSERT INTO pagamento (pag_nome, pag_status) VALUES (?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, pagamento.getNome());
			stmt.setString(2, pagamento.getStatus());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}


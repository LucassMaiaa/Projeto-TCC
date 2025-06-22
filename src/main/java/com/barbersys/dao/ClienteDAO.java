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

import com.barbersys.model.Cliente;
import com.barbersys.util.DatabaseConnection;

public class ClienteDAO {

	public static int clienteCount(String nome) {
		int total = 0;
		StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM cliente WHERE 1=1");

		if (nome != null && !nome.trim().isEmpty()) {
			sql.append(" AND LOWER(cli_nome) LIKE ?");
		}

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;

			if (nome != null && !nome.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nome.toLowerCase() + "%");
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
	
	public static List<Cliente> buscarTodosClientes() {
		List<Cliente> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM cliente ORDER BY cli_codigo DESC");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {


			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Cliente cliente = new Cliente();
				cliente.setId(rs.getLong("cli_codigo"));
				cliente.setNome(rs.getString("cli_nome"));
				cliente.setEmail(rs.getString("cli_email"));
				cliente.setTelefone(rs.getString("cli_telefone"));
				cliente.setCpf(rs.getString("cli_cpf"));
				lista.add(cliente);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static List<Cliente> buscarCliente(String nome, int first, int pageSize) {
		List<Cliente> lista = new ArrayList<>();
		StringBuilder sql = new StringBuilder("SELECT * FROM cliente WHERE 1=1");

		if (nome != null && !nome.trim().isEmpty()) {
			sql.append(" AND LOWER(cli_nome) LIKE ?");
		}

		sql.append(" ORDER BY cli_codigo DESC LIMIT ?, ?");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;

			if (nome != null && !nome.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nome.toLowerCase() + "%");
			}

			ps.setInt(paramIndex++, first);
			ps.setInt(paramIndex, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Cliente cliente = new Cliente();
				cliente.setId(rs.getLong("cli_codigo"));
				cliente.setNome(rs.getString("cli_nome"));
				cliente.setEmail(rs.getString("cli_email"));
				cliente.setTelefone(rs.getString("cli_telefone"));
				cliente.setCpf(rs.getString("cli_cpf"));
				lista.add(cliente);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static void atualizar(Cliente cliente) {
		String sql = "UPDATE cliente SET cli_nome = ?, cli_email = ?, cli_telefone = ?, cli_cpf = ? WHERE cli_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			
			String telefoneFormat =  cliente.getTelefone().replace("(", "").replace(")", "").replace("-", "").replace(" ", "").trim();
			String cpfFormat = cliente.getCpf().replace(".", "").replace("-", "").replace(" ", "").trim();

			stmt.setString(1, cliente.getNome());
			stmt.setString(2, cliente.getEmail());
			stmt.setString(3, telefoneFormat);
			stmt.setString(4, cpfFormat);
			stmt.setLong(5, cliente.getId());

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void deletar(Cliente cliente) {
		String sql = "DELETE FROM cliente WHERE cli_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, cliente.getId());
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void salvar(Cliente cliente) {
		String sql = "INSERT INTO cliente (cli_nome, cli_email, cli_telefone, cli_cpf) VALUES (?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			
			String telefoneFormat =  cliente.getTelefone().replace("(", "").replace(")", "").replace("-", "").replace(" ", "").trim();
			String cpfFormat = cliente.getCpf().replace(".", "").replace("-", "").replace(" ", "").trim();
			
			stmt.setString(1, cliente.getNome());
			stmt.setString(2, cliente.getEmail());
			stmt.setString(3, telefoneFormat);
			stmt.setString(4, cpfFormat);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}

package com.barbersys.dao;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.barbersys.model.Cliente;
import com.barbersys.model.Usuario;
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
	
	private static Cliente mapResultSetToCliente(ResultSet rs) throws SQLException {
		Cliente cliente = new Cliente();
		cliente.setId(rs.getLong("cli_codigo"));
		cliente.setNome(rs.getString("cli_nome"));
		cliente.setEmail(rs.getString("cli_email"));
		cliente.setTelefone(rs.getString("cli_telefone"));
		cliente.setCpf(rs.getString("cli_cpf"));

		if (rs.getObject("usu_codigo") != null) {
			Usuario usuario = new Usuario();
			usuario.setId(rs.getLong("usu_codigo"));
			usuario.setLogin(rs.getString("usu_login"));
			// A senha não é trafegada por segurança
			cliente.setUsuario(usuario);
		}
		return cliente;
	}

	public static List<Cliente> buscarTodosClientes() {
		List<Cliente> lista = new ArrayList<>();
		String sql = "SELECT * FROM cliente c LEFT JOIN usuario u ON c.usu_codigo = u.usu_codigo ORDER BY c.cli_codigo DESC";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				lista.add(mapResultSetToCliente(rs));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static List<Cliente> buscarCliente(String nome, int first, int pageSize) {
		List<Cliente> lista = new ArrayList<>();
		String sql = "SELECT * FROM cliente c LEFT JOIN usuario u ON c.usu_codigo = u.usu_codigo WHERE 1=1";

		if (nome != null && !nome.trim().isEmpty()) {
			sql += " AND LOWER(c.cli_nome) LIKE ?";
		}

		sql += " ORDER BY c.cli_codigo DESC LIMIT ?, ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			int paramIndex = 1;

			if (nome != null && !nome.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nome.toLowerCase() + "%");
			}

			ps.setInt(paramIndex++, first);
			ps.setInt(paramIndex, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				lista.add(mapResultSetToCliente(rs));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return lista;
	}

	public static Cliente buscarClientePorUsuarioId(Long usuarioId) {
		Cliente cliente = null;
		String sql = "SELECT * FROM cliente c LEFT JOIN usuario u ON c.usu_codigo = u.usu_codigo WHERE c.usu_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setLong(1, usuarioId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				cliente = mapResultSetToCliente(rs);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return cliente;
	}

	public static void atualizar(Cliente cliente) throws SQLException {
        if (cliente.getUsuario() != null && cliente.getUsuario().getId() != null && cliente.getUsuario().getId() > 0) {
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(cliente.getUsuario());
        }

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
            throw e;
		}
	}

	public static void deletar(Cliente cliente) throws SQLException {
		String sql = "DELETE FROM cliente WHERE cli_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setLong(1, cliente.getId());
			stmt.executeUpdate();

            if (cliente.getUsuario() != null && cliente.getUsuario().getId() != null && cliente.getUsuario().getId() > 0) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                usuarioDAO.deletar(cliente.getUsuario());
            }

		} catch (SQLException e) {
			e.printStackTrace();
            throw e;
		}
	}

	public static void salvar(Cliente cliente) throws SQLException {
		String sql = "INSERT INTO cliente (cli_nome, cli_email, cli_telefone, cli_cpf, usu_codigo) VALUES (?, ?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			
			String telefoneFormat =  cliente.getTelefone() != null ? cliente.getTelefone().replaceAll("[^0-9]", "") : null;
			String cpfFormat = cliente.getCpf() != null ? cliente.getCpf().replaceAll("[^0-9]", "") : null;
			
			stmt.setString(1, cliente.getNome());
			stmt.setString(2, cliente.getEmail());
			stmt.setString(3, telefoneFormat);
			stmt.setString(4, cpfFormat);
            stmt.setLong(5, cliente.getUsuario().getId());
			stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cliente.setId(generatedKeys.getLong(1));
                }
            }

		} catch (SQLException e) {
			e.printStackTrace();
            throw e;
		}
	}

}
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
		funcionario.setTelefone(rs.getString("fun_telefone"));
		funcionario.setCpf(rs.getString("fun_cpf"));
		funcionario.setEndereco(rs.getString("fun_endereco"));
		
		// Novos campos
		funcionario.setSexo(rs.getString("fun_sexo"));
		funcionario.setDataNascimento(rs.getDate("fun_data_nascimento"));
		funcionario.setDataAdmissao(rs.getDate("fun_data_admissao"));
		funcionario.setObservacoes(rs.getString("fun_observacoes"));
		funcionario.setCep(rs.getString("fun_cep"));
		funcionario.setRua(rs.getString("fun_rua"));
		funcionario.setNumero(rs.getString("fun_numero"));
		funcionario.setComplemento(rs.getString("fun_complemento"));
		funcionario.setBairro(rs.getString("fun_bairro"));
		funcionario.setCidade(rs.getString("fun_cidade"));
		funcionario.setEstado(rs.getString("fun_estado"));

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
				horario.setDomingo(rs.getBoolean("hor_domingo"));
				horario.setSegunda(rs.getBoolean("hor_segunda"));
				horario.setTerca(rs.getBoolean("hor_terca"));
				horario.setQuarta(rs.getBoolean("hor_quarta"));
				horario.setQuinta(rs.getBoolean("hor_quinta"));
				horario.setSexta(rs.getBoolean("hor_sexta"));
				horario.setSabado(rs.getBoolean("hor_sabado"));
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

		String sql = "UPDATE funcionario SET fun_nome = ?, fun_status = ?, fun_telefone = ?, fun_cpf = ?, fun_endereco = ?, " +
					 "fun_sexo = ?, fun_data_nascimento = ?, fun_data_admissao = ?, fun_observacoes = ?, " +
					 "fun_cep = ?, fun_rua = ?, fun_numero = ?, fun_complemento = ?, fun_bairro = ?, fun_cidade = ?, fun_estado = ? " +
					 "WHERE fun_codigo = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			String telefoneFormat = funcionario.getTelefone() != null ? funcionario.getTelefone().replaceAll("[^0-9]", "") : "";
			String cpfFormat = funcionario.getCpf() != null ? funcionario.getCpf().replaceAll("[^0-9]", "") : "";
			String cepFormat = funcionario.getCep() != null ? funcionario.getCep().replaceAll("[^0-9]", "") : "";

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
			stmt.setString(3, telefoneFormat);
			stmt.setString(4, cpfFormat);
			stmt.setString(5, funcionario.getEndereco());
			stmt.setString(6, funcionario.getSexo());
			
			if (funcionario.getDataNascimento() != null) {
				stmt.setDate(7, new java.sql.Date(funcionario.getDataNascimento().getTime()));
			} else {
				stmt.setNull(7, java.sql.Types.DATE);
			}
			
			if (funcionario.getDataAdmissao() != null) {
				stmt.setDate(8, new java.sql.Date(funcionario.getDataAdmissao().getTime()));
			} else {
				stmt.setNull(8, java.sql.Types.DATE);
			}
			
			stmt.setString(9, funcionario.getObservacoes());
			stmt.setString(10, cepFormat);
			stmt.setString(11, funcionario.getRua());
			stmt.setString(12, funcionario.getNumero());
			stmt.setString(13, funcionario.getComplemento());
			stmt.setString(14, funcionario.getBairro());
			stmt.setString(15, funcionario.getCidade());
			stmt.setString(16, funcionario.getEstado());
			stmt.setLong(17, funcionario.getId());

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
		String sql = "INSERT INTO funcionario (fun_nome, fun_status, usu_codigo, fun_telefone, fun_cpf, fun_endereco, " +
					 "fun_sexo, fun_data_nascimento, fun_data_admissao, fun_observacoes, " +
					 "fun_cep, fun_rua, fun_numero, fun_complemento, fun_bairro, fun_cidade, fun_estado) " +
					 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			String telefoneFormat = funcionario.getTelefone() != null ? funcionario.getTelefone().replaceAll("[^0-9]", "") : "";
			String cpfFormat = funcionario.getCpf() != null ? funcionario.getCpf().replaceAll("[^0-9]", "") : "";
			String cepFormat = funcionario.getCep() != null ? funcionario.getCep().replaceAll("[^0-9]", "") : "";

			stmt.setString(1, funcionario.getNome());
			stmt.setString(2, funcionario.getStatus());
            stmt.setLong(3, funcionario.getUsuario().getId());
			stmt.setString(4, telefoneFormat);
			stmt.setString(5, cpfFormat);
			stmt.setString(6, funcionario.getEndereco());
			stmt.setString(7, funcionario.getSexo());
			
			if (funcionario.getDataNascimento() != null) {
				stmt.setDate(8, new java.sql.Date(funcionario.getDataNascimento().getTime()));
			} else {
				stmt.setNull(8, java.sql.Types.DATE);
			}
			
			if (funcionario.getDataAdmissao() != null) {
				stmt.setDate(9, new java.sql.Date(funcionario.getDataAdmissao().getTime()));
			} else {
				stmt.setNull(9, java.sql.Types.DATE);
			}
			
			stmt.setString(10, funcionario.getObservacoes());
			stmt.setString(11, cepFormat);
			stmt.setString(12, funcionario.getRua());
			stmt.setString(13, funcionario.getNumero());
			stmt.setString(14, funcionario.getComplemento());
			stmt.setString(15, funcionario.getBairro());
			stmt.setString(16, funcionario.getCidade());
			stmt.setString(17, funcionario.getEstado());
			
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
	
	public static Funcionario buscarPorCPFRecuperacao(String cpf) {
		String cpfLimpo = cpf.replaceAll("[^0-9]", "");
		String sql = "SELECT * FROM funcionario f LEFT JOIN usuario u ON f.usu_codigo = u.usu_codigo WHERE f.fun_cpf = ? AND f.fun_status = 'A'";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			
			ps.setString(1, cpfLimpo);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return mapResultSetToFuncionario(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Funcionario buscarPorEmailRecuperacao(String email) {
		String sql = "SELECT f.*, u.* FROM funcionario f " +
					 "LEFT JOIN usuario u ON f.usu_codigo = u.usu_codigo " +
					 "WHERE LOWER(u.usu_login) = LOWER(?) AND f.fun_status = 'A'";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			
			System.out.println("Buscando funcionário por login: " + email);
			ps.setString(1, email.trim());
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				Funcionario func = mapResultSetToFuncionario(rs);
				if (func.getUsuario() != null) {
					func.getUsuario().setUser(rs.getString("usu_user"));
				}
				System.out.println("Funcionário encontrado: " + func.getNome());
				return func;
			} else {
				System.out.println("Nenhum funcionário encontrado com login: " + email);
			}
		} catch (Exception e) {
			System.err.println("Erro ao buscar funcionário por login: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public static Funcionario buscarPorTelefoneRecuperacao(String telefone) {
		String telefoneLimpo = telefone.replaceAll("[^0-9]", "");
		String sql = "SELECT * FROM funcionario f LEFT JOIN usuario u ON f.usu_codigo = u.usu_codigo WHERE f.fun_telefone = ? AND f.fun_status = 'A'";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			
			ps.setString(1, telefoneLimpo);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return mapResultSetToFuncionario(rs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean atualizarSenha(Long funcionarioId, String novaSenha) {
		return atualizarSenhaFuncionario(funcionarioId, novaSenha);
	}
	
	public static boolean atualizarSenhaFuncionario(Long funcionarioId, String novaSenha) {
		String sql = "UPDATE usuario u INNER JOIN funcionario f ON u.usu_codigo = f.usu_codigo " +
					 "SET u.usu_senha = ? WHERE f.fun_codigo = ?";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement ps = conn.prepareStatement(sql)) {
			
			ps.setString(1, novaSenha);
			ps.setLong(2, funcionarioId);
			
			int rows = ps.executeUpdate();
			return rows > 0;
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
package com.barbersys.dao;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.barbersys.model.Agendamento;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Pagamento;
import com.barbersys.model.Servicos;
import com.barbersys.util.DatabaseConnection;
import java.time.LocalTime;

public class AgendamentoDAO {

	public static int agendamentoCount(String nomeCliente, String nomeFuncionario, String status, Date dataFiltro) {
		cancelarAgendamentosAtrasados();
		int total = 0;
		StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT a.age_codigo) " + "FROM agendamento a "
				+ "JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "JOIN servicos s ON s.ser_codigo = ags.ser_codigo "
				+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo "
				+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " + "WHERE 1=1 ");

		if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
			sql.append(
					" AND ( (a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?) )");
		}
		if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
			sql.append(" AND LOWER(f.fun_nome) LIKE ?");
		}
		if (status != null && !status.trim().isEmpty()) {
			sql.append(" AND a.age_status = ?");
		}
		if (dataFiltro != null) {
			sql.append(" AND a.age_data = ?");
		}

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;
			if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
				String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
				ps.setString(paramIndex++, nomeParam);
				ps.setString(paramIndex++, nomeParam);
			}
			if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nomeFuncionario.toLowerCase() + "%");
			}
			if (status != null && !status.trim().isEmpty()) {
				ps.setString(paramIndex++, status);
			}
			if (dataFiltro != null) {
				ps.setDate(paramIndex++, new java.sql.Date(dataFiltro.getTime()));
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

	public static List<Agendamento> buscarAgendamentos(String nomeCliente, String nomeFuncionario, String status,
			Date dataFiltro, int first, int pageSize) {
		cancelarAgendamentosAtrasados();
		Map<Long, Agendamento> mapaAgendamentos = new LinkedHashMap<>();

		StringBuilder sql = new StringBuilder("SELECT " + "a.age_codigo AS agendamento_id, "
				+ "a.age_status AS agendamento_status, " + "a.age_data AS agendamento_data, "
				+ "a.age_hora AS agendamento_hora, " + "a.age_tipo_cadastro AS agendamento_tipo, "
				+ "a.age_pago AS agendamento_pago, " + "a.age_observacoes AS agendamento_observacoes, "
				+ "a.age_sexo AS agendamento_sexo, " // Adicionado
				+ "s.ser_codigo AS servico_id, " + "s.ser_nome AS servico_nome, "
				+ "s.ser_preco AS servico_preco, " + "s.ser_minutos AS servico_minutos, " 
				+ "c.cli_codigo AS cliente_id, " + "c.cli_nome AS cliente_nome, "
				+ "a.age_nome_cliente AS nome_cliente_avulso, " + "f.fun_codigo AS funcionario_id, "
				+ "f.fun_nome AS funcionario_nome, " + "p.pag_codigo AS pagamento_id, "
				+ "p.pag_nome AS pagamento_nome, " + "p.pag_integra_caixa AS pagamento_integra_caixa "
				+ "FROM agendamento a " + "JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "JOIN servicos s ON s.ser_codigo = ags.ser_codigo "
				+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo "
				+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo "
				+ "LEFT JOIN pagamento p ON a.pag_codigo = p.pag_codigo " + "WHERE 1=1 ");

		if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
			sql.append(
					" AND ( (a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?) )");
		}
		if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
			sql.append(" AND LOWER(f.fun_nome) LIKE ?");
		}
		if (status != null && !status.trim().isEmpty()) {
			sql.append(" AND a.age_status = ?");
		}
		if (dataFiltro != null) {
			sql.append(" AND a.age_data = ?");
		}
		sql.append(" ORDER BY a.age_codigo DESC LIMIT ?, ?");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;
			if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
				String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
				ps.setString(paramIndex++, nomeParam);
				ps.setString(paramIndex++, nomeParam);
			}
			if (nomeFuncionario != null && !nomeFuncionario.trim().isEmpty()) {
				ps.setString(paramIndex++, "%" + nomeFuncionario.toLowerCase() + "%");
			}
			if (status != null && !status.trim().isEmpty()) {
				ps.setString(paramIndex++, status);
			}
			if (dataFiltro != null) {
				ps.setDate(paramIndex++, new java.sql.Date(dataFiltro.getTime()));
			}

			ps.setInt(paramIndex++, first);
			ps.setInt(paramIndex, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Long agendamentoId = rs.getLong("agendamento_id");
				Agendamento agendamento = mapaAgendamentos.get(agendamentoId);

				if (agendamento == null) {
					agendamento = new Agendamento();
					agendamento.setId(agendamentoId);
					agendamento.setStatus(rs.getString("agendamento_status"));
					agendamento.setDataCriado(rs.getDate("agendamento_data"));
					agendamento.setHoraSelecionada(rs.getTime("agendamento_hora").toLocalTime());
					agendamento.setTipoCadastro(rs.getString("agendamento_tipo"));
					agendamento.setPago(rs.getString("agendamento_pago"));
					agendamento.setObservacoes(rs.getString("agendamento_observacoes"));
					agendamento.setSexo(rs.getString("agendamento_sexo")); // Adicionado

					Long clienteId = rs.getLong("cliente_id");
					String clienteNome = rs.getString("cliente_nome");
					String nomeAvulso = rs.getString("nome_cliente_avulso");

					if (clienteId != 0 && clienteNome != null) {
						Cliente cliente = new Cliente();
						cliente.setId(clienteId);
						cliente.setNome(clienteNome);
						agendamento.setCliente(cliente);
					} else if (nomeAvulso != null && !nomeAvulso.trim().isEmpty()) {
						agendamento.setNomeClienteAvulso(nomeAvulso);
					}

					Funcionario funcionario = new Funcionario();
					funcionario.setId(rs.getLong("funcionario_id"));
					funcionario.setNome(rs.getString("funcionario_nome"));
					agendamento.setFuncionario(funcionario);

					if (rs.getObject("pagamento_id") != null) {
						Pagamento pagamento = new Pagamento();
						pagamento.setId(rs.getLong("pagamento_id"));
						pagamento.setNome(rs.getString("pagamento_nome"));
						pagamento.setIntegraCaixa(rs.getBoolean("pagamento_integra_caixa"));
						agendamento.setPagamento(pagamento);
					}

					agendamento.setServicos(new ArrayList<>());
					mapaAgendamentos.put(agendamentoId, agendamento);
				}

				Servicos servico = new Servicos();
				servico.setId(rs.getLong("servico_id"));
				servico.setNome(rs.getString("servico_nome"));
				servico.setPreco(rs.getDouble("servico_preco"));
				servico.setMinutos(rs.getInt("servico_minutos"));
				agendamento.getServicos().add(servico);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<>(mapaAgendamentos.values());
	}

	public static void atualizar(Agendamento agendamento, List<Long> servicos) {
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. Calcular duração total dos novos serviços
			int totalMinutos = 0;
			List<Servicos> servicosCompletos = new ArrayList<>();
			for (Long id : servicos) {
				Servicos servico = ServicosDAO.buscarPorId(id);
				if (servico != null) {
					totalMinutos += servico.getMinutos();
					servicosCompletos.add(servico);
				}
			}
			if (totalMinutos == 0) totalMinutos = 30;
			
			System.out.println("✅ Atualizando agendamento ID " + agendamento.getId() + " com duração: " + totalMinutos + " minutos");

			// 2. Deletar serviços antigos
			String deleteServicosSql = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
			try (PreparedStatement deleteStmt = conn.prepareStatement(deleteServicosSql)) {
				deleteStmt.setLong(1, agendamento.getId());
				deleteStmt.executeUpdate();
			}

			// 3. Atualizar o agendamento (UPDATE simples, não DELETE + INSERT)
			String updateSql = "UPDATE agendamento SET " +
							   "age_status = ?, age_data = ?, age_hora = ?, age_duracao_minutos = ?, " +
							   "fun_codigo = ?, cli_codigo = ?, age_nome_cliente = ?, " +
							   "age_tipo_cadastro = ?, age_pago = ?, pag_codigo = ?, " +
							   "age_observacoes = ?, age_sexo = ? " +
							   "WHERE age_codigo = ?";
			
			try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
				updateStmt.setString(1, agendamento.getStatus());
				updateStmt.setDate(2, new java.sql.Date(agendamento.getDataCriado().getTime()));
				updateStmt.setTime(3, java.sql.Time.valueOf(agendamento.getHoraSelecionada()));
				updateStmt.setInt(4, totalMinutos);
				updateStmt.setLong(5, agendamento.getFuncionario().getId());

				if (agendamento.getCliente() != null) {
					updateStmt.setLong(6, agendamento.getCliente().getId());
					updateStmt.setNull(7, java.sql.Types.VARCHAR);
				} else {
					updateStmt.setNull(6, java.sql.Types.BIGINT);
					updateStmt.setString(7, agendamento.getNomeClienteAvulso());
				}
				
				updateStmt.setString(8, agendamento.getTipoCadastro());
				updateStmt.setString(9, agendamento.getPago());
				
				if (agendamento.getPagamento() != null) {
					updateStmt.setLong(10, agendamento.getPagamento().getId());
				} else {
					updateStmt.setNull(10, java.sql.Types.BIGINT);
				}
				
				// Observações
				if (agendamento.getObservacoes() != null && !agendamento.getObservacoes().trim().isEmpty()) {
					updateStmt.setString(11, agendamento.getObservacoes());
				} else {
					updateStmt.setNull(11, java.sql.Types.VARCHAR);
				}
				
				// Sexo
				if ("I".equals(agendamento.getTipoCadastro())) {
					updateStmt.setString(12, agendamento.getSexo());
				} else {
					updateStmt.setNull(12, java.sql.Types.VARCHAR);
				}
				
				updateStmt.setLong(13, agendamento.getId());
				updateStmt.executeUpdate();
			}

			// 4. Inserir os NOVOS serviços
			String insertServicosSql = "INSERT INTO agendamento_servico (age_codigo, ser_codigo) VALUES (?, ?)";
			try (PreparedStatement insertStmt = conn.prepareStatement(insertServicosSql)) {
				for (Servicos servico : servicosCompletos) {
					insertStmt.setLong(1, agendamento.getId());
					insertStmt.setLong(2, servico.getId());
					insertStmt.addBatch();
				}
				insertStmt.executeBatch();
			}

			conn.commit();
			System.out.println("✅ Agendamento atualizado com sucesso!");

		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
			throw new RuntimeException("Erro ao atualizar agendamento: " + e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void atualizarServicosDoAgendamento(Connection conn, Agendamento agendamento, List<Long> servicos)
			throws SQLException {
		String deleteSql = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
		try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
			deleteStmt.setLong(1, agendamento.getId());
			deleteStmt.executeUpdate();
		}

		String insertSql = "INSERT INTO agendamento_servico (age_codigo, ser_codigo) VALUES (?, ?)";
		try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
			for (Long id : servicos) {
				Servicos servico = ServicosDAO.buscarPorId(id);
				insertStmt.setLong(1, agendamento.getId());
				insertStmt.setLong(2, servico.getId());
				insertStmt.addBatch();
			}
			insertStmt.executeBatch();
		}
	}

	public static void atualizarInformacoesPagamento(Long agendamentoId, String status, Long pagamentoId) {
		String sql = "UPDATE agendamento SET age_pago = ?, pag_codigo = ? WHERE age_codigo = ?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, status);
			if (pagamentoId != null) {
				stmt.setLong(2, pagamentoId);
			} else {
				stmt.setNull(2, java.sql.Types.BIGINT);
			}
			stmt.setLong(3, agendamentoId);
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void deletarAgendamento(Long agendamentoId) {
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. Deletar serviços associados
			String sqlDeleteServicos = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sqlDeleteServicos)) {
				stmt.setLong(1, agendamentoId);
				stmt.executeUpdate();
			}

			// 2. Deletar o agendamento
			String sqlDeleteAgendamento = "DELETE FROM agendamento WHERE age_codigo = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sqlDeleteAgendamento)) {
				stmt.setLong(1, agendamentoId);
				stmt.executeUpdate();
			}

			conn.commit();
			System.out.println("✅ Agendamento ID " + agendamentoId + " deletado com sucesso!");
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void cancelarAgendamento(Long agendamentoId) {
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// Simplesmente marca como cancelado ('I')
			String sql = "UPDATE agendamento SET age_status = 'I' WHERE age_codigo = ?";
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, agendamentoId);
				stmt.executeUpdate();
			}

			conn.commit();
			System.out.println("✅ Agendamento ID " + agendamentoId + " cancelado com sucesso!");
		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Agendamento buscarAgendamentoPorId(Connection conn, Long agendamentoId) throws SQLException {
		String sql = "SELECT * FROM agendamento a "
				+ "LEFT JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "LEFT JOIN servicos s ON ags.ser_codigo = s.ser_codigo "
				+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo "
				+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " + "WHERE a.age_codigo = ?";

		Agendamento agendamento = null;
		Map<Long, Servicos> servicosMap = new HashMap<>();

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, agendamentoId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (agendamento == null) {
					agendamento = new Agendamento();
					agendamento.setId(rs.getLong("age_codigo"));
					agendamento.setStatus(rs.getString("age_status"));
					agendamento.setDataCriado(rs.getDate("age_data"));
					agendamento.setHoraSelecionada(rs.getTime("age_hora").toLocalTime());
									agendamento.setTipoCadastro(rs.getString("age_tipo_cadastro"));
									agendamento.setPago(rs.getString("age_pago"));
									agendamento.setObservacoes(rs.getString("age_observacoes"));
									agendamento.setSexo(rs.getString("age_sexo")); // Adicionado
					Funcionario f = new Funcionario();
					f.setId(rs.getLong("fun_codigo"));
					f.setNome(rs.getString("fun_nome"));
					agendamento.setFuncionario(f);

					if (rs.getObject("cli_codigo") != null) {
						Cliente c = new Cliente();
						c.setId(rs.getLong("cli_codigo"));
						c.setNome(rs.getString("cli_nome"));
						c.setTelefone(rs.getString("cli_telefone"));
						c.setSexo(rs.getString("cli_sexo"));
						c.setObservacoes(rs.getString("cli_observacoes"));
						agendamento.setCliente(c);
					} else {
						agendamento.setNomeClienteAvulso(rs.getString("age_nome_cliente"));
					}
				}
				if (rs.getObject("ser_codigo") != null) {
					Long serId = rs.getLong("ser_codigo");
					if (!servicosMap.containsKey(serId)) {
						Servicos servico = new Servicos();
						servico.setId(serId);
						servico.setNome(rs.getString("ser_nome"));
						servico.setPreco(rs.getDouble("ser_preco"));
						servico.setMinutos(rs.getInt("ser_minutos"));
						servicosMap.put(serId, servico);
					}
				}
			}
			if (agendamento != null) {
				agendamento.setServicos(new ArrayList<>(servicosMap.values()));
			}
		}
		return agendamento;
	}

	public static void salvar(Agendamento agendamento, List<Long> servicos) {
		String sql = "INSERT INTO agendamento (age_status, age_data, age_hora, age_duracao_minutos, fun_codigo, cli_codigo, age_nome_cliente, age_tipo_cadastro, age_pago, age_observacoes, age_sexo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		Connection conn = null;
		try {
			conn = DatabaseConnection.getConnection();
			conn.setAutoCommit(false);

			// 1. Calcular a duração total em minutos
			int totalMinutos = 0;
			List<Servicos> servicosCompletos = new ArrayList<>();
			for (Long id : servicos) {
				Servicos servico = ServicosDAO.buscarPorId(id);
				if (servico != null) {
					totalMinutos += servico.getMinutos();
					servicosCompletos.add(servico);
				}
			}
			if (totalMinutos == 0) totalMinutos = 30;
			
			System.out.println("✅ Salvando agendamento ÚNICO com duração: " + totalMinutos + " minutos");

			// 2. Salvar UM ÚNICO agendamento com a duração total
			try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setString(1, agendamento.getStatus());
				stmt.setDate(2, new java.sql.Date(agendamento.getDataCriado().getTime()));
				stmt.setTime(3, java.sql.Time.valueOf(agendamento.getHoraSelecionada()));
				stmt.setInt(4, totalMinutos); // Duração total
				stmt.setLong(5, agendamento.getFuncionario().getId());

				if (agendamento.getCliente() != null) {
					stmt.setLong(6, agendamento.getCliente().getId());
					stmt.setNull(7, java.sql.Types.VARCHAR);
				} else {
					stmt.setNull(6, java.sql.Types.BIGINT);
					stmt.setString(7, agendamento.getNomeClienteAvulso());
				}
				stmt.setString(8, agendamento.getTipoCadastro());
				stmt.setString(9, "N"); // Default 'Não pago'
				
				// Observação
				String obs = agendamento.getObservacoes();
				if (obs != null && !obs.trim().isEmpty()) {
					stmt.setString(10, obs);
				} else {
					stmt.setNull(10, java.sql.Types.VARCHAR);
				}
				
				// Sexo (apenas se for cliente avulso)
				if ("I".equals(agendamento.getTipoCadastro())) {
					stmt.setString(11, agendamento.getSexo());
				} else {
					stmt.setNull(11, java.sql.Types.VARCHAR);
				}
				
				stmt.executeUpdate();

				// Pegar o ID gerado
				try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						Long agendamentoId = generatedKeys.getLong(1);
						agendamento.setId(agendamentoId);
						System.out.println("✅ Agendamento salvo com ID: " + agendamentoId);
						
						// 3. Associar os serviços
						String sqlInsertServico = "INSERT INTO agendamento_servico (age_codigo, ser_codigo) VALUES (?, ?)";
						try (PreparedStatement psServico = conn.prepareStatement(sqlInsertServico)) {
							for (Servicos servico : servicosCompletos) {
								psServico.setLong(1, agendamentoId);
								psServico.setLong(2, servico.getId());
								psServico.addBatch();
							}
							psServico.executeBatch();
							System.out.println("✅ " + servicosCompletos.size() + " serviço(s) associado(s)");
						}
					}
				}
			}

			conn.commit();
			System.out.println("✅ Agendamento salvo com sucesso!");

		} catch (SQLException e) {
			if (conn != null) {
				try {
					conn.rollback();
				} catch (SQLException ex) {
					ex.printStackTrace();
				}
			}
			e.printStackTrace();
			throw new RuntimeException("Erro ao salvar agendamento: " + e.getMessage());
		} finally {
			if (conn != null) {
				try {
					conn.setAutoCommit(true);
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static Agendamento getAgendamentoComServicos(Connection conn, Long agendamentoId) throws SQLException {
		String sql = "SELECT s.ser_minutos, a.age_hora FROM agendamento a "
				+ "JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "JOIN servicos s ON ags.ser_codigo = s.ser_codigo " + "WHERE a.age_codigo = ?";

		Agendamento agendamento = new Agendamento();
		agendamento.setHoraSelecionada(null);
		agendamento.setServicos(new ArrayList<>());

		try (PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setLong(1, agendamentoId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (agendamento.getHoraSelecionada() == null) {
					agendamento.setHoraSelecionada(rs.getTime("age_hora").toLocalTime());
				}
				Servicos servico = new Servicos();
				servico.setMinutos(rs.getInt("ser_minutos"));
				agendamento.getServicos().add(servico);
			}
		}
		return agendamento;
	}

	public static List<Agendamento> buscarAgendamentosPorClienteId(Long clienteId, String status, int first,
			int pageSize) {
		Map<Long, Agendamento> mapaAgendamentos = new LinkedHashMap<>();

		StringBuilder sql = new StringBuilder("SELECT " + "a.age_codigo AS agendamento_id, "
				+ "a.age_status AS agendamento_status, " + "a.age_data AS agendamento_data, "
				+ "a.age_hora AS agendamento_hora, " + "a.age_tipo_cadastro AS agendamento_tipo, "
				+ "a.age_nome_cliente AS nome_cliente_avulso, " + "a.age_observacoes AS agendamento_observacoes, "
				+ "s.ser_codigo AS servico_id, " + "s.ser_nome AS servico_nome, " 
				+ "s.ser_preco AS servico_preco, " + "s.ser_minutos AS servico_minutos, "
				+ "f.fun_codigo AS funcionario_id, " + "f.fun_nome AS funcionario_nome, " 
				+ "c.cli_codigo AS cliente_id, " + "c.cli_nome AS cliente_nome, "
				+ "c.cli_observacoes AS cliente_observacoes "
				+ "FROM agendamento a " + "JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "JOIN servicos s ON s.ser_codigo = ags.ser_codigo "
				+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo "
				+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " + "WHERE a.cli_codigo = ? AND a.age_status = ? "
				+ "ORDER BY a.age_data DESC, a.age_hora DESC LIMIT ?, ?");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement ps = conn.prepareStatement(sql.toString())) {

			ps.setLong(1, clienteId);
			ps.setString(2, status);
			ps.setInt(3, first);
			ps.setInt(4, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				Long agendamentoId = rs.getLong("agendamento_id");
				Agendamento agendamento = mapaAgendamentos.get(agendamentoId);

				if (agendamento == null) {
					agendamento = new Agendamento();
					agendamento.setId(agendamentoId);
					agendamento.setStatus(rs.getString("agendamento_status"));
					agendamento.setDataCriado(rs.getDate("agendamento_data"));
					agendamento.setHoraSelecionada(rs.getTime("agendamento_hora").toLocalTime());
					agendamento.setTipoCadastro(rs.getString("agendamento_tipo"));
					agendamento.setNomeClienteAvulso(rs.getString("nome_cliente_avulso"));
					agendamento.setObservacoes(rs.getString("agendamento_observacoes"));

					Funcionario funcionario = new Funcionario();
					funcionario.setId(rs.getLong("funcionario_id"));
					funcionario.setNome(rs.getString("funcionario_nome"));
					agendamento.setFuncionario(funcionario);

					if (rs.getObject("cliente_id") != null) {
						Cliente cliente = new Cliente();
						cliente.setId(rs.getLong("cliente_id"));
						cliente.setNome(rs.getString("cliente_nome"));
						cliente.setObservacoes(rs.getString("cliente_observacoes"));
						agendamento.setCliente(cliente);
					}

					agendamento.setServicos(new ArrayList<>());
					mapaAgendamentos.put(agendamentoId, agendamento);
				}

				Servicos servico = new Servicos();
				servico.setId(rs.getLong("servico_id"));
				servico.setNome(rs.getString("servico_nome"));
				servico.setPreco(rs.getDouble("servico_preco"));
				servico.setMinutos(rs.getInt("servico_minutos"));
				agendamento.getServicos().add(servico);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<>(mapaAgendamentos.values());
	}

	// MÉTODO ATUALIZADO para ignorar um agendamento específico (útil na edição)
	public static List<LocalTime> getHorariosOcupados(Long funcionarioId, java.util.Date data,
			Long agendamentoIdParaExcluir) {
		List<LocalTime> horariosOcupados = new ArrayList<>();
		
		// Busca TODOS os agendamentos ativos do funcionário na data (EXCETO o que está sendo editado)
		String sql = "SELECT age_hora, age_duracao_minutos " +
					 "FROM agendamento " +
					 "WHERE fun_codigo = ? AND age_data = ? AND age_status = 'A' " +
					 (agendamentoIdParaExcluir != null ? "AND age_codigo != ? " : "") +
					 "ORDER BY age_hora";

		try (Connection conn = DatabaseConnection.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(sql)) {
				ps.setLong(1, funcionarioId);
				ps.setDate(2, new java.sql.Date(data.getTime()));
				if (agendamentoIdParaExcluir != null) {
					ps.setLong(3, agendamentoIdParaExcluir);
				}
				ResultSet rs = ps.executeQuery();
				
				while (rs.next()) {
					LocalTime horaInicio = rs.getTime("age_hora").toLocalTime();
					int duracaoMinutos = rs.getInt("age_duracao_minutos");
					
					// Marca TODOS os slots de 30min que esse agendamento ocupa
					int numeroSlots = (duracaoMinutos + 29) / 30; // Arredonda para cima
					for (int i = 0; i < numeroSlots; i++) {
						LocalTime slotOcupado = horaInicio.plusMinutes((long) i * 30);
						if (!horariosOcupados.contains(slotOcupado)) {
							horariosOcupados.add(slotOcupado);
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return horariosOcupados;
	}

	public static void cancelarAgendamentosAtrasados() {
		String sql = "UPDATE agendamento SET age_status = 'I' WHERE age_status = 'A' AND age_data < CURDATE()";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace(); // Idealmente, logar isso em um sistema de logs
		}
	}

	public static List<Agendamento> buscarAgendamentosRelatorioAnalitico(java.util.Date dataInicial,
			java.util.Date dataFinal, String nomeCliente, Long funcionarioId, String status, int first, int pageSize) {

		List<Agendamento> resultado = new ArrayList<>();

		StringBuilder sql = new StringBuilder(
				"SELECT DISTINCT a.age_codigo, a.age_data, a.age_hora, a.age_status, a.age_nome_cliente, "
						+ "c.cli_codigo, c.cli_nome, " + "f.fun_codigo, f.fun_nome " + "FROM agendamento a "
						+ "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
						+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo "
						+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " + "WHERE 1=1 ");

		if (dataInicial != null) {
			sql.append("AND DATE(a.age_data) >= ? ");
		}
		if (dataFinal != null) {
			sql.append("AND DATE(a.age_data) <= ? ");
		}
		if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
			sql.append("AND ((a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?)) ");
		}
		if (funcionarioId != null) {
			sql.append("AND a.fun_codigo = ? ");
		}
		if (status != null && !status.trim().isEmpty()) {
			sql.append("AND a.age_status = ? ");
		}

		sql.append("ORDER BY a.age_data DESC, a.age_hora DESC ");
		sql.append("LIMIT ? OFFSET ?");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;
			if (dataInicial != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
			}
			if (dataFinal != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
			}
			if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
				String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
				stmt.setString(paramIndex++, nomeParam);
				stmt.setString(paramIndex++, nomeParam);
			}
			if (funcionarioId != null) {
				stmt.setLong(paramIndex++, funcionarioId);
			}
			if (status != null && !status.trim().isEmpty()) {
				stmt.setString(paramIndex++, status);
			}
			stmt.setInt(paramIndex++, pageSize);
			stmt.setInt(paramIndex++, first);

			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				Agendamento agendamento = new Agendamento();
				agendamento.setId(rs.getLong("age_codigo"));
				agendamento.setDataCriado(rs.getDate("age_data"));

				// Converte String ou Time para LocalTime
				String horaStr = rs.getString("age_hora");
				if (horaStr != null && !horaStr.trim().isEmpty()) {
					try {
						agendamento.setHoraSelecionada(LocalTime.parse(horaStr));
					} catch (Exception e) {
						// Se falhar parse, tenta pegar como Time
						Time time = rs.getTime("age_hora");
						if (time != null) {
							agendamento.setHoraSelecionada(time.toLocalTime());
						}
					}
				}

				agendamento.setStatus(rs.getString("age_status"));
				agendamento.setNomeClienteAvulso(rs.getString("age_nome_cliente"));

				Cliente cliente = new Cliente();
				cliente.setId(rs.getLong("cli_codigo"));
				cliente.setNome(rs.getString("cli_nome"));
				agendamento.setCliente(cliente);

				Funcionario funcionario = new Funcionario();
				funcionario.setId(rs.getLong("fun_codigo"));
				funcionario.setNome(rs.getString("fun_nome"));
				agendamento.setFuncionario(funcionario);

				resultado.add(agendamento);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return resultado;
	}

	/**
	 * Conta agendamentos para o relatório analítico
	 */
	public static int contarAgendamentosRelatorioAnalitico(java.util.Date dataInicial, java.util.Date dataFinal,
			String nomeCliente, Long funcionarioId, String status) {

		StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT a.age_codigo) as total " + "FROM agendamento a "
				+ "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
				+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo " + "WHERE 1=1 ");

		if (dataInicial != null) {
			sql.append("AND DATE(a.age_data) >= ? ");
		}
		if (dataFinal != null) {
			sql.append("AND DATE(a.age_data) <= ? ");
		}
		if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
			sql.append("AND ((a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?)) ");
		}
		if (funcionarioId != null) {
			sql.append("AND a.fun_codigo = ? ");
		}
		if (status != null && !status.trim().isEmpty()) {
			sql.append("AND a.age_status = ? ");
		}

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;
			if (dataInicial != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
			}
			if (dataFinal != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
			}
			if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
				String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
				stmt.setString(paramIndex++, nomeParam);
				stmt.setString(paramIndex++, nomeParam);
			}
			if (funcionarioId != null) {
				stmt.setLong(paramIndex++, funcionarioId);
			}
			if (status != null && !status.trim().isEmpty()) {
				stmt.setString(paramIndex++, status);
			}

			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("total");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * Busca todos os agendamentos para o relatório analítico (para PDF)
	 */
	public static List<Agendamento> buscarTodosAgendamentosRelatorioAnalitico(java.util.Date dataInicial,
			java.util.Date dataFinal, String nomeCliente, Long funcionarioId, String status) {

		List<Agendamento> resultado = new ArrayList<>();

		StringBuilder sql = new StringBuilder(
				"SELECT DISTINCT a.age_codigo, a.age_data, a.age_hora, a.age_status, a.age_nome_cliente, "
						+ "c.cli_codigo, c.cli_nome, " + "f.fun_codigo, f.fun_nome " + "FROM agendamento a "
						+ "INNER JOIN agendamento_servico ags ON a.age_codigo = ags.age_codigo "
						+ "LEFT JOIN cliente c ON a.cli_codigo = c.cli_codigo "
						+ "JOIN funcionario f ON a.fun_codigo = f.fun_codigo " + "WHERE 1=1 ");

		if (dataInicial != null) {
			sql.append("AND DATE(a.age_data) >= ? ");
		}
		if (dataFinal != null) {
			sql.append("AND DATE(a.age_data) <= ? ");
		}
		if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
			sql.append("AND ((a.cli_codigo IS NOT NULL AND LOWER(c.cli_nome) LIKE ?) OR (a.cli_codigo IS NULL AND LOWER(a.age_nome_cliente) LIKE ?)) ");
		}
		if (funcionarioId != null) {
			sql.append("AND a.fun_codigo = ? ");
		}
		if (status != null && !status.trim().isEmpty()) {
			sql.append("AND a.age_status = ? ");
		}

		sql.append("ORDER BY a.age_data DESC, a.age_hora DESC");

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

			int paramIndex = 1;
			if (dataInicial != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataInicial.getTime()));
			}
			if (dataFinal != null) {
				stmt.setDate(paramIndex++, new java.sql.Date(dataFinal.getTime()));
			}
			if (nomeCliente != null && !nomeCliente.trim().isEmpty()) {
				String nomeParam = "%" + nomeCliente.toLowerCase() + "%";
				stmt.setString(paramIndex++, nomeParam);
				stmt.setString(paramIndex++, nomeParam);
			}
			if (funcionarioId != null) {
				stmt.setLong(paramIndex++, funcionarioId);
			}
			if (status != null && !status.trim().isEmpty()) {
				stmt.setString(paramIndex++, status);
			}

			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				Agendamento agendamento = new Agendamento();
				agendamento.setId(rs.getLong("age_codigo"));
				agendamento.setDataCriado(rs.getDate("age_data"));

				// Converte String ou Time para LocalTime
				String horaStr = rs.getString("age_hora");
				if (horaStr != null && !horaStr.trim().isEmpty()) {
					try {
						agendamento.setHoraSelecionada(LocalTime.parse(horaStr));
					} catch (Exception e) {
						// Se falhar parse, tenta pegar como Time
						Time time = rs.getTime("age_hora");
						if (time != null) {
							agendamento.setHoraSelecionada(time.toLocalTime());
						}
					}
				}

				agendamento.setStatus(rs.getString("age_status"));
				agendamento.setNomeClienteAvulso(rs.getString("age_nome_cliente"));

				Cliente cliente = new Cliente();
				cliente.setId(rs.getLong("cli_codigo"));
				cliente.setNome(rs.getString("cli_nome"));
				agendamento.setCliente(cliente);

				Funcionario funcionario = new Funcionario();
				funcionario.setId(rs.getLong("fun_codigo"));
				funcionario.setNome(rs.getString("fun_nome"));
				agendamento.setFuncionario(funcionario);

				resultado.add(agendamento);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return resultado;
	}

	/**
	 * Conta agendamentos pendentes de um funcionário
	 */
	public static int contarAgendamentosPendentesPorFuncionario(Long funcionarioId) {
		String sql = "SELECT COUNT(*) as total FROM agendamento " +
		             "WHERE fun_codigo = ? AND age_status IN ('A', 'P')";
		
		try (Connection conn = DatabaseConnection.getConnection();
		     PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, funcionarioId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				int total = rs.getInt("total");
				System.out.println("🔍 Funcionário " + funcionarioId + " tem " + total + " agendamento(s) pendente(s)");
				return total;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Conta agendamentos pendentes em um horário específico de um funcionário
	 */
	public static int contarAgendamentosPendentesPorHorario(Long horarioId) {
		// Busca o horário para pegar funcionário e período
		String sqlHorario = "SELECT fun_codigo, hor_hora_inicio, hor_hora_fim FROM horario WHERE hor_codigo = ?";
		
		try (Connection conn = DatabaseConnection.getConnection();
		     PreparedStatement stmtHorario = conn.prepareStatement(sqlHorario)) {
			
			stmtHorario.setLong(1, horarioId);
			ResultSet rsHorario = stmtHorario.executeQuery();
			
			if (!rsHorario.next()) {
				System.err.println("⚠️ Horário ID " + horarioId + " não encontrado no banco!");
				return 0; // Horário não encontrado
			}
			
			Long funCodigo = rsHorario.getLong("fun_codigo");
			java.sql.Time horInicio = rsHorario.getTime("hor_hora_inicio");
			java.sql.Time horFim = rsHorario.getTime("hor_hora_fim");
			rsHorario.close();
			
			System.out.println("🔍 Verificando agendamentos para:");
			System.out.println("   - Horário ID: " + horarioId);
			System.out.println("   - Funcionário: " + funCodigo);
			System.out.println("   - Período: " + horInicio + " até " + horFim);
			
			// PRIMEIRO: Vamos ver TODOS os agendamentos desse funcionário para debug
			String sqlDebug = "SELECT age_codigo, age_data, age_hora, age_status FROM agendamento WHERE fun_codigo = ?";
			try (PreparedStatement stmtDebug = conn.prepareStatement(sqlDebug)) {
				stmtDebug.setLong(1, funCodigo);
				ResultSet rsDebug = stmtDebug.executeQuery();
				System.out.println("   📋 TODOS os agendamentos do funcionário " + funCodigo + ":");
				while (rsDebug.next()) {
					System.out.println("      - ID: " + rsDebug.getLong("age_codigo") + 
					                 " | Data: " + rsDebug.getDate("age_data") + 
					                 " | Hora: " + rsDebug.getTime("age_hora") + 
					                 " | Status: " + rsDebug.getString("age_status"));
				}
			}
			
			// Conta agendamentos pendentes desse funcionário no período do horário
			// MUDANÇA: Usar <= para incluir agendamentos no horário final
			String sqlCount = "SELECT COUNT(*) as total FROM agendamento " +
			                 "WHERE fun_codigo = ? " +
			                 "AND age_data >= CURDATE() " +
			                 "AND age_hora >= ? " +
			                 "AND age_hora <= ? " + // MUDEI < para <=
			                 "AND age_status = 'A'";
			
			try (PreparedStatement stmtCount = conn.prepareStatement(sqlCount)) {
				stmtCount.setLong(1, funCodigo);
				stmtCount.setTime(2, horInicio);
				stmtCount.setTime(3, horFim);
				
				System.out.println("   📝 Executando query: " + sqlCount);
				System.out.println("   📝 Parâmetros: funCodigo=" + funCodigo + ", horInicio=" + horInicio + ", horFim=" + horFim);
				
				ResultSet rsCount = stmtCount.executeQuery();
				if (rsCount.next()) {
					int total = rsCount.getInt("total");
					System.out.println("   ✅ Total de agendamentos pendentes encontrados: " + total);
					return total;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("❌ Erro ao contar agendamentos por horário: " + e.getMessage());
		}
		return 0;
	}

	/**
	 * Cancela todos os agendamentos pendentes de um funcionário
	 */
	public static int cancelarAgendamentosPendentesPorFuncionario(Long funcionarioId) {
		String sql = "UPDATE agendamento SET age_status = 'I' " +
		             "WHERE fun_codigo = ? AND age_status IN ('A', 'P')";
		
		try (Connection conn = DatabaseConnection.getConnection();
		     PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, funcionarioId);
			int qtd = stmt.executeUpdate();
			System.out.println("🔴 Cancelados " + qtd + " agendamentos do funcionário ID: " + funcionarioId);
			return qtd;
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Erro ao cancelar agendamentos: " + e.getMessage());
		}
		return 0;
	}

	/**
	 * Cancela agendamentos pendentes em um horário específico
	 */
	public static int cancelarAgendamentosPendentesPorHorario(Long horarioId) {
		// Busca o horário para pegar funcionário e período
		String sqlHorario = "SELECT fun_codigo, hor_hora_inicio, hor_hora_fim FROM horario WHERE hor_codigo = ?";
		
		try (Connection conn = DatabaseConnection.getConnection();
		     PreparedStatement stmtHorario = conn.prepareStatement(sqlHorario)) {
			
			stmtHorario.setLong(1, horarioId);
			ResultSet rsHorario = stmtHorario.executeQuery();
			
			if (!rsHorario.next()) {
				return 0; // Horário não encontrado
			}
			
			Long funCodigo = rsHorario.getLong("fun_codigo");
			java.sql.Time horInicio = rsHorario.getTime("hor_hora_inicio");
			java.sql.Time horFim = rsHorario.getTime("hor_hora_fim");
			rsHorario.close();
			
			// Cancela agendamentos pendentes desse funcionário no período do horário
			String sqlUpdate = "UPDATE agendamento SET age_status = 'I' " +
			                  "WHERE fun_codigo = ? " +
			                  "AND age_hora >= ? " +
			                  "AND age_hora < ? " +
			                  "AND age_status IN ('A', 'P')";
			
			try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
				stmtUpdate.setLong(1, funCodigo);
				stmtUpdate.setTime(2, horInicio);
				stmtUpdate.setTime(3, horFim);
				
				int count = stmtUpdate.executeUpdate();
				System.out.println("🔴 Cancelados " + count + " agendamentos do horário ID: " + horarioId);
				return count;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Erro ao cancelar agendamentos: " + e.getMessage());
		}
		return 0;
	}
	
	/**
	 * DELETA todos os agendamentos de um funcionário (TODOS os status)
	 * Usado ao deletar o funcionário para evitar erro de foreign key
	 */
	public static int deletarTodosPorFuncionario(Long funcionarioId) {
		try (Connection conn = DatabaseConnection.getConnection()) {
			
			// PASSO 1: Buscar todos os agendamentos do funcionário
			String sqlBuscar = "SELECT age_codigo FROM agendamento WHERE fun_codigo = ?";
			List<Long> agendamentosIds = new ArrayList<>();
			
			try (PreparedStatement stmtBuscar = conn.prepareStatement(sqlBuscar)) {
				stmtBuscar.setLong(1, funcionarioId);
				ResultSet rs = stmtBuscar.executeQuery();
				while (rs.next()) {
					agendamentosIds.add(rs.getLong("age_codigo"));
				}
			}
			
			System.out.println("📋 Encontrados " + agendamentosIds.size() + " agendamentos do funcionário ID: " + funcionarioId);
			
			if (!agendamentosIds.isEmpty()) {
				// PASSO 2: Deletar NOTIFICAÇÕES de cada agendamento
				String sqlDeleteNotificacoes = "DELETE FROM notificacao WHERE age_codigo = ?";
				int totalNotificacoes = 0;
				
				try (PreparedStatement stmtDeleteNotificacoes = conn.prepareStatement(sqlDeleteNotificacoes)) {
					for (Long ageId : agendamentosIds) {
						stmtDeleteNotificacoes.setLong(1, ageId);
						totalNotificacoes += stmtDeleteNotificacoes.executeUpdate();
					}
				}
				System.out.println("🗑️ Deletados " + totalNotificacoes + " registros de notificacao");
				
				// PASSO 3: Deletar AGENDAMENTO_SERVICO de cada agendamento
				String sqlDeleteServicos = "DELETE FROM agendamento_servico WHERE age_codigo = ?";
				int totalServicos = 0;
				
				try (PreparedStatement stmtDeleteServicos = conn.prepareStatement(sqlDeleteServicos)) {
					for (Long ageId : agendamentosIds) {
						stmtDeleteServicos.setLong(1, ageId);
						totalServicos += stmtDeleteServicos.executeUpdate();
					}
				}
				System.out.println("🗑️ Deletados " + totalServicos + " registros de agendamento_servico");
			}
			
			// PASSO 4: Deletar todos os agendamentos do funcionário
			String sqlDeleteAgendamentos = "DELETE FROM agendamento WHERE fun_codigo = ?";
			int qtdAgendamentos = 0;
			
			try (PreparedStatement stmtDeleteAgendamentos = conn.prepareStatement(sqlDeleteAgendamentos)) {
				stmtDeleteAgendamentos.setLong(1, funcionarioId);
				qtdAgendamentos = stmtDeleteAgendamentos.executeUpdate();
			}
			
			System.out.println("🗑️ Deletados " + qtdAgendamentos + " agendamentos do funcionário ID: " + funcionarioId);
			return qtdAgendamentos;
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.err.println("Erro ao deletar agendamentos: " + e.getMessage());
		}
		return 0;
	}

}
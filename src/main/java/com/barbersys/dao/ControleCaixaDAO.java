package com.barbersys.dao;

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
import com.barbersys.util.DatabaseConnection;

public class ControleCaixaDAO {

	public static List<Map<String, Object>> buscarCaixasContagem(Date dataSelecionada) {
		System.out.println("🔍 ===== BUSCAR CAIXAS CONTAGEM DIA =====");
		Double totalEntradas = 0.0;
		Double totalSaidas = 0.0;

		List<Map<String, Object>> listaCarregada = new ArrayList<>();
		Map<String, Object> lista = new HashMap<>();
		String sql = "SELECT con_valor, con_data, con_movimentacao FROM controlecaixa WHERE con_data = ?";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
			String dataFiltro = dataFormatada.format(dataSelecionada);
			System.out.println("📅 Data filtro: " + dataFiltro);

			ps.setString(1, dataFiltro);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				ControleCaixa caixa = new ControleCaixa();
				caixa.setValor(rs.getDouble("con_valor"));
				caixa.setData(rs.getDate("con_data"));
				caixa.setMovimentacao(rs.getString("con_movimentacao"));

				System.out.println("📝 Movimentação: " + caixa.getMovimentacao() + " | Valor: R$ " + String.format("%.2f", caixa.getValor()));

				if (caixa.getMovimentacao().equals("Entrada") || caixa.getMovimentacao().equals("Entrada automática")) {
					totalEntradas += caixa.getValor();
					System.out.println("  ✅ Contabilizado em ENTRADAS");

				} else if (caixa.getMovimentacao().equals("Saida") || caixa.getMovimentacao().equals("Saída de estorno")) {
					totalSaidas += Math.abs(caixa.getValor()); // Math.abs porque estorno vem negativo
					System.out.println("  ✅ Contabilizado em SAÍDAS");

				} else if (caixa.getMovimentacao().equals("Abertura de Caixa") || caixa.getMovimentacao().equals("Fechamento de Caixa")) {
					System.out.println("  ⏭️  IGNORADO (controle interno)");
				}

			}
			
			System.out.println("💰 TOTAL ENTRADAS DIA: R$ " + String.format("%.2f", totalEntradas));
			System.out.println("💸 TOTAL SAÍDAS DIA: R$ " + String.format("%.2f", totalSaidas));
			System.out.println("🔍 ===== FIM BUSCAR CAIXAS CONTAGEM DIA =====\n");

			lista.put("entrada", totalEntradas);
			lista.put("saida", totalSaidas);

			listaCarregada.add(lista);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return listaCarregada;
	}

	public static List<Map<String, Object>> buscarCaixasContagemPorMes(Date dataSelecionada) {
		System.out.println("🔍 ===== BUSCAR CAIXAS CONTAGEM MÊS =====");
		Double totalEntradas = 0.0;
		Double totalSaidas = 0.0;

		List<Map<String, Object>> listaCarregada = new ArrayList<>();
		Map<String, Object> lista = new HashMap<>();

		String sql = "SELECT con_valor, con_data, con_movimentacao FROM controlecaixa "
				+ "WHERE MONTH(con_data) = ? AND YEAR(con_data) = ?";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			java.util.Calendar calendar = java.util.Calendar.getInstance();
			calendar.setTime(dataSelecionada);
			int mes = calendar.get(java.util.Calendar.MONTH) + 1;
			int ano = calendar.get(java.util.Calendar.YEAR);
			
			System.out.println("📅 Mês/Ano filtro: " + mes + "/" + ano);

			ps.setInt(1, mes);
			ps.setInt(2, ano);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				ControleCaixa caixa = new ControleCaixa();
				caixa.setValor(rs.getDouble("con_valor"));
				caixa.setData(rs.getDate("con_data"));
				caixa.setMovimentacao(rs.getString("con_movimentacao"));
				
				System.out.println("📝 Movimentação: " + caixa.getMovimentacao() + " | Valor: R$ " + String.format("%.2f", caixa.getValor()));

				// Soma APENAS entradas reais (manual e automática)
				if (caixa.getMovimentacao().equals("Entrada")
						|| caixa.getMovimentacao().equals("Entrada automática")) {
					totalEntradas += caixa.getValor();
					System.out.println("  ✅ Contabilizado em ENTRADAS");
				} 
				// Soma APENAS saídas reais (manual e estorno)
				else if (caixa.getMovimentacao().equals("Saida") || caixa.getMovimentacao().equals("Saída de estorno")) {
					totalSaidas += Math.abs(caixa.getValor()); // Math.abs porque estorno vem negativo
					System.out.println("  ✅ Contabilizado em SAÍDAS");
				}
				// Ignora: Abertura de Caixa e Fechamento de Caixa
				else if (caixa.getMovimentacao().equals("Abertura de Caixa") || caixa.getMovimentacao().equals("Fechamento de Caixa")) {
					System.out.println("  ⏭️  IGNORADO (controle interno)");
				}
			}
			
			System.out.println("💰 TOTAL ENTRADAS MÊS: R$ " + String.format("%.2f", totalEntradas));
			System.out.println("💸 TOTAL SAÍDAS MÊS: R$ " + String.format("%.2f", totalSaidas));
			System.out.println("🔍 ===== FIM BUSCAR CAIXAS CONTAGEM MÊS =====\n");

			lista.put("entrada", totalEntradas);
			lista.put("saida", totalSaidas);
			listaCarregada.add(lista);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return listaCarregada;
	}

	public static List<ControleCaixa> buscarCaixasPaginado(int first, int pageSize, Date dataSelecionada,
			String tipoValor, String sortField, String sortOrder) {
		List<ControleCaixa> lista = new ArrayList<>();
		
		// Mapeamento de campos JSF para colunas do banco
		String colunaBanco = "con_codigo";
		if ("id".equals(sortField)) colunaBanco = "con_codigo";
		else if ("movimentacao".equals(sortField)) colunaBanco = "con_movimentacao";
		else if ("valor".equals(sortField)) colunaBanco = "con_valor";
		else if ("horaAtual".equals(sortField)) colunaBanco = "con_hora";
		else if ("motivo".equals(sortField)) colunaBanco = "con_motivo";
		
		// Garante que sortOrder seja apenas ASC ou DESC
		String ordem = "DESC";
		if ("1".equals(sortOrder) || "ASC".equalsIgnoreCase(sortOrder)) {
			ordem = "ASC";
		}
		
		String sql = "SELECT con_codigo, con_valor, con_movimentacao, "
				+ "con_hora, con_motivo, con_data FROM controlecaixa  WHERE con_data = ?" + "AND (" + "      ? = '' "
				+ "   OR con_movimentacao = ? " + "   OR con_movimentacao = 'Abertura de Caixa' "
				+ "   OR con_movimentacao = 'Fechamento de Caixa'" + ") " 
				+ "ORDER BY " + colunaBanco + " " + ordem + " LIMIT ?, ? ";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
			String dataFiltro = dataFormatada.format(dataSelecionada);

			ps.setString(1, dataFiltro);
			ps.setString(2, tipoValor);
			ps.setString(3, tipoValor);
			ps.setInt(4, first);
			ps.setInt(5, pageSize);

			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				ControleCaixa caixa = new ControleCaixa();
				caixa.setId(rs.getLong("con_codigo"));
				caixa.setValor(rs.getDouble("con_valor"));
				caixa.setMovimentacao(rs.getString("con_movimentacao"));
				caixa.setHoraAtual(rs.getString("con_hora"));
				caixa.setMotivo(rs.getString("con_motivo"));
				caixa.setData(rs.getDate("con_data"));
				lista.add(caixa);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return lista;
	}

	public static int contarTotalCaixas(Date dataSelecionada, String tipoValor) {
		String sql = "SELECT COUNT(*) FROM controlecaixa WHERE con_data = ? AND (? = '' OR con_movimentacao = ?)";
		int total = 0;

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql);) {

			SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
			String dataFiltro = dataFormatada.format(dataSelecionada);

			ps.setString(1, dataFiltro);
			ps.setString(2, tipoValor);
			ps.setString(3, tipoValor);

			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				total = rs.getInt(1);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return total;
	}

	public static Map<String, Double> buscarEntradasESaidasDesdeUltimaAbertura(Date dataSelecionada) {
		Map<String, Double> resultado = new HashMap<>();
		double totalEntradas = 0.0;
		double totalSaidas = 0.0;

		String sql = "SELECT con_codigo, con_valor, con_data, con_movimentacao "
				+ "FROM controlecaixa WHERE con_data = ? ORDER BY con_codigo DESC";

		try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
			String dataFiltro = dataFormatada.format(dataSelecionada);

			ps.setString(1, dataFiltro);
			ResultSet rs = ps.executeQuery();

			Long idUltimaAbertura = -1L;

			while (rs.next()) {
				String movimentacao = rs.getString("con_movimentacao");
				if (movimentacao.equals("Abertura de Caixa")) {
					idUltimaAbertura = rs.getLong("con_codigo");
					break;
				}
			}

			if (idUltimaAbertura != -1L) {
				String sqlMovimentacoes = "SELECT con_valor, con_movimentacao "
						+ "FROM controlecaixa WHERE con_data = ? AND con_codigo > ?";

				try (PreparedStatement ps2 = conn.prepareStatement(sqlMovimentacoes)) {
					ps2.setString(1, dataFiltro);
					ps2.setLong(2, idUltimaAbertura);

					ResultSet rs2 = ps2.executeQuery();
					while (rs2.next()) {
						double valor = rs2.getDouble("con_valor");
						String movimentacao = rs2.getString("con_movimentacao");

						// Soma ENTRADAS (manual e automática)
						if (movimentacao.equals("Entrada") || movimentacao.equals("Entrada automática")) {
							totalEntradas += valor;
						} 
						// Soma SAÍDAS (manual e estorno)
						else if (movimentacao.equals("Saida") || movimentacao.equals("Saída de estorno")) {
							totalSaidas += Math.abs(valor); // Math.abs porque estorno pode vir negativo
						}
					}
				}
			}

			resultado.put("entrada", totalEntradas);
			resultado.put("saida", totalSaidas);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultado;
	}

	public static ControleCaixa buscarUltimoRegistroPorData(Date dataSelecionada) {
	    String sql = "SELECT * FROM controlecaixa WHERE con_data = ? ORDER BY con_hora DESC LIMIT 1";
	    ControleCaixa controle = null;

	    try (Connection conn = DatabaseConnection.getConnection();
	         PreparedStatement ps = conn.prepareStatement(sql)) {

	        SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
	        String dataFiltro = dataFormatada.format(dataSelecionada);

	        ps.setString(1, dataFiltro);
	        ResultSet rs = ps.executeQuery();

	        if (rs.next()) {
	            controle = new ControleCaixa();
	            controle.setId(rs.getLong("con_codigo"));
	            controle.setHoraAtual(rs.getString("con_hora"));
	            controle.setData(rs.getDate("con_data"));
	            controle.setValor(rs.getDouble("con_valor"));
	            controle.setMovimentacao(rs.getString("con_movimentacao"));
	            controle.setMotivo(rs.getString("con_motivo"));

	            CaixaData caixaData = new CaixaData();
	            caixaData.setId(rs.getLong("cai_codigo"));
	            controle.setCaixaData(caixaData);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return controle;
	}
	
	public static boolean ultimoRegistroEhFechamento(Date dataSelecionada) {
	    ControleCaixa ultimoRegistro = buscarUltimoRegistroPorData(dataSelecionada);
	    if (ultimoRegistro == null) {
	        return false;
	    }
	    return "Fechamento de Caixa".equalsIgnoreCase(ultimoRegistro.getMovimentacao());
	}

	public static void salvar(ControleCaixa caixa) {
		String sql = "INSERT INTO controlecaixa (con_valor, con_movimentacao, "
				+ "con_hora, con_motivo, con_data, cai_codigo) VALUES (?, ?, ?, ?, ?, ?)";
		try (Connection conn = DatabaseConnection.getConnection(); // Usando a conexão correta
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setDouble(1, caixa.getValor());
			stmt.setString(2, caixa.getMovimentacao());
			stmt.setString(3, caixa.getHoraAtual());
			stmt.setString(4, caixa.getMotivo());
			stmt.setDate(5, new java.sql.Date(caixa.getData().getTime()));
			stmt.setLong(6, caixa.getCaixaData().getId());
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
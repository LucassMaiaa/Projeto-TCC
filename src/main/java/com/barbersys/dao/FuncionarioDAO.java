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

import com.barbersys.model.ControleCaixa;
import com.barbersys.model.Funcionario;
import com.barbersys.util.DatabaseConnection;

public class FuncionarioDAO {
	
	 public static int funcionarioCount(){   
		String sql = "SELECT COUNT(*) FROM funcionario";
        int total = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {           
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                total = rs.getInt(1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return total;
    }
     
	 public static List<Funcionario> buscarFuncionario(int first, int pageSize) {
	        List<Funcionario> lista = new ArrayList<>();
	        String sql = "SELECT * FROM funcionario ORDER BY fun_codigo DESC LIMIT ?, ?";
	        
	        try (Connection conn = DatabaseConnection.getConnection(); 
	             PreparedStatement ps = conn.prepareStatement(sql)) {
	                        
	            ps.setInt(1, first);
	            ps.setInt(2, pageSize);    

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

    

    public static List<ControleCaixa> buscarCaixasPaginado(int first, int pageSize, Date dataSelecionada, String tipoValor) {
        List<ControleCaixa> lista = new ArrayList<>();
        String sql = "SELECT con_codigo, con_valor, con_movimentacao, "
                + "con_hora, con_motivo, con_data FROM controlecaixa  WHERE con_data = ?"
                + "AND (? = '' OR con_movimentacao = ?) ORDER BY con_codigo DESC LIMIT ?, ? ";
        
        try (Connection conn = DatabaseConnection.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
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

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {
            
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
    
      public static void salvar(ControleCaixa caixa) {
            String sql = "INSERT INTO controlecaixa (con_valor, con_movimentacao, "
                    + "con_hora, con_motivo, con_data) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection(); // Usando a conexão correta
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, caixa.getValor());
                stmt.setString(2, caixa.getMovimentacao());
                stmt.setString(3, caixa.getHoraAtual());
                stmt.setString(4, caixa.getMotivo());
                stmt.setDate(5, new java.sql.Date (caixa.getData().getTime()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
}

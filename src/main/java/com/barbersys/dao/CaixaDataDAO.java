package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import com.barbersys.model.CaixaData;
import com.barbersys.util.DatabaseConnection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaixaDataDAO {
	
	  public static List<CaixaData> verificaExisteData(Date dataSelecionada) {
        String sql = "SELECT * FROM caixadata WHERE cai_data = ?";
        List<CaixaData> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {
            
            SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd");
            String dataFiltro = dataFormatada.format(dataSelecionada);
            
            ps.setString(1, dataFiltro);
            
            ResultSet rs = ps.executeQuery();
            
            if(rs.next()) {
            	CaixaData caixaData = new CaixaData();
            	caixaData.setId(rs.getLong("cai_codigo"));
            	caixaData.setValorInicial(rs.getDouble("cai_valor_inicial"));
            	caixaData.setValorFinal(rs.getDouble("cai_valor_final"));
            	caixaData.setDataCadastro(rs.getDate("cai_data"));
            	caixaData.setStatus(rs.getString("cai_status"));
            	list.add(caixaData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
	  
	public static void atualizar(CaixaData caixaDataAtualizada) {
		    String sql = "UPDATE caixadata SET cai_valor_inicial = ?, cai_valor_final = ?, cai_status = ? WHERE cai_codigo = ?";

		    try (Connection conn = DatabaseConnection.getConnection();
		         PreparedStatement stmt = conn.prepareStatement(sql)) {

		        stmt.setDouble(1, caixaDataAtualizada.getValorInicial());
		        stmt.setDouble(2, caixaDataAtualizada.getValorFinal());
		        stmt.setString(3, caixaDataAtualizada.getStatus());
		        stmt.setLong(4, caixaDataAtualizada.getId());

		        stmt.executeUpdate();
		    } catch (SQLException e) {
		        e.printStackTrace();
		    }
	}

	  
	public static void salvar(CaixaData caixa) {
       String sql = "INSERT INTO caixadata (cai_valor_inicial, cai_valor_final, "
               + "cai_data, cai_status) VALUES (?, ?, ?, ?)";
       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
    	   java.util.Date dataAtual = new java.util.Date();
    	   
           stmt.setDouble(1, caixa.getValorInicial());
           stmt.setDouble(2, caixa.getValorFinal());
           stmt.setDate(3, new java.sql.Date (dataAtual.getTime()));
           stmt.setString(4, caixa.getStatus());
           stmt.executeUpdate();
       } catch (SQLException e) {
           e.printStackTrace();
       }
   }
}
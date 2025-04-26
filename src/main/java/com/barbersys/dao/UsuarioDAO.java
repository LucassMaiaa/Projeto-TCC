package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.barbersys.model.Usuario;
import com.barbersys.util.DatabaseConnection;

public class UsuarioDAO {

    public Usuario autenticar(String login, String senha) {
        Usuario usuario = null;
        String sql = "SELECT usu_codigo, usu_login, usu_senha FROM usuario WHERE usu_login = ? AND usu_senha = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = new Usuario();
                    usuario.setId(rs.getInt("usu_codigo"));
                    usuario.setLogin(rs.getString("usu_login"));
                    usuario.setSenha(rs.getString("usu_senha")); 
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usuario;
    }
    
    
}

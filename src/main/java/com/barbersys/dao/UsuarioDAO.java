package com.barbersys.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.barbersys.model.Perfil;
import com.barbersys.model.Usuario;
import com.barbersys.util.DatabaseConnection;

public class UsuarioDAO {

    public Usuario autenticar(String login, String senha) {
        Usuario usuario = null;
        String sql = "SELECT u.usu_codigo, u.usu_login, u.usu_senha, u.per_codigo, p.per_nome FROM usuario u " +
                     "JOIN perfil p ON u.per_codigo = p.per_codigo WHERE u.usu_login = ? AND u.usu_senha = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = new Usuario();
                    usuario.setId(rs.getLong("usu_codigo"));
                    usuario.setLogin(rs.getString("usu_login"));
                    usuario.setSenha(rs.getString("usu_senha")); 

                    Perfil perfil = new Perfil();
                    perfil.setId(rs.getLong("per_codigo"));
                    perfil.setNome(rs.getString("per_nome"));
                    usuario.setPerfil(perfil);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usuario;
    }
    
    public Usuario salvar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (usu_login, usu_senha, per_codigo) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getLogin());
            stmt.setString(2, usuario.getSenha());
            stmt.setLong(3, usuario.getPerfil().getId());
            
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    usuario.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        return usuario;
    }

    public void atualizar(Usuario usuario) throws SQLException {
        // Não atualiza a senha se ela estiver vazia
        String sql = "UPDATE usuario SET usu_login = ?" + 
                     (usuario.getSenha() != null && !usuario.getSenha().isEmpty() ? ", usu_senha = ?" : "") + 
                     " WHERE usu_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getLogin());
            if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
                stmt.setString(2, usuario.getSenha());
                stmt.setLong(3, usuario.getId());
            } else {
                stmt.setLong(2, usuario.getId());
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void deletar(Usuario usuario) throws SQLException {
        String sql = "DELETE FROM usuario WHERE usu_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, usuario.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public java.util.List<com.barbersys.model.Usuario> buscarUsuariosPorPerfis(java.util.List<String> nomesPerfis) {
        java.util.List<com.barbersys.model.Usuario> usuarios = new java.util.ArrayList<>();
        if (nomesPerfis == null || nomesPerfis.isEmpty()) {
            return usuarios;
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(nomesPerfis.size(), "?"));
        String sql = "SELECT u.usu_codigo, u.usu_login, p.per_codigo, p.per_nome FROM usuario u " +
                     "JOIN perfil p ON u.per_codigo = p.per_codigo WHERE p.per_nome IN (" + placeholders + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int i = 0; i < nomesPerfis.size(); i++) {
                stmt.setString(i + 1, nomesPerfis.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.barbersys.model.Usuario usuario = new com.barbersys.model.Usuario();
                    usuario.setId(rs.getLong("usu_codigo"));
                    usuario.setLogin(rs.getString("usu_login"));

                    com.barbersys.model.Perfil perfil = new com.barbersys.model.Perfil();
                    perfil.setId(rs.getLong("per_codigo"));
                    perfil.setNome(rs.getString("per_nome"));
                    usuario.setPerfil(perfil);
                    
                    usuarios.add(usuario);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usuarios;
    }
}
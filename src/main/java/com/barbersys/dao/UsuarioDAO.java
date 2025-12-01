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

    // Autentica usuário verificando login, senha e status ativo
    public Usuario autenticar(String login, String senha) {
        Usuario usuario = null;
        String sql = "SELECT u.usu_codigo, u.usu_login, u.usu_senha, u.usu_user, u.per_codigo, p.per_nome, " +
                     "c.cli_status, f.fun_status " +
                     "FROM usuario u " +
                     "JOIN perfil p ON u.per_codigo = p.per_codigo " +
                     "LEFT JOIN cliente c ON u.usu_codigo = c.usu_codigo " +
                     "LEFT JOIN funcionario f ON u.usu_codigo = f.usu_codigo " +
                     "WHERE u.usu_login = ? AND u.usu_senha = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, login);
            stmt.setString(2, senha);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String clienteStatus = rs.getString("cli_status");
                    String funcionarioStatus = rs.getString("fun_status");
                    
                    if (clienteStatus != null && !"A".equals(clienteStatus)) {
                        return null;
                    }
                    
                    if (funcionarioStatus != null && !"A".equals(funcionarioStatus)) {
                        return null;
                    }
                    
                    usuario = new Usuario();
                    usuario.setId(rs.getLong("usu_codigo"));
                    usuario.setLogin(rs.getString("usu_login"));
                    usuario.setSenha(rs.getString("usu_senha"));
                    usuario.setUser(rs.getString("usu_user"));

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
    
    // Verifica se usuário existe mas está inativo
    public boolean verificarUsuarioInativo(String login, String senha) {
        String sql = "SELECT c.cli_status, f.fun_status " +
                     "FROM usuario u " +
                     "LEFT JOIN cliente c ON u.usu_codigo = c.usu_codigo " +
                     "LEFT JOIN funcionario f ON u.usu_codigo = f.usu_codigo " +
                     "WHERE u.usu_login = ? AND u.usu_senha = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, login);
            stmt.setString(2, senha);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String clienteStatus = rs.getString("cli_status");
                    String funcionarioStatus = rs.getString("fun_status");
                    
                    if ((clienteStatus != null && !"A".equals(clienteStatus)) ||
                        (funcionarioStatus != null && !"A".equals(funcionarioStatus))) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    // Verifica se já existe usuário com este login
    public boolean loginExiste(String login) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuario WHERE usu_login = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    // Busca usuário por ID
    public Usuario buscarPorId(Long id) {
        Usuario usuario = null;
        String sql = "SELECT u.usu_codigo, u.usu_login, u.usu_senha, u.usu_user, u.per_codigo, p.per_nome FROM usuario u " +
                     "JOIN perfil p ON u.per_codigo = p.per_codigo WHERE u.usu_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    usuario = new Usuario();
                    usuario.setId(rs.getLong("usu_codigo"));
                    usuario.setLogin(rs.getString("usu_login"));
                    usuario.setSenha(rs.getString("usu_senha"));
                    usuario.setUser(rs.getString("usu_user"));

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

    // Salva novo usuário
    public Usuario salvar(Usuario usuario) throws SQLException {
        if (loginExiste(usuario.getLogin())) {
            throw new SQLException("Login já existe no sistema. Por favor, escolha outro.");
        }
        
        String sql = "INSERT INTO usuario (usu_login, usu_senha, usu_user, per_codigo) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, usuario.getLogin());
            stmt.setString(2, usuario.getSenha());
            stmt.setString(3, usuario.getUser());
            stmt.setLong(4, usuario.getPerfil().getId());
            
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

    // Atualiza dados do usuário
    public void atualizar(Usuario usuario) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE usuario SET usu_login = ?");
        
        if (usuario.getUser() != null && !usuario.getUser().isEmpty()) {
            sql.append(", usu_user = ?");
        }
        
        if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
            sql.append(", usu_senha = ?");
        }
        
        sql.append(" WHERE usu_codigo = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            stmt.setString(paramIndex++, usuario.getLogin());
            
            if (usuario.getUser() != null && !usuario.getUser().isEmpty()) {
                stmt.setString(paramIndex++, usuario.getUser());
            }
            
            if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
                stmt.setString(paramIndex++, usuario.getSenha());
            }
            
            stmt.setLong(paramIndex, usuario.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Remove usuário do banco
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
    
    // Inativa usuário
    public void inativar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuario SET usu_ativo = 0 WHERE usu_codigo = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, usuario.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    // Busca usuários por perfis específicos
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
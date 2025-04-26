package com.barbersys.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    // Configurações da conexão: ajuste os valores conforme seu ambiente
    private static final String URL = "jdbc:mysql://localhost:3306/barber_sys_tcc?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    private static final String USER = "barbersys";
    private static final String PASS = "1234";
    
    // Método para obter a conexão
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // 🔥 Adicionando essa linha para carregar o driver
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado!", e);
        }
    }
    
    // Método principal para testar a conexão (opcional)
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ Conexão estabelecida com sucesso!");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao conectar: " + e.getMessage());
        }
    }
}

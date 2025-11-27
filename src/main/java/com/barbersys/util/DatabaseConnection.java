package com.barbersys.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    
    // Adiciona parâmetros para evitar connection leak e timeout
    private static final String URL = "jdbc:mysql://localhost:3306/barber_sys_tcc?"
            + "allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC"
            + "&autoReconnect=true"           // Reconecta automaticamente
            + "&maxReconnects=3"               // Tenta reconectar até 3 vezes
            + "&socketTimeout=10000"           // Timeout de 10 segundos
            + "&connectTimeout=5000"           // Timeout de conexão 5 segundos
            + "&maxPerformance";               // Otimização de performance
    
    private static final String USER = "root";
    private static final String PASS = "1234";
    
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); 
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado!", e);
        }
    }
    
}

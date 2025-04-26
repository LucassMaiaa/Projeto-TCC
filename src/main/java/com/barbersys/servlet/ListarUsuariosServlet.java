package com.barbersys.servlet;

import com.barbersys.util.DatabaseConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/listarUsuarios")
public class ListarUsuariosServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter();
             Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Exemplo de consulta: ajuste conforme a sua tabela de usuários
            String sql = "SELECT usu_codigo, usu_login, usu_senha FROM usuario";
            ResultSet rs = stmt.executeQuery(sql);
            
            out.println("<html><body><h1>Lista de Usuários</h1><ul>");
            while (rs.next()) {
                out.println("<li>ID: " + rs.getInt("id") + " - Nome: " 
                        + rs.getString("nome") + " - Email: " + rs.getString("email") + "</li>");
            }
            out.println("</ul></body></html>");
            
        } catch (Exception e) {
            throw new ServletException("Erro ao acessar o banco de dados", e);
        }
    }
}

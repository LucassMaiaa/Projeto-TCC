package com.barbersys.util;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.barbersys.model.Usuario;

@WebFilter("*.xhtml")
public class AuthorizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String requestPath = httpRequest.getRequestURI();
        
        // Páginas públicas (não requerem autenticação)
        if (requestPath.contains("login.xhtml") || 
            requestPath.contains("registro.xhtml") ||
            requestPath.contains("recuperar_senha.xhtml") ||
            requestPath.contains("validar_codigo_recuperacao.xhtml") ||
            requestPath.contains("validar_codigo_registro.xhtml") ||
            requestPath.contains("/javax.faces.resource/")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Verifica se há usuário logado
        Usuario usuarioLogado = null;
        if (session != null) {
            usuarioLogado = (Usuario) session.getAttribute("usuarioLogado");
        }
        
        // Se não está logado, redireciona para login
        if (usuarioLogado == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.xhtml");
            return;
        }
        
        Long perfilId = usuarioLogado.getPerfil() != null ? usuarioLogado.getPerfil().getId() : null;
        
        // Controle de acesso baseado em perfil
        if (perfilId != null) {
            
            // PERFIL CLIENTE (ID = 3)
            if (perfilId == 3) {
                // Cliente só pode acessar agendamentoCliente.xhtml e configuracoes.xhtml
                if (!requestPath.contains("agendamentoCliente.xhtml") && 
                    !requestPath.contains("configuracoes.xhtml")) {
                    httpResponse.sendRedirect(httpRequest.getContextPath() + "/agendamentoCliente.xhtml");
                    return;
                }
            }
            
            // PERFIL FUNCIONÁRIO (ID = 2)
            else if (perfilId == 2) {
                // Funcionário pode acessar: agendamento, controle_caixa, configuracoes_geral
                if (!requestPath.contains("agendamento.xhtml") && 
                    !requestPath.contains("controle_caixa.xhtml") && 
                    !requestPath.contains("configuracoes_geral.xhtml") &&
                    !requestPath.contains("modal_avaliacao.xhtml")) { // Modal de avaliação usado no agendamento
                    httpResponse.sendRedirect(httpRequest.getContextPath() + "/agendamento.xhtml");
                    return;
                }
            }
            
            // PERFIL ADMINISTRADOR (ID = 1) - Acesso total, não precisa verificar
        }
        
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Inicialização se necessário
    }

    @Override
    public void destroy() {
        // Cleanup se necessário
    }
}

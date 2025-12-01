package com.barbersys.controller;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.barbersys.dao.UsuarioDAO;
import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Usuario;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;

@Getter
@Setter
@ManagedBean(name = "usuarioController")
@SessionScoped
public class UsuarioController implements Serializable{

    private String login;
    private String senha;
    private boolean loginFalhou = false; 
    private final UsuarioDAO usuarioDAO = new UsuarioDAO(); 

    // Método de login
    public String efetuarLogin() {
        // Primeiro verifica se existe usuário com esse login e senha
        Usuario usuarioAutenticado = usuarioDAO.autenticar(login, senha);

        if (usuarioAutenticado != null) {
            // Carregar Cliente ou Funcionário associado
            if (usuarioAutenticado.getPerfil() != null) {
                if (usuarioAutenticado.getPerfil().getId() == 3L) { // Cliente
                    Cliente cliente = ClienteDAO.buscarClientePorUsuarioId(usuarioAutenticado.getId());
                    usuarioAutenticado.setClienteAssociado(cliente);
                } else if (usuarioAutenticado.getPerfil().getId() == 2L) { // Funcionário
                    Funcionario funcionario = FuncionarioDAO.buscarFuncionarioPorUsuarioId(usuarioAutenticado.getId());
                    usuarioAutenticado.setFuncionarioAssociado(funcionario);
                }
            }

            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("usuarioLogado", usuarioAutenticado);
            loginFalhou = false;
            PrimeFaces.current().ajax().addCallbackParam("loginFalhou", false);
            
            // Determina a URL de redirecionamento com base no perfil
            Long perfilId = usuarioAutenticado.getPerfil() != null ? usuarioAutenticado.getPerfil().getId() : null;
            String redirectUrl = "dashboard.xhtml"; // Padrão para Admin
            
            if (perfilId == 3L) {
                redirectUrl = "agendamentoCliente.xhtml";
            } else if (perfilId == 2L) {
                redirectUrl = "agendamento.xhtml";
            }
            
            // Adiciona parâmetro de callback para redirecionar via JavaScript
            PrimeFaces.current().ajax().addCallbackParam("redirectUrl", redirectUrl);
            return null; // Não faz navegação servidor-side
        } else {
            // Verifica se o problema é usuário inativo
            boolean usuarioInativo = usuarioDAO.verificarUsuarioInativo(login, senha);
            
            if (usuarioInativo) {
                // Usuário existe mas está inativo
                loginFalhou = true;
                PrimeFaces.current().ajax().addCallbackParam("loginFalhou", true);
                PrimeFaces.current().ajax().addCallbackParam("usuarioInativo", true);
            } else {
                // Credenciais inválidas
                loginFalhou = true;
                PrimeFaces.current().ajax().addCallbackParam("loginFalhou", true);
                PrimeFaces.current().ajax().addCallbackParam("usuarioInativo", false);
            }
            return null;
        }
    }

    public String getNomeUsuarioLogado() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        if (usuarioLogado != null && usuarioLogado.getUser() != null && !usuarioLogado.getUser().isEmpty()) {
            return usuarioLogado.getUser();
        }
        return usuarioLogado != null ? usuarioLogado.getLogin() : "Visitante"; // Fallback
    }
    
    public Usuario getUsuarioLogado() {
        return (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
    }

    // Método para logout
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login.xhtml?faces-redirect=true";
    }
}

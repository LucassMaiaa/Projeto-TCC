package com.barbersys.controller;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.barbersys.dao.UsuarioDAO;
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
    private boolean loginFalhou = false; // Controla a exibição do diálogo
    private final UsuarioDAO usuarioDAO = new UsuarioDAO(); // Instância única do DAO

    // Método de login
    public String efetuarLogin() {
        Usuario usuarioAutenticado = usuarioDAO.autenticar(login, senha);

        if (usuarioAutenticado != null) {
            // Adiciona usuário na sessão e redireciona para o dashboard
            FacesContext.getCurrentInstance().getExternalContext().getSessionMap().put("usuarioLogado", usuarioAutenticado);
            loginFalhou = false; // Garante que o diálogo não abra
            return "dashboard.xhtml?faces-redirect=true";
        } else {
            // Exibe mensagem de erro e abre o diálogo
            loginFalhou = true;
            FacesMessage mensagem = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", "Usuário ou senha inválidos");
            FacesContext.getCurrentInstance().addMessage(null, mensagem);
            PrimeFaces.current().ajax().update("dlgErro"); // Atualiza os componentes
            PrimeFaces.current().executeScript("PF('dlgErro').show();"); // Abre o diálogo

            return null;
        }
    }

    // Método para logout
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login.xhtml?faces-redirect=true";
    }
}

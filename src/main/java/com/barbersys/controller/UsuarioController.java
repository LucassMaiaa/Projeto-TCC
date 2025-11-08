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
    private boolean loginFalhou = false; // Controla a exibição do diálogo
    private final UsuarioDAO usuarioDAO = new UsuarioDAO(); // Instância única do DAO

    // Método de login
    public String efetuarLogin() {
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
            return "dashboard.xhtml?faces-redirect=true";
        } else {
            // Marca que o login falhou para exibir SweetAlert2
            loginFalhou = true;
            PrimeFaces.current().ajax().addCallbackParam("loginFalhou", true);
            return null;
        }
    }

    public String getNomeUsuarioLogado() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("usuarioLogado");
        if (usuarioLogado != null) {
            if (usuarioLogado.getClienteAssociado() != null) {
                return usuarioLogado.getClienteAssociado().getNome();
            } else if (usuarioLogado.getFuncionarioAssociado() != null) {
                return usuarioLogado.getFuncionarioAssociado().getNome();
            }
        }
        return usuarioLogado != null ? usuarioLogado.getLogin() : "Visitante"; // Fallback
    }

    // Método para logout
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login.xhtml?faces-redirect=true";
    }
}

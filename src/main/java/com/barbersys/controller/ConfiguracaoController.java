package com.barbersys.controller;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class ConfiguracaoController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Cliente clienteModel = new Cliente();
    private String nomeUsuario;
    private String senhaAtual;
    private String novaSenha;
    private String confirmaNovaSenha;
    private boolean alterarSenha = false;
    
    @PostConstruct
    public void init() {
        System.out.println("=== INIT ConfiguracaoController ===");
        try {
            carregarDadosCliente();
            
            // Carregar nome de usuário
            if (clienteModel != null && clienteModel.getUsuario() != null) {
                this.nomeUsuario = clienteModel.getUsuario().getUser();
            }
            
            // Se não carregou, inicializa um cliente vazio para evitar null
            if (clienteModel == null) {
                System.out.println("Cliente Model é NULL - inicializando vazio");
                clienteModel = new Cliente();
                clienteModel.setUsuario(new Usuario());
            } else {
                System.out.println("Cliente carregado: " + clienteModel.getNome());
                System.out.println("Sexo: " + clienteModel.getSexo());
            }
        } catch (Exception e) {
            System.out.println("ERRO no INIT: " + e.getMessage());
            e.printStackTrace();
            clienteModel = new Cliente();
            clienteModel.setUsuario(new Usuario());
        }
    }
    
    private void carregarDadosCliente() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("usuarioLogado");
        
        if (usuarioLogado != null && usuarioLogado.getPerfil() != null 
                && usuarioLogado.getPerfil().getId() == 3L) {
            Cliente cliente = ClienteDAO.buscarClientePorUsuarioId(usuarioLogado.getId());
            if (cliente != null) {
                this.clienteModel = cliente;
            }
        }
    }
    
    public void salvarConfiguracoes() {
        System.out.println("=== MÉTODO salvarConfiguracoes CHAMADO ===");
        try {
            // Validações básicas
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome de usuário é obrigatório");
                return;
            }
            
            if (clienteModel.getNome() == null || clienteModel.getNome().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório");
                System.out.println("ERRO: Nome vazio");
                return;
            }
            
            if (clienteModel.getEmail() == null || clienteModel.getEmail().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório");
                System.out.println("ERRO: Email vazio");
                return;
            }
            
            if (clienteModel.getTelefone() == null || clienteModel.getTelefone().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
                System.out.println("ERRO: Telefone vazio");
                return;
            }
            
            if (clienteModel.getSexo() == null || clienteModel.getSexo().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
                System.out.println("ERRO: Sexo vazio");
                return;
            }
            
            System.out.println("Cliente ID: " + clienteModel.getId());
            System.out.println("Cliente Nome: " + clienteModel.getNome());
            System.out.println("Alterar Senha: " + alterarSenha);
            
            // Validar alteração de senha se solicitado
            if (alterarSenha) {
                System.out.println("Validando alteração de senha...");
                if (!validarAlteracaoSenha()) {
                    return;
                }
                
                // Atualizar senha
                System.out.println("Atualizando senha...");
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                clienteModel.getUsuario().setSenha(novaSenha);
                usuarioDAO.atualizar(clienteModel.getUsuario());
                System.out.println("Senha atualizada com sucesso!");
            }
            
            // Atualizar nome de usuário
            clienteModel.getUsuario().setUser(nomeUsuario);
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(clienteModel.getUsuario());
            
            // Atualizar o usuário na sessão
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("usuarioLogado");
            if (usuarioLogado != null) {
                usuarioLogado.setUser(nomeUsuario);
                FacesContext.getCurrentInstance().getExternalContext()
                        .getSessionMap().put("usuarioLogado", usuarioLogado);
            }
            
            // Salvar dados do cliente
            System.out.println("Atualizando dados do cliente...");
            ClienteDAO.atualizar(clienteModel);
            System.out.println("Cliente atualizado com sucesso!");
            
            addMessage(FacesMessage.SEVERITY_INFO, "Configurações salvas com sucesso!");
            
            // Limpar campos de senha
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
            alterarSenha = false;
            
            System.out.println("=== SUCESSO ===");
            
        } catch (Exception e) {
            System.out.println("ERRO EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
        }
    }
    
    private boolean validarAlteracaoSenha() {
        if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual é obrigatória");
            return false;
        }
        
        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nova senha é obrigatória");
            return false;
        }
        
        if (confirmaNovaSenha == null || confirmaNovaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Confirmação de senha é obrigatória");
            return false;
        }
        
        if (!novaSenha.equals(confirmaNovaSenha)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "As senhas não coincidem");
            return false;
        }
        
        // Verificar se a senha atual está correta
        if (!clienteModel.getUsuario().getSenha().equals(senhaAtual)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual incorreta");
            return false;
        }
        
        return true;
    }
    
    public String getIconeAvatar() {
        if (clienteModel.getSexo() == null) {
            return "fa-user"; // Padrão
        }
        
        switch (clienteModel.getSexo().toUpperCase()) {
            case "M":
                return "fa-user"; // Masculino
            case "F":
                return "fa-user-nurse"; // Feminino
            case "O":
                return "fa-user-astronaut"; // Outro/Neutro
            default:
                return "fa-user";
        }
    }
    
    public String getCorAvatar() {
        if (clienteModel.getSexo() == null) {
            return "#6366F1"; // Roxo padrão
        }
        
        switch (clienteModel.getSexo().toUpperCase()) {
            case "M":
                return "#3B82F6"; // Azul
            case "F":
                return "#EC4899"; // Rosa
            case "O":
                return "#8B5CF6"; // Roxo
            default:
                return "#6366F1";
        }
    }
    
    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, ""));
    }
}

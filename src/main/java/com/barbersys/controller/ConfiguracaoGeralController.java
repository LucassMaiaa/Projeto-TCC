package com.barbersys.controller;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class ConfiguracaoGeralController implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private Usuario usuarioModel = new Usuario();
    private Funcionario funcionarioModel = new Funcionario();
    private String nomeUsuario;
    private String loginUsuario;
    private String senhaAtual;
    private String novaSenha;
    private String confirmaNovaSenha;
    private boolean alterarSenha = false;
    private boolean adminPerfil = false;
    private boolean funcionarioPerfil = false;
    
    @PostConstruct
    public void init() {
        System.out.println("=== INIT ConfiguracaoGeralController ===");
        try {
            carregarDadosUsuario();
            
            // Carregar nome de usuário e login
            if (usuarioModel != null) {
                this.nomeUsuario = usuarioModel.getUser();
                this.loginUsuario = usuarioModel.getLogin();
            }
            
            if (usuarioModel == null) {
                System.out.println("Usuario Model é NULL - inicializando vazio");
                usuarioModel = new Usuario();
            } else {
                System.out.println("Usuario carregado: " + usuarioModel.getLogin());
                
                // Verificar se perfil não é null
                if (usuarioModel.getPerfil() != null) {
                    System.out.println("Perfil ID: " + usuarioModel.getPerfil().getId());
                    
                    adminPerfil = (usuarioModel.getPerfil().getId() == 1L);
                    funcionarioPerfil = (usuarioModel.getPerfil().getId() == 2L);
                    
                    // Se for funcionário, carrega dados completos
                    if (funcionarioPerfil) {
                        carregarDadosFuncionario();
                    }
                } else {
                    System.out.println("ERRO: Perfil é NULL!");
                    adminPerfil = false;
                    funcionarioPerfil = false;
                }
            }
        } catch (Exception e) {
            System.out.println("ERRO no INIT: " + e.getMessage());
            e.printStackTrace();
            usuarioModel = new Usuario();
            adminPerfil = false;
            funcionarioPerfil = false;
        }
    }
    
    private void carregarDadosUsuario() {
        Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                .getExternalContext().getSessionMap().get("usuarioLogado");
        
        if (usuarioLogado != null) {
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Usuario usuarioAtualizado = usuarioDAO.buscarPorId(usuarioLogado.getId());
            if (usuarioAtualizado != null) {
                this.usuarioModel = usuarioAtualizado;
            }
        }
    }
    
    private void carregarDadosFuncionario() {
        Funcionario funcionario = FuncionarioDAO.buscarFuncionarioPorUsuarioId(usuarioModel.getId());
        if (funcionario != null) {
            this.funcionarioModel = funcionario;
            System.out.println("Funcionario carregado: " + funcionarioModel.getNome());
        } else {
            System.out.println("Funcionario não encontrado para usuario ID: " + usuarioModel.getId());
        }
    }
    
    public void onAlterarSenhaChange() {
        System.out.println("Checkbox alterarSenha mudou para: " + alterarSenha);
        // Adiciona o valor ao contexto de callback para uso no oncomplete do AJAX
        org.primefaces.PrimeFaces.current().ajax().addCallbackParam("alterarSenha", alterarSenha);
    }
    
    public void salvarConfiguracoes() {
        System.out.println("=== MÉTODO salvarConfiguracoes CHAMADO ===");
        try {
            // Validar nome de usuário
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome de usuário é obrigatório");
                return;
            }
            
            System.out.println("Usuario ID: " + usuarioModel.getId());
            System.out.println("É Admin: " + adminPerfil);
            System.out.println("É Funcionário: " + funcionarioPerfil);
            System.out.println("Alterar Senha: " + alterarSenha);
            
            // Se for funcionário, validar e salvar dados
            if (funcionarioPerfil) {
                if (!validarDadosFuncionario()) {
                    return;
                }
                
                System.out.println("Atualizando dados do funcionário...");
                FuncionarioDAO.atualizar(funcionarioModel);
                System.out.println("Funcionário atualizado com sucesso!");
            }
            
            // Validar alteração de senha se solicitado
            if (alterarSenha) {
                System.out.println("Validando alteração de senha...");
                if (!validarAlteracaoSenha()) {
                    return;
                }
                
                // Atualizar senha
                System.out.println("Atualizando senha...");
                usuarioModel.setSenha(novaSenha);
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                usuarioDAO.atualizar(usuarioModel);
                System.out.println("Senha atualizada com sucesso!");
            }
            
            // Atualizar nome de usuário
            usuarioModel.setUser(nomeUsuario);
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(usuarioModel);
            
            // Atualizar sessão
            FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("usuarioLogado", usuarioModel);
            
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
    
    private boolean validarDadosFuncionario() {
        if (funcionarioModel.getNome() == null || funcionarioModel.getNome().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório");
            return false;
        }
        
        if (funcionarioModel.getTelefone() == null || funcionarioModel.getTelefone().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
            return false;
        }
        
        if (funcionarioModel.getSexo() == null || funcionarioModel.getSexo().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
            return false;
        }
        
        return true;
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
        
        if (!usuarioModel.getSenha().equals(senhaAtual)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual incorreta");
            return false;
        }
        
        if (!novaSenha.equals(confirmaNovaSenha)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nova senha e confirmação não coincidem");
            return false;
        }
        
        if (novaSenha.length() < 6) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nova senha deve ter no mínimo 6 caracteres");
            return false;
        }
        
        return true;
    }
    
    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, message, null));
    }
    
    public String getIconeAvatar() {
        if (usuarioModel.getPerfil() != null) {
            Long perfilId = usuarioModel.getPerfil().getId();
            if (perfilId == 1L) { // Admin
                return "fa-user-shield";
            } else if (perfilId == 2L) { // Funcionário
                return "fa-user-tie";
            }
        }
        return "fa-user";
    }
    
    public String getTipoPerfil() {
        if (usuarioModel.getPerfil() != null) {
            Long perfilId = usuarioModel.getPerfil().getId();
            if (perfilId == 1L) {
                return "Administrador";
            } else if (perfilId == 2L) {
                return "Funcionário";
            }
        }
        return "Usuário";
    }
}

package com.barbersys.controller;

import java.io.Serializable;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Usuario;
import com.barbersys.util.EmailService;

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
    private String emailOriginal; // Para comparar se o email foi alterado
    private String codigoDigitado;
    private String codigoGerado;
    private boolean emailAlterado = false;
    
    @PostConstruct
    public void init() {
        try {
            carregarDadosCliente();
            
            if (clienteModel != null && clienteModel.getUsuario() != null) {
                this.nomeUsuario = clienteModel.getUsuario().getUser();
                
                if (clienteModel.getUsuario().getLogin() != null) {
                    clienteModel.setEmail(clienteModel.getUsuario().getLogin());
                    this.emailOriginal = clienteModel.getUsuario().getLogin();
                }
            }
            
            if (clienteModel == null) {
                clienteModel = new Cliente();
                clienteModel.setUsuario(new Usuario());
            }
        } catch (Exception e) {
            e.printStackTrace();
            clienteModel = new Cliente();
            clienteModel.setUsuario(new Usuario());
        }
    }
    
    // Manipula alteração do checkbox de alterar senha
    public void onAlterarSenhaChange() {
        if (!alterarSenha) {
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        }
        org.primefaces.PrimeFaces.current().ajax().addCallbackParam("alterarSenha", alterarSenha);
    }
    
    // Carrega dados do cliente logado
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
    
    // Salva as configurações do cliente
    public void salvarConfiguracoes() {
        try {
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome de usuário é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getNome() == null || clienteModel.getNome().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getEmail() == null || clienteModel.getEmail().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (!clienteModel.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getTelefone() == null || clienteModel.getTelefone().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getSexo() == null || clienteModel.getSexo().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (!clienteModel.getEmail().equals(emailOriginal)) {
                enviarCodigoVerificacao();
                return;
            }
            
            finalizarSalvamento();
            
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            limparCamposSenha();
        }
    }
    
    // Valida a alteração de senha
    private boolean validarAlteracaoSenha() {
        if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual é obrigatória");
            limparCamposSenha();
            return false;
        }
        
        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nova senha é obrigatória");
            limparCamposSenha();
            return false;
        }
        
        if (novaSenha.length() < 8) {
            addMessage(FacesMessage.SEVERITY_ERROR, "A nova senha deve ter no mínimo 8 caracteres");
            limparCamposSenha();
            return false;
        }
        
        if (confirmaNovaSenha == null || confirmaNovaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Confirmação de senha é obrigatória");
            limparCamposSenha();
            return false;
        }
        
        if (!novaSenha.equals(confirmaNovaSenha)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "As senhas não coincidem");
            limparCamposSenha();
            return false;
        }
        
        if (!clienteModel.getUsuario().getSenha().equals(senhaAtual)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual incorreta");
            limparCamposSenha();
            return false;
        }
        
        return true;
    }
    
    // Envia código de verificação por email
    private void enviarCodigoVerificacao() {
        try {
            if (!clienteModel.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido. Por favor, digite um email válido (ex: usuario@email.com)");
                limparCamposSenha();
                return;
            }
            
            if (!clienteModel.getEmail().equals(emailOriginal)) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                try {
                    if (usuarioDAO.loginExiste(clienteModel.getEmail())) {
                        addMessage(FacesMessage.SEVERITY_ERROR, "Este email já está cadastrado no sistema.");
                        limparCamposSenha();
                        return;
                    }
                } catch (java.sql.SQLException e) {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao verificar email: " + e.getMessage());
                    limparCamposSenha();
                    return;
                }
            }
            
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                clienteModel.getEmail(), 
                clienteModel.getNome(), 
                codigoGerado
            );
            
            if (enviado) {
                emailAlterado = true;
                addMessage(FacesMessage.SEVERITY_INFO, "Código de verificação enviado para " + clienteModel.getEmail());
                PrimeFaces.current().executeScript("PF('dlgValidarCodigoCliente').show();");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código de verificação");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código: " + e.getMessage());
        }
    }
    
    // Valida o código digitado pelo usuário
    public void validarCodigo() {
        if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "error", "Digite o código de verificação"
            ));
            return;
        }
        
        if (codigoDigitado.trim().equals(codigoGerado)) {
            codigoDigitado = null;
            PrimeFaces.current().executeScript("PF('dlgValidarCodigoCliente').hide();");
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "success", "Código validado! Finalizando alterações..."
            ));
            finalizarSalvamento();
        } else {
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "error", "Código inválido. Tente novamente."
            ));
        }
    }
    
    // Reenvia código de verificação
    public void reenviarCodigo() {
        try {
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                clienteModel.getEmail(), 
                clienteModel.getNome(), 
                codigoGerado
            );
            
            if (enviado) {
                PrimeFaces.current().executeScript(String.format(
                    "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                    "info", "Código reenviado para " + clienteModel.getEmail()
                ));
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código: " + e.getMessage());
        }
    }
    
    // Finaliza o salvamento das configurações
    private void finalizarSalvamento() {
        try {
            if (alterarSenha) {
                if (!validarAlteracaoSenha()) {
                    return;
                }
                clienteModel.getUsuario().setSenha(novaSenha);
            }
            
            clienteModel.getUsuario().setUser(nomeUsuario);
            clienteModel.getUsuario().setLogin(clienteModel.getEmail());
            
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(clienteModel.getUsuario());
            
            emailOriginal = clienteModel.getEmail();
            
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("usuarioLogado");
            if (usuarioLogado != null) {
                usuarioLogado.setUser(nomeUsuario);
                usuarioLogado.setLogin(clienteModel.getEmail());
                FacesContext.getCurrentInstance().getExternalContext()
                        .getSessionMap().put("usuarioLogado", usuarioLogado);
            }
            
            ClienteDAO.atualizar(clienteModel);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Configurações salvas com sucesso!");
            
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
            alterarSenha = false;
            codigoDigitado = null;
            codigoGerado = null;
            emailAlterado = false;
            
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            if (e.getMessage().contains("Login já existe")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            }
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        }
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
    
    public String getTipoPerfil() {
        return "Cliente";
    }
    
    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, ""));
    }
    
    private void limparCamposSenha() {
        senhaAtual = null;
        novaSenha = null;
        confirmaNovaSenha = null;
        codigoDigitado = null;
    }
}

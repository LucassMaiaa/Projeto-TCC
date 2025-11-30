package com.barbersys.controller;

import java.sql.SQLException;
import java.util.Random;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Perfil;
import com.barbersys.model.Usuario;
import com.barbersys.util.EmailService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@SessionScoped
public class RegistroController {

    private Cliente clienteModel = new Cliente();
    private String confirmaSenha;
    private String codigoDigitado;
    private String codigoGerado;
    private boolean codigoEnviado = false;
    private boolean codigoValidado = false;
    private String email;

    public RegistroController() {
        clienteModel.setUsuario(new Usuario());
    }

    public void enviarCodigoRegistro() {
        try {
            // Validações básicas
            if (email == null || email.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "E-mail é obrigatório");
                return;
            }
            
            // Valida formato do email
            if (!email.contains("@") || !email.contains(".")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido. Por favor, digite um email válido (ex: usuario@email.com)");
                return;
            }
            
            // Verifica se o email já existe no sistema ANTES de enviar o código
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            try {
                if (usuarioDAO.loginExiste(email)) {
                    addMessage(FacesMessage.SEVERITY_ERROR, "Este email já está cadastrado no sistema.");
                    return;
                }
            } catch (SQLException e) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao verificar email: " + e.getMessage());
                return;
            }
            
            if (clienteModel.getNome() == null || clienteModel.getNome().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório");
                return;
            }
            
            if (clienteModel.getTelefone() == null || clienteModel.getTelefone().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
                return;
            }
            
            if (clienteModel.getCpf() == null || clienteModel.getCpf().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "CPF é obrigatório");
                return;
            }
            
            if (clienteModel.getSexo() == null || clienteModel.getSexo().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
                return;
            }
            
            if (clienteModel.getDataNascimento() == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Data de nascimento é obrigatória");
                return;
            }
            
            if (clienteModel.getUsuario().getSenha() == null || clienteModel.getUsuario().getSenha().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Senha é obrigatória");
                return;
            }
            
            if (confirmaSenha == null || confirmaSenha.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Confirmação de senha é obrigatória");
                return;
            }
            
            if (!clienteModel.getUsuario().getSenha().equals(confirmaSenha)) {
                addMessage(FacesMessage.SEVERITY_ERROR, "As senhas não coincidem");
                return;
            }

            // Definir o email como login do usuário
            clienteModel.getUsuario().setLogin(email);
            
            // Gerar código de 6 dígitos
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            
            // Enviar email
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                email, 
                clienteModel.getNome(), 
                codigoGerado
            );
            
            if (enviado) {
                codigoEnviado = true;
                addMessage(FacesMessage.SEVERITY_INFO, "Código de verificação enviado para " + email);
                PrimeFaces.current().executeScript("setTimeout(function(){ window.location.href = 'validar_codigo_registro.xhtml'; }, 1500);");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código de verificação");
            }
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código: " + e.getMessage());
        }
    }
    
    public void validarCodigo() {
        if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Digite o código de verificação");
            return;
        }
        
        System.out.println("Código digitado: " + codigoDigitado);
        System.out.println("Código gerado: " + codigoGerado);
        
        if (codigoDigitado.trim().equals(codigoGerado)) {
            codigoValidado = true;
            addMessage(FacesMessage.SEVERITY_INFO, "Código validado! Finalizando cadastro...");
            registrarCliente();
        } else {
            addMessage(FacesMessage.SEVERITY_ERROR, "Código inválido. Tente novamente.");
        }
    }
    
    public void reenviarCodigo() {
        try {
            // Gerar novo código
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            
            // Enviar email
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                email, 
                clienteModel.getNome(), 
                codigoGerado
            );
            
            if (enviado) {
                addMessage(FacesMessage.SEVERITY_INFO, "Novo código enviado para " + email);
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código");
            }
            
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código: " + e.getMessage());
        }
    }
    
    public void registrarCliente() {
        try {
            if (!codigoValidado) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Valide o código de verificação primeiro");
                return;
            }

            // Salvar o usuário primeiro
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Perfil perfil = new Perfil();
            perfil.setId(3L); // 3 = Cliente
            clienteModel.getUsuario().setPerfil(perfil);
            clienteModel.getUsuario().setUser(clienteModel.getNome());
            Usuario usuarioSalvo = usuarioDAO.salvar(clienteModel.getUsuario());
            clienteModel.setUsuario(usuarioSalvo);

            // Salvar o cliente
            ClienteDAO.salvar(clienteModel);

            addMessage(FacesMessage.SEVERITY_INFO, "Conta criada com sucesso! Redirecionando...");
            PrimeFaces.current().executeScript("setTimeout(function(){ window.location.href = 'login.xhtml'; }, 2000);");

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("Login já existe")) {
                addMessage(FacesMessage.SEVERITY_WARN, "Este e-mail já está em uso. Por favor, use outro.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao criar conta: " + e.getMessage());
            }
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro inesperado ao criar conta: " + e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, detail, ""));
    }

    public String voltarLogin() {
        return "login.xhtml?faces-redirect=true";
    }

    public java.util.Date getHoje() {
        return new java.util.Date();
    }
    
    public int getAnoAtual() {
        return java.time.LocalDate.now().getYear();
    }
}

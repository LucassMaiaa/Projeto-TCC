package com.barbersys.controller;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Funcionario;
import com.barbersys.util.EmailService;

import lombok.Data;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Date;

@Data
@ManagedBean
@SessionScoped
public class RecuperacaoSenhaController implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String cpfOuEmail;
    private String codigoDigitado;
    private String novaSenha;
    private String confirmarSenha;
    private String codigoGerado;
    private Date dataGeracao;
    private String tipoUsuario;
    private Long idUsuario;
    private String emailUsuario;
    private String nomeUsuario;
    
    private static final long TEMPO_EXPIRACAO = 15 * 60 * 1000;
    
    // Solicita código de recuperação por CPF ou email
    public String solicitarCodigo() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            if (cpfOuEmail == null || cpfOuEmail.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Por favor, informe seu CPF ou Email"));
                return null;
            }
            
            String cpfLimpo = cpfOuEmail.replaceAll("[^0-9]", "");
            Cliente cliente = null;
            Funcionario funcionario = null;
            
            if (cpfLimpo.length() == 11) {
                cliente = ClienteDAO.buscarPorCPFRecuperacao(cpfLimpo);
            }
            if (cliente == null) {
                cliente = ClienteDAO.buscarPorEmailRecuperacao(cpfOuEmail);
            }
            if (cliente == null) {
                if (cpfLimpo.length() == 11) {
                    funcionario = FuncionarioDAO.buscarPorCPFRecuperacao(cpfLimpo);
                }
                if (funcionario == null) {
                    funcionario = FuncionarioDAO.buscarPorEmailRecuperacao(cpfOuEmail);
                }
            }
            
            if (cliente == null && funcionario == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "CPF ou Email não encontrado no sistema"));
                return null;
            }
            
            if (cliente != null) {
                tipoUsuario = "CLIENTE";
                idUsuario = cliente.getId();
                emailUsuario = cliente.getUsuario() != null ? cliente.getUsuario().getLogin() : null;
                nomeUsuario = cliente.getNome();
            } else {
                tipoUsuario = "FUNCIONARIO";
                idUsuario = funcionario.getId();
                emailUsuario = funcionario.getUsuario() != null ? funcionario.getUsuario().getLogin() : null;
                nomeUsuario = funcionario.getNome();
            }
            
            if (emailUsuario == null || emailUsuario.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Usuário não possui email cadastrado. Entre em contato com o administrador."));
                return null;
            }
            
            codigoGerado = EmailService.gerarCodigo();
            dataGeracao = new Date();
            
            boolean enviado = EmailService.enviarCodigoRecuperacao(emailUsuario, nomeUsuario, codigoGerado);
            
            if (enviado) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Código enviado para " + mascarEmail(emailUsuario)));
                return "validar_codigo_recuperacao?faces-redirect=true";
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Não foi possível enviar o email. Tente novamente."));
                return null;
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao processar solicitação: " + e.getMessage()));
            return null;
        }
    }
    
    // Valida código digitado pelo usuário
    public String validarCodigo() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Por favor, digite o código"));
                return null;
            }
            
            if (codigoGerado == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Sessão expirada. Por favor, solicite um novo código"));
                limparSessao();
                return null;
            }
            
            long tempoDecorrido = new Date().getTime() - dataGeracao.getTime();
            if (tempoDecorrido > TEMPO_EXPIRACAO) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Código expirado. Por favor, solicite um novo código"));
                limparSessao();
                return null;
            }
            
            if (!codigoDigitado.trim().equals(codigoGerado)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Código inválido. Verifique e tente novamente"));
                return null;
            }
            
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Código validado! Defina sua nova senha"));
            return null;
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao validar código: " + e.getMessage()));
            return null;
        }
    }
    
    // Atualiza senha do usuário
    public String redefinirSenha() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            if (novaSenha == null || novaSenha.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Por favor, informe a nova senha"));
                return null;
            }
            
            if (novaSenha.length() < 6) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "A senha deve ter no mínimo 6 caracteres"));
                return null;
            }
            
            if (confirmarSenha == null || !novaSenha.equals(confirmarSenha)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "As senhas não conferem"));
                return null;
            }
            
            if (tipoUsuario == null || idUsuario == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Sessão expirada. Por favor, inicie o processo novamente"));
                limparSessao();
                return null;
            }
            
            boolean sucesso = false;
            if ("CLIENTE".equals(tipoUsuario)) {
                sucesso = ClienteDAO.atualizarSenha(idUsuario, novaSenha);
            } else if ("FUNCIONARIO".equals(tipoUsuario)) {
                sucesso = FuncionarioDAO.atualizarSenha(idUsuario, novaSenha);
            }
            
            if (sucesso) {
                limparSessao();
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Senha redefinida com sucesso! Faça login com sua nova senha"));
                return "login.xhtml?faces-redirect=true";
            } else {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Não foi possível redefinir a senha. Tente novamente"));
                return null;
            }
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao redefinir senha: " + e.getMessage()));
            return null;
        }
    }
    
    // Reenvia código de recuperação
    public String reenviarCodigo() {
        if (emailUsuario != null && nomeUsuario != null) {
            codigoGerado = EmailService.gerarCodigo();
            dataGeracao = new Date();
            
            boolean enviado = EmailService.enviarCodigoRecuperacao(emailUsuario, nomeUsuario, codigoGerado);
            
            if (enviado) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Sucesso", "Novo código enviado para seu email"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Não foi possível reenviar o código"));
            }
        }
        return null;
    }
    
    private void limparSessao() {
        cpfOuEmail = null;
        codigoDigitado = null;
        novaSenha = null;
        confirmarSenha = null;
        codigoGerado = null;
        dataGeracao = null;
        tipoUsuario = null;
        idUsuario = null;
        emailUsuario = null;
        nomeUsuario = null;
    }
    
    private String mascarEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] partes = email.split("@");
        String usuario = partes[0];
        String dominio = partes[1];
        
        if (usuario.length() <= 3) {
            return usuario.charAt(0) + "***@" + dominio;
        }
        return usuario.substring(0, 3) + "***@" + dominio;
    }
    
    public String getEmailMascarado() { 
        return mascarEmail(emailUsuario); 
    }
    
    public boolean isCodigoEnviado() {
        return codigoGerado != null && dataGeracao != null;
    }
    
    public boolean isCodigoValidado() {
        return codigoGerado != null && codigoDigitado != null && codigoGerado.equals(codigoDigitado);
    }
}

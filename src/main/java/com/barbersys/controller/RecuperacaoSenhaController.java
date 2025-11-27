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
    
    // Atributos para o fluxo
    private String cpfOuEmail;
    private String codigoDigitado;
    private String novaSenha;
    private String confirmarSenha;
    
    // Atributos de controle
    private String codigoGerado;
    private Date dataGeracao;
    private String tipoUsuario; // "CLIENTE" ou "FUNCIONARIO"
    private Long idUsuario;
    private String emailUsuario;
    private String nomeUsuario;
    
    // Tempo de expiração do código em milissegundos (15 minutos)
    private static final long TEMPO_EXPIRACAO = 15 * 60 * 1000;
    
    /**
     * PASSO 1: Solicitar código de recuperação
     */
    public String solicitarCodigo() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            // Validação
            if (cpfOuEmail == null || cpfOuEmail.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Por favor, informe seu CPF ou Email"));
                return null;
            }
            
            // Limpar CPF se for informado
            String cpfLimpo = cpfOuEmail.replaceAll("[^0-9]", "");
            
            // Buscar usuário (primeiro tenta cliente, depois funcionário)
            Cliente cliente = null;
            Funcionario funcionario = null;
            
            // Tentar buscar por CPF ou Email
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
            
            // Verificar se encontrou algum usuário
            if (cliente == null && funcionario == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "CPF ou Email não encontrado no sistema"));
                return null;
            }
            
            // Definir tipo e dados do usuário
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
            
            // Verificar se tem email cadastrado
            if (emailUsuario == null || emailUsuario.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Usuário não possui email cadastrado. Entre em contato com o administrador."));
                return null;
            }
            
            // Gerar código
            codigoGerado = EmailService.gerarCodigo();
            dataGeracao = new Date();
            
            // Enviar email
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
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao processar solicitação: " + e.getMessage()));
            return null;
        }
    }
    
    /**
     * PASSO 2: Validar código digitado
     */
    public String validarCodigo() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            // Validação básica
            if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Por favor, digite o código"));
                return null;
            }
            
            // Verificar se o código foi gerado
            if (codigoGerado == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Sessão expirada. Por favor, solicite um novo código"));
                limparSessao();
                return null;
            }
            
            // Verificar expiração (15 minutos)
            long tempoDecorrido = new Date().getTime() - dataGeracao.getTime();
            if (tempoDecorrido > TEMPO_EXPIRACAO) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Código expirado. Por favor, solicite um novo código"));
                limparSessao();
                return null;
            }
            
            // Validar código
            if (!codigoDigitado.trim().equals(codigoGerado)) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Código inválido. Verifique e tente novamente"));
                return null;
            }
            
            // Código válido, ir para redefinir senha (permanece na mesma página)
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, 
                "Sucesso", "Código validado! Defina sua nova senha"));
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao validar código: " + e.getMessage()));
            return null;
        }
    }
    
    /**
     * PASSO 3: Redefinir senha
     */
    public String redefinirSenha() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            
            // Validações
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
            
            // Verificar sessão
            if (tipoUsuario == null || idUsuario == null) {
                context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Erro", "Sessão expirada. Por favor, inicie o processo novamente"));
                limparSessao();
                return null;
            }
            
            // Atualizar senha no banco
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
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                "Erro", "Erro ao redefinir senha: " + e.getMessage()));
            return null;
        }
    }
    
    /**
     * Reenviar código
     */
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
    
    /**
     * Limpar sessão
     */
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
    
    /**
     * Mascara email para exibição
     */
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
    
    // Métodos auxiliares para JSF
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

package com.barbersys.controller;

import java.io.Serializable;
import java.util.Random;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;

import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Usuario;
import com.barbersys.util.EmailService;

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
    private String loginOriginal; // Para comparar se o email foi alterado
    private String codigoDigitado;
    private String codigoGerado;
    private boolean emailAlterado = false;
    
    @PostConstruct
    public void init() {
        try {
            carregarDadosUsuario();
            
            if (usuarioModel != null) {
                this.nomeUsuario = usuarioModel.getUser();
                this.loginUsuario = usuarioModel.getLogin();
                this.loginOriginal = usuarioModel.getLogin();
            }
            
            if (usuarioModel == null) {
                usuarioModel = new Usuario();
            } else {
                if (usuarioModel.getPerfil() != null) {
                    adminPerfil = (usuarioModel.getPerfil().getId() == 1L);
                    funcionarioPerfil = (usuarioModel.getPerfil().getId() == 2L);
                    
                    if (funcionarioPerfil) {
                        carregarDadosFuncionario();
                    }
                } else {
                    adminPerfil = false;
                    funcionarioPerfil = false;
                }
            }
        } catch (Exception e) {
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
    
    // Carrega dados do funcionário associado ao usuário logado
    private void carregarDadosFuncionario() {
        Funcionario funcionario = FuncionarioDAO.buscarFuncionarioPorUsuarioId(usuarioModel.getId());
        if (funcionario != null) {
            this.funcionarioModel = funcionario;
        }
    }
    
    // Gerencia mudança do checkbox de alteração de senha
    public void onAlterarSenhaChange() {
        if (!alterarSenha) {
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        }
        
        org.primefaces.PrimeFaces.current().ajax().addCallbackParam("alterarSenha", alterarSenha);
    }
    
    // Salva as configurações do usuário/funcionário
    public void salvarConfiguracoes() {
        try {
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome de usuário é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (loginUsuario == null || loginUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório");
                limparCamposSenha();
                return;
            }
            
            if (!loginUsuario.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido");
                limparCamposSenha();
                return;
            }
            
            if (!loginUsuario.equals(loginOriginal)) {
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
    
    // Envia código de verificação por email quando usuário altera o email
    private void enviarCodigoVerificacao() {
        try {
            if (!loginUsuario.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido. Por favor, digite um email válido (ex: usuario@email.com)");
                limparCamposSenha();
                return;
            }
            
            if (!loginUsuario.equals(loginOriginal)) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                try {
                    if (usuarioDAO.loginExiste(loginUsuario)) {
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
            
            String nomeParaEmail = nomeUsuario;
            if (funcionarioPerfil && funcionarioModel != null && funcionarioModel.getNome() != null) {
                nomeParaEmail = funcionarioModel.getNome();
            }
            
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                loginUsuario, 
                nomeParaEmail, 
                codigoGerado
            );
            
            if (enviado) {
                emailAlterado = true;
                addMessage(FacesMessage.SEVERITY_INFO, "Código de verificação enviado para " + loginUsuario);
                String modalWidget = adminPerfil ? "dlgValidarEmailAdmin" : "dlgValidarCodigoGeral";
                PrimeFaces.current().executeScript("PF('" + modalWidget + "').show();");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código de verificação");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código: " + e.getMessage());
        }
    }
    
    // Valida o código de verificação enviado por email
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
            
            String modalWidget = adminPerfil ? "dlgValidarEmailAdmin" : "dlgValidarCodigoGeral";
            PrimeFaces.current().executeScript("PF('" + modalWidget + "').hide();");
            
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
    
    // Reenvia código de verificação por email
    public void reenviarCodigo() {
        try {
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            
            String nomeParaEmail = nomeUsuario;
            if (funcionarioPerfil && funcionarioModel != null && funcionarioModel.getNome() != null) {
                nomeParaEmail = funcionarioModel.getNome();
            }
            
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                loginUsuario, 
                nomeParaEmail, 
                codigoGerado
            );
            
            if (enviado) {
                addMessage(FacesMessage.SEVERITY_INFO, "Novo código enviado para " + loginUsuario);
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código: " + e.getMessage());
        }
    }
    
    // Finaliza o salvamento das configurações no banco de dados
    private void finalizarSalvamento() {
        try {
            if (funcionarioPerfil) {
                if (!validarDadosFuncionario()) {
                    return;
                }
                
                FuncionarioDAO.atualizar(funcionarioModel);
            }
            
            if (alterarSenha) {
                if (!validarAlteracaoSenha()) {
                    return;
                }
                
                usuarioModel.setSenha(novaSenha);
            }
            
            usuarioModel.setUser(nomeUsuario);
            usuarioModel.setLogin(loginUsuario);
            
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(usuarioModel);
            
            loginOriginal = loginUsuario;
            
            FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("usuarioLogado", usuarioModel);
            
            addMessage(FacesMessage.SEVERITY_INFO, "Configurações salvas com sucesso!");
            
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
            alterarSenha = false;
            codigoDigitado = null;
            codigoGerado = null;
            emailAlterado = false;
            
        } catch (java.sql.SQLException e) {
            if (e.getMessage().contains("Login já existe")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            }
            limparCamposSenha();
        } catch (Exception e) {
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            limparCamposSenha();
        }
    }
    
    private boolean validarDadosFuncionario() {
        // Validar Nome Completo
        if (funcionarioModel.getNome() == null || funcionarioModel.getNome().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nome completo é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar Telefone
        if (funcionarioModel.getTelefone() == null || funcionarioModel.getTelefone().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar Sexo
        if (funcionarioModel.getSexo() == null || funcionarioModel.getSexo().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar CEP
        if (funcionarioModel.getCep() == null || funcionarioModel.getCep().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "CEP é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar Cidade
        if (funcionarioModel.getCidade() == null || funcionarioModel.getCidade().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Cidade é obrigatória");
            limparCamposSenha();
            return false;
        }
        
        // Validar Estado/UF
        if (funcionarioModel.getEstado() == null || funcionarioModel.getEstado().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "UF é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar Rua
        if (funcionarioModel.getRua() == null || funcionarioModel.getRua().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Rua é obrigatória");
            limparCamposSenha();
            return false;
        }
        
        // Validar Número
        if (funcionarioModel.getNumero() == null || funcionarioModel.getNumero().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Número é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validar Bairro
        if (funcionarioModel.getBairro() == null || funcionarioModel.getBairro().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Bairro é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        return true;
    }
    
    private boolean validarAlteracaoSenha() {
        // Validação 1: Campo Senha Atual
        if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Campo 'Senha Atual' é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validação 2: Campo Nova Senha
        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Campo 'Nova Senha' é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validação 3: Campo Confirmar Nova Senha
        if (confirmaNovaSenha == null || confirmaNovaSenha.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Campo 'Confirmar Nova Senha' é obrigatório");
            limparCamposSenha();
            return false;
        }
        
        // Validação 4: Senha Atual Correta
        if (!usuarioModel.getSenha().equals(senhaAtual)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual incorreta");
            limparCamposSenha();
            return false;
        }
        
        // Validação 5: Tamanho mínimo da Nova Senha (8 caracteres)
        if (novaSenha.length() < 8) {
            addMessage(FacesMessage.SEVERITY_ERROR, "A nova senha deve ter no mínimo 8 caracteres");
            limparCamposSenha();
            return false;
        }
        
        // Validação 6: Nova Senha e Confirmação Coincidem
        if (!novaSenha.equals(confirmaNovaSenha)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Nova senha e confirmação não coincidem");
            limparCamposSenha();
            return false;
        }
        
        return true;
    }
    
    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, message, ""));
    }
    
    private void limparCamposSenha() {
        senhaAtual = null;
        novaSenha = null;
        confirmaNovaSenha = null;
        codigoDigitado = null;
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

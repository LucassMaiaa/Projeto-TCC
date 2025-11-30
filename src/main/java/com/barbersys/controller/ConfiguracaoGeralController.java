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
        System.out.println("=== INIT ConfiguracaoGeralController ===");
        try {
            carregarDadosUsuario();
            
            // Carregar nome de usuário e login
            if (usuarioModel != null) {
                this.nomeUsuario = usuarioModel.getUser();
                this.loginUsuario = usuarioModel.getLogin();
                this.loginOriginal = usuarioModel.getLogin(); // Guardar login original para comparação
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
        
        // Se desmarcou o checkbox, limpar os campos de senha
        if (!alterarSenha) {
            System.out.println("🧹 Limpando campos de senha...");
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        }
        
        // Adiciona o valor ao contexto de callback para uso no oncomplete do AJAX
        org.primefaces.PrimeFaces.current().ajax().addCallbackParam("alterarSenha", alterarSenha);
    }
    
    public void salvarConfiguracoes() {
        System.out.println("=== MÉTODO salvarConfiguracoes CHAMADO ===");
        try {
            // Validar nome de usuário
            if (nomeUsuario == null || nomeUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome de usuário é obrigatório");
                limparCamposSenha();
                return;
            }
            
            // Validar login (email)
            if (loginUsuario == null || loginUsuario.trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório");
                limparCamposSenha();
                return;
            }
            
            // Validar formato de email
            if (!loginUsuario.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido");
                limparCamposSenha();
                return;
            }
            
            System.out.println("Usuario ID: " + usuarioModel.getId());
            System.out.println("Nome Usuario: " + nomeUsuario);
            System.out.println("Login (Email): " + loginUsuario);
            System.out.println("Login Original: " + loginOriginal);
            System.out.println("É Admin: " + adminPerfil);
            System.out.println("É Funcionário: " + funcionarioPerfil);
            System.out.println("Alterar Senha: " + alterarSenha);
            
            // Verificar se o email foi alterado
            if (!loginUsuario.equals(loginOriginal)) {
                System.out.println("✉️ EMAIL ALTERADO! Enviando código de verificação...");
                enviarCodigoVerificacao();
                return; // Para aqui e espera validação do código
            }
            
            // Se não alterou email, continua normalmente
            finalizarSalvamento();
            
        } catch (Exception e) {
            System.out.println("❌ ERRO EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            limparCamposSenha();
        }
    }
    
    private void enviarCodigoVerificacao() {
        try {
            // Validar formato do email
            if (!loginUsuario.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido. Por favor, digite um email válido (ex: usuario@email.com)");
                limparCamposSenha();
                return;
            }
            
            // Verificar se o email já existe no sistema (apenas se foi alterado)
            if (!loginUsuario.equals(loginOriginal)) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                try {
                    if (usuarioDAO.loginExiste(loginUsuario)) {
                        addMessage(FacesMessage.SEVERITY_ERROR, "Este email já está cadastrado no sistema.");
                        limparCamposSenha();
                        return;
                    }
                } catch (java.sql.SQLException e) {
                    System.out.println("❌ ERRO ao verificar email: " + e.getMessage());
                    addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao verificar email: " + e.getMessage());
                    limparCamposSenha();
                    return;
                }
            }
            
            // Gerar código de 6 dígitos
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            System.out.println("🔐 Código gerado: " + codigoGerado);
            
            // Buscar nome para o email
            String nomeParaEmail = nomeUsuario;
            if (funcionarioPerfil && funcionarioModel != null && funcionarioModel.getNome() != null) {
                nomeParaEmail = funcionarioModel.getNome();
            }
            
            // Enviar email
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                loginUsuario, 
                nomeParaEmail, 
                codigoGerado
            );
            
            if (enviado) {
                emailAlterado = true;
                addMessage(FacesMessage.SEVERITY_INFO, "Código de verificação enviado para " + loginUsuario);
                System.out.println("✅ Email enviado com sucesso!");
                // Abrir modal correto dependendo do perfil
                String modalWidget = adminPerfil ? "dlgValidarEmailAdmin" : "dlgValidarCodigoGeral";
                PrimeFaces.current().executeScript("PF('" + modalWidget + "').show();");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código de verificação");
                System.out.println("❌ Falha ao enviar email");
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERRO ao enviar código: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao enviar código: " + e.getMessage());
        }
    }
    
    public void validarCodigo() {
        System.out.println("🔍 Validando código...");
        System.out.println("Código digitado: " + codigoDigitado);
        System.out.println("Código gerado: " + codigoGerado);
        
        if (codigoDigitado == null || codigoDigitado.trim().isEmpty()) {
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "error", "Digite o código de verificação"
            ));
            return;
        }
        
        if (codigoDigitado.trim().equals(codigoGerado)) {
            System.out.println("✅ Código VÁLIDO!");
            
            // Limpar código após validação bem-sucedida
            codigoDigitado = null;
            
            // Fechar modal correto dependendo do perfil
            String modalWidget = adminPerfil ? "dlgValidarEmailAdmin" : "dlgValidarCodigoGeral";
            PrimeFaces.current().executeScript("PF('" + modalWidget + "').hide();");
            
            // Mostrar mensagem de sucesso
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "success", "Código validado! Finalizando alterações..."
            ));
            
            // Finalizar salvamento
            finalizarSalvamento();
        } else {
            System.out.println("❌ Código INVÁLIDO!");
            PrimeFaces.current().executeScript(String.format(
                "Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
                "error", "Código inválido. Tente novamente."
            ));
        }
    }
    
    public void reenviarCodigo() {
        try {
            // Gerar novo código
            codigoGerado = String.format("%06d", new Random().nextInt(999999));
            System.out.println("🔐 Novo código gerado: " + codigoGerado);
            
            // Buscar nome para o email
            String nomeParaEmail = nomeUsuario;
            if (funcionarioPerfil && funcionarioModel != null && funcionarioModel.getNome() != null) {
                nomeParaEmail = funcionarioModel.getNome();
            }
            
            // Enviar email
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                loginUsuario, 
                nomeParaEmail, 
                codigoGerado
            );
            
            if (enviado) {
                addMessage(FacesMessage.SEVERITY_INFO, "Novo código enviado para " + loginUsuario);
                System.out.println("✅ Novo código enviado!");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código");
                System.out.println("❌ Falha ao reenviar código");
            }
            
        } catch (Exception e) {
            System.out.println("❌ ERRO ao reenviar código: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao reenviar código: " + e.getMessage());
        }
    }
    
    private void finalizarSalvamento() {
        try {
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
                System.out.println("Senha será atualizada!");
            }
            
            // Atualizar nome de usuário (usu_user) e login (usu_login/email)
            usuarioModel.setUser(nomeUsuario);
            usuarioModel.setLogin(loginUsuario);
            
            System.out.println("🔄 Atualizando usuário no banco...");
            System.out.println("   - ID: " + usuarioModel.getId());
            System.out.println("   - User: " + usuarioModel.getUser());
            System.out.println("   - Login: " + usuarioModel.getLogin());
            System.out.println("   - Senha será alterada: " + alterarSenha);
            
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(usuarioModel);
            System.out.println("✅ Usuário atualizado no banco!");
            
            // Atualizar o login original para a próxima vez
            loginOriginal = loginUsuario;
            
            // Atualizar sessão com os novos dados
            FacesContext.getCurrentInstance().getExternalContext()
                .getSessionMap().put("usuarioLogado", usuarioModel);
            System.out.println("✅ Sessão atualizada!");
            
            addMessage(FacesMessage.SEVERITY_INFO, "Configurações salvas com sucesso!");
            
            // Limpar campos de senha e código APENAS EM CASO DE SUCESSO
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
            alterarSenha = false;
            codigoDigitado = null;
            codigoGerado = null; // Só limpa após sucesso
            emailAlterado = false;
            
            System.out.println("=== SUCESSO ===");
            
        } catch (java.sql.SQLException e) {
            System.out.println("❌ ERRO SQL: " + e.getMessage());
            // Tratar erro de login duplicado
            if (e.getMessage().contains("Login já existe")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            }
            limparCamposSenha();
        } catch (Exception e) {
            System.out.println("❌ ERRO EXCEPTION: " + e.getMessage());
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

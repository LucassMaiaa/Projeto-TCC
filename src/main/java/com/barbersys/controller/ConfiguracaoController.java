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
        System.out.println("=== INIT ConfiguracaoController ===");
        try {
            carregarDadosCliente();
            
            // Carregar nome de usuário (usu_user) e email
            if (clienteModel != null && clienteModel.getUsuario() != null) {
                System.out.println("📝 Carregando dados do usuário...");
                System.out.println("   - Usuario ID: " + clienteModel.getUsuario().getId());
                System.out.println("   - usu_user: " + clienteModel.getUsuario().getUser());
                System.out.println("   - usu_login: " + clienteModel.getUsuario().getLogin());
                
                // Nome de usuário vem de usu_user
                this.nomeUsuario = clienteModel.getUsuario().getUser();
                System.out.println("✅ Nome Usuario carregado: " + this.nomeUsuario);
                
                // Email deve vir do campo usu_login do usuário
                if (clienteModel.getUsuario().getLogin() != null) {
                    clienteModel.setEmail(clienteModel.getUsuario().getLogin());
                    this.emailOriginal = clienteModel.getUsuario().getLogin(); // Guardar email original
                    System.out.println("✅ Email carregado: " + clienteModel.getEmail());
                }
            } else {
                System.out.println("⚠️ ClienteModel ou Usuario é NULL!");
            }
            
            // Se não carregou, inicializa um cliente vazio para evitar null
            if (clienteModel == null) {
                System.out.println("❌ Cliente Model é NULL - inicializando vazio");
                clienteModel = new Cliente();
                clienteModel.setUsuario(new Usuario());
            } else {
                System.out.println("✅ Cliente carregado: " + clienteModel.getNome());
                System.out.println("   - Sexo: " + clienteModel.getSexo());
                System.out.println("   - Email: " + clienteModel.getEmail());
                System.out.println("   - Nome Usuario: " + this.nomeUsuario);
            }
        } catch (Exception e) {
            System.out.println("❌ ERRO no INIT: " + e.getMessage());
            e.printStackTrace();
            clienteModel = new Cliente();
            clienteModel.setUsuario(new Usuario());
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
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getNome() == null || clienteModel.getNome().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Nome é obrigatório");
                System.out.println("ERRO: Nome vazio");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getEmail() == null || clienteModel.getEmail().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email é obrigatório");
                System.out.println("ERRO: Email vazio");
                limparCamposSenha();
                return;
            }
            
            // Validação de formato de email
            if (!clienteModel.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido");
                System.out.println("ERRO: Formato de email inválido");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getTelefone() == null || clienteModel.getTelefone().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Telefone é obrigatório");
                System.out.println("ERRO: Telefone vazio");
                limparCamposSenha();
                return;
            }
            
            if (clienteModel.getSexo() == null || clienteModel.getSexo().trim().isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Sexo é obrigatório");
                System.out.println("ERRO: Sexo vazio");
                limparCamposSenha();
                return;
            }
            
            System.out.println("Cliente ID: " + clienteModel.getId());
            System.out.println("Cliente Nome: " + clienteModel.getNome());
            System.out.println("Email atual: " + clienteModel.getEmail());
            System.out.println("Email original: " + emailOriginal);
            System.out.println("Alterar Senha: " + alterarSenha);
            
            // Verificar se o email foi alterado
            if (!clienteModel.getEmail().equals(emailOriginal)) {
                System.out.println("✉️ EMAIL ALTERADO! Enviando código de verificação...");
                enviarCodigoVerificacao();
                return; // Para aqui e espera validação do código
            }
            
            // Se não alterou email, continua normalmente
            finalizarSalvamento();
            
        } catch (Exception e) {
            System.out.println("ERRO EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            limparCamposSenha();
        }
    }
    
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
        
        // Validação de tamanho mínimo de senha (8 caracteres)
        if (novaSenha.length() < 8) {
            addMessage(FacesMessage.SEVERITY_ERROR, "A nova senha deve ter no mínimo 8 caracteres");
            System.out.println("ERRO: Senha com menos de 8 caracteres");
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
        
        // Verificar se a senha atual está correta
        if (!clienteModel.getUsuario().getSenha().equals(senhaAtual)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Senha atual incorreta");
            limparCamposSenha();
            return false;
        }
        
        return true;
    }
    
    private void enviarCodigoVerificacao() {
        try {
            // Validar formato do email
            if (!clienteModel.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Email inválido. Por favor, digite um email válido (ex: usuario@email.com)");
                limparCamposSenha();
                return;
            }
            
            // Verificar se o email já existe no sistema (apenas se foi alterado)
            if (!clienteModel.getEmail().equals(emailOriginal)) {
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                try {
                    if (usuarioDAO.loginExiste(clienteModel.getEmail())) {
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
            
            // Enviar email
            EmailService emailService = new EmailService();
            boolean enviado = emailService.enviarCodigoVerificacao(
                clienteModel.getEmail(), 
                clienteModel.getNome(), 
                codigoGerado
            );
            
            if (enviado) {
                emailAlterado = true;
                addMessage(FacesMessage.SEVERITY_INFO, "Código de verificação enviado para " + clienteModel.getEmail());
                System.out.println("✅ Email enviado com sucesso!");
                // Abrir modal
                PrimeFaces.current().executeScript("PF('dlgValidarCodigoCliente').show();");
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
            
            // Fechar modal
            PrimeFaces.current().executeScript("PF('dlgValidarCodigoCliente').hide();");
            
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
            
            // Enviar email
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
            System.out.println("========== INICIANDO SALVAMENTO ==========");
            System.out.println("Nome Usuario: " + nomeUsuario);
            System.out.println("Nome Cliente: " + clienteModel.getNome());
            System.out.println("Email: " + clienteModel.getEmail());
            System.out.println("Telefone: " + clienteModel.getTelefone());
            System.out.println("Sexo: " + clienteModel.getSexo());
            System.out.println("Alterar Senha: " + alterarSenha);
            
            // Validar alteração de senha se solicitado
            if (alterarSenha) {
                System.out.println("Validando alteração de senha...");
                if (!validarAlteracaoSenha()) {
                    return;
                }
                
                // Atualizar senha
                System.out.println("Atualizando senha...");
                clienteModel.getUsuario().setSenha(novaSenha);
                System.out.println("Senha será atualizada!");
            }
            
            // Atualizar nome de usuário (usu_user)
            clienteModel.getUsuario().setUser(nomeUsuario);
            
            // Atualizar email (usu_login)
            clienteModel.getUsuario().setLogin(clienteModel.getEmail());
            
            System.out.println("🔄 Atualizando usuário...");
            System.out.println("   - Usuario ID: " + clienteModel.getUsuario().getId());
            System.out.println("   - User: " + clienteModel.getUsuario().getUser());
            System.out.println("   - Login: " + clienteModel.getUsuario().getLogin());
            
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            usuarioDAO.atualizar(clienteModel.getUsuario());
            System.out.println("✅ Usuário atualizado!");
            
            // Atualizar o email original para a próxima vez
            emailOriginal = clienteModel.getEmail();
            
            // Atualizar o usuário na sessão
            Usuario usuarioLogado = (Usuario) FacesContext.getCurrentInstance()
                    .getExternalContext().getSessionMap().get("usuarioLogado");
            if (usuarioLogado != null) {
                usuarioLogado.setUser(nomeUsuario);
                usuarioLogado.setLogin(clienteModel.getEmail());
                FacesContext.getCurrentInstance().getExternalContext()
                        .getSessionMap().put("usuarioLogado", usuarioLogado);
                System.out.println("✅ Sessão atualizada!");
            }
            
            // Salvar dados do cliente
            System.out.println("🔄 Atualizando cliente...");
            System.out.println("   - Cliente ID: " + clienteModel.getId());
            System.out.println("   - Nome: " + clienteModel.getNome());
            System.out.println("   - Telefone: " + clienteModel.getTelefone());
            System.out.println("   - Sexo: " + clienteModel.getSexo());
            System.out.println("   - CEP: " + clienteModel.getCep());
            System.out.println("   - Cidade: " + clienteModel.getCidade());
            
            ClienteDAO.atualizar(clienteModel);
            System.out.println("✅ Cliente atualizado com sucesso!");
            
            addMessage(FacesMessage.SEVERITY_INFO, "Configurações salvas com sucesso!");
            
            // Limpar campos de senha e código
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
            alterarSenha = false;
            codigoDigitado = null;
            codigoGerado = null;
            emailAlterado = false;
            
            System.out.println("========== SALVAMENTO CONCLUÍDO ==========");
            
        } catch (java.sql.SQLException e) {
            System.out.println("❌ ERRO SQL: " + e.getMessage());
            e.printStackTrace();
            // Tratar erro de login duplicado
            if (e.getMessage().contains("Login já existe")) {
                addMessage(FacesMessage.SEVERITY_ERROR, "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
            } else {
                addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            }
            // Limpar campos de senha
            senhaAtual = null;
            novaSenha = null;
            confirmaNovaSenha = null;
        } catch (Exception e) {
            System.out.println("❌ ERRO EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar configurações: " + e.getMessage());
            // Limpar campos de senha
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

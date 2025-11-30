package com.barbersys.controller;

import java.util.List;
import java.util.Map;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Cliente;
import com.barbersys.model.Perfil;
import com.barbersys.model.Usuario;
import com.barbersys.util.EmailService;

import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class ClienteController {

	private String nomeCliente;
	private String statusSelecionado = "";
	private Cliente clienteModel = new Cliente();
	private LazyDataModel<Cliente> lstCliente;
	private String editarModel;
	
	private String loginOriginal;
	private String codigoValidacao;
	private String codigoGerado;
	private boolean aguardandoValidacao = false;
	private String confirmarSenha; // Campo auxiliar para confirmar senha

	@PostConstruct
	public void init() {
		lstCliente = new LazyDataModel<Cliente>() {
            private static final long serialVersionUID = 1L;

			@Override
			public List<Cliente> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return ClienteDAO.buscarCliente(nomeCliente, statusSelecionado, first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return ClienteDAO.clienteCount(nomeCliente, statusSelecionado);
			}

		};
	}
    
    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 4000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

	public void clienteSelecionado(Cliente event) {
		// SEMPRE recarrega do banco para evitar dados antigos
		if (event != null && event.getId() != null) {
			try {
				clienteModel = ClienteDAO.buscarPorId(event.getId());
				if (clienteModel.getUsuario() == null) {
					clienteModel.setUsuario(new Usuario());
				}
				editarModel = "A";
				loginOriginal = clienteModel.getUsuario().getLogin();
				aguardandoValidacao = false;
			} catch (Exception e) {
				e.printStackTrace();
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao carregar cliente", "Erro!"));
			}
		}
	}

	public void novoCliente() {
		editarModel = "I";
		clienteModel = new Cliente();
        clienteModel.setUsuario(new Usuario());
		loginOriginal = null;
		aguardandoValidacao = false;
		confirmarSenha = null;
	}
	
	public void cancelarCliente() {
		// SEMPRE recarrega do banco ou limpa
		try {
			if (clienteModel != null && clienteModel.getId() != null) {
				// Recarrega do banco para descartar alterações
				clienteModel = ClienteDAO.buscarPorId(clienteModel.getId());
				if (clienteModel != null && clienteModel.getUsuario() != null) {
					loginOriginal = clienteModel.getUsuario().getLogin();
				}
			} else {
				// Limpa o modelo
				clienteModel = new Cliente();
				clienteModel.setUsuario(new Usuario());
				loginOriginal = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// Em caso de erro, limpa tudo
			clienteModel = new Cliente();
			clienteModel.setUsuario(new Usuario());
			loginOriginal = null;
		}
		
		aguardandoValidacao = false;
		codigoValidacao = "";
		codigoGerado = "";
		confirmarSenha = null;
	}
	
	public void prepararSalvarCliente() {
		// Valida campos obrigatórios ANTES de tentar enviar email
		if (!validarCamposCliente()) {
			return;
		}
		
		String loginAtual = clienteModel.getUsuario().getLogin();
		
		// Verifica se é novo cliente OU se o login foi alterado
		boolean loginAlterado = loginOriginal == null || !loginAtual.equals(loginOriginal);
		
		if (editarModel.equals("I") || (editarModel.equals("A") && loginAlterado)) {
			// Precisa validar email
			enviarCodigoValidacaoCliente();
		} else {
			// Não precisa validar, salva direto
			atualizarCliente();
		}
	}
	
	private void enviarCodigoValidacaoCliente() {
		try {
			String email = clienteModel.getUsuario().getLogin();
			
			// Valida se o email tem formato válido
			if (email == null || email.trim().isEmpty() || !email.contains("@") || !email.contains(".")) {
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, 
						"Email inválido. Por favor, digite um email válido (ex: usuario@email.com)", "Erro!"));
				return;
			}
			
			// Verifica se o email já existe no sistema (apenas se for um novo cliente ou se mudou o email)
			UsuarioDAO usuarioDAO = new UsuarioDAO();
			if (editarModel.equals("I") || !email.equals(loginOriginal)) {
				try {
					if (usuarioDAO.loginExiste(email)) {
						FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, 
								"Este email já está cadastrado no sistema.", "Erro!"));
						return;
					}
				} catch (SQLException e) {
					FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, 
							"Erro ao verificar email: " + e.getMessage(), "Erro!"));
					return;
				}
			}
			
			codigoGerado = String.format("%06d", (int)(Math.random() * 1000000));
			
			EmailService emailService = new EmailService();
			String nomeCliente = clienteModel.getNome() != null && !clienteModel.getNome().isEmpty() 
				? clienteModel.getNome() : "Usuário";
			
			boolean emailEnviado = emailService.enviarEmailValidacao(email, nomeCliente, codigoGerado);
			
			if (emailEnviado) {
				aguardandoValidacao = true;
				codigoValidacao = "";
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Código enviado para " + email, "Sucesso!"));
				PrimeFaces.current().executeScript("PF('dlgValidarEmailCliente').show();");
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, 
						"Não foi possível enviar o email. Verifique se o endereço está correto e tente novamente.", "Erro!"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null,
				new FacesMessage(FacesMessage.SEVERITY_ERROR, 
					"Erro ao enviar código. Verifique o email e tente novamente.", "Erro!"));
		}
	}
	
	public void validarCodigoCliente() {
		System.out.println("🔍 Validando código do CLIENTE...");
		System.out.println("📝 Código digitado: " + codigoValidacao);
		System.out.println("✅ Código esperado: " + codigoGerado);
		
		if (codigoValidacao == null || codigoValidacao.trim().isEmpty()) {
			exibirAlerta("error", "Código é obrigatório");
			System.out.println("❌ Código vazio!");
			// NÃO FECHA NADA - retorna para o usuário tentar novamente
			return;
		}
		
		if (codigoValidacao != null && codigoValidacao.equals(codigoGerado)) {
			System.out.println("✅ Código correto! Salvando cliente...");
			aguardandoValidacao = false;
			
			// Limpa o código após validação bem-sucedida
			codigoValidacao = null;
			codigoGerado = null;
			
			// Salva o cliente (só fecha modais SE SALVAR COM SUCESSO)
			if (editarModel.equals("I")) {
				adicionarNovoCliente();
			} else {
				atualizarCliente();
			}
		} else {
			System.out.println("❌ Código incorreto!");
			exibirAlerta("error", "Código incorreto! Tente novamente.");
			// NÃO FECHA NADA - mantém ambos os modais abertos
			// Não retorna, não fecha, não faz nada - apenas mostra o erro
		}
	}
	
	public void reenviarCodigoCliente() {
		enviarCodigoValidacaoCliente();
		exibirAlerta("info", "Código reenviado para " + clienteModel.getUsuario().getLogin());
	}

	
	private boolean validarCamposCliente() {
		// Login
		if (clienteModel.getUsuario() == null || clienteModel.getUsuario().getLogin() == null || clienteModel.getUsuario().getLogin().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login é obrigatório", "Erro!"));
			return false;
		}
		
		// Senha (apenas para novo cliente)
		if ("I".equals(editarModel)) {
			if (clienteModel.getUsuario().getSenha() == null || clienteModel.getUsuario().getSenha().trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Senha é obrigatório", "Erro!"));
				return false;
			}
			
			// Validação de tamanho mínimo
			if (clienteModel.getUsuario().getSenha().length() < 8) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "A senha deve ter no mínimo 8 caracteres", "Erro!"));
				return false;
			}
			
			// Validação de senhas iguais
			if (confirmarSenha == null || !clienteModel.getUsuario().getSenha().equals(confirmarSenha)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "As senhas não conferem. Digite senhas iguais nos dois campos.", "Erro!"));
				return false;
			}
		}
		
		// Para edição, se a senha foi preenchida, valida
		if ("A".equals(editarModel) && clienteModel.getUsuario().getSenha() != null && !clienteModel.getUsuario().getSenha().trim().isEmpty()) {
			// Validação de tamanho mínimo
			if (clienteModel.getUsuario().getSenha().length() < 8) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "A senha deve ter no mínimo 8 caracteres", "Erro!"));
				return false;
			}
			
			// Validação de senhas iguais
			if (confirmarSenha == null || !clienteModel.getUsuario().getSenha().equals(confirmarSenha)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "As senhas não conferem. Digite senhas iguais nos dois campos.", "Erro!"));
				return false;
			}
		}
		
		// Nome Completo
		if (clienteModel.getNome() == null || clienteModel.getNome().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Nome Completo é obrigatório", "Erro!"));
			return false;
		}
		
		// CPF
		if (clienteModel.getCpf() == null || clienteModel.getCpf().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo CPF é obrigatório", "Erro!"));
			return false;
		}
		
		// Data de Nascimento
		if (clienteModel.getDataNascimento() == null) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Data de Nascimento é obrigatório", "Erro!"));
			return false;
		}
		
		// Telefone
		if (clienteModel.getTelefone() == null || clienteModel.getTelefone().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Telefone é obrigatório", "Erro!"));
			return false;
		}
		
		// Sexo
		if (clienteModel.getSexo() == null || clienteModel.getSexo().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Sexo é obrigatório", "Erro!"));
			return false;
		}
		
		return true;
	}
	
	public void adicionarNovoCliente() {
		System.out.println("💾 Iniciando salvamento do cliente...");
		try {
			System.out.println("📝 Salvando usuário...");
            // Salvar o usuário primeiro
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Perfil perfil = new Perfil();
            perfil.setId(3L); // 3 para cliente
            clienteModel.getUsuario().setPerfil(perfil);
            
            // Define o usu_user como o nome completo do cliente
            clienteModel.getUsuario().setUser(clienteModel.getNome());
            
            Usuario usuarioSalvo = usuarioDAO.salvar(clienteModel.getUsuario());
            clienteModel.setUsuario(usuarioSalvo);

			ClienteDAO.salvar(clienteModel);

			exibirAlerta("success", "Cliente cadastrado com sucesso!");

			// SÓ FECHA OS MODAIS SE CHEGOU AQUI (SUCESSO TOTAL)
			PrimeFaces.current().executeScript("PF('dlgValidarEmailCliente').hide();");
			PrimeFaces.current().executeScript("PF('dlgCli').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (SQLException e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao salvar cliente: " + e.getMessage(), "Erro!"));
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
		} catch (Exception e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar cliente: " + e.getMessage(), "Erro!"));
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
		}
	}

	public void atualizarCliente() {
		try {
			// Atualiza o usu_user com o nome completo do cliente
			if (clienteModel.getUsuario() != null) {
				clienteModel.getUsuario().setUser(clienteModel.getNome());
			}
			
            ClienteDAO.atualizar(clienteModel);
            exibirAlerta("success", "Cliente editado com sucesso!");
            
            // SÓ FECHA OS MODAIS SE CHEGOU AQUI (SUCESSO TOTAL)
            PrimeFaces.current().executeScript("PF('dlgValidarEmailCliente').hide();");
            PrimeFaces.current().executeScript("PF('dlgCli').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao atualizar cliente: " + e.getMessage(), "Erro!"));
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao atualizar cliente: " + e.getMessage(), "Erro!"));
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
        }

	}

	public void deletaCliente() {
		try {
            ClienteDAO.deletar(clienteModel);

            exibirAlerta("success", "Cliente inativado com sucesso!");

            PrimeFaces.current().executeScript("PF('dlgClic').hide();");
            PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao inativar cliente!");
        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro inesperado ao deletar cliente: " + e.getMessage());
        }
	}
	
	public java.util.Date getHoje() {
		return new java.util.Date();
	}
	
	// Getter e Setter para confirmarSenha
	public String getConfirmarSenha() {
		return confirmarSenha;
	}
	
	public void setConfirmarSenha(String confirmarSenha) {
		this.confirmarSenha = confirmarSenha;
	}

}
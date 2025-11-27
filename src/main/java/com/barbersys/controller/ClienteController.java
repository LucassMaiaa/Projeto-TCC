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
		clienteModel = event;
        if (clienteModel.getUsuario() == null) {
            clienteModel.setUsuario(new Usuario());
        }
		editarModel = "A";
		loginOriginal = clienteModel.getUsuario().getLogin();
		aguardandoValidacao = false;
	}

	public void novoCliente() {
		editarModel = "I";
		clienteModel = new Cliente();
        clienteModel.setUsuario(new Usuario());
		loginOriginal = null;
		aguardandoValidacao = false;
	}
	
	public void prepararSalvarCliente() {
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
			codigoGerado = String.format("%06d", (int)(Math.random() * 1000000));
			
			EmailService emailService = new EmailService();
			String nomeCliente = clienteModel.getNome() != null && !clienteModel.getNome().isEmpty() 
				? clienteModel.getNome() : "Usuário";
			
			boolean emailEnviado = emailService.enviarEmailValidacao(email, nomeCliente, codigoGerado);
			
			if (emailEnviado) {
				aguardandoValidacao = true;
				codigoValidacao = "";
				exibirAlerta("info", "Código enviado para " + email);
				PrimeFaces.current().executeScript("PF('dlgValidarEmailCliente').show();");
			} else {
				exibirAlerta("error", "Erro ao enviar código de validação");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao enviar código: " + e.getMessage());
		}
	}
	
	public void validarCodigoCliente() {
		System.out.println("🔍 Validando código do CLIENTE...");
		System.out.println("📝 Código digitado: " + codigoValidacao);
		System.out.println("✅ Código esperado: " + codigoGerado);
		
		if (codigoValidacao == null || codigoValidacao.trim().isEmpty()) {
			exibirAlerta("error", "Código é obrigatório");
			System.out.println("❌ Código vazio!");
			return;
		}
		
		if (codigoValidacao != null && codigoValidacao.equals(codigoGerado)) {
			System.out.println("✅ Código correto! Salvando cliente...");
			aguardandoValidacao = false;
			
			// Limpa o código após validação bem-sucedida
			codigoValidacao = null;
			codigoGerado = null;
			
			// Fecha o dialog de validação
			PrimeFaces.current().executeScript("PF('dlgValidarEmailCliente').hide();");
			
			// Salva o cliente
			if (editarModel.equals("I")) {
				adicionarNovoCliente();
			} else {
				atualizarCliente();
			}
		} else {
			System.out.println("❌ Código incorreto!");
			exibirAlerta("error", "Código incorreto! Tente novamente.");
		}
	}
	
	public void reenviarCodigoCliente() {
		enviarCodigoValidacaoCliente();
	}

	public void adicionarNovoCliente() {
		System.out.println("💾 Iniciando salvamento do cliente...");
		try {
			if (clienteModel.getNome().isEmpty()) {
				System.out.println("❌ Nome vazio!");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente obrigatório", "Erro!"));
				return;
			}
			if (clienteModel.getTelefone().isEmpty()) {
				System.out.println("❌ Telefone vazio!");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo telefone obrigatório", "Erro!"));
				return;
			}
			if (clienteModel.getCpf().isEmpty()) {
				System.out.println("❌ CPF vazio!");
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo CPF obrigatório", "Erro!"));
				return;
			}
            if (clienteModel.getUsuario().getLogin() == null || clienteModel.getUsuario().getLogin().isEmpty()) {
				System.out.println("❌ Login vazio!");
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login do usuário obrigatório", "Erro!"));
                return;
            }
            if (clienteModel.getUsuario().getSenha() == null || clienteModel.getUsuario().getSenha().isEmpty()) {
				System.out.println("❌ Senha vazia!");
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Senha do usuário obrigatório", "Erro!"));
                return;
            }

			System.out.println("📝 Salvando usuário...");
            // Salvar o usuário primeiro
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Perfil perfil = new Perfil();
            perfil.setId(3L); // 3 para cliente
            clienteModel.getUsuario().setPerfil(perfil);
            Usuario usuarioSalvo = usuarioDAO.salvar(clienteModel.getUsuario());
            clienteModel.setUsuario(usuarioSalvo);

			ClienteDAO.salvar(clienteModel);

			exibirAlerta("success", "Cliente cadastrado com sucesso!");

			PrimeFaces.current().executeScript("PF('dlgClic').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (SQLException e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao salvar cliente: " + e.getMessage(), "Erro!"));
		} catch (Exception e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar cliente: " + e.getMessage(), "Erro!"));
		}
	}

	public void atualizarCliente() {
		try {
            if (clienteModel.getNome().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente obrigatório", "Erro!"));
                return;
            }
            if (clienteModel.getTelefone().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo telefone obrigatório", "Erro!"));
                return;
            }
            if (clienteModel.getCpf().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo CPF obrigatório", "Erro!"));
                return;
            }
            if (clienteModel.getUsuario().getLogin() == null || clienteModel.getUsuario().getLogin().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login do usuário obrigatório", "Erro!"));
                return;
            }

            ClienteDAO.atualizar(clienteModel);
            exibirAlerta("success", "Cliente editado com sucesso!");
            PrimeFaces.current().executeScript("PF('dlgClic').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao atualizar cliente: " + e.getMessage(), "Erro!"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao atualizar cliente: " + e.getMessage(), "Erro!"));
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

}
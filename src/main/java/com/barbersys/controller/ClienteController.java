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

import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class ClienteController {

	private String nomeCliente;
	private Cliente clienteModel = new Cliente();
	private LazyDataModel<Cliente> lstCliente;
	private String editarModel;

	@PostConstruct
	public void init() {
		lstCliente = new LazyDataModel<Cliente>() {
            private static final long serialVersionUID = 1L;

			@Override
			public List<Cliente> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return ClienteDAO.buscarCliente(nomeCliente, first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return ClienteDAO.clienteCount(nomeCliente);
			}

		};
	}
    
    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

	public void clienteSelecionado(Cliente event) {
		clienteModel = event;
        if (clienteModel.getUsuario() == null) {
            clienteModel.setUsuario(new Usuario());
        }
		editarModel = "A";
	}

	public void novoCliente() {
		editarModel = "I";
		clienteModel = new Cliente();
        clienteModel.setUsuario(new Usuario());

	}

	public void adicionarNovoCliente() {
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
            if (clienteModel.getUsuario().getSenha() == null || clienteModel.getUsuario().getSenha().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Senha do usuário obrigatório", "Erro!"));
                return;
            }

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

            exibirAlerta("success", "Cliente deletado com sucesso!");

            PrimeFaces.current().executeScript("PF('dlgClic').hide();");
            PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao deletar cliente: " + e.getMessage(), "Erro!"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao deletar cliente: " + e.getMessage(), "Erro!"));
        }
	}

}
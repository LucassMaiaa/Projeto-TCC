package com.barbersys.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.model.Cliente;

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

	public void clienteSelecionado(Cliente event) {
		clienteModel = event;
		editarModel = "A";
	}

	public void novoCliente() {
		editarModel = "I";
		clienteModel = new Cliente();

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

			ClienteDAO.salvar(clienteModel);

			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'success',"
							+ "  title: '<span style=\"font-size: 14px\">Cliente cadastrado com sucesso!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

			PrimeFaces.current().executeScript("PF('dlgClic').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar cliente: " + e.getMessage(), "Erro!"));
		}
	}

	public void atualizarCliente() {
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

		ClienteDAO.atualizar(clienteModel);
		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Cliente editado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
		PrimeFaces.current().executeScript("PF('dlgClic').hide();");
		PrimeFaces.current().ajax().update("form");

	}

	public void deletaCliente() {
		ClienteDAO.deletar(clienteModel);

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Cliente deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

		PrimeFaces.current().executeScript("PF('dlgClic').hide();");
		PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
		PrimeFaces.current().ajax().update("form");
	}

}
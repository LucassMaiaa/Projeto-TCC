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

import com.barbersys.dao.ServicosDAO;
import com.barbersys.model.Servicos;

import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class ServicosController {

	private String nomeServicos;
	private String statusSelecionado = "";
	private Servicos servicosModel = new Servicos();
	private LazyDataModel<Servicos> lstServicos;
	private String editarModel;

	@PostConstruct
	public void init() {
		lstServicos = new LazyDataModel<Servicos>() {

			@Override
			public List<Servicos> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return ServicosDAO.buscarServico(nomeServicos, statusSelecionado , first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return ServicosDAO.servicosCount(nomeServicos, statusSelecionado);
			}

		};
	}

	public void servicoSelecionado(Servicos event) {
		servicosModel = event;
		editarModel = "A";
	}

	public void novoServico() {
		editarModel = "I";
		servicosModel = new Servicos();
	}

	public void adicionarNovoServico() {
		try {
			if (servicosModel.getNome().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do serviço obrigatório", "Erro!"));
				return;
			}
			if (servicosModel.getMinutos() == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo tempo estimado do serviço obrigatório", "Erro!"));
				return;
			}
			if (servicosModel.getPreco() == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo preço do serviço obrigatório", "Erro!"));
				return;
			}

			ServicosDAO.salvar(servicosModel);

			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'success',"
							+ "  title: '<span style=\"font-size: 14px\">Serviço cadastrado com sucesso!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

			PrimeFaces.current().executeScript("PF('dlgService').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (Exception e) {
			System.out.println("Erro inesperado ao salvar serviço: " + e.getMessage());
		}
	}

	public void atualizarServico() {
		if (servicosModel.getNome().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do serviço obrigatório", "Erro!"));
			return;
		}
		if (servicosModel.getMinutos() == null) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo tempo estimado do serviço obrigatório", "Erro!"));
			return;
		}
		if (servicosModel.getPreco() == null) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo preço do serviço obrigatório", "Erro!"));
			return;
		}

		ServicosDAO.atualizar(servicosModel);
		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Serviço editado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
		PrimeFaces.current().executeScript("PF('dlgService').hide();");
		PrimeFaces.current().ajax().update("form");

	}

	public void deletaServico() {
		ServicosDAO.deletar(servicosModel);

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Serviço deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

		PrimeFaces.current().executeScript("PF('dlgService').hide();");
		PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
		PrimeFaces.current().ajax().update("form");
	}

}



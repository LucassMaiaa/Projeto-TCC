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

import com.barbersys.dao.PagamentoDAO;
import com.barbersys.model.Pagamento;

import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class PagamentoController {

	private String nomePagamento;
	private String statusSelecionado = "";
	private Pagamento pagamentoModel = new Pagamento();
	private LazyDataModel<Pagamento> lstPagamento;
	private String editarModel;

	@PostConstruct
	public void init() {
		lstPagamento = new LazyDataModel<Pagamento>() {

			@Override
			public List<Pagamento> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return PagamentoDAO.buscarPagamento(nomePagamento, statusSelecionado , first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return PagamentoDAO.pagamentoCount(nomePagamento, statusSelecionado);
			}

		};
	}

	public void pagamentoSelecionado(Pagamento event) {
		pagamentoModel = event;
		editarModel = "A";
	}

	public void novoPagamento() {
		editarModel = "I";
		pagamentoModel = new Pagamento();

	}

	public void adicionarNovoPagamento() {
		try {
			if (pagamentoModel.getNome().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do pagamento obrigatório", "Erro!"));
				return;
			}

			PagamentoDAO.salvar(pagamentoModel);

			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'success',"
							+ "  title: '<span style=\"font-size: 14px\">Pagamento cadastrado com sucesso!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

			PrimeFaces.current().executeScript("PF('dlgPag').hide();");
			PrimeFaces.current().ajax().update("form");

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar pagamento: " + e.getMessage(), "Erro!"));
		}
	}

	public void atualizarPagamento() {
		if (pagamentoModel.getNome().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo nome do cliente obrigatório", "Erro!"));
			return;
		}

		PagamentoDAO.atualizar(pagamentoModel);
		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Pagamento editado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
		PrimeFaces.current().executeScript("PF('dlgPag').hide();");
		PrimeFaces.current().ajax().update("form");

	}

	public void deletaPagamento() {
		PagamentoDAO.deletar(pagamentoModel);

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Pagamento deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

		PrimeFaces.current().executeScript("PF('dlgPag').hide();");
		PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
		PrimeFaces.current().ajax().update("form");
	}

}


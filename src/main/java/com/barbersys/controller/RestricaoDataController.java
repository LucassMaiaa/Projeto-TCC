package com.barbersys.controller;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.RestricaoDataDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.RestricaoData;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class RestricaoDataController implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String descricaoFiltro;
	private RestricaoData restricaoModel = new RestricaoData();
	private LazyDataModel<RestricaoData> lstRestricoes;
	private List<Funcionario> lstFuncionario;
	private String editarModel;
	private Long funcionarioId;
	
	@PostConstruct
	public void init() {
		lstRestricoes = new LazyDataModel<RestricaoData>() {
			private static final long serialVersionUID = 1L;

			@Override
			public List<RestricaoData> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return RestricaoDataDAO.buscarRestricoes(descricaoFiltro, first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return RestricaoDataDAO.restricaoCount(descricaoFiltro);
			}
		};
		
		lstFuncionario = FuncionarioDAO.buscarTodosFuncionarios();
	}
	
	private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}
	
	public void restricaoSelecionada(RestricaoData event) {
		restricaoModel = event;
		editarModel = "A";
		if (restricaoModel.getFuncionario() != null) {
			funcionarioId = restricaoModel.getFuncionario().getId();
		} else {
			funcionarioId = null;
		}
	}
	
	public void novaRestricao() {
		editarModel = "I";
		restricaoModel = new RestricaoData();
		restricaoModel.setTipo("G"); // Padrão: Geral
		funcionarioId = null;
	}
	
	public void adicionarNovaRestricao() {
		try {
			if (restricaoModel.getData() == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo data é obrigatório", "Erro!"));
				return;
			}
			
			if (restricaoModel.getDescricao() == null || restricaoModel.getDescricao().trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo descrição é obrigatório", "Erro!"));
				return;
			}
			
			if ("F".equals(restricaoModel.getTipo()) && (funcionarioId == null || funcionarioId == 0)) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione um funcionário", "Erro!"));
				return;
			}
			
			// Buscar funcionário se tipo for específico
			if ("F".equals(restricaoModel.getTipo()) && funcionarioId != null && funcionarioId > 0) {
				Funcionario func = lstFuncionario.stream()
					.filter(f -> f.getId().equals(funcionarioId))
					.findFirst()
					.orElse(null);
				restricaoModel.setFuncionario(func);
			} else {
				restricaoModel.setFuncionario(null);
			}
			
			RestricaoDataDAO.salvar(restricaoModel);
			exibirAlerta("success", "Restrição cadastrada com sucesso!");
			PrimeFaces.current().executeScript("PF('dlgRestricao').hide();");
			PrimeFaces.current().ajax().update("form");
			
		} catch (SQLException e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar restrição: " + e.getMessage(), "Erro!"));
		}
	}
	
	public void atualizarRestricao() {
		try {
			if (restricaoModel.getData() == null) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo data é obrigatório", "Erro!"));
				return;
			}
			
			if (restricaoModel.getDescricao() == null || restricaoModel.getDescricao().trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo descrição é obrigatório", "Erro!"));
				return;
			}
			
			if ("F".equals(restricaoModel.getTipo()) && (funcionarioId == null || funcionarioId == 0)) {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Selecione um funcionário", "Erro!"));
				return;
			}
			
			// Buscar funcionário se tipo for específico
			if ("F".equals(restricaoModel.getTipo()) && funcionarioId != null && funcionarioId > 0) {
				Funcionario func = lstFuncionario.stream()
					.filter(f -> f.getId().equals(funcionarioId))
					.findFirst()
					.orElse(null);
				restricaoModel.setFuncionario(func);
			} else {
				restricaoModel.setFuncionario(null);
			}
			
			RestricaoDataDAO.atualizar(restricaoModel);
			exibirAlerta("success", "Restrição atualizada com sucesso!");
			PrimeFaces.current().executeScript("PF('dlgRestricao').hide();");
			PrimeFaces.current().ajax().update("form");
			
		} catch (SQLException e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao atualizar restrição: " + e.getMessage(), "Erro!"));
		}
	}
	
	public void deletarRestricao() {
		try {
			RestricaoDataDAO.deletar(restricaoModel);
			exibirAlerta("success", "Restrição deletada com sucesso!");
			PrimeFaces.current().executeScript("PF('dlgRestricao').hide();");
			PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
			PrimeFaces.current().ajax().update("form");
		} catch (SQLException e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao deletar restrição: " + e.getMessage(), "Erro!"));
		}
	}
	
	public void onTipoChange() {
		if ("G".equals(restricaoModel.getTipo())) {
			restricaoModel.setFuncionario(null);
			funcionarioId = null;
		}
		PrimeFaces.current().ajax().update("form:dlgRestricaoForm");
	}
}

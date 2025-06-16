package com.barbersys.controller;

import java.time.LocalTime;
import java.util.ArrayList;
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

import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.HorarioDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class FuncionarioController {

	private String nomeFuncionario;
	private String statusSelecionado = "";
	private LocalTime dataInicial = LocalTime.of(0, 0);
	private LocalTime dataFinal = LocalTime.of(0, 0);
	private Funcionario funcionarioModel = new Funcionario();
	private Horario horarioModel = new Horario();
	private LazyDataModel<Funcionario> lstFuncionario;
	private LazyDataModel<Horario> lstHorarios;
	private List<Horario> lstHorarioAux = new ArrayList<Horario>();
	private String editarModel;
	private int indexListAux;

	@PostConstruct
	public void init() {
		lstFuncionario = new LazyDataModel<Funcionario>() {

			@Override
			public List<Funcionario> load(int first, int pageSize, Map<String, SortMeta> sortBy,
					Map<String, FilterMeta> filterBy) {
				return FuncionarioDAO.buscarFuncionario(nomeFuncionario, statusSelecionado, first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return FuncionarioDAO.funcionarioCount(nomeFuncionario, statusSelecionado);
			}

		};
	}

	public void carregarHorariosFuncionario() {
		if (funcionarioModel != null && funcionarioModel.getId() != null && funcionarioModel.getId() > 0) {
			lstHorarios = new LazyDataModel<Horario>() {
				@Override
				public List<Horario> load(int first, int pageSize, Map<String, SortMeta> sortBy,
						Map<String, FilterMeta> filterBy) {
					return HorarioDAO.buscarHorariosPorFuncionarioPaginado(funcionarioModel, first, pageSize);
				}

				@Override
				public int count(Map<String, FilterMeta> filterBy) {
					return HorarioDAO.countHorariosPorFuncionario(funcionarioModel);
				}
			};
		} else {
			lstHorarios = new LazyDataModel<Horario>() {
				@Override
				public List<Horario> load(int first, int pageSize, Map<String, SortMeta> sortBy,
						Map<String, FilterMeta> filterBy) {
					return List.of();
				}

				@Override
				public int count(Map<String, FilterMeta> filterBy) {
					return 0;
				}
			};
		}
	}

	public void limpaListaHorario() {
		lstHorarioAux = new ArrayList<Horario>();
		dataInicial = LocalTime.of(0, 0);
		dataFinal = LocalTime.of(0, 0);
		PrimeFaces.current().ajax().update("form");
	}

	public void funcionarioSelecionado(Funcionario event) {
		funcionarioModel = event;
		editarModel = "A";
		dataInicial = LocalTime.of(0, 0);
		dataFinal = LocalTime.of(0, 0);

		carregarHorariosFuncionario();
	}

	public void novoFuncionario() {
		editarModel = "I";
		funcionarioModel = new Funcionario();
		horarioModel = new Horario();

	}

	public void adicionarNovoFuncionario() {
		try {
			if (funcionarioModel.getNome().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Campo nome do funcionário obrigatório", "Erro!"));
			} else {
				FuncionarioDAO.salvar(funcionarioModel);

				if (funcionarioModel.getId() != null) {
					for (Horario item : lstHorarioAux) {
						item.setFuncionario(funcionarioModel);
						HorarioDAO.salvar(item);
					}

					funcionarioModel = new Funcionario();
					lstHorarioAux.clear();
					dataInicial = LocalTime.of(0, 0);
					dataFinal = LocalTime.of(0, 0);
					PrimeFaces.current().executeScript("Swal.fire({" + "  icon: 'success',"
							+ "  title: '<span style=\"font-size: 14px\">Funcionário criado com sucesso!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

					carregarHorariosFuncionario();
				} else {
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o funcionário!", "Erro!"));
				}
				PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
				PrimeFaces.current().ajax().update("form");
			}

		} catch (Exception e) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar funcionário e horários: " + e.getMessage(), "Erro!"));
		}
	}

	public void atualizarFuncionario() {
		if (funcionarioModel.getNome() != null) {
			FuncionarioDAO.atualizar(funcionarioModel);
			PrimeFaces.current()
					.executeScript("Swal.fire({" + "  icon: 'success',"
							+ "  title: '<span style=\"font-size: 14px\">Funcionário editado com sucesso!</span>',"
							+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
			PrimeFaces.current().ajax().update("form");
		} else {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"O campo nome do funcionário é obrigatório.!", "Erro!"));
		}

	}

	public void novoHorario() {
		if (funcionarioModel != null && funcionarioModel.getId() != null) {
			if (dataInicial != null && dataFinal != null && dataInicial.isBefore(dataFinal)) {
				horarioModel.setHoraInicial(dataInicial);
				horarioModel.setHoraFinal(dataFinal);
				horarioModel.setFuncionario(funcionarioModel);
				HorarioDAO.salvar(horarioModel);
				PrimeFaces.current()
						.executeScript("Swal.fire({" + "  icon: 'success',"
								+ "  title: '<span style=\"font-size: 14px\">Horário adicionado com sucesso!</span>',"
								+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"A hora inicial deve ser menor que a hora final.!", "Erro!"));
				dataInicial = LocalTime.of(0, 0);
				dataFinal = LocalTime.of(0, 0);
				PrimeFaces.current().ajax().update("form:dlgFuncForm");
			}

			carregarHorariosFuncionario();
		} else {
			if (dataInicial != null && dataFinal != null && dataInicial.isBefore(dataFinal)) {
				Horario horario = new Horario();
				horario.setHoraFinal(dataFinal);
				horario.setHoraInicial(dataInicial);
				lstHorarioAux.add(horario);

				PrimeFaces.current()
						.executeScript("Swal.fire({" + "  icon: 'success',"
								+ "  title: '<span style=\"font-size: 14px\">Horário adicionado com sucesso!</span>',"
								+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");

			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"A hora inicial deve ser menor que a hora final.!", "Erro!"));

				dataInicial = LocalTime.of(0, 0);
				dataFinal = LocalTime.of(0, 0);
				PrimeFaces.current().ajax().update("form:dlgFuncForm");
			}
		}

	}

	public void recebeValorDeleteHorario(Horario event) {
		horarioModel = event;
		PrimeFaces.current().executeScript("PF('dlgHora').show();");
	}

	public void recebeValorDeleteHorarioAux(int index) {
		indexListAux = index;
		PrimeFaces.current().executeScript("PF('dlgHoraAux').show();");
	}

	public void deletaFuncionario() {
		FuncionarioDAO.deletar(funcionarioModel);

		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Funcionário deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
		PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
		PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
		PrimeFaces.current().ajax().update("form");
	}

	public void deletaHorarioAux() {
		lstHorarioAux.remove(indexListAux);
		PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
		PrimeFaces.current().ajax().update("form:dlgFuncForm");
		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Horário deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
	}

	public void deletaHorario() {
		HorarioDAO.deletar(horarioModel.getId());
		PrimeFaces.current().executeScript("PF('dlgHora').hide();");
		PrimeFaces.current().ajax().update("form:dlgFuncForm");
		PrimeFaces.current()
				.executeScript("Swal.fire({" + "  icon: 'success',"
						+ "  title: '<span style=\"font-size: 14px\">Horário deletado com sucesso!</span>',"
						+ "  showConfirmButton: false," + "  timer: 2000," + "  width: '350px'" + "});");
		carregarHorariosFuncionario();
	}

}

package com.barbersys.controller;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.sql.SQLException;

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
import com.barbersys.dao.HorarioDAO;
import com.barbersys.dao.UsuarioDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.model.Perfil;
import com.barbersys.model.Usuario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class FuncionarioController {

	private String nomeFuncionario;
	private String statusSelecionado = "";
	private Date dataInicial;
	private Date dataFinal;
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
            private static final long serialVersionUID = 1L;

			@Override
			public List<Funcionario> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
				return FuncionarioDAO.buscarFuncionario(nomeFuncionario, statusSelecionado, first, pageSize);
			}

			@Override
			public int count(Map<String, FilterMeta> filterBy) {
				return FuncionarioDAO.funcionarioCount(nomeFuncionario, statusSelecionado);
			}

		};
	}
    
    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 2000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

	public void carregarHorariosFuncionario() {
		if (funcionarioModel != null && funcionarioModel.getId() != null && funcionarioModel.getId() > 0) {
			lstHorarios = new LazyDataModel<Horario>() {
                private static final long serialVersionUID = 1L;

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
                private static final long serialVersionUID = 1L;

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
		dataInicial = null;
		dataFinal = null;
		PrimeFaces.current().ajax().update("form");
	}

	public void funcionarioSelecionado(Funcionario event) {
		funcionarioModel = event;
        if (funcionarioModel.getUsuario() == null) {
            funcionarioModel.setUsuario(new Usuario());
        }
		editarModel = "A";
		dataInicial = null;
		dataFinal = null;

		carregarHorariosFuncionario();
	}

	public void novoFuncionario() {
		editarModel = "I";
		funcionarioModel = new Funcionario();
		horarioModel = new Horario();
        funcionarioModel.setUsuario(new Usuario());

	}

	public void adicionarNovoFuncionario() {
		try {
			if (funcionarioModel.getNome().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Campo nome do funcionário obrigatório", "Erro!"));
			} else {
                if (funcionarioModel.getUsuario().getLogin() == null || funcionarioModel.getUsuario().getLogin().isEmpty()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login do usuário obrigatório", "Erro!"));
                    return;
                }
                if (funcionarioModel.getUsuario().getSenha() == null || funcionarioModel.getUsuario().getSenha().isEmpty()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Senha do usuário obrigatório", "Erro!"));
                    return;
                }

                // Salvar o usuário primeiro
                UsuarioDAO usuarioDAO = new UsuarioDAO();
                Perfil perfil = new Perfil();
                perfil.setId(2L); // 2 para funcionário
                funcionarioModel.getUsuario().setPerfil(perfil);
                Usuario usuarioSalvo = usuarioDAO.salvar(funcionarioModel.getUsuario());
                funcionarioModel.setUsuario(usuarioSalvo);

				FuncionarioDAO.salvar(funcionarioModel);

				if (funcionarioModel.getId() != null) {
					for (Horario item : lstHorarioAux) {
						item.setFuncionario(funcionarioModel);
						HorarioDAO.salvar(item);
					}

					funcionarioModel = new Funcionario();
					lstHorarioAux.clear();
					dataInicial = null;
					dataFinal = null;
					exibirAlerta("success", "Funcionário criado com sucesso!");
			
					carregarHorariosFuncionario();
				} else {
					FacesContext.getCurrentInstance().addMessage(null,
							new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o funcionário!", "Erro!"));
				}
				PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
				PrimeFaces.current().ajax().update("form");
			}

		} catch (SQLException e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao salvar funcionário: " + e.getMessage(), "Erro!"));
		} catch (Exception e) {
            e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar funcionário: " + e.getMessage(), "Erro!"));
		}
	}

	public void atualizarFuncionario() {
		try {
            if (funcionarioModel.getNome() != null) {
                if (funcionarioModel.getUsuario().getLogin() == null || funcionarioModel.getUsuario().getLogin().isEmpty()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login do usuário obrigatório", "Erro!"));
                    return;
                }

                FuncionarioDAO.atualizar(funcionarioModel);
                exibirAlerta("success", "Funcionário editado com sucesso!");
                PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
                PrimeFaces.current().ajax().update("form");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao atualizar funcionário: " + e.getMessage(), "Erro!"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao atualizar funcionário: " + e.getMessage(), "Erro!"));
        }

	}

	public void novoHorario() {
		// Se os campos de data estiverem vazios, não faz nada para não validar ao salvar o funcionário
		if (dataInicial == null && dataFinal == null) {
			return;
		}
		
		if (dataInicial == null || dataFinal == null || !dataInicial.before(dataFinal)) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"A hora inicial deve ser menor que a hora final!", "Erro!"));
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			return;
		}

		LocalTime horaInicial = dataInicial.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
		LocalTime horaFinal = dataFinal.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

		if (horaInicial.getMinute() % 30 != 0 || horaFinal.getMinute() % 30 != 0) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Os horários devem ser em intervalos de 30 minutos (ex: 08:00, 08:30).", "Erro!"));
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			return;
		}

		if (funcionarioModel != null && funcionarioModel.getId() != null) {
			// Editando um funcionário existente
			horarioModel.setHoraInicial(horaInicial);
			horarioModel.setHoraFinal(horaFinal);
			horarioModel.setFuncionario(funcionarioModel);
			HorarioDAO.salvar(horarioModel);
			carregarHorariosFuncionario();
		} else {
			// Criando um novo funcionário
			Horario horario = new Horario();
			horario.setHoraFinal(horaFinal);
			horario.setHoraInicial(horaInicial);
			lstHorarioAux.add(horario);
		}

		exibirAlerta("success", "Horário adicionado com sucesso!");
		
		// Limpa os campos após adicionar
		dataInicial = null;
		dataFinal = null;
		
		PrimeFaces.current().ajax().update("form:dlgFuncForm");
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
		try {
            FuncionarioDAO.deletar(funcionarioModel);

            exibirAlerta("success", "Funcionário deletado com sucesso!");
            PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
            PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao deletar funcionário: " + e.getMessage(), "Erro!"));
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao deletar funcionário: " + e.getMessage(), "Erro!"));
        }
	}

	public void deletaHorarioAux() {
		lstHorarioAux.remove(indexListAux);
		PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
		PrimeFaces.current().ajax().update("form:dlgFuncForm");		exibirAlerta("success", "Horário deletado com sucesso!");
	}

	public void deletaHorario() {
		HorarioDAO.deletar(horarioModel.getId());
		PrimeFaces.current().executeScript("PF('dlgHora').hide();");
		PrimeFaces.current().ajax().update("form:dlgFuncForm");
		exibirAlerta("success", "Horário deletado com sucesso!");
		carregarHorariosFuncionario();
	}

}
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
import com.barbersys.dao.AgendamentoDAO;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;
import com.barbersys.model.Perfil;
import com.barbersys.model.Usuario;
import com.barbersys.util.EmailService;

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
	
	private String loginOriginal;
	private String codigoValidacao;
	private String codigoGerado;
	private boolean aguardandoValidacao = false;
	private String confirmarSenha; // Campo auxiliar para confirmar senha
	
	// Variáveis para confirmação de exclusão de horário com agendamentos
	private Horario horarioParaExcluir;
	private int qtdAgendamentosAfetar = 0;

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
	
	// Limpa todas as variáveis do controller
	private void limparTodasVariaveis() {
		funcionarioModel = new Funcionario();
		funcionarioModel.setUsuario(new Usuario());
		horarioModel = new Horario();
		lstHorarioAux = new ArrayList<Horario>();
		dataInicial = null;
		dataFinal = null;
		loginOriginal = null;
		codigoValidacao = null;
		codigoGerado = null;
		aguardandoValidacao = false;
		confirmarSenha = null;
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
		indexListAux = 0;
		editarModel = null;
	}
    
	// Exibe alerta Sweet Alert
    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 4000, width: '350px' });",
				icon, title);
		PrimeFaces.current().executeScript(script);
	}

	// Carrega horários paginados do funcionário
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

	// Limpa lista auxiliar de horários
	public void limpaListaHorario() {
		lstHorarioAux = new ArrayList<Horario>();
		dataInicial = null;
		dataFinal = null;
		PrimeFaces.current().ajax().update("form");
	}

	// Carrega funcionário selecionado para edição
	public void funcionarioSelecionado(Funcionario event) {
		if (event != null && event.getId() != null) {
			try {
				limparTodasVariaveis();
				funcionarioModel = FuncionarioDAO.buscarPorId(event.getId());
				
				if (funcionarioModel == null) {
					FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Funcionário não encontrado", "Erro!"));
					return;
				}
				
				if (funcionarioModel.getUsuario() == null) {
					funcionarioModel.setUsuario(new Usuario());
				}
				
				editarModel = "A";
				dataInicial = null;
				dataFinal = null;
				horarioModel = new Horario();
				loginOriginal = funcionarioModel.getUsuario().getLogin();
				aguardandoValidacao = false;
				confirmarSenha = null;
				codigoValidacao = null;
				codigoGerado = null;
				funcionarioModel.getUsuario().setSenha(null);
				
				lstHorarioAux.clear();
				lstHorarioAux.addAll(HorarioDAO.listarPorFuncionario(funcionarioModel.getId()));
				carregarHorariosFuncionario();
				
			} catch (Exception e) {
				e.printStackTrace();
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao carregar funcionário: " + e.getMessage(), "Erro!"));
			}
		}
	}

	// Prepara formulário para novo funcionário
	public void novoFuncionario() {
		limparTodasVariaveis();
		editarModel = "I";
		funcionarioModel = new Funcionario();
		funcionarioModel.setUsuario(new Usuario());
		funcionarioModel.setStatus("A");
		horarioModel = new Horario();
		loginOriginal = null;
		aguardandoValidacao = false;
		confirmarSenha = null;
		codigoValidacao = null;
		codigoGerado = null;
		lstHorarioAux.clear();
		dataInicial = null;
		dataFinal = null;
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
	}
	
	// Cancela edição/cadastro do funcionário
	public void cancelarFuncionario() {
		if (funcionarioModel != null && funcionarioModel.getId() != null && funcionarioModel.getId() > 0) {
			try {
				Funcionario funcionarioRecarregado = FuncionarioDAO.buscarPorId(funcionarioModel.getId());
				
				if (funcionarioRecarregado != null) {
					funcionarioModel = funcionarioRecarregado;
					
					if (funcionarioModel.getUsuario() == null) {
						funcionarioModel.setUsuario(new Usuario());
					}
					
					if (funcionarioModel.getUsuario() != null) {
						loginOriginal = funcionarioModel.getUsuario().getLogin();
					}
					
					funcionarioModel.getUsuario().setSenha(null);
					
					lstHorarioAux.clear();
					lstHorarioAux.addAll(HorarioDAO.listarPorFuncionario(funcionarioModel.getId()));
				} else {
					limparTodasVariaveis();
				}
			} catch (Exception e) {
				e.printStackTrace();
				limparTodasVariaveis();
			}
		} else {
			limparTodasVariaveis();
		}
		
		aguardandoValidacao = false;
		codigoValidacao = null;
		codigoGerado = null;
		confirmarSenha = null;
		dataInicial = null;
		dataFinal = null;
		horarioModel = new Horario();
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
	}
	
	// Prepara salvamento do funcionário (valida e envia código se necessário)
	public void prepararSalvarFuncionario() {
		if (!validarCamposFuncionario()) {
			return;
		}
		
		String loginAtual = funcionarioModel.getUsuario().getLogin();
		boolean loginAlterado = loginOriginal == null || !loginAtual.equals(loginOriginal);
		
		if (editarModel.equals("I") || (editarModel.equals("A") && loginAlterado)) {
			enviarCodigoValidacaoFuncionario();
		} else {
			atualizarFuncionario();
		}
	}
	
	// Envia código de validação por email
	private void enviarCodigoValidacaoFuncionario() {
		try {
			String email = funcionarioModel.getUsuario().getLogin();
			
			// Valida se o email tem formato válido
			if (email == null || email.trim().isEmpty() || !email.contains("@") || !email.contains(".")) {
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, 
						"Email inválido. Por favor, digite um email válido (ex: usuario@email.com)", "Erro!"));
				return;
			}
			
			// Verifica se o email já existe no sistema (apenas se for um novo funcionário ou se mudou o email)
			UsuarioDAO usuarioDAO = new UsuarioDAO();
			if (editarModel.equals("I") || !email.equals(loginOriginal)) {
				if (usuarioDAO.loginExiste(email)) {
					FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, 
							"Este email já está cadastrado no sistema.", "Erro!"));
					return;
				}
			}
			
			codigoGerado = String.format("%06d", (int)(Math.random() * 1000000));
			
			EmailService emailService = new EmailService();
			String nomeFuncionario = funcionarioModel.getNome() != null && !funcionarioModel.getNome().isEmpty() 
				? funcionarioModel.getNome() : "Usuário";
			
			boolean emailEnviado = emailService.enviarEmailValidacao(email, nomeFuncionario, codigoGerado);
			
			if (emailEnviado) {
				aguardandoValidacao = true;
				codigoValidacao = "";
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Código enviado para " + email, "Sucesso!"));
				PrimeFaces.current().executeScript("PF('dlgValidarEmailFuncionario').show();");
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
	
	// Valida código de verificação enviado por email
	public void validarCodigoFuncionario() {
		if (codigoValidacao == null || codigoValidacao.trim().isEmpty()) {
			exibirAlerta("error", "Código é obrigatório");
			return;
		}
		
		if (codigoValidacao != null && codigoValidacao.equals(codigoGerado)) {
			aguardandoValidacao = false;
			codigoValidacao = null;
			
			if (editarModel.equals("I")) {
				adicionarNovoFuncionario();
			} else {
				atualizarFuncionario();
			}
		} else {
			exibirAlerta("error", "Código incorreto! Tente novamente.");
			codigoValidacao = null;
		}
	}
	
	public void reenviarCodigoFuncionario() {
		enviarCodigoValidacaoFuncionario();
		exibirAlerta("info", "Código reenviado para " + funcionarioModel.getUsuario().getLogin());
	}
	
	private boolean validarCamposFuncionario() {
		// Login
		if (funcionarioModel.getUsuario() == null || funcionarioModel.getUsuario().getLogin() == null || funcionarioModel.getUsuario().getLogin().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Login é obrigatório", "Erro!"));
			return false;
		}
		
		// Senha (apenas para novo funcionário)
		if ("I".equals(editarModel)) {
			if (funcionarioModel.getUsuario().getSenha() == null || funcionarioModel.getUsuario().getSenha().trim().isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Senha é obrigatório", "Erro!"));
				return false;
			}
			
			// Validação de tamanho mínimo
			if (funcionarioModel.getUsuario().getSenha().length() < 8) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "A senha deve ter no mínimo 8 caracteres", "Erro!"));
				return false;
			}
			
			// Validação de senhas iguais
			if (confirmarSenha == null || !funcionarioModel.getUsuario().getSenha().equals(confirmarSenha)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "As senhas não conferem. Digite senhas iguais nos dois campos.", "Erro!"));
				return false;
			}
		}
		
		// Para edição, se a senha foi preenchida, valida
		if ("A".equals(editarModel) && funcionarioModel.getUsuario().getSenha() != null && !funcionarioModel.getUsuario().getSenha().trim().isEmpty()) {
			// Validação de tamanho mínimo
			if (funcionarioModel.getUsuario().getSenha().length() < 8) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "A senha deve ter no mínimo 8 caracteres", "Erro!"));
				return false;
			}
			
			// Validação de senhas iguais
			if (confirmarSenha == null || !funcionarioModel.getUsuario().getSenha().equals(confirmarSenha)) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "As senhas não conferem. Digite senhas iguais nos dois campos.", "Erro!"));
				return false;
			}
		}
		
		// Nome Completo
		if (funcionarioModel.getNome() == null || funcionarioModel.getNome().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Nome Completo é obrigatório", "Erro!"));
			return false;
		}
		
		// CPF/CNPJ - Obrigatório
		if (funcionarioModel.getCpf() == null || funcionarioModel.getCpf().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo CPF é obrigatório", "Erro!"));
			return false;
		}
		
		// CPF/CNPJ - Validação de formato
		if (!com.barbersys.util.CpfCnpjValidator.validarDocumento(funcionarioModel.getCpf())) {
			String tipo = com.barbersys.util.CpfCnpjValidator.identificarTipo(funcionarioModel.getCpf());
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, tipo + " inválido. Por favor, digite um " + tipo + " válido.", "Erro!"));
			return false;
		}
		
		// CPF/CNPJ - Verifica duplicidade em TODO O SISTEMA (funcionários e clientes)
		Long funcionarioIdAtual = ("A".equals(editarModel) && funcionarioModel.getId() != null) ? funcionarioModel.getId() : null;
		if (FuncionarioDAO.existeCpfCnpjNoSistema(funcionarioModel.getCpf(), funcionarioIdAtual)) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Este CPF já está cadastrado no sistema.", "Erro!"));
			return false;
		}
		
		// Data de Nascimento
		if (funcionarioModel.getDataNascimento() == null) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Data de Nascimento é obrigatório", "Erro!"));
			return false;
		}
		
		// Telefone
		if (funcionarioModel.getTelefone() == null || funcionarioModel.getTelefone().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Telefone é obrigatório", "Erro!"));
			return false;
		}
		
		// Sexo
		if (funcionarioModel.getSexo() == null || funcionarioModel.getSexo().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Sexo é obrigatório", "Erro!"));
			return false;
		}
		
		// Data de Admissão
		if (funcionarioModel.getDataAdmissao() == null) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Data de Admissão é obrigatório", "Erro!"));
			return false;
		}
		
		// CEP
		if (funcionarioModel.getCep() == null || funcionarioModel.getCep().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo CEP é obrigatório", "Erro!"));
			return false;
		}
		
		// Rua
		if (funcionarioModel.getRua() == null || funcionarioModel.getRua().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Rua é obrigatório", "Erro!"));
			return false;
		}
		
		// Número
		if (funcionarioModel.getNumero() == null || funcionarioModel.getNumero().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Número é obrigatório", "Erro!"));
			return false;
		}
		
		// Bairro
		if (funcionarioModel.getBairro() == null || funcionarioModel.getBairro().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Bairro é obrigatório", "Erro!"));
			return false;
		}
		
		// Cidade
		if (funcionarioModel.getCidade() == null || funcionarioModel.getCidade().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo Cidade é obrigatório", "Erro!"));
			return false;
		}
		
		// UF
		if (funcionarioModel.getEstado() == null || funcionarioModel.getEstado().trim().isEmpty()) {
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Campo UF é obrigatório", "Erro!"));
			return false;
		}
		
		// Horários - Verifica se tem pelo menos 1 horário (novo funcionário)
		if ("I".equals(editarModel)) {
			if (lstHorarioAux == null || lstHorarioAux.isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "É necessário cadastrar pelo menos 1 horário de trabalho", "Erro!"));
				return false;
			}
		}
		
		// Horários - Verifica se tem pelo menos 1 horário (edição)
		// Verifica a lista TEMPORÁRIA (lstHorarioAux), não o banco de dados
		if ("A".equals(editarModel)) {
			if (lstHorarioAux == null || lstHorarioAux.isEmpty()) {
				FacesContext.getCurrentInstance().addMessage(null, 
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "É necessário ter pelo menos 1 horário de trabalho cadastrado", "Erro!"));
				return false;
			}
		}
		
		return true;
	}

	// Salva novo funcionário no banco
	public void adicionarNovoFuncionario() {
		try {
			UsuarioDAO usuarioDAO = new UsuarioDAO();
			Perfil perfil = new Perfil();
			perfil.setId(2L);
			funcionarioModel.getUsuario().setPerfil(perfil);
			funcionarioModel.getUsuario().setUser(funcionarioModel.getNome());
			
			Usuario usuarioSalvo = usuarioDAO.salvar(funcionarioModel.getUsuario());
			funcionarioModel.setUsuario(usuarioSalvo);

			FuncionarioDAO.salvar(funcionarioModel);

			if (funcionarioModel.getId() != null) {
				for (Horario item : lstHorarioAux) {
					item.setFuncionario(funcionarioModel);
					HorarioDAO.salvar(item);
				}

				limparTodasVariaveis();
				exibirAlerta("success", "Funcionário criado com sucesso!");
				PrimeFaces.current().executeScript("PF('dlgValidarEmailFuncionario').hide();");
				PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
				PrimeFaces.current().ajax().update("form");
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o funcionário!", "Erro!"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
			if (e.getMessage().contains("Login já existe")) {
				exibirAlerta("error", "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Erro ao salvar funcionário: " + e.getMessage(), "Erro!"));
			}
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar funcionário: " + e.getMessage(), "Erro!"));
		}
	}

	// Atualiza funcionário existente (verifica agendamentos antes)
	public void atualizarFuncionario() {
		try {
			if (funcionarioModel.getId() != null && "I".equals(funcionarioModel.getStatus())) {
				try {
					Funcionario funcAtual = FuncionarioDAO.buscarPorId(funcionarioModel.getId());
					if (funcAtual != null && "A".equals(funcAtual.getStatus())) {
						int qtdAgendamentos = AgendamentoDAO.contarAgendamentosPendentesPorFuncionario(funcionarioModel.getId());
						
						if (qtdAgendamentos > 0) {
							qtdAgendamentosAfetar = qtdAgendamentos;
							PrimeFaces.current().ajax().update("form:dlgConfirmarInativarFuncionario");
							PrimeFaces.current().executeScript("PF('dlgConfirmarInativarFuncionario').show();");
							return;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			List<Long> horariosParaDeletar = new ArrayList<>();
			int totalAgendamentosAfetar = 0;
			
			if (funcionarioModel.getId() != null) {
				List<Horario> horariosNoBanco = HorarioDAO.listarPorFuncionario(funcionarioModel.getId());
				
				for (Horario horarioBanco : horariosNoBanco) {
					boolean existeNaLista = false;
					
					for (Horario horarioLista : lstHorarioAux) {
						if (horarioLista.getId() != null && horarioLista.getId().equals(horarioBanco.getId())) {
							existeNaLista = true;
							break;
						}
					}
					
					if (!existeNaLista) {
						int qtd = AgendamentoDAO.contarAgendamentosPendentesPorHorario(horarioBanco.getId());
						if (qtd > 0) {
							horariosParaDeletar.add(horarioBanco.getId());
							totalAgendamentosAfetar += qtd;
						}
					}
				}
				
				if (!horariosParaDeletar.isEmpty()) {
					qtdAgendamentosAfetar = totalAgendamentosAfetar;
					FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
						.put("horariosParaDeletar", horariosParaDeletar);
					PrimeFaces.current().ajax().update("form:dlgConfirmarExclusaoHorarioSalvar");
					PrimeFaces.current().executeScript("PF('dlgConfirmarExclusaoHorarioSalvar').show();");
					return;
				}
			}
			
			executarAtualizacaoFuncionario();
			
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao atualizar funcionário: " + e.getMessage(), "Erro!"));
		}
	}
	
	// Executa atualização do funcionário no banco
	public void executarAtualizacaoFuncionario() {
		try {
			if (funcionarioModel.getUsuario() != null) {
				funcionarioModel.getUsuario().setUser(funcionarioModel.getNome());
			}
			
			FuncionarioDAO.atualizar(funcionarioModel);
			
			@SuppressWarnings("unchecked")
			List<Long> horariosParaDeletar = (List<Long>) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get("horariosParaDeletar");
			
			if (horariosParaDeletar != null && !horariosParaDeletar.isEmpty()) {
				for (Long horarioId : horariosParaDeletar) {
					AgendamentoDAO.cancelarAgendamentosPendentesPorHorario(horarioId);
				}
				FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("horariosParaDeletar");
			}
			
			if (funcionarioModel.getId() != null) {
				List<Horario> horariosAntigos = HorarioDAO.listarPorFuncionario(funcionarioModel.getId());
				for (Horario h : horariosAntigos) {
					HorarioDAO.deletar(h.getId());
				}
				
				for (Horario item : lstHorarioAux) {
					item.setId(null);
					item.setFuncionario(funcionarioModel);
					HorarioDAO.salvar(item);
				}
			}
			
			exibirAlerta("success", "Funcionário atualizado com sucesso!");
			limparTodasVariaveis();
			PrimeFaces.current().executeScript("PF('dlgValidarEmailFuncionario').hide();");
			PrimeFaces.current().executeScript("PF('dlgConfirmarExclusaoHorarioSalvar').hide();");
			PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
			PrimeFaces.current().ajax().update("form");
			
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
			PrimeFaces.current().ajax().update("form:messages");
			return;
		}

		LocalTime horaInicial = dataInicial.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
		LocalTime horaFinal = dataFinal.toInstant().atZone(ZoneId.systemDefault()).toLocalTime();

		if (horaInicial.getMinute() % 30 != 0 || horaFinal.getMinute() % 30 != 0) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Os horários devem ser em intervalos de 30 minutos (ex: 08:00, 08:30).", "Erro!"));
			PrimeFaces.current().ajax().update("form:messages");
			return;
		}
		
		// Validação: pelo menos um dia da semana deve estar selecionado
		if (!horarioModel.getDomingo() && !horarioModel.getSegunda() && !horarioModel.getTerca() && 
			!horarioModel.getQuarta() && !horarioModel.getQuinta() && !horarioModel.getSexta() && !horarioModel.getSabado()) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Selecione pelo menos um dia da semana!", "Erro!"));
			PrimeFaces.current().ajax().update("form:messages");
			return;
		}
		
		// Validação de conflito de horários
		String conflito = verificarConflitoHorarios(horaInicial, horaFinal, horarioModel.getId());
		if (conflito != null) {
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					conflito, "Conflito de Horários!"));
			PrimeFaces.current().ajax().update("form:messages");
			return;
		}

		// SEMPRE adiciona em lstHorarioAux (memória temporária)
		// Só salva no banco quando clicar em "Salvar Funcionário"
		Horario horario = new Horario();
		horario.setHoraFinal(horaFinal);
		horario.setHoraInicial(horaInicial);
		horario.setDomingo(horarioModel.getDomingo());
		horario.setSegunda(horarioModel.getSegunda());
		horario.setTerca(horarioModel.getTerca());
		horario.setQuarta(horarioModel.getQuarta());
		horario.setQuinta(horarioModel.getQuinta());
		horario.setSexta(horarioModel.getSexta());
		horario.setSabado(horarioModel.getSabado());
		
		// Se estiver editando, mantém o ID do horário existente
		if (horarioModel.getId() != null) {
			horario.setId(horarioModel.getId());
		}
		
		lstHorarioAux.add(horario);
		horarioModel = new Horario(); // Limpa o modelo

		exibirAlerta("success", "Horário adicionado com sucesso!");
		
		// Limpa os campos após adicionar
		dataInicial = null;
		dataFinal = null;
		
		PrimeFaces.current().ajax().update("form:messages", "dttLstHorarios", "dttLstHorariosAux");
	}
	

	private String verificarConflitoHorarios(LocalTime novoInicio, LocalTime novoFim, Long idHorarioEditando) {
		// Pega os dias selecionados do horário que está sendo adicionado
		List<String> diasNovo = new ArrayList<>();
		if (horarioModel.getDomingo()) diasNovo.add("Domingo");
		if (horarioModel.getSegunda()) diasNovo.add("Segunda");
		if (horarioModel.getTerca()) diasNovo.add("Terça");
		if (horarioModel.getQuarta()) diasNovo.add("Quarta");
		if (horarioModel.getQuinta()) diasNovo.add("Quinta");
		if (horarioModel.getSexta()) diasNovo.add("Sexta");
		if (horarioModel.getSabado()) diasNovo.add("Sábado");
		
		for (Horario horarioExistente : lstHorarioAux) {
			// Pula a comparação com o próprio horário que está sendo editado
			if (idHorarioEditando != null && horarioExistente.getId() != null && 
				horarioExistente.getId().equals(idHorarioEditando)) {
				continue;
			}
			
			// Verifica se há algum dia em comum
			List<String> diasComuns = new ArrayList<>();
			if (horarioModel.getDomingo() && horarioExistente.getDomingo()) diasComuns.add("Domingo");
			if (horarioModel.getSegunda() && horarioExistente.getSegunda()) diasComuns.add("Segunda");
			if (horarioModel.getTerca() && horarioExistente.getTerca()) diasComuns.add("Terça");
			if (horarioModel.getQuarta() && horarioExistente.getQuarta()) diasComuns.add("Quarta");
			if (horarioModel.getQuinta() && horarioExistente.getQuinta()) diasComuns.add("Quinta");
			if (horarioModel.getSexta() && horarioExistente.getSexta()) diasComuns.add("Sexta");
			if (horarioModel.getSabado() && horarioExistente.getSabado()) diasComuns.add("Sábado");
			
			// Se não tem dias em comum, não há conflito
			if (diasComuns.isEmpty()) {
				continue;
			}
			
			// Verifica sobreposição de horários
			LocalTime inicioExistente = horarioExistente.getHoraInicial();
			LocalTime fimExistente = horarioExistente.getHoraFinal();
			
			// Casos de conflito:
			// 1. Novo início está dentro do intervalo existente
			boolean inicioConflita = !novoInicio.isBefore(inicioExistente) && novoInicio.isBefore(fimExistente);
			
			// 2. Novo fim está dentro do intervalo existente
			boolean fimConflita = novoFim.isAfter(inicioExistente) && !novoFim.isAfter(fimExistente);
			
			// 3. Novo horário engloba completamente o existente
			boolean englobaOutro = !novoInicio.isAfter(inicioExistente) && !novoFim.isBefore(fimExistente);
			
			if (inicioConflita || fimConflita || englobaOutro) {
				String diasTexto = String.join(", ", diasComuns);
				return String.format("Conflito nos dias %s com horário existente: %s às %s", 
					diasTexto, inicioExistente, fimExistente);
			}
		}
		
		return null; // Sem conflito
	}

	public void recebeValorDeleteHorario(Horario event) {
		horarioModel = event;
		PrimeFaces.current().executeScript("PF('dlgHora').show();");
	}

	public void recebeValorDeleteHorarioAux(int index) {
		indexListAux = index;
		PrimeFaces.current().executeScript("PF('dlgHoraAux').show();");
	}

	// Desativa funcionário (soft delete)
	public void deletaFuncionario() {
		try {
            FuncionarioDAO.deletar(funcionarioModel);
            exibirAlerta("success", "Funcionário desativado com sucesso! Histórico mantido.");
            PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
            PrimeFaces.current().executeScript("PF('dlgConfirm').hide();");
            PrimeFaces.current().ajax().update("form");
        } catch (SQLException e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro ao desativar funcionário: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            exibirAlerta("error", "Erro inesperado ao desativar funcionário: " + e.getMessage());
        }
	}

	// Remove horário da lista auxiliar
	public void deletaHorarioAux() {
		if (lstHorarioAux.size() <= 1) {
			exibirAlerta("warning", "O funcionário precisa ter pelo menos 1 horário cadastrado.");
			PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
			return;
		}
		
		try {
			lstHorarioAux.remove(indexListAux);
			PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			exibirAlerta("success", "Horário removido da lista!");
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao remover horário: " + e.getMessage());
		}
	}

	// Remove horário da lista auxiliar
	public void deletaHorario() {
		if (lstHorarioAux.size() <= 1) {
			exibirAlerta("warning", "O funcionário precisa ter pelo menos 1 horário cadastrado.");
			PrimeFaces.current().executeScript("PF('dlgHora').hide();");
			return;
		}
		
		try {
			lstHorarioAux.remove(horarioModel);
			PrimeFaces.current().executeScript("PF('dlgHora').hide();");
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			exibirAlerta("success", "Horário removido da lista!");
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao remover horário: " + e.getMessage());
		}
	}
	
	public java.util.Date getHoje() {
		return new java.util.Date();
	}

	// ========== MÉTODOS PARA GERENCIAR AGENDAMENTOS PENDENTES ==========

	public void verificarExclusaoFuncionario() {
		try {
			int qtdAgendamentos = com.barbersys.dao.AgendamentoDAO.contarAgendamentosPendentesPorFuncionario(funcionarioModel.getId());
			
			if (qtdAgendamentos > 0) {
				// Tem agendamentos pendentes - armazena quantidade e mostra modal
				qtdAgendamentosAfetar = qtdAgendamentos;
				PrimeFaces.current().ajax().update("form:dlgConfirmarExclusaoFuncionario");
				PrimeFaces.current().executeScript("PF('dlgConfirmarExclusaoFuncionario').show();");
			} else {
				// Não tem agendamentos - pode excluir diretamente
				PrimeFaces.current().executeScript("PF('dlgConfirm').show();");
			}
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao verificar agendamentos: " + e.getMessage());
		}
	}

	// Desativa funcionário e mantém histórico
	public void confirmarExclusaoComCancelamento() {
		try {
			FuncionarioDAO.deletar(funcionarioModel);
			exibirAlerta("success", "Funcionário desativado! Agendamentos mantidos para histórico.");
			PrimeFaces.current().executeScript("PF('dlgConfirmarExclusaoFuncionario').hide();");
			PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
			PrimeFaces.current().ajax().update("form");
		} catch (SQLException e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao desativar funcionário: " + e.getMessage());
		}
	}
	
	// Inativa funcionário e cancela agendamentos pendentes
	public void confirmarInativarFuncionario() {
		try {
			AgendamentoDAO.cancelarAgendamentosPendentesPorFuncionario(funcionarioModel.getId());
			executarAtualizacaoFuncionario();
			PrimeFaces.current().executeScript("PF('dlgConfirmarInativarFuncionario').hide();");
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao inativar funcionário: " + e.getMessage());
		}
	}

}
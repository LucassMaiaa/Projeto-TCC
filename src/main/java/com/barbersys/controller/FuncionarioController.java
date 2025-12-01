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
	
	/**
	 * Método auxiliar para limpar TODAS as variáveis do controller
	 * Garante que não há resíduos de edições anteriores
	 */
	private void limparTodasVariaveis() {
		System.out.println("🧹 LIMPANDO TODAS AS VARIÁVEIS...");
		
		// Limpa modelo principal
		funcionarioModel = new Funcionario();
		funcionarioModel.setUsuario(new Usuario());
		
		// Limpa modelo de horário
		horarioModel = new Horario();
		
		// Limpa listas
		lstHorarioAux = new ArrayList<Horario>();
		
		// Limpa campos de data/hora
		dataInicial = null;
		dataFinal = null;
		
		// Limpa variáveis de validação
		loginOriginal = null;
		codigoValidacao = null;
		codigoGerado = null;
		aguardandoValidacao = false;
		confirmarSenha = null;
		
		// Limpa variáveis de exclusão
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
		indexListAux = 0;
		
		// Limpa modo de edição
		editarModel = null;
		
		System.out.println("✅ VARIÁVEIS LIMPAS!");
	}
    
    private void exibirAlerta(String icon, String title) {
		String script = String.format(
				"Swal.fire({ icon: '%s', title: '<span style=\"font-size: 14px\">%s</span>', showConfirmButton: false, timer: 4000, width: '350px' });",
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
		// SEMPRE recarrega do banco para evitar dados antigos
		if (event != null && event.getId() != null) {
			try {
				System.out.println("🔍 CARREGANDO FUNCIONÁRIO ID: " + event.getId());
				
				// 1. LIMPA TUDO ANTES DE CARREGAR
				limparTodasVariaveis();
				
				// 2. Recarrega do banco
				funcionarioModel = FuncionarioDAO.buscarPorId(event.getId());
				
				// 3. Verifica se carregou corretamente
				if (funcionarioModel == null) {
					System.err.println("❌ ERRO: Funcionário não encontrado no banco!");
					FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Funcionário não encontrado", "Erro!"));
					return;
				}
				
				System.out.println("✅ Funcionário carregado: " + funcionarioModel.getNome());
				
				// 4. Garante que o usuário existe
				if (funcionarioModel.getUsuario() == null) {
					funcionarioModel.setUsuario(new Usuario());
				}
				
				// 5. Modo edição
				editarModel = "A";
				
				// 6. Limpa campos de horário
				dataInicial = null;
				dataFinal = null;
				horarioModel = new Horario();
				
				// 7. Salva login original
				loginOriginal = funcionarioModel.getUsuario().getLogin();
				
				// 8. Reseta validação e senha
				aguardandoValidacao = false;
				confirmarSenha = null;
				codigoValidacao = null;
				codigoGerado = null;
				
				// 9. LIMPA a senha do modelo (para não mostrar no campo)
				funcionarioModel.getUsuario().setSenha(null);
				
				// 10. Carrega horários do banco para a lista temporária
				lstHorarioAux.clear();
				lstHorarioAux.addAll(HorarioDAO.listarPorFuncionario(funcionarioModel.getId()));
				System.out.println("📋 Carregados " + lstHorarioAux.size() + " horários para edição");
				
				// 11. Carrega lazy model de horários
				carregarHorariosFuncionario();
				
				System.out.println("✅ FUNCIONÁRIO CARREGADO COM SUCESSO!");
				
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("❌ ERRO ao carregar funcionário: " + e.getMessage());
				FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao carregar funcionário: " + e.getMessage(), "Erro!"));
			}
		}
	}

	public void novoFuncionario() {
		System.out.println("➕ CRIANDO NOVO FUNCIONÁRIO...");
		
		// LIMPA TUDO
		limparTodasVariaveis();
		
		// Modo de inserção
		editarModel = "I";
		
		// Cria novos objetos zerados
		funcionarioModel = new Funcionario();
		funcionarioModel.setUsuario(new Usuario());
		funcionarioModel.setStatus("A"); // Status ativo por padrão
		
		horarioModel = new Horario();
		
		// Reseta todas as variáveis
		loginOriginal = null;
		aguardandoValidacao = false;
		confirmarSenha = null;
		codigoValidacao = null;
		codigoGerado = null;
		
		// Limpa campos de horário
		lstHorarioAux.clear();
		dataInicial = null;
		dataFinal = null;
		
		// Reseta variáveis de exclusão
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
		
		System.out.println("✅ NOVO FUNCIONÁRIO INICIALIZADO!");
	}
	
	public void cancelarFuncionario() {
		System.out.println("❌ CANCELANDO EDIÇÃO/CADASTRO...");
		
		// Se estava editando, recarrega do banco
		if (funcionarioModel != null && funcionarioModel.getId() != null && funcionarioModel.getId() > 0) {
			try {
				System.out.println("↻ Recarregando funcionário ID: " + funcionarioModel.getId());
				
				// Recarrega do banco para descartar alterações
				Funcionario funcionarioRecarregado = FuncionarioDAO.buscarPorId(funcionarioModel.getId());
				
				if (funcionarioRecarregado != null) {
					funcionarioModel = funcionarioRecarregado;
					
					// Garante usuário
					if (funcionarioModel.getUsuario() == null) {
						funcionarioModel.setUsuario(new Usuario());
					}
					
					// Salva login original
					if (funcionarioModel.getUsuario() != null) {
						loginOriginal = funcionarioModel.getUsuario().getLogin();
					}
					
					// LIMPA senha (não mostra no campo)
					funcionarioModel.getUsuario().setSenha(null);
					
					// Recarrega horários do banco
					lstHorarioAux.clear();
					lstHorarioAux.addAll(HorarioDAO.listarPorFuncionario(funcionarioModel.getId()));
					System.out.println("↻ Recarregados " + lstHorarioAux.size() + " horários do banco");
				} else {
					System.err.println("⚠️ Funcionário não encontrado no banco, limpando tudo");
					limparTodasVariaveis();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("❌ Erro ao recarregar, limpando tudo");
				// Em caso de erro, limpa tudo
				limparTodasVariaveis();
			}
		} else {
			// Se era novo funcionário, apenas limpa tudo
			System.out.println("🧹 Era novo funcionário, limpando tudo");
			limparTodasVariaveis();
		}
		
		// Limpa variáveis de validação
		aguardandoValidacao = false;
		codigoValidacao = null;
		codigoGerado = null;
		confirmarSenha = null;
		
		// Limpa campos de horário
		dataInicial = null;
		dataFinal = null;
		horarioModel = new Horario();
		
		// Limpa variáveis de exclusão
		horarioParaExcluir = null;
		qtdAgendamentosAfetar = 0;
		
		System.out.println("✅ CANCELAMENTO CONCLUÍDO!");
	}
	
	public void prepararSalvarFuncionario() {
		System.out.println("🔍 Preparando para salvar funcionário...");
		
		// Valida campos obrigatórios ANTES de tentar enviar email
		if (!validarCamposFuncionario()) {
			return;
		}
		
		System.out.println("📝 Login: " + funcionarioModel.getUsuario().getLogin());
		System.out.println("📝 Senha: " + (funcionarioModel.getUsuario().getSenha() != null ? "***" : "NULL"));
		System.out.println("📝 EditarModel: " + editarModel);
		
		String loginAtual = funcionarioModel.getUsuario().getLogin();
		
		// Verifica se é novo funcionário OU se o login foi alterado
		boolean loginAlterado = loginOriginal == null || !loginAtual.equals(loginOriginal);
		
		if (editarModel.equals("I") || (editarModel.equals("A") && loginAlterado)) {
			System.out.println("✅ Precisa validar email");
			// Precisa validar email
			enviarCodigoValidacaoFuncionario();
		} else {
			System.out.println("✅ Não precisa validar, salvando direto");
			// Não precisa validar, salva direto
			atualizarFuncionario();
		}
	}
	
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
	
	public void validarCodigoFuncionario() {
		System.out.println("🔍 Validando código...");
		System.out.println("📝 Código digitado: " + codigoValidacao);
		System.out.println("✅ Código esperado: " + codigoGerado);
		
		if (codigoValidacao == null || codigoValidacao.trim().isEmpty()) {
			exibirAlerta("error", "Código é obrigatório");
			System.out.println("❌ Código vazio!");
			return;
		}
		
		if (codigoValidacao != null && codigoValidacao.equals(codigoGerado)) {
			System.out.println("✅ Código correto! Salvando funcionário...");
			aguardandoValidacao = false;
			
			// Limpa apenas o código digitado para permitir redigitação se der erro
			codigoValidacao = null;
			
			// NÃO limpa codigoGerado - será mantido para permitir nova tentativa
			// Só será limpo após sucesso completo do salvamento
			
			// Salva o funcionário (só fecha modais e limpa código SE SALVAR COM SUCESSO)
			if (editarModel.equals("I")) {
				adicionarNovoFuncionario();
			} else {
				atualizarFuncionario();
			}
		} else {
			System.out.println("❌ Código incorreto!");
			exibirAlerta("error", "Código incorreto! Tente novamente.");
			// Limpa código digitado para nova tentativa
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

	public void adicionarNovoFuncionario() {
		System.out.println("💾 Iniciando salvamento do funcionário...");
		try {
			System.out.println("📝 Salvando usuário...");
			// Salvar o usuário primeiro
			UsuarioDAO usuarioDAO = new UsuarioDAO();
			Perfil perfil = new Perfil();
			perfil.setId(2L); // 2 para funcionário
			funcionarioModel.getUsuario().setPerfil(perfil);
			
			// Define o usu_user como o nome completo do funcionário
			funcionarioModel.getUsuario().setUser(funcionarioModel.getNome());
			
			Usuario usuarioSalvo = usuarioDAO.salvar(funcionarioModel.getUsuario());
			funcionarioModel.setUsuario(usuarioSalvo);
			System.out.println("✅ Usuário salvo com ID: " + usuarioSalvo.getId());

			System.out.println("📝 Salvando funcionário...");
			FuncionarioDAO.salvar(funcionarioModel);
			System.out.println("✅ Funcionário salvo com ID: " + funcionarioModel.getId());

			if (funcionarioModel.getId() != null) {
				System.out.println("📝 Salvando " + lstHorarioAux.size() + " horários...");
				for (Horario item : lstHorarioAux) {
					item.setFuncionario(funcionarioModel);
					HorarioDAO.salvar(item);
				}
				System.out.println("✅ Horários salvos!");

				// LIMPA TUDO após salvar com sucesso
				limparTodasVariaveis();
				
				exibirAlerta("success", "Funcionário criado com sucesso!");
		
				// SÓ FECHA OS MODAIS SE CHEGOU AQUI (SUCESSO TOTAL)
				PrimeFaces.current().executeScript("PF('dlgValidarEmailFuncionario').hide();");
				PrimeFaces.current().executeScript("PF('dlgFunc').hide();");
				PrimeFaces.current().ajax().update("form");
			} else {
				FacesContext.getCurrentInstance().addMessage(null,
						new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro ao salvar o funcionário!", "Erro!"));
				// NÃO FECHA NADA - mantém os modais abertos
			}

		} catch (SQLException e) {
			e.printStackTrace();
			// Tratar erro de login duplicado
			if (e.getMessage().contains("Login já existe")) {
				exibirAlerta("error", "O email informado já está sendo usado por outro usuário. Por favor, escolha outro email.");
			} else {
				FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
						"Erro ao salvar funcionário: " + e.getMessage(), "Erro!"));
			}
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro inesperado ao salvar funcionário: " + e.getMessage(), "Erro!"));
			// NÃO FECHA NADA - mantém os modais abertos para o usuário corrigir
		}
	}

	public void atualizarFuncionario() {
		try {
			System.out.println("💾 Iniciando atualização do funcionário...");
			
			// VERIFICAÇÃO 1: Se está tentando INATIVAR o funcionário (status A → I)
			if (funcionarioModel.getId() != null && "I".equals(funcionarioModel.getStatus())) {
				// Busca o status atual no banco
				try {
					Funcionario funcAtual = FuncionarioDAO.buscarPorId(funcionarioModel.getId());
					if (funcAtual != null && "A".equals(funcAtual.getStatus())) {
						// Está mudando de ATIVO para INATIVO
						// Verifica se tem agendamentos pendentes
						int qtdAgendamentos = AgendamentoDAO.contarAgendamentosPendentesPorFuncionario(funcionarioModel.getId());
						
						if (qtdAgendamentos > 0) {
							System.out.println("⚠️ Funcionário tem " + qtdAgendamentos + " agendamento(s) pendente(s)");
							qtdAgendamentosAfetar = qtdAgendamentos;
							
							// Mostra modal de confirmação
							PrimeFaces.current().ajax().update("form:dlgConfirmarInativarFuncionario");
							PrimeFaces.current().executeScript("PF('dlgConfirmarInativarFuncionario').show();");
							return; // PARA aqui e aguarda confirmação
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// VERIFICAÇÃO 2: Verifica se algum horário que será deletado tem agendamentos pendentes
			List<Long> horariosParaDeletar = new ArrayList<>();
			int totalAgendamentosAfetar = 0;
			
			if (funcionarioModel.getId() != null) {
				// 1. Busca horários atuais do banco
				List<Horario> horariosNoBanco = HorarioDAO.listarPorFuncionario(funcionarioModel.getId());
				
				// 2. Identifica quais horários serão deletados (estão no banco mas não na lista)
				for (Horario horarioBanco : horariosNoBanco) {
					boolean existeNaLista = false;
					
					for (Horario horarioLista : lstHorarioAux) {
						if (horarioLista.getId() != null && horarioLista.getId().equals(horarioBanco.getId())) {
							existeNaLista = true;
							break;
						}
					}
					
					if (!existeNaLista) {
						// Este horário será deletado
						int qtd = AgendamentoDAO.contarAgendamentosPendentesPorHorario(horarioBanco.getId());
						if (qtd > 0) {
							horariosParaDeletar.add(horarioBanco.getId());
							totalAgendamentosAfetar += qtd;
						}
					}
				}
				
				// 3. Se existem agendamentos, mostra modal de confirmação
				if (!horariosParaDeletar.isEmpty()) {
					System.out.println("⚠️ " + horariosParaDeletar.size() + " horário(s) com agendamentos pendentes");
					qtdAgendamentosAfetar = totalAgendamentosAfetar;
					
					// Salva os IDs para cancelar depois
					FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
						.put("horariosParaDeletar", horariosParaDeletar);
					
					// Mostra modal de confirmação
					PrimeFaces.current().ajax().update("form:dlgConfirmarExclusaoHorarioSalvar");
					PrimeFaces.current().executeScript("PF('dlgConfirmarExclusaoHorarioSalvar').show();");
					return; // PARA aqui e aguarda confirmação
				}
			}
			
			// Se chegou aqui, não tem agendamentos OU usuário já confirmou
			executarAtualizacaoFuncionario();
			
		} catch (Exception e) {
			e.printStackTrace();
			FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					"Erro ao atualizar funcionário: " + e.getMessage(), "Erro!"));
		}
	}
	
	public void executarAtualizacaoFuncionario() {
		try {
			System.out.println("💾 Executando atualização do funcionário...");
			
			// Atualiza o usu_user com o nome completo do funcionário
			if (funcionarioModel.getUsuario() != null) {
				funcionarioModel.getUsuario().setUser(funcionarioModel.getNome());
			}
			
			FuncionarioDAO.atualizar(funcionarioModel);
			
			// CANCELA agendamentos dos horários que serão deletados
			@SuppressWarnings("unchecked")
			List<Long> horariosParaDeletar = (List<Long>) FacesContext.getCurrentInstance()
				.getExternalContext().getSessionMap().get("horariosParaDeletar");
			
			if (horariosParaDeletar != null && !horariosParaDeletar.isEmpty()) {
				System.out.println("🗑️ Cancelando agendamentos de " + horariosParaDeletar.size() + " horário(s)");
				for (Long horarioId : horariosParaDeletar) {
					int qtdCancelados = AgendamentoDAO.cancelarAgendamentosPendentesPorHorario(horarioId);
					System.out.println("   ↳ Horário " + horarioId + ": " + qtdCancelados + " agendamento(s) cancelado(s)");
				}
				// Limpa a lista
				FacesContext.getCurrentInstance().getExternalContext().getSessionMap().remove("horariosParaDeletar");
			}
			
			// SINCRONIZA HORÁRIOS: Deleta todos e insere os da lstHorarioAux
			if (funcionarioModel.getId() != null) {
				System.out.println("🔄 Sincronizando horários...");
				
				// 1. Deletar todos os horários existentes do funcionário
				List<Horario> horariosAntigos = HorarioDAO.listarPorFuncionario(funcionarioModel.getId());
				for (Horario h : horariosAntigos) {
					HorarioDAO.deletar(h.getId());
				}
				System.out.println("🗑️ " + horariosAntigos.size() + " horários antigos deletados");
				
				// 2. Inserir os novos horários da lstHorarioAux
				for (Horario item : lstHorarioAux) {
					item.setId(null); // Remove ID para forçar INSERT
					item.setFuncionario(funcionarioModel);
					HorarioDAO.salvar(item);
				}
				System.out.println("✅ " + lstHorarioAux.size() + " horários salvos!");
			}
			
			exibirAlerta("success", "Funcionário atualizado com sucesso!");
			
			// LIMPA TUDO após salvar com sucesso
			limparTodasVariaveis();
			
			// SÓ FECHA OS MODAIS SE CHEGOU AQUI (SUCESSO TOTAL)
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
	
	/**
	 * Verifica se há conflito entre horários nos mesmos dias da semana
	 * @param novoInicio Hora de início do novo horário
	 * @param novoFim Hora de fim do novo horário
	 * @param idHorarioEditando ID do horário sendo editado (null se for novo)
	 * @return Mensagem de erro se houver conflito, null se estiver OK
	 */
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

	public void deletaFuncionario() {
		try {
			// SOFT DELETE: Apenas marca funcionário como inativo
			// NÃO deleta agendamentos - mantém histórico completo!
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

	public void deletaHorarioAux() {
		// Verifica se tem apenas 1 horário
		if (lstHorarioAux.size() <= 1) {
			exibirAlerta("warning", "O funcionário precisa ter pelo menos 1 horário cadastrado.");
			PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
			return;
		}
		
		// Remove APENAS da lista (não do banco)
		// A exclusão real acontece apenas quando salvar o funcionário
		try {
			lstHorarioAux.remove(indexListAux);
			PrimeFaces.current().executeScript("PF('dlgHoraAux').hide();");
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			exibirAlerta("success", "Horário removido da lista!");
			System.out.println("✅ Horário removido da lista temporária (lstHorarioAux)");
			
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao remover horário: " + e.getMessage());
		}
	}

	public void deletaHorario() {
		// Verifica se tem apenas 1 horário
		if (lstHorarioAux.size() <= 1) {
			exibirAlerta("warning", "O funcionário precisa ter pelo menos 1 horário cadastrado.");
			PrimeFaces.current().executeScript("PF('dlgHora').hide();");
			return;
		}
		
		// Remove APENAS da lista (não do banco)
		// A exclusão real acontece apenas quando salvar o funcionário
		try {
			lstHorarioAux.remove(horarioModel);
			PrimeFaces.current().executeScript("PF('dlgHora').hide();");
			PrimeFaces.current().ajax().update("form:dlgFuncForm");
			exibirAlerta("success", "Horário removido da lista!");
			System.out.println("✅ Horário removido da lista temporária (lstHorarioAux)");
			
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

	public void confirmarExclusaoComCancelamento() {
		try {
			// SOFT DELETE: Apenas marca funcionário como inativo
			// NÃO deleta agendamentos - mantém histórico completo!
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
	
	public void confirmarInativarFuncionario() {
		try {
			// Cancela todos os agendamentos pendentes do funcionário
			int qtdCancelados = AgendamentoDAO.cancelarAgendamentosPendentesPorFuncionario(funcionarioModel.getId());
			System.out.println("🔴 Cancelados " + qtdCancelados + " agendamentos ao inativar funcionário");
			
			// Agora pode salvar como inativo
			executarAtualizacaoFuncionario();
			
			// Fecha o modal de confirmação
			PrimeFaces.current().executeScript("PF('dlgConfirmarInativarFuncionario').hide();");
			
		} catch (Exception e) {
			e.printStackTrace();
			exibirAlerta("error", "Erro ao inativar funcionário: " + e.getMessage());
		}
	}

}
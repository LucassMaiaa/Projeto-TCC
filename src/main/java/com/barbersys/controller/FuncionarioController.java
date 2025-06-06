package com.barbersys.controller;

import java.sql.Time;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import com.barbersys.dao.ControleCaixaDAO;
import com.barbersys.dao.FuncionarioDAO;
import com.barbersys.dao.HorarioDAO;
import com.barbersys.model.ControleCaixa;
import com.barbersys.model.Funcionario;
import com.barbersys.model.Horario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class FuncionarioController {
	
	private String nomeFuncionario;
	private Time dataInicial;
	private Time dataFinal;
	private Funcionario funcionarioModel = new Funcionario();
	private LazyDataModel<Funcionario> lstFuncionario;
	private LazyDataModel<Horario> lstHorarios;
	private String editarModel;
	
	 @PostConstruct
	    public void init() {
		 lstFuncionario = new LazyDataModel<Funcionario>() {

	            @Override
	            public List<Funcionario> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
	                return FuncionarioDAO.buscarFuncionario(first, pageSize); 
	            }
	            
	              @Override
	            public int count(Map<String, FilterMeta> filterBy) {
	                return FuncionarioDAO.funcionarioCount();
	            }
	            
	        };
	        
	    }
	 
	 	public void carregarHorariosFuncionario() {
		    if (funcionarioModel != null) {
		        lstHorarios = new LazyDataModel<Horario>() {
		            @Override
		            public List<Horario> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
		                return HorarioDAO.buscarHorariosPorFuncionarioPaginado(funcionarioModel.getId(), first, pageSize);
		            }

		            @Override
		            public int count(Map<String, FilterMeta> filterBy) {
		                return HorarioDAO.countHorariosPorFuncionario(funcionarioModel.getId());
		            }
		        };
		    }
		}
	 	
	 	public void funcionarioSelecionado(Funcionario event) {
	 		funcionarioModel = event;
	 		editarModel = "A";
	 	}
	 	
	 	public void novoFuncionario() {
	 		funcionarioModel = new Funcionario();
	 		editarModel = "I";
	 	}

	    
}

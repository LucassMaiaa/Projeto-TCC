package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "restricao_data")
public class RestricaoData implements Serializable {
	
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "res_codigo")
	private Long id;
	
	@Column(name = "res_data")
	private Date data;
	
	@Column(name = "res_descricao")
	private String descricao;
	
	@Column(name = "res_tipo")
	private String tipo; // "G" = Geral (todos), "F" = Funcionário específico
	
	@ManyToOne
	@JoinColumn(name = "fun_codigo")
	private Funcionario funcionario;
	
	@Column(name = "res_status")
	private String status = "A"; // A = Ativo, I = Inativo
}

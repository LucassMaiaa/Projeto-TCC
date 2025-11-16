package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "funcionario")
public class Funcionario implements Serializable{
	
	@Id
	private Long id; 
	
	@Column
	private String nome;
	
	@Column
	private String status = "A";
	
	@Column(name = "fun_telefone")
	private String telefone;
	
	@Column(name = "fun_cpf")
	private String cpf;
	
	@Column(name = "fun_sexo")
	private String sexo; // M=Masculino, F=Feminino, O=Outro
	
	@Column(name = "fun_data_nascimento")
	@Temporal(TemporalType.DATE)
	private Date dataNascimento;
	
	@Column(name = "fun_data_admissao")
	@Temporal(TemporalType.DATE)
	private Date dataAdmissao;
	
	@Column(name = "fun_observacoes")
	private String observacoes;
	
	// Endereço separado (mantém o antigo por compatibilidade)
	@Column(name = "fun_endereco")
	private String endereco;
	
	@Column(name = "fun_cep")
	private String cep;
	
	@Column(name = "fun_rua")
	private String rua;
	
	@Column(name = "fun_numero")
	private String numero;
	
	@Column(name = "fun_complemento")
	private String complemento;
	
	@Column(name = "fun_bairro")
	private String bairro;
	
	@Column(name = "fun_cidade")
	private String cidade;
	
	@Column(name = "fun_estado")
	private String estado;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "usu_codigo")
	private Usuario usuario = new Usuario();
}

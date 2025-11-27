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
@Table(name = "cliente")
public class Cliente implements Serializable{
	
	@Id
	private Long id; 
	
	@Column
	private String nome;
	
	@Column
	private String email;
	
	@Column
	private String telefone;
	
	@Column
	private String cpf;
	
	@Column(name = "cli_sexo")
	private String sexo; // M=Masculino, F=Feminino, O=Outro
	
	@Column(name = "cli_data_nascimento")
	@Temporal(TemporalType.DATE)
	private Date dataNascimento;
	
	@Column(name = "cli_observacoes")
	private String observacoes;
	
	// Endereço separado
	@Column(name = "cli_cep")
	private String cep;
	
	@Column(name = "cli_rua")
	private String rua;
	
	@Column(name = "cli_numero")
	private String numero;
	
	@Column(name = "cli_complemento")
	private String complemento;
	
	@Column(name = "cli_bairro")
	private String bairro;
	
	@Column(name = "cli_cidade")
	private String cidade;
	
	@Column(name = "cli_estado")
	private String estado;
	
	@Column(name = "cli_ativo")
	private Boolean ativo = true;
	
	@Column(name = "cli_status")
	private String status = "A"; // A=Ativo, I=Inativo

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "usu_codigo")
	private Usuario usuario = new Usuario();
	
}

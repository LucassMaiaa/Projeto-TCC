package com.barbersys.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pagamento")
public class Pagamento implements Serializable{
	
	@Id
	private Long id; 
	
	@Column
	private String nome;
	
	@Column
	private String status = "A";
	
	@Column
	private Boolean integraCaixa;
}


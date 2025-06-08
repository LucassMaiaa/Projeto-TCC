package com.barbersys.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CaixaData {
	
	@Id
	private Long id;
	
	@Column
	private Double valorInicial;
	
	@Column
	private Double valorFinal;
	
	@Column
	private Date dataCadastro;
	
	@Column
	private String status = "I";
	
}

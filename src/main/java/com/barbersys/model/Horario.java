package com.barbersys.model;

import java.sql.Time;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.persistence.Column;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean
@ViewScoped
public class Horario {
	
	@Id
	private Long id;
	
	@Column
	private Time horaInicial;
	
	@Column
	private Time horaFinal;
	
	private Long funcionarioId;
	
}

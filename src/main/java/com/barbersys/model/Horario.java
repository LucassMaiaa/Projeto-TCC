package com.barbersys.model;

import java.time.LocalTime;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
	private LocalTime  horaInicial;

	@Column
	private LocalTime  horaFinal;

	@ManyToOne
	@JoinColumn(name = "fun_codigo")
	private Funcionario funcionario;

}

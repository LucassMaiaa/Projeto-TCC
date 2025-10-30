package com.barbersys.model;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "usu_codigo")
	private Usuario usuario = new Usuario(); // Inicialização direta aqui
}

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
	
	// Dias da semana (1=Domingo, 2=Segunda, 3=Terça, 4=Quarta, 5=Quinta, 6=Sexta, 7=Sábado)
	@Column(name = "hor_domingo")
	private Boolean domingo = false;
	
	@Column(name = "hor_segunda")
	private Boolean segunda = false;
	
	@Column(name = "hor_terca")
	private Boolean terca = false;
	
	@Column(name = "hor_quarta")
	private Boolean quarta = false;
	
	@Column(name = "hor_quinta")
	private Boolean quinta = false;
	
	@Column(name = "hor_sexta")
	private Boolean sexta = false;
	
	@Column(name = "hor_sabado")
	private Boolean sabado = false;
	
	// Método auxiliar para exibir os dias da semana formatados
	public String getDiasSemanaFormatados() {
		java.util.List<String> dias = new java.util.ArrayList<>();
		if (domingo != null && domingo) dias.add("Dom");
		if (segunda != null && segunda) dias.add("Seg");
		if (terca != null && terca) dias.add("Ter");
		if (quarta != null && quarta) dias.add("Qua");
		if (quinta != null && quinta) dias.add("Qui");
		if (sexta != null && sexta) dias.add("Sex");
		if (sabado != null && sabado) dias.add("Sáb");
		
		return dias.isEmpty() ? "Nenhum" : String.join(", ", dias);
	}

}
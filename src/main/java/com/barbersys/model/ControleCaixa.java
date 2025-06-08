/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
@Table(name = "controlecaixa")
public class ControleCaixa implements Serializable{
    
    @Id
    private Long id;
    
    @Column
    private Double valor;
    
    @Column
    private String movimentacao;
    
    @Column
    private String horaAtual;
    
    @Column
    private String motivo;
    
    @Column
    private Date data;
    
    @ManyToOne
	@JoinColumn(name = "cai_codigo")
	private CaixaData caixaData;
    
}

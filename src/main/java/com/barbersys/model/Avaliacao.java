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
@Table(name = "avaliacao")
public class Avaliacao implements Serializable {
    
    @Id
    @Column(name = "ava_codigo")
    private Long id;
    
    @Column(name = "ava_nota")
    private Integer nota;
    
    @Column(name = "ava_comentario")
    private String comentario;
    
    @Column(name = "ava_data_criacao")
    private Date dataCriacao;
    
    @ManyToOne
    @JoinColumn(name = "age_codigo")
    private Agendamento agendamento;
    
    @ManyToOne
    @JoinColumn(name = "cli_codigo")
    private Cliente cliente;
    
    @ManyToOne
    @JoinColumn(name = "fun_codigo")
    private Funcionario funcionario;
}

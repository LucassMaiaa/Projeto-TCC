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
@Table(name = "perfil")
public class Perfil implements Serializable {
    
    @Id
    @Column(name = "per_codigo")
    private Long id;
    
    @Column(name = "per_nome")
    private String nome;
}

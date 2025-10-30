package com.barbersys.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient; // Importar Transient
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "usu_codigo")
    private Long id;

    @Column(name = "usu_login")
    private String login;

    @Column(name = "usu_senha")
    private String senha;
    
    @ManyToOne
    @JoinColumn(name = "per_codigo")
    private Perfil perfil;

    @Transient // Não persistir no banco de dados, apenas para uso em memória
    private Cliente clienteAssociado;

    @Transient // Não persistir no banco de dados, apenas para uso em memória
    private Funcionario funcionarioAssociado;
}

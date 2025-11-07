package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProdutividadeFuncionario implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long funcionarioId;
    private String funcionarioNome;
    private Date data;
    private Integer atendimentosRealizados;
    private Double taxaCancelamento;
    private Double mediaAvaliacoes;
}

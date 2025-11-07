package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaturamentoMensal implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long servicoId;
    private String tipoServico;
    private Date data;
    private Integer quantidadeServicos;
    private Double valorUnitario;
    private Double totalFaturado;
    
    public FaturamentoMensal() {
    }
    
    public FaturamentoMensal(Long servicoId, String tipoServico, Date data, 
                             Integer quantidadeServicos, Double valorUnitario, Double totalFaturado) {
        this.servicoId = servicoId;
        this.tipoServico = tipoServico;
        this.data = data;
        this.quantidadeServicos = quantidadeServicos;
        this.valorUnitario = valorUnitario;
        this.totalFaturado = totalFaturado;
    }
}

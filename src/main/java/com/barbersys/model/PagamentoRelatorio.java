package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagamentoRelatorio implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String nomeCliente;
    private String formaPagamento;
    private Double valor;
    private Date data;
    private String statusPagamento; // "S" = Pago, "N" = Pendente
}

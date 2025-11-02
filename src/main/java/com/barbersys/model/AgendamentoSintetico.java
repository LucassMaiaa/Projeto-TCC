package com.barbersys.model;

import java.io.Serializable;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgendamentoSintetico implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Date data;
    private Integer totalAgendamentos;
    private Integer finalizados;
    private Integer cancelados;
}

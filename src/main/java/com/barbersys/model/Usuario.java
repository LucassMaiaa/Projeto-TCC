package com.barbersys.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String login;
    private String senha;
    
}

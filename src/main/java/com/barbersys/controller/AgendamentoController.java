/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.barbersys.controller;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Lucas
 */

@Getter
@Setter
@ManagedBean(name = "agendamentoController")
@ViewScoped
public class AgendamentoController implements Serializable{
    
    private String tipoCadastro = "c";
    
}

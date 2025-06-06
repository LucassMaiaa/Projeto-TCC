package com.barbersys.controller;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ManagedBean(name = "homeController")
@ViewScoped
public class HomeController implements Serializable{

	private String dataSelecionada = "m";
	private String teste = "s";
}

package com.barbersys.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@ManagedBean
@Table(name = "agendamento")
public class Agendamento {
	@Id
	private Long id;

	@Column
	private String status = "A";

	@Column
	private java.util.Date dataCriado;

	@Column
	private LocalTime horaSelecionada;

	@Column
	private String nomeClienteAvulso;
	
	@Column
	private String tipoCadastro;

	@Column(name = "age_pago")
	private String pago = "N";
	
	@Column(name = "age_observacoes")
	private String observacoes;

	@Column(name = "age_sexo")
	private String sexo;
	
	@Column(name = "age_duracao_minutos")
	private Integer duracaoMinutos = 30;

	@ManyToMany
	@JoinTable(name = "agendamento_servico", joinColumns = @JoinColumn(name = "age_codigo"), inverseJoinColumns = @JoinColumn(name = "ser_codigo"))
	private List<Servicos> servicos = new ArrayList<Servicos>();

	@ManyToOne
	@JoinColumn(name = "fun_codigo")
	private Funcionario funcionario;

	@ManyToOne
	@JoinColumn(name = "cli_codigo")
	private Cliente cliente;

	@ManyToOne
	@JoinColumn(name = "pag_codigo")
	private Pagamento pagamento;

    public String getServicosConcatenados() {
        if (servicos == null || servicos.isEmpty()) {
            return "";
        }
        List<String> nomes = new ArrayList<>();
        for (Servicos s : servicos) {
            nomes.add(s.getNome());
        }
        return String.join(", ", nomes);
    }
    
    public double getValorTotal() {
        if (servicos == null || servicos.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Servicos s : servicos) {
            if (s.getPreco() != null) {
                total += s.getPreco();
            }
        }
        return total;
    }
    
    public int getDuracaoTotalMinutos() {
        if (servicos == null || servicos.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Servicos s : servicos) {
            if (s.getMinutos() != null) {
                total += s.getMinutos();
            }
        }
        return total;
    }
    
    public String getDuracaoTotalFormatada() {
        int totalMinutos = getDuracaoTotalMinutos();
        if (totalMinutos == 0) {
            return "0min";
        }
        
        int horas = totalMinutos / 60;
        int minutos = totalMinutos % 60;
        
        if (horas > 0 && minutos > 0) {
            return horas + "h " + minutos + "min";
        } else if (horas > 0) {
            return horas + "h";
        } else {
            return minutos + "min";
        }
    }
    
    public String getObservacoesExibicao() {
        // Prioridade: observações do agendamento > observações do cliente > null
        if (this.observacoes != null && !this.observacoes.trim().isEmpty()) {
            return this.observacoes;
        }
        
        if (this.cliente != null && this.cliente.getObservacoes() != null && !this.cliente.getObservacoes().trim().isEmpty()) {
            return this.cliente.getObservacoes();
        }
        
        return null;
    }
    
    // Getters e Setters explícitos para sexo (garantir funcionamento mesmo se Lombok falhar)
    public String getSexo() {
        return sexo;
    }
    
    public void setSexo(String sexo) {
        this.sexo = sexo;
    }
}
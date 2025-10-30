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
}
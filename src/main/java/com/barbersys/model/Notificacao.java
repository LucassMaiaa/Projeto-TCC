package com.barbersys.model;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;
import java.util.Objects;

@Getter
@Setter
public class Notificacao {

    private Long id; // not_codigo
    private String mensagem; // not_mensagem
    private Date dataEnvio; // not_data_envio
    private String status; // not_status
    private String lida; // not_lida (S/N)
    private Agendamento agendamento; // age_codigo
    private Cliente cliente; // cli_codigo

    public Notificacao() {
        this.lida = "N"; // Por padrão, notificação não lida
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notificacao that = (Notificacao) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
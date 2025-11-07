package com.barbersys.util;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import com.barbersys.dao.ClienteDAO;
import com.barbersys.model.Cliente;

@FacesConverter("clienteConverter")
public class ClienteConverter implements Converter {
    
    private ClienteDAO clienteDAO = new ClienteDAO();

    @Override
    public Object getAsObject(FacesContext fc, UIComponent uic, String value) {
        if (value != null && value.trim().length() > 0) {
            try {
                Long id = Long.parseLong(value);
                return clienteDAO.buscarPorId(id);
            } catch (NumberFormatException e) {
                throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro de Conversão", "Cliente inválido."));
            }
        }
        return null;
    }

    @Override
    public String getAsString(FacesContext fc, UIComponent uic, Object object) {
        if (object != null) {
            return String.valueOf(((Cliente) object).getId());
        }
        return "";
    }
}

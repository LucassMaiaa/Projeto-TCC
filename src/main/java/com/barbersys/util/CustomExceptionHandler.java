package com.barbersys.util;

import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationHandler;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

public class CustomExceptionHandler extends ExceptionHandlerWrapper {

    private final ExceptionHandler wrapped;

    public CustomExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return this.wrapped;
    }

    @Override
    public void handle() throws FacesException {
        final Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
        
        while (i.hasNext()) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable t = context.getException();
            
            FacesContext fc = FacesContext.getCurrentInstance();
            
            try {
                if (t instanceof ViewExpiredException) {
                    ViewExpiredException vee = (ViewExpiredException) t;
                    Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
                    NavigationHandler nav = fc.getApplication().getNavigationHandler();
                    
                    try {
                        // Adiciona uma mensagem para ser exibida na tela de login
                        // Verifica se o Flash está disponível antes de usar
                        if (fc.getExternalContext().getFlash() != null) {
                            fc.getExternalContext().getFlash().setKeepMessages(true);
                        }
                        fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN,
                                "Sua sessão expirou.", "Por favor, faça o login novamente."));

                        // Redireciona para a página de login
                        nav.handleNavigation(fc, null, "/login.xhtml?faces-redirect=true");
                    } finally {
                        i.remove();
                    }
                }
            } finally {
                // Deixa o handler padrão cuidar do resto
            }
        }
        // Ao final, chama o handler padrão para qualquer outra exceção
        getWrapped().handle();
    }
}
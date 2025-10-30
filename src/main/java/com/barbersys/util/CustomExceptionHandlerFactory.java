package com.barbersys.util;

import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerFactory;

public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

    private final ExceptionHandlerFactory parent;

    public CustomExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        ExceptionHandler result = parent.getExceptionHandler();
        result = new CustomExceptionHandler(result);
        return result;
    }
}
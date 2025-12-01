package com.barbersys.util;

import com.barbersys.dao.NotificacaoDAO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Timer;
import java.util.TimerTask;

// Listener para tarefas automatizadas ao iniciar o sistema
@WebListener
public class NotificacaoCleanupListener implements ServletContextListener {

    private Timer timer;

    // Inicializa tarefas ao iniciar aplicação
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executarLimpeza();
        
        timer = new Timer("NotificacaoCleanupTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                executarLimpeza();
            }
        }, 24 * 60 * 60 * 1000, 24 * 60 * 60 * 1000);
    }

    // Cancela tarefas ao encerrar aplicação
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (timer != null) {
            timer.cancel();
        }
    }
    
    // Executa limpeza de notificações antigas
    private void executarLimpeza() {
        try {
            NotificacaoDAO dao = new NotificacaoDAO();
            dao.deletarNotificacoesAntigas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

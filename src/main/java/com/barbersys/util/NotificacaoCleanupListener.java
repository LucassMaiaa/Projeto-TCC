package com.barbersys.util;

import com.barbersys.dao.NotificacaoDAO;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Listener que executa tarefas automatizadas do sistema
 * - Limpeza de notificações antigas (lidas há mais de 7 dias)
 */
@WebListener
public class NotificacaoCleanupListener implements ServletContextListener {

    private Timer timer;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================");
        System.out.println("🚀 Sistema BarberSys Iniciado");
        System.out.println("========================================");
        
        // Executa limpeza imediatamente ao iniciar
        executarLimpeza();
        
        // Agenda limpeza automática a cada 24 horas
        timer = new Timer("NotificacaoCleanupTimer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                executarLimpeza();
            }
        }, 24 * 60 * 60 * 1000, 24 * 60 * 60 * 1000); // 24 horas
        
        System.out.println("✅ Limpeza automática de notificações agendada (a cada 24h)");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (timer != null) {
            timer.cancel();
            System.out.println("🛑 Timer de limpeza de notificações cancelado");
        }
    }
    
    private void executarLimpeza() {
        try {
            System.out.println("🧹 Iniciando limpeza de notificações antigas...");
            NotificacaoDAO dao = new NotificacaoDAO();
            dao.deletarNotificacoesAntigas();
        } catch (Exception e) {
            System.out.println("❌ Erro ao executar limpeza de notificações: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

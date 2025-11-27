package com.barbersys.util;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class EmailService {
    
    // CONFIGURAÇÕES DO GMAIL
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String EMAIL_FROM = "lukas.maia2014@gmail.com";
    private static final String EMAIL_PASSWORD = "yjjmuqwsrznraypa"; // Senha de app do Gmail
    
    /**
     * Envia email com código de recuperação de senha
     */
    public static boolean enviarCodigoRecuperacao(String destinatario, String nomeUsuario, String codigo) {
        try {
            // Configuração das propriedades do servidor SMTP
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            // Autenticação
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });
            
            // Criar mensagem
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "BarberSys"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Recuperação de Senha - BarberSys");
            
            // Conteúdo HTML do email
            String conteudoHtml = gerarHtmlEmail(nomeUsuario, codigo);
            message.setContent(conteudoHtml, "text/html; charset=utf-8");
            
            // Enviar email
            Transport.send(message);
            
            System.out.println("✅ Email enviado com sucesso para: " + destinatario);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gera HTML bonito para o email
     */
    private static String gerarHtmlEmail(String nomeUsuario, String codigo) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Arial', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #23A4D5 0%, #1E88B8 100%); padding: 40px 20px; text-align: center; }" +
                "        .header h1 { color: #ffffff; margin: 0; font-size: 32px; font-weight: 700; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 18px; color: #374151; margin-bottom: 20px; }" +
                "        .message { font-size: 16px; color: #6B7280; line-height: 1.6; margin-bottom: 30px; }" +
                "        .code-box { background: #F3F4F6; border: 2px dashed #23A4D5; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                "        .code { font-size: 36px; font-weight: 700; color: #23A4D5; letter-spacing: 8px; font-family: 'Courier New', monospace; }" +
                "        .code-label { font-size: 14px; color: #6B7280; margin-bottom: 10px; text-transform: uppercase; }" +
                "        .warning { background: #FEF3C7; border-left: 4px solid #F59E0B; padding: 15px; margin: 20px 0; font-size: 14px; color: #92400E; }" +
                "        .footer { background: #F9FAFB; padding: 20px; text-align: center; font-size: 12px; color: #9CA3AF; border-top: 1px solid #E5E7EB; }" +
                "        .footer a { color: #23A4D5; text-decoration: none; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🔐 BarberSys</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <div class='greeting'>Olá, <strong>" + nomeUsuario + "</strong>!</div>" +
                "            <div class='message'>" +
                "                Recebemos uma solicitação para recuperação de senha da sua conta no <strong>BarberSys</strong>." +
                "                <br><br>" +
                "                Use o código abaixo para redefinir sua senha:" +
                "            </div>" +
                "            <div class='code-box'>" +
                "                <div class='code-label'>Seu código de verificação</div>" +
                "                <div class='code'>" + codigo + "</div>" +
                "            </div>" +
                "            <div class='warning'>" +
                "                <strong>⚠️ Atenção:</strong> Este código é válido por <strong>15 minutos</strong>. " +
                "                Se você não solicitou a recuperação de senha, ignore este email." +
                "            </div>" +
                "            <div class='message'>" +
                "                Se tiver alguma dúvida, entre em contato com nosso suporte." +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>Este é um email automático, por favor não responda.</p>" +
                "            <p>&copy; 2024 BarberSys. Todos os direitos reservados.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Envia email com código de verificação para registro
     */
    public boolean enviarCodigoVerificacao(String destinatario, String nomeUsuario, String codigo) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "BarberSys"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Código de Verificação - BarberSys");
            
            String conteudoHtml = gerarHtmlEmailVerificacao(nomeUsuario, codigo);
            message.setContent(conteudoHtml, "text/html; charset=utf-8");
            
            Transport.send(message);
            
            System.out.println("✅ Email de verificação enviado para: " + destinatario);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar email de verificação: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gera HTML para email de verificação de registro
     */
    private String gerarHtmlEmailVerificacao(String nomeUsuario, String codigo) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Arial', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #23A4D5 0%, #1E88B8 100%); padding: 40px 20px; text-align: center; }" +
                "        .header h1 { color: #ffffff; margin: 0; font-size: 32px; font-weight: 700; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 18px; color: #374151; margin-bottom: 20px; }" +
                "        .message { font-size: 16px; color: #6B7280; line-height: 1.6; margin-bottom: 30px; }" +
                "        .code-box { background: #F3F4F6; border: 2px dashed #23A4D5; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                "        .code { font-size: 36px; font-weight: 700; color: #23A4D5; letter-spacing: 8px; font-family: 'Courier New', monospace; }" +
                "        .code-label { font-size: 14px; color: #6B7280; margin-bottom: 10px; text-transform: uppercase; }" +
                "        .warning { background: #DBEAFE; border-left: 4px solid #23A4D5; padding: 15px; margin: 20px 0; font-size: 14px; color: #1E40AF; }" +
                "        .footer { background: #F9FAFB; padding: 20px; text-align: center; font-size: 12px; color: #9CA3AF; border-top: 1px solid #E5E7EB; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>🎉 BarberSys</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <div class='greeting'>Olá, <strong>" + nomeUsuario + "</strong>!</div>" +
                "            <div class='message'>" +
                "                Bem-vindo ao <strong>BarberSys</strong>! Estamos felizes em tê-lo conosco." +
                "                <br><br>" +
                "                Para concluir seu cadastro, use o código de verificação abaixo:" +
                "            </div>" +
                "            <div class='code-box'>" +
                "                <div class='code-label'>Código de Verificação</div>" +
                "                <div class='code'>" + codigo + "</div>" +
                "            </div>" +
                "            <div class='warning'>" +
                "                <strong>ℹ️ Informação:</strong> Este código é válido por <strong>15 minutos</strong>." +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>Este é um email automático, por favor não responda.</p>" +
                "            <p>&copy; 2024 BarberSys. Todos os direitos reservados.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Envia email com código de validação para cadastro/alteração
     */
    public static boolean enviarEmailValidacao(String destinatario, String nomeUsuario, String codigo) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
                }
            });
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_FROM, "BarberSys"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject("Validação de Email - BarberSys");
            
            String conteudoHtml = gerarHtmlEmailValidacao(nomeUsuario, codigo);
            message.setContent(conteudoHtml, "text/html; charset=utf-8");
            
            Transport.send(message);
            
            System.out.println("✅ Email de validação enviado para: " + destinatario);
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar email de validação: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gera HTML para email de validação
     */
    private static String gerarHtmlEmailValidacao(String nomeUsuario, String codigo) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <style>" +
                "        body { font-family: 'Arial', sans-serif; background-color: #f4f4f4; margin: 0; padding: 0; }" +
                "        .container { max-width: 600px; margin: 40px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }" +
                "        .header { background: linear-gradient(135deg, #23A4D5 0%, #1E88B8 100%); padding: 40px 20px; text-align: center; }" +
                "        .header h1 { color: #ffffff; margin: 0; font-size: 32px; font-weight: 700; }" +
                "        .content { padding: 40px 30px; }" +
                "        .greeting { font-size: 18px; color: #374151; margin-bottom: 20px; }" +
                "        .message { font-size: 16px; color: #6B7280; line-height: 1.6; margin-bottom: 30px; }" +
                "        .code-box { background: #F3F4F6; border: 2px dashed #23A4D5; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }" +
                "        .code { font-size: 36px; font-weight: 700; color: #23A4D5; letter-spacing: 8px; font-family: 'Courier New', monospace; }" +
                "        .code-label { font-size: 14px; color: #6B7280; margin-bottom: 10px; text-transform: uppercase; }" +
                "        .warning { background: #DBEAFE; border-left: 4px solid #23A4D5; padding: 15px; margin: 20px 0; font-size: 14px; color: #1E40AF; }" +
                "        .footer { background: #F9FAFB; padding: 20px; text-align: center; font-size: 12px; color: #9CA3AF; border-top: 1px solid #E5E7EB; }" +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <div class='header'>" +
                "            <h1>✉️ BarberSys</h1>" +
                "        </div>" +
                "        <div class='content'>" +
                "            <div class='greeting'>Olá, <strong>" + nomeUsuario + "</strong>!</div>" +
                "            <div class='message'>" +
                "                Para validar seu email no <strong>BarberSys</strong>, use o código abaixo:" +
                "            </div>" +
                "            <div class='code-box'>" +
                "                <div class='code-label'>Código de Validação</div>" +
                "                <div class='code'>" + codigo + "</div>" +
                "            </div>" +
                "            <div class='warning'>" +
                "                <strong>ℹ️ Informação:</strong> Este código é válido por <strong>15 minutos</strong>." +
                "            </div>" +
                "        </div>" +
                "        <div class='footer'>" +
                "            <p>Este é um email automático, por favor não responda.</p>" +
                "            <p>&copy; 2024 BarberSys. Todos os direitos reservados.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }
    
    /**
     * Gera código aleatório de 6 dígitos
     */
    public static String gerarCodigo() {
        return String.format("%06d", (int)(Math.random() * 999999));
    }
}

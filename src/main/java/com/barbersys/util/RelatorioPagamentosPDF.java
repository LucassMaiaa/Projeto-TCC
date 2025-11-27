package com.barbersys.util;

import com.barbersys.model.PagamentoRelatorio;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

public class RelatorioPagamentosPDF {

    // CORES CLEAN ESTILO EXCEL
    private static final Color COR_PRETO = Color.BLACK;
    private static final Color COR_CINZA_TEXTO = new Color(64, 64, 64);
    private static final Color COR_CINZA_HEADER = new Color(217, 217, 217);
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_BORDA_EXCEL = new Color(208, 206, 206);
    private static final Color COR_VERDE = new Color(46, 125, 50);
    private static final Color COR_VERDE_CLARO = new Color(232, 245, 233);
    private static final Color COR_LARANJA = new Color(245, 124, 0);
    private static final Color COR_LARANJA_CLARO = new Color(255, 244, 230);
    
    // FONTES CLEAN
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, COR_PRETO);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_CINZA_TEXTO);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_PRETO);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);

    public static void gerarPDF(List<PagamentoRelatorio> pagamentos, 
                                 java.util.Date dataInicial, 
                                 java.util.Date dataFinal) throws Exception {
        
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        externalContext.responseReset();
        externalContext.setResponseContentType("application/pdf");
        externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"relatorio_pagamentos.pdf\"");
        
        OutputStream output = externalContext.getResponseOutputStream();
        PdfWriter.getInstance(document, output);
        
        document.open();
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        
        // Título em preto à esquerda
        Paragraph titulo = new Paragraph("Relatório de Pagamentos", TITLE_FONT);
        titulo.setAlignment(Element.ALIGN_LEFT);
        titulo.setSpacingAfter(8);
        document.add(titulo);
        
        // Período
        String periodoTexto = "Período: ";
        if (dataInicial != null && dataFinal != null) {
            periodoTexto += sdf.format(dataInicial) + " até " + sdf.format(dataFinal);
        } else if (dataInicial != null) {
            periodoTexto += "A partir de " + sdf.format(dataInicial);
        } else if (dataFinal != null) {
            periodoTexto += "Até " + sdf.format(dataFinal);
        } else {
            periodoTexto += "Todos os registros";
        }
        Paragraph periodo = new Paragraph(periodoTexto, SUBTITLE_FONT);
        periodo.setAlignment(Element.ALIGN_LEFT);
        periodo.setSpacingAfter(3);
        document.add(periodo);
        
        // Data de geração
        Paragraph dataGeracao = new Paragraph("Data de Geração: " + sdf.format(new java.util.Date()), SUBTITLE_FONT);
        dataGeracao.setAlignment(Element.ALIGN_LEFT);
        dataGeracao.setSpacingAfter(20);
        document.add(dataGeracao);
        
        // Criação da tabela
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.7f, 2.5f, 2.0f, 1.2f, 1.2f, 1.2f});
        table.setSpacingBefore(10);
        
        // Cabeçalho da tabela
        adicionarCelulaCabecalho(table, "Cód.");
        adicionarCelulaCabecalho(table, "Cliente");
        adicionarCelulaCabecalho(table, "Forma Pag.");
        adicionarCelulaCabecalho(table, "Valor");
        adicionarCelulaCabecalho(table, "Data");
        adicionarCelulaCabecalho(table, "Status");
        
        // Preenchendo a tabela com os dados
        SimpleDateFormat dataTabela = new SimpleDateFormat("dd/MM/yyyy");
        int index = 0;
        int codigo = 1;
        
        double totalPago = 0.0;
        double totalPendente = 0.0;
        int countPago = 0;
        int countPendente = 0;
        
        for (PagamentoRelatorio pagamento : pagamentos) {
            Color bgColor = COR_BRANCO; // Todas brancas
            
            // Código (usa índice sequencial)
            adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
            
            // Cliente
            String nomeCliente = pagamento.getNomeCliente() != null 
                ? pagamento.getNomeCliente() 
                : "-";
            adicionarCelulaDados(table, nomeCliente, Element.ALIGN_LEFT, bgColor);
            
            // Forma de Pagamento
            String formaPagamento = pagamento.getFormaPagamento() != null 
                ? pagamento.getFormaPagamento() 
                : "Não definido";
            adicionarCelulaDados(table, formaPagamento, Element.ALIGN_LEFT, bgColor);
            
            // Valor
            String valorFormatado = String.format("R$ %.2f", pagamento.getValor() != null ? pagamento.getValor() : 0.0);
            adicionarCelulaDados(table, valorFormatado, Element.ALIGN_RIGHT, bgColor);
            
            // Data
            String dataFormatada = pagamento.getData() != null 
                ? dataTabela.format(pagamento.getData()) 
                : "-";
            adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
            
            // Status
            String statusTexto = "";
            Color bgColorStatus = COR_BRANCO;
            if ("S".equals(pagamento.getStatusPagamento())) {
                statusTexto = "Pago";
                bgColorStatus = COR_VERDE_CLARO;
                totalPago += (pagamento.getValor() != null ? pagamento.getValor() : 0.0);
                countPago++;
            } else if ("N".equals(pagamento.getStatusPagamento())) {
                statusTexto = "Pendente";
                bgColorStatus = COR_LARANJA_CLARO;
                totalPendente += (pagamento.getValor() != null ? pagamento.getValor() : 0.0);
                countPendente++;
            } else {
                statusTexto = "-";
                bgColorStatus = COR_BRANCO;
            }
            
            PdfPCell cellStatus = new PdfPCell(new Phrase(statusTexto, CELL_BOLD_FONT));
            cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellStatus.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellStatus.setPadding(7);
            cellStatus.setBackgroundColor(bgColorStatus);
            cellStatus.setBorderWidth(1);
            cellStatus.setBorderColor(COR_BORDA_EXCEL);
            table.addCell(cellStatus);
            
            index++;
        }
        
        document.add(table);
        
        // Resumo final
        document.add(new Paragraph("\n\n"));
        
        Paragraph resumoTitulo = new Paragraph("Resumo:", CELL_BOLD_FONT);
        resumoTitulo.setSpacingBefore(10);
        document.add(resumoTitulo);
        
        document.add(new Paragraph(String.format("Pagos: %d (R$ %.2f)", countPago, totalPago), CELL_FONT));
        document.add(new Paragraph(String.format("Pendentes: %d (R$ %.2f)", countPendente, totalPendente), CELL_FONT));
        document.add(new Paragraph(String.format("Total Geral: %d (R$ %.2f)", pagamentos.size(), totalPago + totalPendente), CELL_BOLD_FONT));
        
        document.close();
        output.flush();
        facesContext.responseComplete();
    }
    
    private static void adicionarCelulaCabecalho(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, HEADER_FONT));
        cell.setBackgroundColor(COR_CINZA_HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderWidth(1);
        cell.setBorderColor(COR_BORDA_EXCEL);
        table.addCell(cell);
    }
    
    private static void adicionarCelulaDados(PdfPTable table, String texto, int alinhamento, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, CELL_FONT));
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7);
        cell.setBackgroundColor(bgColor);
        cell.setBorderWidth(1);
        cell.setBorderColor(COR_BORDA_EXCEL);
        table.addCell(cell);
    }
}
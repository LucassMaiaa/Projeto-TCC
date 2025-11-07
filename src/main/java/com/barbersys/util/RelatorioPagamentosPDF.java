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

    private static final Color COLOR_HEADER = new Color(231, 74, 70);
    private static final Color COLOR_PRIMARY = new Color(231, 74, 70);
    private static final Color COLOR_ROW_EVEN = new Color(245, 245, 245);
    private static final Color COLOR_SUCCESS = new Color(40, 167, 69);
    private static final Color COLOR_WARNING = new Color(255, 193, 7);

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
        
        // Título
        Font fontTitulo = new Font(Font.HELVETICA, 18, Font.BOLD, COLOR_PRIMARY);
        Paragraph titulo = new Paragraph("Relatório de Pagamentos", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(10);
        document.add(titulo);
        
        // Período
        if (dataInicial != null || dataFinal != null) {
            Font fontPeriodo = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
            String periodoTexto = "Período: ";
            if (dataInicial != null && dataFinal != null) {
                periodoTexto += sdf.format(dataInicial) + " até " + sdf.format(dataFinal);
            } else if (dataInicial != null) {
                periodoTexto += "A partir de " + sdf.format(dataInicial);
            } else {
                periodoTexto += "Até " + sdf.format(dataFinal);
            }
            Paragraph periodo = new Paragraph(periodoTexto, fontPeriodo);
            periodo.setAlignment(Element.ALIGN_CENTER);
            periodo.setSpacingAfter(5);
            document.add(periodo);
        }
        
        // Data de geração
        Font fontData = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
        Paragraph dataGeracao = new Paragraph("Gerado em: " + sdf.format(new java.util.Date()), fontData);
        dataGeracao.setAlignment(Element.ALIGN_CENTER);
        dataGeracao.setSpacingAfter(15);
        document.add(dataGeracao);
        
        // Total de registros
        Font fontTotal = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
        Paragraph total = new Paragraph("Total de registros: " + pagamentos.size(), fontTotal);
        total.setSpacingAfter(10);
        document.add(total);
        
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
            Color bgColor = (index % 2 == 0) ? Color.WHITE : COLOR_ROW_EVEN;
            
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
            Color statusColor = Color.BLACK;
            if ("S".equals(pagamento.getStatusPagamento())) {
                statusTexto = "Pago";
                statusColor = COLOR_SUCCESS;
                totalPago += (pagamento.getValor() != null ? pagamento.getValor() : 0.0);
                countPago++;
            } else if ("N".equals(pagamento.getStatusPagamento())) {
                statusTexto = "Pendente";
                statusColor = COLOR_WARNING;
                totalPendente += (pagamento.getValor() != null ? pagamento.getValor() : 0.0);
                countPendente++;
            } else {
                statusTexto = "-";
            }
            
            PdfPCell cellStatus = new PdfPCell(new Phrase(statusTexto, new Font(Font.HELVETICA, 9, Font.BOLD, statusColor)));
            cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellStatus.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellStatus.setPadding(8);
            cellStatus.setBackgroundColor(bgColor);
            cellStatus.setBorder(Rectangle.NO_BORDER);
            table.addCell(cellStatus);
            
            index++;
        }
        
        document.add(table);
        
        // Resumo final
        document.add(new Paragraph(" "));
        
        Font fontResumo = new Font(Font.HELVETICA, 10, Font.BOLD, Color.DARK_GRAY);
        Paragraph resumoTitulo = new Paragraph("Resumo:", fontResumo);
        resumoTitulo.setSpacingBefore(10);
        document.add(resumoTitulo);
        
        Font fontResumoItem = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        document.add(new Paragraph(String.format("Pagos: %d (R$ %.2f)", countPago, totalPago), fontResumoItem));
        document.add(new Paragraph(String.format("Pendentes: %d (R$ %.2f)", countPendente, totalPendente), fontResumoItem));
        document.add(new Paragraph(String.format("Total Geral: %d (R$ %.2f)", pagamentos.size(), totalPago + totalPendente), new Font(Font.HELVETICA, 9, Font.BOLD, Color.DARK_GRAY)));
        
        document.close();
        output.flush();
        facesContext.responseComplete();
    }
    
    private static void adicionarCelulaCabecalho(PdfPTable table, String texto) {
        Font fontCabecalho = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fontCabecalho));
        cell.setBackgroundColor(COLOR_HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }
    
    private static void adicionarCelulaDados(PdfPTable table, String texto, int alinhamento, Color bgColor) {
        Font fontDados = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(texto, fontDados));
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }
}
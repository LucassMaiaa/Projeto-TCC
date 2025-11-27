package com.barbersys.util;

import com.barbersys.model.AgendamentoSintetico;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import java.awt.Color;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

public class RelatorioAgendamentosPDF {

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
    private static final Color COR_VERMELHO = new Color(211, 47, 47);
    private static final Color COR_VERMELHO_CLARO = new Color(255, 235, 238);
    
    // FONTES CLEAN
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, COR_PRETO);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_CINZA_TEXTO);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_PRETO);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);

    public static void gerar(List<AgendamentoSintetico> agendamentos, java.util.Date dataInicial, java.util.Date dataFinal) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseHeader("Content-Disposition", 
                "attachment; filename=\"relatorio-agendamentos-" + System.currentTimeMillis() + ".pdf\"");

            OutputStream outputStream = externalContext.getResponseOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título em preto à esquerda
            Paragraph titulo = new Paragraph("Relatório Sintético de Agendamentos", TITLE_FONT);
            titulo.setAlignment(Element.ALIGN_LEFT);
            titulo.setSpacingAfter(8);
            document.add(titulo);

            // Período
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            String periodoTexto = "Período: ";
            if (dataInicial != null && dataFinal != null) {
                periodoTexto += dateFormat.format(dataInicial) + " até " + dateFormat.format(dataFinal);
            } else if (dataInicial != null) {
                periodoTexto += "A partir de " + dateFormat.format(dataInicial);
            } else if (dataFinal != null) {
                periodoTexto += "Até " + dateFormat.format(dataFinal);
            } else {
                periodoTexto += "Todos os registros";
            }
            Paragraph periodo = new Paragraph(periodoTexto, SUBTITLE_FONT);
            periodo.setAlignment(Element.ALIGN_LEFT);
            periodo.setSpacingAfter(3);
            document.add(periodo);

            // Data de geração
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Paragraph dataGeracao = new Paragraph("Data de Geração: " + sdf.format(new java.util.Date()), SUBTITLE_FONT);
            dataGeracao.setAlignment(Element.ALIGN_LEFT);
            dataGeracao.setSpacingAfter(20);
            document.add(dataGeracao);

            // Criação da tabela
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.7f, 1.2f, 1.5f, 1.8f, 1.3f});
            table.setSpacingBefore(10);

            // Cabeçalho da tabela
            adicionarCelulaCabecalho(table, "Cód.");
            adicionarCelulaCabecalho(table, "Data");
            adicionarCelulaCabecalho(table, "Total Agend.");
            adicionarCelulaCabecalho(table, "Finalizados");
            adicionarCelulaCabecalho(table, "Cancelados");

            // Preenchendo a tabela com os dados
            SimpleDateFormat dataTabela = new SimpleDateFormat("dd/MM/yyyy");
            int codigo = 1;
            
            int totalGeral = 0;
            int finalizadosGeral = 0;
            int canceladosGeral = 0;
            
            for (AgendamentoSintetico agendamento : agendamentos) {
                Color bgColor = COR_BRANCO;
                
                // Código (usa índice sequencial)
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Data
                String dataFormatada = agendamento.getData() != null 
                    ? dataTabela.format(agendamento.getData()) 
                    : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Total de Agendamentos
                String totalAgend = String.valueOf(agendamento.getTotalAgendamentos());
                adicionarCelulaDados(table, totalAgend, Element.ALIGN_CENTER, bgColor);
                
                // Finalizados (verde claro)
                String finalizados = String.valueOf(agendamento.getFinalizados());
                PdfPCell cellFinalizado = new PdfPCell(new Phrase(finalizados, CELL_BOLD_FONT));
                cellFinalizado.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellFinalizado.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellFinalizado.setPadding(7);
                cellFinalizado.setBackgroundColor(COR_VERDE_CLARO);
                cellFinalizado.setBorderWidth(1);
                cellFinalizado.setBorderColor(COR_BORDA_EXCEL);
                table.addCell(cellFinalizado);
                
                // Cancelados (vermelho claro)
                String cancelados = String.valueOf(agendamento.getCancelados());
                PdfPCell cellCancelado = new PdfPCell(new Phrase(cancelados, CELL_BOLD_FONT));
                cellCancelado.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCancelado.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellCancelado.setPadding(7);
                cellCancelado.setBackgroundColor(COR_VERMELHO_CLARO);
                cellCancelado.setBorderWidth(1);
                cellCancelado.setBorderColor(COR_BORDA_EXCEL);
                table.addCell(cellCancelado);
                
                // Acumular totais
                totalGeral += agendamento.getTotalAgendamentos();
                finalizadosGeral += agendamento.getFinalizados();
                canceladosGeral += agendamento.getCancelados();
            }

            document.add(table);

            // Totalizadores
            document.add(new Paragraph("\n\n"));
            Paragraph totalizadores = new Paragraph(
                "TOTAIS: Agendamentos: " + totalGeral + 
                " | Finalizados: " + finalizadosGeral + 
                " | Cancelados: " + canceladosGeral, 
                CELL_BOLD_FONT
            );
            totalizadores.setAlignment(Element.ALIGN_RIGHT);
            totalizadores.setSpacingBefore(10);
            document.add(totalizadores);

            document.close();
            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
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
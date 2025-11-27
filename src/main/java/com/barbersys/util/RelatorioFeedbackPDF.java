package com.barbersys.util;

import com.barbersys.model.Avaliacao;
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

public class RelatorioFeedbackPDF {

    // CORES CLEAN ESTILO EXCEL
    private static final Color COR_PRETO = Color.BLACK;
    private static final Color COR_CINZA_TEXTO = new Color(64, 64, 64);
    private static final Color COR_CINZA_HEADER = new Color(217, 217, 217);
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_BORDA_EXCEL = new Color(208, 206, 206);
    private static final Color COR_AMARELO = new Color(255, 193, 7);
    private static final Color COR_AMARELO_CLARO = new Color(255, 244, 230);
    
    // FONTES CLEAN
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, COR_PRETO);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_CINZA_TEXTO);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_PRETO);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);

    public static void gerar(List<Avaliacao> avaliacoes, String filtroNota, Long filtroFuncionarioId, java.util.Date dataInicial, java.util.Date dataFinal) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseHeader("Content-Disposition", 
                "attachment; filename=\"relatorio-feedback-" + System.currentTimeMillis() + ".pdf\"");

            OutputStream outputStream = externalContext.getResponseOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título em preto à esquerda
            Paragraph titulo = new Paragraph("Relatório de Feedback dos Serviços", TITLE_FONT);
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
            table.setWidths(new float[]{0.7f, 1.2f, 2.0f, 0.9f, 3.8f});
            table.setSpacingBefore(10);

            // Cabeçalho da tabela
            adicionarCelulaCabecalho(table, "Cód.");
            adicionarCelulaCabecalho(table, "Data");
            adicionarCelulaCabecalho(table, "Funcionário");
            adicionarCelulaCabecalho(table, "Nota");
            adicionarCelulaCabecalho(table, "Comentários");

            // Preenchendo a tabela com os dados
            SimpleDateFormat dataTabela = new SimpleDateFormat("dd/MM/yyyy");
            int codigo = 1;
            
            for (Avaliacao avaliacao : avaliacoes) {
                Color bgColor = COR_BRANCO;
                
                // Código (usa índice sequencial)
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Data
                String dataFormatada = avaliacao.getDataCriacao() != null 
                    ? dataTabela.format(avaliacao.getDataCriacao()) 
                    : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Funcionário
                String nomeFuncionario = avaliacao.getFuncionario() != null 
                    ? avaliacao.getFuncionario().getNome() 
                    : "-";
                adicionarCelulaDados(table, nomeFuncionario, Element.ALIGN_LEFT, bgColor);
                
                // Avaliação (nota com destaque amarelo)
                String notaFormatada = avaliacao.getNota() != null 
                    ? String.format("%.1f", avaliacao.getNota().doubleValue())
                    : "-";
                PdfPCell cellNota = new PdfPCell(new Phrase(notaFormatada, CELL_BOLD_FONT));
                cellNota.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellNota.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellNota.setPadding(7);
                cellNota.setBackgroundColor(COR_AMARELO_CLARO);
                cellNota.setBorderWidth(1);
                cellNota.setBorderColor(COR_BORDA_EXCEL);
                table.addCell(cellNota);
                
                // Comentários
                String comentario = avaliacao.getComentario() != null && !avaliacao.getComentario().isEmpty()
                    ? avaliacao.getComentario() 
                    : "Sem comentários";
                adicionarCelulaDados(table, comentario, Element.ALIGN_LEFT, bgColor);
            }

            document.add(table);

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
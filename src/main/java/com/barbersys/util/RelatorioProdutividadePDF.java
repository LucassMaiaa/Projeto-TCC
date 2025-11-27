package com.barbersys.util;

import com.barbersys.model.ProdutividadeFuncionario;
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

public class RelatorioProdutividadePDF {

    // CORES CLEAN ESTILO EXCEL
    private static final Color COR_PRETO = Color.BLACK;
    private static final Color COR_CINZA_TEXTO = new Color(64, 64, 64);
    private static final Color COR_CINZA_HEADER = new Color(217, 217, 217);
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_BORDA_EXCEL = new Color(208, 206, 206);
    private static final Color COR_AZUL_INFO = new Color(25, 118, 210);
    private static final Color COR_AZUL_CLARO = new Color(227, 242, 253);
    
    // FONTES CLEAN
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, COR_PRETO);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_CINZA_TEXTO);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_PRETO);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);

    public static void gerar(List<ProdutividadeFuncionario> produtividades, java.util.Date dataInicial, 
            java.util.Date dataFinal, Long funcionarioId) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);

        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseHeader("Content-Disposition", 
                "attachment; filename=\"relatorio-produtividade-" + System.currentTimeMillis() + ".pdf\"");

            OutputStream outputStream = externalContext.getResponseOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título em preto à esquerda
            Paragraph titulo = new Paragraph("Relatório de Produtividade dos Funcionários", TITLE_FONT);
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

            // Tabela de dados
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1f, 3f, 2f, 2.5f, 2.5f, 2f});
            table.setSpacingBefore(10);

            // Cabeçalhos
            adicionarCelulaCabecalho(table, "Código");
            adicionarCelulaCabecalho(table, "Funcionário");
            adicionarCelulaCabecalho(table, "Data");
            adicionarCelulaCabecalho(table, "Atendimentos");
            adicionarCelulaCabecalho(table, "Taxa Cancel. (%)");
            adicionarCelulaCabecalho(table, "Média Aval.");

            // Dados
            SimpleDateFormat dataTabela = new SimpleDateFormat("dd/MM/yyyy");
            int codigo = 1;

            for (ProdutividadeFuncionario prod : produtividades) {
                Color bgColor = COR_BRANCO;
                
                // Código
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Funcionário
                String funcionario = prod.getFuncionarioNome() != null ? prod.getFuncionarioNome() : "-";
                adicionarCelulaDados(table, funcionario, Element.ALIGN_LEFT, bgColor);
                
                // Data
                String dataFormatada = prod.getData() != null ? dataTabela.format(prod.getData()) : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Atendimentos Realizados (azul claro)
                String atendimentos = prod.getAtendimentosRealizados() != null 
                    ? String.valueOf(prod.getAtendimentosRealizados()) 
                    : "0";
                PdfPCell cellAtend = new PdfPCell(new Phrase(atendimentos, CELL_BOLD_FONT));
                cellAtend.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellAtend.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellAtend.setPadding(7);
                cellAtend.setBackgroundColor(COR_AZUL_CLARO);
                cellAtend.setBorderWidth(1);
                cellAtend.setBorderColor(COR_BORDA_EXCEL);
                table.addCell(cellAtend);
                
                // Taxa de Cancelamento
                String taxa = prod.getTaxaCancelamento() != null 
                    ? String.format("%.2f%%", prod.getTaxaCancelamento()) 
                    : "0.00%";
                adicionarCelulaDados(table, taxa, Element.ALIGN_CENTER, bgColor);
                
                // Média de Avaliações
                String media = prod.getMediaAvaliacoes() != null 
                    ? String.format("%.1f", prod.getMediaAvaliacoes()) 
                    : "0.0";
                adicionarCelulaDados(table, media, Element.ALIGN_CENTER, bgColor);
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
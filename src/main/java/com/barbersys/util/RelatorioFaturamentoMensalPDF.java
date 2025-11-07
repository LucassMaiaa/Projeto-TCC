package com.barbersys.util;

import com.barbersys.model.FaturamentoMensal;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RelatorioFaturamentoMensalPDF {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(231, 74, 70));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.BLACK);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.BLACK);

    public static void gerarRelatorio(List<FaturamentoMensal> dados, Date dataInicial, Date dataFinal) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"relatorio_faturamento_mensal.pdf\"");
            
            PdfWriter.getInstance(document, response.getOutputStream());
            document.open();
            
            // Cabeçalho
            adicionarCabecalho(document, dataInicial, dataFinal);
            document.add(new Paragraph("\n"));
            
            // Tabela
            PdfPTable table = criarTabela();
            preencherTabela(table, dados);
            document.add(table);
            
            // Total Geral
            adicionarTotalGeral(document, dados);
            
            document.close();
            facesContext.responseComplete();
            
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void adicionarCabecalho(Document document, Date dataInicial, Date dataFinal) throws DocumentException {
        Paragraph title = new Paragraph("Relatório de Faturamento Mensal", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String periodo = "Período: ";
        
        if (dataInicial != null && dataFinal != null) {
            periodo += sdf.format(dataInicial) + " até " + sdf.format(dataFinal);
        } else if (dataInicial != null) {
            periodo += "A partir de " + sdf.format(dataInicial);
        } else if (dataFinal != null) {
            periodo += "Até " + sdf.format(dataFinal);
        } else {
            periodo += "Todos os registros";
        }
        
        Paragraph subtitle = new Paragraph(periodo, SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);
        
        Paragraph dataGeracao = new Paragraph("Data de Geração: " + sdf.format(new Date()), SUBTITLE_FONT);
        dataGeracao.setAlignment(Element.ALIGN_CENTER);
        document.add(dataGeracao);
    }
    
    private static PdfPTable criarTabela() {
        float[] columnWidths = {1f, 3f, 2f, 2f, 2f, 2.5f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        
        // Cabeçalhos
        adicionarCelulaCabecalho(table, "Código");
        adicionarCelulaCabecalho(table, "Tipo de Serviço");
        adicionarCelulaCabecalho(table, "Data");
        adicionarCelulaCabecalho(table, "Quantidade");
        adicionarCelulaCabecalho(table, "Valor Unitário");
        adicionarCelulaCabecalho(table, "Total Faturado");
        
        return table;
    }
    
    private static void adicionarCelulaCabecalho(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, HEADER_FONT));
        cell.setBackgroundColor(new Color(231, 74, 70));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        table.addCell(cell);
    }
    
    private static void preencherTabela(PdfPTable table, List<FaturamentoMensal> dados) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int contador = 1;
        
        for (FaturamentoMensal item : dados) {
            // Código
            adicionarCelulaDados(table, String.valueOf(contador++), Element.ALIGN_CENTER);
            
            // Tipo de Serviço
            adicionarCelulaDados(table, item.getTipoServico(), Element.ALIGN_LEFT);
            
            // Data
            String dataFormatada = item.getData() != null ? sdf.format(item.getData()) : "-";
            adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER);
            
            // Quantidade
            adicionarCelulaDados(table, String.valueOf(item.getQuantidadeServicos()), Element.ALIGN_CENTER);
            
            // Valor Unitário
            String valorUnitario = String.format("R$ %.2f", item.getValorUnitario());
            adicionarCelulaDados(table, valorUnitario, Element.ALIGN_RIGHT);
            
            // Total Faturado
            String totalFaturado = String.format("R$ %.2f", item.getTotalFaturado());
            PdfPCell cellTotal = new PdfPCell(new Phrase(totalFaturado, CELL_FONT));
            cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellTotal.setPadding(6f);
            cellTotal.setBackgroundColor(new java.awt.Color(240, 248, 255));
            table.addCell(cellTotal);
        }
    }
    
    private static void adicionarCelulaDados(PdfPTable table, String texto, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, CELL_FONT));
        cell.setHorizontalAlignment(alinhamento);
        cell.setPadding(6f);
        table.addCell(cell);
    }
    
    private static void adicionarTotalGeral(Document document, List<FaturamentoMensal> dados) throws DocumentException {
        double totalGeral = 0.0;
        int quantidadeTotal = 0;
        
        for (FaturamentoMensal item : dados) {
            totalGeral += item.getTotalFaturado();
            quantidadeTotal += item.getQuantidadeServicos();
        }
        
        document.add(new Paragraph("\n"));
        
        Paragraph resumo = new Paragraph();
        resumo.add(new Chunk("Total de Registros: ", TOTAL_FONT));
        resumo.add(new Chunk(String.valueOf(dados.size()), CELL_FONT));
        resumo.add(new Chunk("  |  ", CELL_FONT));
        resumo.add(new Chunk("Quantidade Total de Serviços: ", TOTAL_FONT));
        resumo.add(new Chunk(String.valueOf(quantidadeTotal), CELL_FONT));
        resumo.setAlignment(Element.ALIGN_RIGHT);
        document.add(resumo);
        
        Paragraph total = new Paragraph();
        total.add(new Chunk("TOTAL GERAL FATURADO: ", TOTAL_FONT));
        total.add(new Chunk(String.format("R$ %.2f", totalGeral), 
            new Font(Font.HELVETICA, 12, Font.BOLD, new Color(231, 74, 70))));
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);
    }
}

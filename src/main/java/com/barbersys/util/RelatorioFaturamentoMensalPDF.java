package com.barbersys.util;

import com.barbersys.model.FaturamentoMensal;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RelatorioFaturamentoMensalPDF {

    // CORES CLEAN ESTILO EXCEL
    private static final Color COR_PRETO = Color.BLACK;                       // Título e dados
    private static final Color COR_CINZA_TEXTO = new Color(64, 64, 64);      // #404040 - Subtítulos
    private static final Color COR_VERDE_FATURAMENTO = new Color(46, 125, 50); // #2E7D32 - Total
    private static final Color COR_CINZA_HEADER = new Color(217, 217, 217);   // #D9D9D9 - Header Excel
    private static final Color COR_BRANCO = Color.WHITE;
    private static final Color COR_BORDA_EXCEL = new Color(208, 206, 206);    // #D0CECE - Bordas Excel
    private static final Color COR_VERDE_CLARO = new Color(232, 245, 233);    // #E8F5E9 - Destaque

    // FONTES CLEAN ESTILO EXCEL
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, COR_PRETO);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_CINZA_TEXTO);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, COR_PRETO);
    private static final Font CELL_BOLD_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, COR_PRETO);
    private static final Font TOTAL_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, COR_PRETO);
    private static final Font TOTAL_VALUE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, COR_VERDE_FATURAMENTO);

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
        // Título em preto (clean)
        Paragraph title = new Paragraph("Relatório de Faturamento Mensal", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(8f);
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
        subtitle.setAlignment(Element.ALIGN_LEFT);
        subtitle.setSpacingAfter(3f);
        document.add(subtitle);
        
        Paragraph dataGeracao = new Paragraph("Data de Geração: " + sdf.format(new Date()), SUBTITLE_FONT);
        dataGeracao.setAlignment(Element.ALIGN_LEFT);
        dataGeracao.setSpacingAfter(20f);
        document.add(dataGeracao);
    }
    
    private static PdfPTable criarTabela() {
        float[] columnWidths = {0.8f, 3f, 1.8f, 1.5f, 1.8f, 2f};
        PdfPTable table = new PdfPTable(columnWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        
        // Cabeçalhos em cinza profissional (não vermelho)
        adicionarCelulaCabecalho(table, "Cód.");
        adicionarCelulaCabecalho(table, "Tipo de Serviço");
        adicionarCelulaCabecalho(table, "Data");
        adicionarCelulaCabecalho(table, "Qtd");
        adicionarCelulaCabecalho(table, "Valor Unit.");
        adicionarCelulaCabecalho(table, "Total Faturado");
        
        return table;
    }
    
    private static void adicionarCelulaCabecalho(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, HEADER_FONT));
        cell.setBackgroundColor(COR_CINZA_HEADER); // Cinza Excel
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setBorderWidth(1f);
        cell.setBorderColor(COR_BORDA_EXCEL);
        table.addCell(cell);
    }
    
    private static void preencherTabela(PdfPTable table, List<FaturamentoMensal> dados) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int contador = 1;
        
        for (FaturamentoMensal item : dados) {
            Color bgColor = COR_BRANCO; // Todas as células brancas (estilo Excel)
            
            // Código
            adicionarCelulaDados(table, String.valueOf(contador++), Element.ALIGN_CENTER, bgColor, false);
            
            // Tipo de Serviço
            adicionarCelulaDados(table, item.getTipoServico(), Element.ALIGN_LEFT, bgColor, false);
            
            // Data
            String dataFormatada = item.getData() != null ? sdf.format(item.getData()) : "-";
            adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor, false);
            
            // Quantidade
            adicionarCelulaDados(table, String.valueOf(item.getQuantidadeServicos()), Element.ALIGN_CENTER, bgColor, false);
            
            // Valor Unitário - PRETO
            String valorUnitario = String.format("R$ %.2f", item.getValorUnitario());
            adicionarCelulaDados(table, valorUnitario, Element.ALIGN_RIGHT, bgColor, false);
            
            // Total Faturado - VERDE COM DESTAQUE
            String totalFaturado = String.format("R$ %.2f", item.getTotalFaturado());
            adicionarCelulaDados(table, totalFaturado, Element.ALIGN_RIGHT, COR_VERDE_CLARO, true);
        }
    }
    
    private static void adicionarCelulaDados(PdfPTable table, String texto, int alinhamento, Color bgColor, boolean destaque) {
        Font font = destaque ? CELL_BOLD_FONT : CELL_FONT;
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7f);
        cell.setBackgroundColor(bgColor);
        cell.setBorderWidth(1f);
        cell.setBorderColor(COR_BORDA_EXCEL);
        table.addCell(cell);
    }
    
    private static void adicionarTotalGeral(Document document, List<FaturamentoMensal> dados) throws DocumentException {
        double totalGeral = 0.0;
        int quantidadeTotal = 0;
        
        for (FaturamentoMensal item : dados) {
            totalGeral += item.getTotalFaturado();
            quantidadeTotal += item.getQuantidadeServicos();
        }
        
        document.add(new Paragraph("\n\n"));
        
        // Linha separadora sutil
        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(200, 200, 200));
        document.add(new Chunk(line));
        
        document.add(new Paragraph("\n"));
        
        // Resumo em preto
        Paragraph resumo = new Paragraph();
        resumo.add(new Chunk("Total de Registros: ", CELL_FONT));
        resumo.add(new Chunk(String.valueOf(dados.size()), CELL_BOLD_FONT));
        resumo.add(new Chunk("  |  ", CELL_FONT));
        resumo.add(new Chunk("Quantidade Total: ", CELL_FONT));
        resumo.add(new Chunk(String.valueOf(quantidadeTotal), CELL_BOLD_FONT));
        resumo.setAlignment(Element.ALIGN_RIGHT);
        resumo.setSpacingAfter(8f);
        document.add(resumo);
        
        // Total em VERDE (positivo, faz sentido!)
        Paragraph total = new Paragraph();
        total.add(new Chunk("TOTAL GERAL FATURADO: ", TOTAL_LABEL_FONT));
        total.add(new Chunk(String.format("R$ %.2f", totalGeral), TOTAL_VALUE_FONT));
        total.setAlignment(Element.ALIGN_RIGHT);
        document.add(total);
    }
}

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

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(231, 74, 70));
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(127, 140, 141));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_CELL = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(52, 73, 94));
    private static final Font FONT_CELL_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(52, 73, 94));

    private static final Color COLOR_HEADER = new Color(231, 74, 70); // #E74A46
    private static final Color COLOR_ROW_EVEN = new Color(246, 247, 251);
    private static final Color COLOR_BADGE_SUCCESS = new Color(16, 185, 129); // #10B981
    private static final Color COLOR_BADGE_DANGER = new Color(239, 68, 68); // #EF4444
    private static final Color COLOR_BADGE_INFO = new Color(59, 130, 246); // #3B82F6

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

            // Título do relatório
            Paragraph titulo = new Paragraph("Relatório Sintético de Agendamentos", FONT_TITLE);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10);
            document.add(titulo);

            // Subtítulo com data de geração
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            Paragraph subtitulo = new Paragraph("Gerado em: " + sdf.format(new java.util.Date()), FONT_SUBTITLE);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(5);
            document.add(subtitulo);

            // Informações de filtros aplicados
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            boolean temFiltro = false;
            StringBuilder filtrosTexto = new StringBuilder();
            
            if (dataInicial != null) {
                filtrosTexto.append("Data inicial: ").append(dateFormat.format(dataInicial));
                temFiltro = true;
            }
            
            if (dataFinal != null) {
                if (temFiltro) filtrosTexto.append(" | ");
                filtrosTexto.append("Data final: ").append(dateFormat.format(dataFinal));
                temFiltro = true;
            }
            
            if (temFiltro) {
                Paragraph filtros = new Paragraph("Filtros aplicados: " + filtrosTexto.toString(), FONT_SUBTITLE);
                filtros.setAlignment(Element.ALIGN_CENTER);
                filtros.setSpacingAfter(15);
                document.add(filtros);
            } else {
                document.add(new Paragraph(" ", FONT_SUBTITLE));
                document.add(new Paragraph(" ", FONT_SUBTITLE));
            }

            // Total de registros
            Paragraph total = new Paragraph("Total de dias: " + agendamentos.size(), FONT_CELL_BOLD);
            total.setSpacingAfter(15);
            document.add(total);

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
            int index = 0;
            int codigo = 1;
            
            int totalGeral = 0;
            int finalizadosGeral = 0;
            int canceladosGeral = 0;
            
            for (AgendamentoSintetico agendamento : agendamentos) {
                Color bgColor = (index % 2 == 0) ? Color.WHITE : COLOR_ROW_EVEN;
                
                // Código (usa índice sequencial)
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Data
                String dataFormatada = agendamento.getData() != null 
                    ? dataTabela.format(agendamento.getData()) 
                    : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Total de Agendamentos
                String totalAgend = String.valueOf(agendamento.getTotalAgendamentos());
                PdfPCell cellTotal = new PdfPCell(new Phrase(totalAgend, new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_BADGE_INFO)));
                cellTotal.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellTotal.setPadding(8);
                cellTotal.setBackgroundColor(bgColor);
                cellTotal.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellTotal);
                
                // Finalizados
                String finalizados = String.valueOf(agendamento.getFinalizados());
                PdfPCell cellFinalizado = new PdfPCell(new Phrase(finalizados, new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_BADGE_SUCCESS)));
                cellFinalizado.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellFinalizado.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellFinalizado.setPadding(8);
                cellFinalizado.setBackgroundColor(bgColor);
                cellFinalizado.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellFinalizado);
                
                // Cancelados
                String cancelados = String.valueOf(agendamento.getCancelados());
                PdfPCell cellCancelado = new PdfPCell(new Phrase(cancelados, new Font(Font.HELVETICA, 9, Font.BOLD, COLOR_BADGE_DANGER)));
                cellCancelado.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellCancelado.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellCancelado.setPadding(8);
                cellCancelado.setBackgroundColor(bgColor);
                cellCancelado.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellCancelado);
                
                // Acumular totais
                totalGeral += agendamento.getTotalAgendamentos();
                finalizadosGeral += agendamento.getFinalizados();
                canceladosGeral += agendamento.getCancelados();
                
                index++;
            }

            document.add(table);

            // Totalizadores
            document.add(new Paragraph(" "));
            Paragraph totalizadores = new Paragraph(
                "TOTAIS: Agendamentos: " + totalGeral + 
                " | Finalizados: " + finalizadosGeral + 
                " | Cancelados: " + canceladosGeral, 
                FONT_CELL_BOLD
            );
            totalizadores.setAlignment(Element.ALIGN_RIGHT);
            totalizadores.setSpacingBefore(10);
            document.add(totalizadores);

            // Rodapé
            document.add(new Paragraph(" "));
            Paragraph rodape = new Paragraph("BarberSys - Sistema de Gerenciamento de Barbearia", FONT_SUBTITLE);
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(20);
            document.add(rodape);

            document.close();
            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao gerar PDF: " + e.getMessage(), e);
        }
    }

    private static void adicionarCelulaCabecalho(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_HEADER));
        cell.setBackgroundColor(COLOR_HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private static void adicionarCelulaDados(PdfPTable table, String texto, int alinhamento, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CELL));
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBackgroundColor(bgColor);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }
}
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

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(44, 62, 80));
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(127, 140, 141));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_CELL = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(52, 73, 94));
    private static final Font FONT_CELL_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(52, 73, 94));

    private static final Color COLOR_HEADER = new Color(231, 74, 70); // #E74A46
    private static final Color COLOR_ROW_EVEN = new Color(246, 247, 251);
    private static final Color COLOR_RATING = new Color(231, 74, 70); // #E74A46

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

            // Título do relatório
            Paragraph titulo = new Paragraph("Relatório de Feedback dos Serviços", FONT_TITLE);
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
            
            if (filtroNota != null && !filtroNota.isEmpty()) {
                filtrosTexto.append("Avaliação: ").append(filtroNota).append(".0");
                temFiltro = true;
            }
            
            if (filtroFuncionarioId != null) {
                if (temFiltro) filtrosTexto.append(" | ");
                filtrosTexto.append("Funcionário selecionado");
                temFiltro = true;
            }
            
            if (dataInicial != null) {
                if (temFiltro) filtrosTexto.append(" | ");
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
            Paragraph total = new Paragraph("Total de avaliações: " + avaliacoes.size(), FONT_CELL_BOLD);
            total.setSpacingAfter(15);
            document.add(total);

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
            int index = 0;
            
            for (Avaliacao avaliacao : avaliacoes) {
                Color bgColor = (index % 2 == 0) ? Color.WHITE : COLOR_ROW_EVEN;
                
                // Código
                adicionarCelulaDados(table, String.valueOf(avaliacao.getId()), Element.ALIGN_CENTER, bgColor);
                
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
                
                // Avaliação (nota numérica)
                String notaFormatada = avaliacao.getNota() != null 
                    ? String.format("%.1f", avaliacao.getNota().doubleValue())
                    : "-";
                PdfPCell cellNota = new PdfPCell(new Phrase(notaFormatada, new Font(Font.HELVETICA, 10, Font.BOLD, COLOR_RATING)));
                cellNota.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellNota.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellNota.setPadding(8);
                cellNota.setBackgroundColor(bgColor);
                cellNota.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellNota);
                
                // Comentários
                String comentario = avaliacao.getComentario() != null && !avaliacao.getComentario().isEmpty()
                    ? avaliacao.getComentario() 
                    : "Sem comentários";
                adicionarCelulaDados(table, comentario, Element.ALIGN_LEFT, bgColor);
                
                index++;
            }

            document.add(table);

            // Rodapé
            document.add(new Paragraph(" "));
            Paragraph rodape = new Paragraph("BarberSys - Sistema de Gestão de Barbearias", FONT_SUBTITLE);
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

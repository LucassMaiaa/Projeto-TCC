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

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(231, 74, 70));
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(127, 140, 141));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_CELL = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(52, 73, 94));
    private static final Font FONT_CELL_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(52, 73, 94));

    private static final Color COLOR_HEADER = new Color(231, 74, 70); // #E74A46
    private static final Color COLOR_ROW_EVEN = new Color(246, 247, 251);

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

            // Título do relatório
            Paragraph titulo = new Paragraph("Relatório de Produtividade dos Funcionários", FONT_TITLE);
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
            
            if (funcionarioId != null) {
                if (temFiltro) filtrosTexto.append(" | ");
                filtrosTexto.append("Funcionário selecionado");
                temFiltro = true;
            }
            
            if (temFiltro) {
                Paragraph filtros = new Paragraph("Filtros aplicados: " + filtrosTexto.toString(), FONT_SUBTITLE);
                filtros.setAlignment(Element.ALIGN_CENTER);
                filtros.setSpacingAfter(15);
                document.add(filtros);
            } else {
                document.add(new Paragraph(" "));
            }

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
            int index = 0;
            int codigo = 1;

            for (ProdutividadeFuncionario prod : produtividades) {
                Color bgColor = (index % 2 == 0) ? Color.WHITE : COLOR_ROW_EVEN;
                
                // Código
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Funcionário
                String funcionario = prod.getFuncionarioNome() != null ? prod.getFuncionarioNome() : "-";
                adicionarCelulaDados(table, funcionario, Element.ALIGN_LEFT, bgColor);
                
                // Data
                String dataFormatada = prod.getData() != null ? dataTabela.format(prod.getData()) : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Atendimentos Realizados
                String atendimentos = prod.getAtendimentosRealizados() != null 
                    ? String.valueOf(prod.getAtendimentosRealizados()) 
                    : "0";
                adicionarCelulaDados(table, atendimentos, Element.ALIGN_CENTER, bgColor);
                
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
                
                index++;
            }

            document.add(table);

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
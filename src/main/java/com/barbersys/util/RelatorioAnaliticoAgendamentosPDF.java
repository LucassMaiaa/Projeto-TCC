package com.barbersys.util;

import com.barbersys.model.Agendamento;
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

public class RelatorioAnaliticoAgendamentosPDF {

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

    public static void gerar(List<Agendamento> agendamentos, java.util.Date dataInicial, java.util.Date dataFinal,
            String nomeCliente, Long funcionarioId, String statusFiltro) {
        Document document = new Document(PageSize.A4.rotate(), 40, 40, 50, 50); // Paisagem para mais colunas

        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();

            externalContext.responseReset();
            externalContext.setResponseContentType("application/pdf");
            externalContext.setResponseHeader("Content-Disposition", 
                "attachment; filename=\"relatorio-analitico-agendamentos-" + System.currentTimeMillis() + ".pdf\"");

            OutputStream outputStream = externalContext.getResponseOutputStream();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Título em preto à esquerda
            Paragraph titulo = new Paragraph("Relatório Analítico de Agendamentos", TITLE_FONT);
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
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.7f, 2.0f, 2.0f, 1.0f, 0.8f, 1.2f});
            table.setSpacingBefore(10);

            // Cabeçalho da tabela
            adicionarCelulaCabecalho(table, "Cód.");
            adicionarCelulaCabecalho(table, "Cliente");
            adicionarCelulaCabecalho(table, "Funcionário");
            adicionarCelulaCabecalho(table, "Data");
            adicionarCelulaCabecalho(table, "Hora");
            adicionarCelulaCabecalho(table, "Status");

            // Preenchendo a tabela com os dados
            SimpleDateFormat dataTabela = new SimpleDateFormat("dd/MM/yyyy");
            int codigo = 1;
            
            int totalFinalizado = 0;
            int totalCancelado = 0;
            int totalPendente = 0;
            
            for (Agendamento agendamento : agendamentos) {
                Color bgColor = COR_BRANCO;
                
                // Código (usa índice sequencial)
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Cliente
                String clienteNome = agendamento.getCliente() != null && agendamento.getCliente().getNome() != null
                    ? agendamento.getCliente().getNome()
                    : (agendamento.getNomeClienteAvulso() != null ? agendamento.getNomeClienteAvulso() : "-");
                adicionarCelulaDados(table, clienteNome, Element.ALIGN_LEFT, bgColor);
                
                // Funcionário
                String nomeFuncionario = agendamento.getFuncionario() != null 
                    ? agendamento.getFuncionario().getNome() 
                    : "-";
                adicionarCelulaDados(table, nomeFuncionario, Element.ALIGN_LEFT, bgColor);
                
                // Data
                String dataFormatada = agendamento.getDataCriado() != null 
                    ? dataTabela.format(agendamento.getDataCriado()) 
                    : "-";
                adicionarCelulaDados(table, dataFormatada, Element.ALIGN_CENTER, bgColor);
                
                // Hora
                String hora = agendamento.getHoraSelecionada() != null ? agendamento.getHoraSelecionada().toString() : "-";
                adicionarCelulaDados(table, hora, Element.ALIGN_CENTER, bgColor);
                
                // Status
                String status = agendamento.getStatus();
                String statusDesc = "";
                Color bgColorStatus = COR_BRANCO;
                
                if ("F".equals(status)) {
                    statusDesc = "FINALIZADO";
                    bgColorStatus = COR_VERDE_CLARO;
                    totalFinalizado++;
                } else if ("I".equals(status)) {
                    statusDesc = "CANCELADO";
                    bgColorStatus = COR_VERMELHO_CLARO;
                    totalCancelado++;
                } else if ("A".equals(status)) {
                    statusDesc = "PENDENTE";
                    bgColorStatus = COR_LARANJA_CLARO;
                    totalPendente++;
                }
                
                PdfPCell cellStatus = new PdfPCell(new Phrase(statusDesc, CELL_BOLD_FONT));
                cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellStatus.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellStatus.setPadding(7);
                cellStatus.setBackgroundColor(bgColorStatus);
                cellStatus.setBorderWidth(1);
                cellStatus.setBorderColor(COR_BORDA_EXCEL);
                table.addCell(cellStatus);
            }

            document.add(table);

            // Totalizadores
            document.add(new Paragraph("\n\n"));
            Paragraph totalizadores = new Paragraph(
                "TOTAIS: Finalizados: " + totalFinalizado +
                " | Cancelados: " + totalCancelado +
                " | Pendentes: " + totalPendente,
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
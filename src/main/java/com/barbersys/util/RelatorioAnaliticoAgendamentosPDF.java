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

    private static final Font FONT_TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(44, 62, 80));
    private static final Font FONT_SUBTITLE = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(127, 140, 141));
    private static final Font FONT_HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font FONT_CELL = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(52, 73, 94));
    private static final Font FONT_CELL_BOLD = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(52, 73, 94));

    private static final Color COLOR_HEADER = new Color(231, 74, 70); // #E74A46
    private static final Color COLOR_ROW_EVEN = new Color(246, 247, 251);
    private static final Color COLOR_BADGE_SUCCESS = new Color(16, 185, 129); // #10B981
    private static final Color COLOR_BADGE_DANGER = new Color(239, 68, 68); // #EF4444
    private static final Color COLOR_BADGE_WARNING = new Color(245, 158, 11); // #F59E0B

    public static void gerar(List<Agendamento> agendamentos, java.util.Date dataInicial, java.util.Date dataFinal,
            Long clienteId, Long funcionarioId, String statusFiltro) {
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

            // Título do relatório
            Paragraph titulo = new Paragraph("Relatório Analítico de Agendamentos", FONT_TITLE);
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
            
            if (clienteId != null) {
                if (temFiltro) filtrosTexto.append(" | ");
                filtrosTexto.append("Cliente selecionado");
                temFiltro = true;
            }
            
            if (funcionarioId != null) {
                if (temFiltro) filtrosTexto.append(" | ");
                filtrosTexto.append("Funcionário selecionado");
                temFiltro = true;
            }
            
            if (statusFiltro != null && !statusFiltro.trim().isEmpty()) {
                if (temFiltro) filtrosTexto.append(" | ");
                String statusDesc = "";
                switch (statusFiltro) {
                    case "F": statusDesc = "Finalizados"; break;
                    case "I": statusDesc = "Cancelados"; break;
                    case "A": statusDesc = "Pendentes"; break;
                }
                filtrosTexto.append("Status: ").append(statusDesc);
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
            Paragraph total = new Paragraph("Total de agendamentos: " + agendamentos.size(), FONT_CELL_BOLD);
            total.setSpacingAfter(15);
            document.add(total);

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
            int index = 0;
            int codigo = 1;
            
            int totalFinalizado = 0;
            int totalCancelado = 0;
            int totalPendente = 0;
            
            for (Agendamento agendamento : agendamentos) {
                Color bgColor = (index % 2 == 0) ? Color.WHITE : COLOR_ROW_EVEN;
                
                // Código (usa índice sequencial)
                adicionarCelulaDados(table, String.valueOf(codigo++), Element.ALIGN_CENTER, bgColor);
                
                // Cliente
                String nomeCliente = agendamento.getCliente() != null && agendamento.getCliente().getNome() != null
                    ? agendamento.getCliente().getNome()
                    : (agendamento.getNomeClienteAvulso() != null ? agendamento.getNomeClienteAvulso() : "-");
                adicionarCelulaDados(table, nomeCliente, Element.ALIGN_LEFT, bgColor);
                
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
                Color statusColor = COLOR_BADGE_SUCCESS;
                
                if ("F".equals(status)) {
                    statusDesc = "FINALIZADO";
                    statusColor = COLOR_BADGE_SUCCESS;
                    totalFinalizado++;
                } else if ("I".equals(status)) {
                    statusDesc = "CANCELADO";
                    statusColor = COLOR_BADGE_DANGER;
                    totalCancelado++;
                } else if ("A".equals(status)) {
                    statusDesc = "PENDENTE";
                    statusColor = COLOR_BADGE_WARNING;
                    totalPendente++;
                }
                
                PdfPCell cellStatus = new PdfPCell(new Phrase(statusDesc, new Font(Font.HELVETICA, 8, Font.BOLD, statusColor)));
                cellStatus.setHorizontalAlignment(Element.ALIGN_CENTER);
                cellStatus.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cellStatus.setPadding(8);
                cellStatus.setBackgroundColor(bgColor);
                cellStatus.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellStatus);
                
                index++;
            }

            document.add(table);

            // Totalizadores
            document.add(new Paragraph(" "));
            Paragraph totalizadores = new Paragraph(
                "TOTAIS: Finalizados: " + totalFinalizado + 
                " | Cancelados: " + totalCancelado + 
                " | Pendentes: " + totalPendente, 
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

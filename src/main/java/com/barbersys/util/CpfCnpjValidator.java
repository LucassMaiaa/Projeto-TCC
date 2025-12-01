package com.barbersys.util;

/**
 * Classe utilitária para validação de CPF e CNPJ
 */
public class CpfCnpjValidator {

    /**
     * Valida CPF ou CNPJ automaticamente baseado no tamanho
     * @param documento CPF ou CNPJ (com ou sem formatação)
     * @return true se válido, false caso contrário
     */
    public static boolean validarDocumento(String documento) {
        if (documento == null || documento.trim().isEmpty()) {
            return false;
        }
        
        // Remove formatação
        documento = documento.replaceAll("[^0-9]", "");
        
        // Verifica tamanho
        if (documento.length() == 11) {
            return validarCPF(documento);
        } else if (documento.length() == 14) {
            return validarCNPJ(documento);
        }
        
        return false;
    }
    
    /**
     * Valida CPF usando algoritmo de verificação de dígitos
     * @param cpf CPF com 11 dígitos (apenas números)
     * @return true se válido, false caso contrário
     */
    public static boolean validarCPF(String cpf) {
        if (cpf == null) {
            return false;
        }
        
        // Remove formatação
        cpf = cpf.replaceAll("[^0-9]", "");
        
        // Verifica se tem 11 dígitos
        if (cpf.length() != 11) {
            return false;
        }
        
        // Verifica se todos os dígitos são iguais (ex: 111.111.111-11)
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        try {
            // Calcula o primeiro dígito verificador
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito >= 10) {
                primeiroDigito = 0;
            }
            
            // Verifica o primeiro dígito
            if (Character.getNumericValue(cpf.charAt(9)) != primeiroDigito) {
                return false;
            }
            
            // Calcula o segundo dígito verificador
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito >= 10) {
                segundoDigito = 0;
            }
            
            // Verifica o segundo dígito
            return Character.getNumericValue(cpf.charAt(10)) == segundoDigito;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Valida CNPJ usando algoritmo de verificação de dígitos
     * @param cnpj CNPJ com 14 dígitos (apenas números)
     * @return true se válido, false caso contrário
     */
    public static boolean validarCNPJ(String cnpj) {
        if (cnpj == null) {
            return false;
        }
        
        // Remove formatação
        cnpj = cnpj.replaceAll("[^0-9]", "");
        
        // Verifica se tem 14 dígitos
        if (cnpj.length() != 14) {
            return false;
        }
        
        // Verifica se todos os dígitos são iguais (ex: 11.111.111/1111-11)
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }
        
        try {
            // Calcula o primeiro dígito verificador
            int[] pesosPrimeiroDigito = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesosPrimeiroDigito[i];
            }
            int primeiroDigito = soma % 11;
            primeiroDigito = (primeiroDigito < 2) ? 0 : (11 - primeiroDigito);
            
            // Verifica o primeiro dígito
            if (Character.getNumericValue(cnpj.charAt(12)) != primeiroDigito) {
                return false;
            }
            
            // Calcula o segundo dígito verificador
            int[] pesosSegundoDigito = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += Character.getNumericValue(cnpj.charAt(i)) * pesosSegundoDigito[i];
            }
            int segundoDigito = soma % 11;
            segundoDigito = (segundoDigito < 2) ? 0 : (11 - segundoDigito);
            
            // Verifica o segundo dígito
            return Character.getNumericValue(cnpj.charAt(13)) == segundoDigito;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Remove formatação de CPF ou CNPJ
     * @param documento CPF ou CNPJ formatado
     * @return Apenas números
     */
    public static String removerFormatacao(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("[^0-9]", "");
    }
    
    /**
     * Formata CPF (###.###.###-##)
     * @param cpf CPF apenas números
     * @return CPF formatado
     */
    public static String formatarCPF(String cpf) {
        if (cpf == null) {
            return "";
        }
        cpf = removerFormatacao(cpf);
        if (cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + 
               cpf.substring(3, 6) + "." + 
               cpf.substring(6, 9) + "-" + 
               cpf.substring(9, 11);
    }
    
    /**
     * Formata CNPJ (##.###.###/####-##)
     * @param cnpj CNPJ apenas números
     * @return CNPJ formatado
     */
    public static String formatarCNPJ(String cnpj) {
        if (cnpj == null) {
            return "";
        }
        cnpj = removerFormatacao(cnpj);
        if (cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + 
               cnpj.substring(2, 5) + "." + 
               cnpj.substring(5, 8) + "/" + 
               cnpj.substring(8, 12) + "-" + 
               cnpj.substring(12, 14);
    }
    
    /**
     * Identifica o tipo de documento
     * @param documento CPF ou CNPJ
     * @return "CPF", "CNPJ" ou "INVÁLIDO"
     */
    public static String identificarTipo(String documento) {
        if (documento == null) {
            return "INVÁLIDO";
        }
        documento = removerFormatacao(documento);
        if (documento.length() == 11) {
            return "CPF";
        } else if (documento.length() == 14) {
            return "CNPJ";
        }
        return "INVÁLIDO";
    }
}

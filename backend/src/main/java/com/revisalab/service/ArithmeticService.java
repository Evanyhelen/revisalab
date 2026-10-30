package com.revisalab.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Avalia expressões aritméticas simples ("quanto é 200 + 50?", "10 * 3", ...)
 * de forma exata e local, sem depender da IA.
 *
 * Modelos de linguagem frequentemente erram contas — em especial com números
 * muito grandes — então esse serviço intercepta perguntas puramente aritméticas
 * antes do {@link AiService} e calcula o resultado com {@link BigDecimal}.
 */
@Component
public class ArithmeticService {

    // Precisão usada apenas em divisões que não terminam (ex.: 1/3).
    private static final MathContext DIV_CTX = new MathContext(12, RoundingMode.HALF_UP);

    // Expoente máximo permitido em "x^y", para evitar cálculos absurdamente lentos.
    private static final int MAX_EXPONENT = 1000;

    // Expressão aritmética "pura": começa por um número e contém pelo menos um operador.
    private static final Pattern EXPR = Pattern.compile(
            "[0-9][0-9\\s.,+*/^×÷()\\-]*[+*/^×÷\\-][0-9\\s.,+*/^×÷()\\-]*");

    /**
     * Tenta interpretar a pergunta como uma expressão aritmética e devolvê-la já
     * resolvida. Retorna {@code null} quando a pergunta não é aritmética pura
     * (ex.: problemas algébricos com variáveis), para que o fluxo normal siga.
     */
    public String tryEvaluate(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }

        String normalized = normalize(question);
        Matcher matcher = EXPR.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }

        String expr = matcher.group().trim();
        if (countNumbers(expr) < 2) {
            return null;
        }

        try {
            BigDecimal result = new Parser(expr).parse();
            if (result == null) {
                return null;
            }
            return "**" + expr + " = " + format(result) + "**";
        } catch (ArithmeticException | IllegalStateException | NumberFormatException e) {
            // Entrada malformada (ex.: "1.2.3 + 4" ou divisão por zero): não é
            // aritmética válida; deixa o fluxo cair para a IA/base local.
            return null;
        }
    }

    private static String normalize(String s) {
        return s.replace('×', '*')
                .replace('÷', '/')
                .replace('−', '-')
                .replace(',', '.');
    }

    /** Conta quantos números literais a expressão possui. */
    private static int countNumbers(String s) {
        int count = 0;
        boolean inNumber = false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c) || c == '.') {
                if (!inNumber) {
                    inNumber = true;
                    count++;
                }
            } else {
                inNumber = false;
            }
        }
        return count;
    }

    private static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            return stripped.toBigInteger().toString();
        }
        return stripped.toPlainString();
    }

    /** Parser recursivo descendente para + - * / ^ e parênteses. */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        BigDecimal parse() {
            skipWs();
            BigDecimal value = expr();
            skipWs();
            return pos == s.length() ? value : null; // sobrou conteúdo → não é aritmética pura
        }

        private BigDecimal expr() {
            BigDecimal left = term();
            while (true) {
                skipWs();
                if (pos < s.length() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) {
                    char op = s.charAt(pos++);
                    BigDecimal right = term();
                    left = op == '+' ? left.add(right) : left.subtract(right);
                } else {
                    return left;
                }
            }
        }

        private BigDecimal term() {
            BigDecimal left = factor();
            while (true) {
                skipWs();
                if (pos < s.length() && (s.charAt(pos) == '*' || s.charAt(pos) == '/')) {
                    char op = s.charAt(pos++);
                    BigDecimal right = factor();
                    if (op == '/') {
                        if (right.compareTo(BigDecimal.ZERO) == 0) {
                            throw new ArithmeticException("Divisão por zero");
                        }
                        left = left.divide(right, DIV_CTX);
                    } else {
                        left = left.multiply(right);
                    }
                } else {
                    return left;
                }
            }
        }

        private BigDecimal factor() {
            BigDecimal base = unary();
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '^') {
                pos++;
                BigDecimal exponent = factor(); // associatividade à direita
                int e = exponent.intValueExact();
                if (Math.abs(e) > MAX_EXPONENT) {
                    throw new ArithmeticException("Expoente muito grande");
                }
                return e >= 0 ? base.pow(e) : BigDecimal.ONE.divide(base.pow(-e), DIV_CTX);
            }
            return base;
        }

        private BigDecimal unary() {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '-') {
                pos++;
                return unary().negate();
            }
            if (pos < s.length() && s.charAt(pos) == '+') {
                pos++;
                return unary();
            }
            return primary();
        }

        private BigDecimal primary() {
            skipWs();
            if (pos >= s.length()) {
                throw new IllegalStateException("Fim inesperado");
            }
            char c = s.charAt(pos);
            if (c == '(') {
                pos++;
                BigDecimal value = expr();
                skipWs();
                if (pos < s.length() && s.charAt(pos) == ')') {
                    pos++;
                    return value;
                }
                throw new IllegalStateException("Parêntese não fechado");
            }
            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                    pos++;
                }
                return new BigDecimal(s.substring(start, pos));
            }
            throw new IllegalStateException("Token inválido: " + c);
        }

        private void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }
    }
}

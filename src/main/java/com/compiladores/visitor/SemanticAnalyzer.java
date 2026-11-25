package com.compiladores.visitor;

import com.compiladores.ast.Ast.Program;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Junta as duas passadas da análise contextual: identificação (CP4) e tipos (CP5).
 * Erros das duas são ordenados pela linha (ordenação estável: na mesma linha,
 * vale a ordem em que foram encontrados). Avisos não impedem a geração de código.
 */
public final class SemanticAnalyzer {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void analyze(Program program) {
        if (program == null) return;
        IdentificationVisitor ids = new IdentificationVisitor();
        ids.analyze(program);
        TypeChecker types = new TypeChecker(ids.getCallDeclarations());
        types.check(program);
        errors.addAll(ids.getErrors());
        errors.addAll(types.getErrors());
        errors.sort(Comparator.comparingInt(SemanticAnalyzer::lineOf));
        warnings.addAll(types.getWarnings());
        warnings.sort(Comparator.comparingInt(SemanticAnalyzer::lineOf));
    }

    private static int lineOf(String msg) {
        int start = "linha ".length(), end = msg.indexOf(':');
        try { return Integer.parseInt(msg.substring(start, end)); } catch (RuntimeException e) { return Integer.MAX_VALUE; }
    }

    public List<String> getErrors() { return List.copyOf(errors); }
    public List<String> getWarnings() { return List.copyOf(warnings); }
    /** Com erro, o compilador não segue para a geração de código. */
    public boolean canGenerateCode() { return errors.isEmpty(); }

    public String report() {
        StringBuilder b = new StringBuilder("ERROS\n");
        if (errors.isEmpty()) b.append("(nenhum)\n"); else errors.forEach(e -> b.append(e).append('\n'));
        b.append("AVISOS\n");
        if (warnings.isEmpty()) b.append("(nenhum)\n"); else warnings.forEach(w -> b.append(w).append('\n'));
        return b.toString();
    }
}

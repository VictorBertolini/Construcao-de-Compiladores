import com.compiladores.ast.Ast;
import com.compiladores.entity.Token;
import com.compiladores.entity.TokenType;
import com.compiladores.symbols.StackSymbolTable;
import com.compiladores.symbols.Symbol;
import com.compiladores.symbols.Symbol.Category;
import com.compiladores.symbols.SymbolTable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolTableTest {
    private static Symbol symbol(String name, int line, int level) {
        Ast.Type node = new Ast.Type(new Token(TokenType.RESERVED_WORD, "int", line, 1));
        return new Symbol(name, Category.VARIABLE, node, level, node);
    }

    @Test
    void declararBuscarSombrearRestaurarEConsultarNivel() {
        SymbolTable table = new StackSymbolTable();
        assertEquals(0, table.level());
        assertNull(table.lookup("inexistente"));

        Symbol outer = symbol("x", 1, table.level());
        assertTrue(table.declare(outer));
        assertSame(outer, table.lookup("x"));
        assertSame(outer, table.lookupLocal("x"));

        table.openScope();
        assertEquals(1, table.level());
        assertNull(table.lookupLocal("x"), "lookupLocal não deve enxergar o pai");
        assertSame(outer, table.lookup("x"));
        Symbol inner = symbol("x", 2, table.level());
        assertTrue(table.declare(inner));
        assertSame(inner, table.lookup("x"));
        assertSame(inner, table.lookupLocal("x"));

        table.openScope();
        assertEquals(2, table.level());
        assertNull(table.lookupLocal("x"));
        assertSame(inner, table.lookup("x"));
        table.closeScope();
        assertEquals(1, table.level());
        table.closeScope();
        assertEquals(0, table.level());
        assertSame(outer, table.lookup("x"));
    }

    @Test
    void redeclaracaoNaoSubstituiPrimeira() {
        SymbolTable table = new StackSymbolTable();
        Symbol first = symbol("a", 1, 0);
        Symbol second = symbol("a", 2, 0);
        assertTrue(table.declare(first));
        assertFalse(table.declare(second));
        assertSame(first, table.lookupLocal("a"));
        assertSame(first, table.lookup("a"));
    }

    @Test
    void buscaInexistenteDevolveNullEFecharGlobalFalhaClaramente() {
        SymbolTable table = new StackSymbolTable();
        assertNull(table.lookupLocal("a"));
        assertNull(table.lookup("a"));
        IllegalStateException exception = assertThrows(IllegalStateException.class, table::closeScope);
        assertTrue(exception.getMessage().contains("global"));
        assertEquals(0, table.level());
    }
}

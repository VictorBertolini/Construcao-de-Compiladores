import org.junit.jupiter.api.Test;

/** Executa a suíte CP5 pelo Maven/JUnit (regras isoladas, arquivos, aninhada). */
public class TypeCheckerTest {
    @Test void regrasIsoladas() { Cp5SmokeTest.testRules(); }
    @Test void programasComSaidaEsperada() throws Exception { Cp5SmokeTest.testFixtures(); }
    @Test void tiposGravadosEFuncaoAninhada() { Cp5SmokeTest.testDecorationAndNested(); }
}

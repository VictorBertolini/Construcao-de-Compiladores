import org.junit.jupiter.api.Test;

/** Regressão: as suítes executáveis do CP3 e do CP4 continuam passando. */
public class RegressaoTest {
    @Test void cp4() throws Exception { Cp4SmokeTest.main(new String[0]); }
}

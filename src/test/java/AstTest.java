import org.junit.jupiter.api.Test;

/** Executa a mesma suíte CP3 pelo Maven/JUnit. */
public class AstTest {
    @Test
    void astVisitorsRoundTripAndCp2Regression() throws Exception {
        Cp3SmokeTest.main(new String[0]);
    }
}

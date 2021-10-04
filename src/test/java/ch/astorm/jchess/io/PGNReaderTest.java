
package ch.astorm.jchess.io;

import ch.astorm.jchess.JChessGame;
import ch.astorm.jchess.JChessGame.Status;
import ch.astorm.jchess.io.PGNReader.PGNReaderException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/*
A lot of PGN can be found here: https://www.pgnmentor.com/files.html
*/
public class PGNReaderTest {
    @Test public void testAnandPGN() throws IOException { testPGN("Anand.zip"); }
    @Test public void testNakamuraPGN() throws IOException { testPGN("Nakamura.zip"); }

    private void testPGN(String resource) throws IOException {
        String pgn;
        try(InputStream input = PGNReaderTest.class.getResourceAsStream(resource);
            ZipInputStream zis = new ZipInputStream(input)) {
            ZipEntry entry = zis.getNextEntry();
            pgn = IOUtils.toString(zis, StandardCharsets.UTF_8);
            zis.closeEntry();
        }

        int counter = 1;
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            System.out.print("Parsing game "+counter+"...");
            JChessGame game = parser.readGame();
            while(game!=null) {
                System.out.println(" done. ("+game.getPosition().getMoveHistory().size()+" moves)");
                assertNotEquals(Status.NOT_FINISHED, game.getStatus());

                ++counter;
                System.out.print("Parsing game "+counter+"...");
                game = parser.readGame();
            }
            System.out.println(" complete.");
        } catch(PGNReaderException pe) {
            System.err.println("Failure of a game in "+resource);
            System.out.println("Game metadata:");
            pe.getGame().getMetadata().forEach((k,v) -> System.out.println("- "+k+": "+v));
            fail("Unable to parse PGN game", pe);
        }
    }

    @Test
    public void testValidPGN() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4  0-1";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game = parser.readGame();
            assertEquals(Status.WIN_BLACK, game.getStatus());
        }
    }

    @Test
    public void testMuptileValidPGN() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4  0-1\n\n" +
                        "[Event \"Wch U20\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4  1-0\n\n" +
                        "[Event \"Wch U20\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4  1/2-1/2";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game1 = parser.readGame();
            assertEquals(Status.WIN_BLACK, game1.getStatus());
            JChessGame game2 = parser.readGame();
            assertEquals(Status.WIN_WHITE, game2.getStatus());
            JChessGame game3 = parser.readGame();
            assertEquals(Status.DRAW, game3.getStatus());
            JChessGame game4 = parser.readGame();
            assertNull(game4);
        }
    }

    @Test
    public void testInvalidPGN() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "[Site \"Kiljava\"]\n" +
                        "[Date \"1984.??.??\"]\n" +
                        "[Round \"?\"]\n" +
                        "[White \"Anand, Viswanathan\"]\n" +
                        "[Black \"Wolff, Patrick G\"]\n" +
                        "[Result \"0-1\"]\n" +
                        "[WhiteElo \"2285\"]\n" +
                        "[BlackElo \"2225\"]\n" +
                        "[ECO \"B09\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d5  0-1";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game = parser.readGame();
            fail("Parsing should have failed");
        } catch(PGNReaderException pe) {
            assertNotNull(pe.getGame());
            assertEquals(3, pe.getMoves().size());
        }
    }

    @Test
    public void testInvalidPGN2() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "[Site \"Kiljava\"]\n" +
                        "[Date \"1984.??.??\"]\n" +
                        "[Round \"?\"]\n" +
                        "[White \"Anand, Viswanathan\"]\n" +
                        "[Black \"Wolff, Patrick G\"]\n" +
                        "[Result \"0-1\"]\n" +
                        "[WhiteElo \"2285\"]\n" +
                        "[BlackElo \"2225\"]\n" +
                        "[ECO \"B09\"]\n" +
                        "\n" +
                        "0.e4 d6 1.d5  0-1";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game = parser.readGame();
            fail("Parsing should have failed");
        } catch(IllegalArgumentException pe) {
            /* ok */
        }
    }

    @Test
    public void testInvalidResultGame() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "[Site \"Kiljava\"]\n" +
                        "[Date \"1984.??.??\"]\n" +
                        "[Round \"?\"]\n" +
                        "[White \"Anand, Viswanathan\"]\n" +
                        "[Black \"Wolff, Patrick G\"]\n" +
                        "[Result \"0-1\"]\n" +
                        "[WhiteElo \"2285\"]\n" +
                        "[BlackElo \"2225\"]\n" +
                        "[ECO \"B09\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4  1-1";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game = parser.readGame();
            fail("Parsing should have failed");
        } catch(IllegalStateException pe) {
            /* ok */
        }
    }

    @Test
    public void testUnfinishedGame() throws Exception {
        String pgn =    "[Event \"Wch U20\"]\n" +
                        "[Site \"Kiljava\"]\n" +
                        "[Date \"1984.??.??\"]\n" +
                        "[Round \"?\"]\n" +
                        "[White \"Anand, Viswanathan\"]\n" +
                        "[Black \"Wolff, Patrick G\"]\n" +
                        "[Result \"0-1\"]\n" +
                        "[WhiteElo \"2285\"]\n" +
                        "[BlackElo \"2225\"]\n" +
                        "[ECO \"B09\"]\n" +
                        "\n" +
                        "1.e4 d6 2.d4 d5";
        try(PGNReader parser = new PGNReader(new StringReader(pgn))) {
            JChessGame game = parser.readGame();
            fail("Parsing should have failed");
        } catch(IllegalStateException pe) {
            /* ok */
        }
    }
}
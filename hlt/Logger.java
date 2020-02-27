package hlt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    private static Logger ourInstance = new Logger();

    public static Logger getInstance() {
        return ourInstance;
    }

    private Logger() {
        try {
            writer = new BufferedWriter(new FileWriter("/home/andrei/Desktop/halite3/src/game.log"));
        } catch (IOException e) {
            // nothing to do
        }
    }

    private static BufferedWriter writer;

    public void enter(Object data) throws IOException {
        writer.write("<><><><><><><><><><><><><><><>\n\t");
        writer.write(data.toString());
        writer.write("\n<><><><><><><><><><><><><><><>\n");
    }

    public void endTurn(Game game) {
        if (game.turnNumber >= Constants.MAX_TURNS) {
            try {
                writer.close();
            } catch (Exception e) {
                // nothing to do
            }
        }
    }
}

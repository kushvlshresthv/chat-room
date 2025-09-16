package client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public class JLineDemo {

    enum Color {
        RED(AttributedStyle.RED),
        GREEN(AttributedStyle.GREEN),
        BLUE(AttributedStyle.BLUE),
        YELLOW(AttributedStyle.YELLOW),
        CYAN(AttributedStyle.CYAN),
        MAGENTA(AttributedStyle.MAGENTA);

        private final int style;

        Color(int style) { this.style = style; }

        public int getStyle() { return style; }
    }

    // Print colored text using reader.printAbove
    public static void printColor(LineReader reader, String message, Color color) {
        AttributedString colored = new AttributedString(message,
                AttributedStyle.DEFAULT.foreground(color.getStyle()));
        reader.printAbove(colored.toAnsi()); // Use printAbove for prompt safety
    }

    public static void main(String[] args) throws Exception {
        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        // Background thread
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(3000);

                    reader.printAbove("[Background] Some log message");

                    printColor(reader, "This is RED text", Color.RED);
                    printColor(reader, "This is GREEN text", Color.GREEN);
                    printColor(reader, "This is BLUE text", Color.BLUE);
                    printColor(reader, "This is YELLOW text", Color.YELLOW);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Main input loop
        while (true) {
            String line = reader.readLine("> ");

            if ("exit".equalsIgnoreCase(line.trim())) {
                break;
            }

            // For main loop, you can still use printAbove or normal println
            reader.printAbove("You typed: " + line);
        }

        terminal.close();
    }
}

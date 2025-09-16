package utils;

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.LinkedList;
import java.util.Queue;

public class ColorPrint {

    static int MAX_LINE_LENGTH = 50;
    static int MAX_AVAILABLE_LENGTH = 50 / 2;

    public static void print(LineReader reader, String message, int color) {
        AttributedString colored = new AttributedString(message,
                AttributedStyle.DEFAULT.foreground(color));

        reader.printAbove(colored.toAnsi()); //NOTE: printAbove removes the readLine("> ") strings, prints something, and then again writes the readLine("> ") string
    }


    //There is the total char-length of the message
    //50% can be used by 'my'
    //first line is right aligned
    //if the message exceeds 50% then rest of the parts are shown in another line
    //then the rest is shown by adding 50% blank spaces(basically left aligned)
    public static void printUserMessage(LineReader reader, String username, int usernameColor, String message) {
        int MAX_USERNAME_LENGTH = 10;
        username = String.format("%-"+MAX_USERNAME_LENGTH +"s", username.trim());
        int messageColor;
        AttributedStringBuilder lineWithColor = new AttributedStringBuilder();

        Queue<AttributedStringBuilder> messagesWithColor;


        if (message.length() > MAX_LINE_LENGTH) {
            messagesWithColor = new LinkedList<>();
            String[] tokens = message.split(" ");
            StringBuilder line = new StringBuilder();

            int tokenCounter = 0;

            while (tokenCounter < tokens.length) {
                while (tokenCounter < tokens.length &&
                        line.length() + tokens[tokenCounter].length() + 1 < MAX_AVAILABLE_LENGTH) {
                    line.append(tokens[tokenCounter]).append(" ");
                    tokenCounter++;
                }
                if (messagesWithColor.isEmpty()) {
                    lineWithColor.append(username, AttributedStyle.DEFAULT.foreground(usernameColor));
                    lineWithColor.append(": ");
                    lineWithColor.append(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
                    messagesWithColor.add(lineWithColor);
                    lineWithColor = new AttributedStringBuilder();
                    line = new StringBuilder();
                } else {
                    messagesWithColor.add(lineWithColor.append(" ".repeat(MAX_USERNAME_LENGTH + 2 /*accounts for :<space> */) + line.toString(), AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)));
                    lineWithColor = new AttributedStringBuilder();
                    line = new StringBuilder();
                }
            }
            while (!messagesWithColor.isEmpty()) {
                reader.printAbove(messagesWithColor.poll().toAnsi());
            }
        } else {
            lineWithColor.append(username, AttributedStyle.DEFAULT.foreground(usernameColor));
            // Separator in DEFAULT (white/normal)

            lineWithColor.append(": ");

            lineWithColor.append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));

            // Print using printAbove so prompt is redrawn correctly
            reader.printAbove(lineWithColor.toAnsi());
        }

    }

    public static void printMyMessage(LineReader reader, String message) {
        Queue<String> messages;

        if (message.length() > MAX_LINE_LENGTH) {
            messages = new LinkedList<>();
            String[] tokens = message.split(" ");
            StringBuilder line = new StringBuilder();

            int tokenCounter = 0;

            while (tokenCounter < tokens.length) {
                while (tokenCounter < tokens.length &&
                        line.length() + tokens[tokenCounter].length() + 1 /* for <space> appended at the end*/ < MAX_AVAILABLE_LENGTH) {
                    line.append(tokens[tokenCounter]).append(" ");
                    tokenCounter++;
                }
                if (messages.isEmpty()) {
                    messages.add(String.format("%" + MAX_LINE_LENGTH + "s", line.toString()));
                    line = new StringBuilder();
                } else {
                    int padding = MAX_LINE_LENGTH - messages.peek().trim().length() - 1;
                    messages.add(" ".repeat(padding) + line.toString());
                    line = new StringBuilder();
                }
            }
            while (!messages.isEmpty()) {
                reader.printAbove(messages.poll());
            }
        } else {
            message = String.format("%" + MAX_LINE_LENGTH + "s", message);
            reader.printAbove(message);
        }
    }
}



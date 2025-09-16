package utils;

import client.JLineDemo;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class ColorPrint {
    public static void print(LineReader reader, String message, int color) {
        AttributedString colored = new AttributedString(message,
                AttributedStyle.DEFAULT.foreground(color));

        reader.printAbove(colored.toAnsi()); //NOTE: printAbove removes the readLine("> ") strings, prints something, and then again writes the readLine("> ") string
    }


    public static void printUserMessage(LineReader reader, String username, int usernameColor, String message) {
        username = String.format("%-10s", username.trim());
        int messageColor;
        AttributedStringBuilder sb = new AttributedStringBuilder();

        // Message in CYAN

        if(!username.trim().equals("you")) {
            sb.append(username, AttributedStyle.DEFAULT.foreground(usernameColor));

            // Separator in DEFAULT (white/normal)
            sb.append(": ");
            messageColor = AttributedStyle.CYAN;
        } else {
            message = String.format("%40s", message.trim());
            messageColor = AttributedStyle.WHITE;
        }

        sb.append(message, AttributedStyle.DEFAULT.foreground(messageColor));


        // Print using printAbove so prompt is redrawn correctly
        reader.printAbove(sb.toAnsi());
    }
}



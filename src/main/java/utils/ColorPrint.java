package utils;

import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.LinkedList;
import java.util.Queue;

public class ColorPrint {

    private static final int TOTAL_LINE_LENGTH = 65;
    /**
     * maximum available space for 'my messages' out of total space
     * */
    private static final int MAX_AVAILABLE_LENGTH = TOTAL_LINE_LENGTH / 2;
    private static final int COLOR_GRAY = 238;
    private static final int COLOR_DARK_GRAY = 237;

    public static void print(LineReader reader, String message, int color) {
        AttributedString colored = new AttributedString(message,
                AttributedStyle.DEFAULT.foreground(color));

        reader.printAbove(colored.toAnsi()); //NOTE: printAbove removes the readLine("> ") strings, prints something, and then again writes the readLine("> ") string
    }

    public static void printAtCenterWithBox(LineReader reader, String message, int color) {
        message = message.trim();
        int messageLength = message.length();

        message = "| " + message + " |";
        String coloredMessage = new AttributedString(message,
                AttributedStyle.DEFAULT.foreground(color)).toAnsi();
        messageLength +=4;


        String boxBorder = "-".repeat(messageLength);
        String coloredBoxBorder = new AttributedString(boxBorder, AttributedStyle.DEFAULT.foreground(color)).toAnsi();

        //Centering by add spaces
        String centeredAndColoredMessage = " ".repeat((TOTAL_LINE_LENGTH - messageLength)/2) + coloredMessage;
        String centeredAndColoredBoxBorder = " ".repeat((TOTAL_LINE_LENGTH - messageLength)/2) + coloredBoxBorder;

        reader.printAbove(centeredAndColoredBoxBorder);
        reader.printAbove(centeredAndColoredMessage);
        reader.printAbove(centeredAndColoredBoxBorder);
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


        if (message.length() > MAX_AVAILABLE_LENGTH) {
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

                line.insert(0, " ");

                //making all lines length = MAX_AVAILABLE_LENGTH by appending <space>
                line.append(" ".repeat(MAX_AVAILABLE_LENGTH - line.length()));

                if(messagesWithColor.isEmpty()) {
                    lineWithColor.append(username, AttributedStyle.DEFAULT.foreground(usernameColor)).append(": ");
                } else {
                    //prepend non-first line with spaces for alignment
                    lineWithColor.append(" ".repeat(12)); //10 for username + ":" + " "
                }

                lineWithColor.append(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).background(COLOR_DARK_GRAY));

                messagesWithColor.add(lineWithColor);

                lineWithColor = new AttributedStringBuilder();
                line = new StringBuilder();
            }
            while (!messagesWithColor.isEmpty()) {
                reader.printAbove(messagesWithColor.poll().toAnsi());
            }
        } else {
            lineWithColor.append(username, AttributedStyle.DEFAULT.foreground(usernameColor));
            // Separator in DEFAULT (white/normal)

            lineWithColor.append(": ");

            lineWithColor.append(" " + message + " ", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).background(COLOR_DARK_GRAY));

            // Print using printAbove so prompt is redrawn correctly
            reader.printAbove(lineWithColor.toAnsi());
        }

    }

    public static void printMyMessage(LineReader reader, String message) {
        Queue<String> messages;

        if (message.length() > MAX_AVAILABLE_LENGTH) {
            messages = new LinkedList<>();
            String[] tokens = message.split(" ");

            //string builder is used so the string is changed frequently
            StringBuilder line = new StringBuilder();

            int tokenCounter = 0;

            while (tokenCounter < tokens.length) {

                //form a line with line.length smaller than MAX_AVAILABLE_LENGTH
                while (tokenCounter < tokens.length &&
                        line.length() + tokens[tokenCounter].length() + 1 /* for <space> appended at the end*/ < MAX_AVAILABLE_LENGTH) {
                    line.append(tokens[tokenCounter]).append(" ");
                    //background colors to the line
                    tokenCounter++;
                }

                //removing the last <space> and adding one at the start
                line.deleteCharAt(line.length() - 1);
                line.insert(0, " ");

                //making each line the same length for consistent background
                //by adding spaces to the right
                line.append(" ".repeat(MAX_AVAILABLE_LENGTH - line.length()));

                String lineWithBackground = new AttributedString(line.toString(), AttributedStyle.DEFAULT.background(COLOR_GRAY)).toAnsi();

                //prepending spaces so that message is right aligned
                //line.length is used because lineWithBackground has color code
                lineWithBackground = " ".repeat(TOTAL_LINE_LENGTH - MAX_AVAILABLE_LENGTH) + lineWithBackground;
                messages.add(lineWithBackground);
                line = new StringBuilder();
            }
            while (!messages.isEmpty()) {
                reader.printAbove(messages.poll());
            }
        } else {
            String messageWithBackground = new AttributedString(" " + message + " ", AttributedStyle.DEFAULT.background(COLOR_GRAY)).toAnsi();

            //prepending spaces so that message is right aligned
            messageWithBackground = " ".repeat(TOTAL_LINE_LENGTH - message.length() - 2 /*due to two spaces added*/) + messageWithBackground;
            reader.printAbove(messageWithBackground);
        }
    }


    public static void printOnlineList(LineReader reader, String list, int color) {
        String[] lines = list.split("-");
        //find maximum line length
        int maxLength = 0;
        int count = 0;
        for (String line : lines) {
            if (line.length() > maxLength) {
                maxLength = line.length();
            }
            count++;
        }



        maxLength = maxLength + 4;

        String border = "-".repeat(maxLength);

        //centering and coloring the border
        String centeredBorder = " ".repeat((TOTAL_LINE_LENGTH - maxLength)/2) + border;

        String centeredAndColoredBorder = new AttributedString(centeredBorder, AttributedStyle.DEFAULT.foreground(color)).toAnsi();

        reader.printAbove(centeredAndColoredBorder);

        for(String line: lines) {

            String centeredLine = "| " + line + " ".repeat(maxLength-line.length() - 4) + " |";
            //centering
            centeredLine = " ".repeat((TOTAL_LINE_LENGTH - maxLength)/2) + centeredLine;

            String coloredAndCenteredLine = new AttributedString(centeredLine, AttributedStyle.DEFAULT.foreground(color)).toAnsi();

            reader.printAbove(coloredAndCenteredLine);
        }

        reader.printAbove(centeredAndColoredBorder);
    }
}



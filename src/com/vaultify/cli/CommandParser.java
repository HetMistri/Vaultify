package com.vaultify.cli;

import java.util.Scanner;

/**
 * Reads user input and forwards commands to CommandRouter.
 */
public class CommandParser {

    public void start() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("vaultify> ");
            String command = scanner.nextLine().trim();

            if (command.isEmpty()) continue;

            // Pass the scanner so CommandRouter can reuse it for interactive prompts
            CommandRouter.handle(command, scanner);
        }
    }
}


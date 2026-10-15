package com.ridehailing.cli;

import java.util.LinkedHashMap;
import java.util.Map;

/** Maps a command name (e.g. "add_user") to the Command that handles it. */
public class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(String name, Command command) {
        commands.put(name, command);
    }

    public Command find(String name) {
        return commands.get(name);
    }
}

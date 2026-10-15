package com.ridehailing.cli;

import com.ridehailing.service.UserService;

/** add_user <userId> <name> */
public class AddUserCommand implements Command {
    private final UserService userService;

    public AddUserCommand(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void execute(String[] args) {
        String userId = args[0];
        String name = args[1];
        userService.registerUser(userId, name);
        System.out.println("Registered user " + userId + " (" + name + ")");
    }
}

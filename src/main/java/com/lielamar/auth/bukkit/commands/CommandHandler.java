package com.lielamar.auth.bukkit.commands;

import com.lielamar.auth.bukkit.TwoFactorAuthentication;
import com.lielamar.auth.bukkit.commands.subcommands.*;
import com.lielamar.auth.shared.handlers.AuthHandler;
import com.lielamar.auth.shared.utils.Constants;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

public class CommandHandler implements CommandExecutor {

    private final TwoFactorAuthentication main;
    private final Set<Command> commands;
    private Command loginCommand, setupCommand;

    public CommandHandler(TwoFactorAuthentication main) {
        this.main = main;
        this.main.getCommand(Constants.mainCommand).setExecutor(this);

        this.commands = new HashSet<>();
        this.setupCommands();
    }

    /**
     * Sets up all of the sub commands of /2fa
     */
    private void setupCommands() {
        loginCommand = new LoginCommand("", main);
        setupCommand = new SetupCommand("", main);
        commands.add(new EnableCommand(Constants.enableCommand, main));
        commands.add(new DisableCommand(Constants.disableCommand, main));
        commands.add(new ReloadCommand(Constants.reloadCommand, main));
        commands.add(new HelpCommand(Constants.helpCommand));
    }

    /**
     * Returns a {@link com.lielamar.auth.bukkit.commands.Command} object related to the given name
     *
     * @param name   Name/Alias of the command to return
     * @return       The command object from the commands set
     */
    public Command getCommand(String name) {
        for(Command cmd : this.commands) {
            if(cmd.getName() != null) {
                if(cmd.getName().equalsIgnoreCase(name))
                    return cmd;
            }

            if(cmd.getAliases() != null) {
                for(String s : cmd.getAliases()) {
                    if(s.equalsIgnoreCase(name))
                        return cmd;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender cs, @Nonnull org.bukkit.command.Command cmd, @Nonnull String cmdLabel, @Nonnull String[] args) {
        if(!(cs instanceof Player)) {
            cs.sendMessage(main.getMessageHandler().getMessage("&cThis command must be ran as a player"));
            return false;
        }

        Player player = (Player)cs;

        if(main.getAuthHandler() == null) {
            main.getMessageHandler().sendMessage(player, "&cSomething went wrong. Please contact a Staff Member!");
            return false;
        } else if(main.getAuthHandler().getAuthState(player.getUniqueId()) == null) {
            main.getAuthHandler().playerJoin(player.getUniqueId());
        }

        if(main.getAuthHandler().getAuthState(player.getUniqueId()).equals(AuthHandler.AuthState.PENDING_LOGIN)) {
            loginCommand.execute(player, args);
        } else if(main.getAuthHandler().getAuthState(player.getUniqueId()).equals(AuthHandler.AuthState.PENDING_SETUP)) {
            setupCommand.execute(player, args);
        } else {
            if(args.length == 0) {
                Command subCommand = getCommand(Constants.helpCommand);
                if(subCommand != null) subCommand.execute(player, args);
            } else {
                Command subCommand = getCommand(args[0]);
                if(subCommand == null) subCommand = getCommand(Constants.helpCommand);

                String[] arguments = new String[args.length-1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                subCommand.execute(player, arguments);
            }
        }
        return false;
    }
}
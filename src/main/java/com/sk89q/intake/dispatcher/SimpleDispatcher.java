/*
 * Intake, a command processing library
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) Intake team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.intake.dispatcher;

import com.google.common.base.Joiner;
import com.sk89q.intake.*;
import com.sk89q.intake.context.CommandContext;
import com.sk89q.intake.context.CommandLocals;
import com.sk89q.intake.util.auth.AuthorizationException;

import java.util.*;

/**
 * A simple implementation of {@link Dispatcher}.
 */
public class SimpleDispatcher implements Dispatcher {

    private final Map<String, CommandMapping> commands = new HashMap<String, CommandMapping>();
    private final SettableDescription description = new SettableDescription();

    /**
     * Create a new instance.
     */
    public SimpleDispatcher() {
        description.getParameters().add(new SettableParameter("subcommand"));
        SettableParameter extraArgs = new SettableParameter("...");
        extraArgs.setOptional(true);
        description.getParameters().add(extraArgs);
    }

    @Override
    public void registerCommand(CommandCallable callable, String... alias) {
        CommandMapping mapping = new ImmutableCommandMapping(callable, alias);
        
        // Check for replacements
        for (String a : alias) {
            String lower = a.toLowerCase();
            if (commands.containsKey(lower)) {
                throw new IllegalArgumentException(
                        "Replacing commands is currently undefined behavior");
            }
        }
        
        for (String a : alias) {
            String lower = a.toLowerCase();
            commands.put(lower, mapping);
        }
    }

    @Override
    public Set<CommandMapping> getCommands() {
        return Collections.unmodifiableSet(new HashSet<CommandMapping>(commands.values()));
    }
    
    @Override
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(commands.keySet());
    }
    
    @Override
    public Set<String> getPrimaryAliases() {
        Set<String> aliases = new HashSet<String>();
        for (CommandMapping mapping : getCommands()) {
            aliases.add(mapping.getPrimaryAlias());
        }
        return Collections.unmodifiableSet(aliases);
    }

    @Override
    public boolean contains(String alias) {
        return commands.containsKey(alias.toLowerCase());
    }

    @Override
    public CommandMapping get(String alias) {
        return commands.get(alias.toLowerCase());
    }

    @Override
    public boolean call(String arguments, CommandLocals locals, String[] parentCommands) throws CommandException, AuthorizationException {
        // We have permission for this command if we have permissions for subcommands
        if (!testPermission(locals)) {
            throw new AuthorizationException();
        }

        String[] split = CommandContext.split(arguments);
        Set<String> aliases = getPrimaryAliases();

        if (aliases.isEmpty()) {
            throw new InvalidUsageException("This command has no sub-commands.", this);
        } else if (split.length > 0) {
            String subCommand = split[0];
            String subArguments = Joiner.on(" ").join(Arrays.copyOfRange(split, 1, split.length));
            String[] subParents = Arrays.copyOf(parentCommands, parentCommands.length + 1);
            subParents[parentCommands.length] = subCommand;
            CommandMapping mapping = get(subCommand);

            if (mapping != null) {
                try {
                    mapping.getCallable().call(subArguments, locals, subParents);
                } catch (AuthorizationException e) {
                    throw e;
                } catch (CommandException e) {
                    e.prependStack(subCommand);
                    throw e;
                } catch (Throwable t) {
                    throw new InvocationCommandException(t);
                }

                return true;
            }

        }

        throw new InvalidUsageException("Please choose a sub-command.", this, true);
    }

    @Override
    public List<String> getSuggestions(String arguments, CommandLocals locals) throws CommandException {
        String[] split = CommandContext.split(arguments);

        if (split.length <= 1) {
            String prefix = split.length > 0 ? split[0] : "";

            List<String> suggestions = new ArrayList<String>();

            for (CommandMapping mapping : getCommands()) {
                if (mapping.getCallable().testPermission(locals)) {
                    for (String alias : mapping.getAllAliases()) {
                        if (prefix.isEmpty() || alias.startsWith(arguments)) {
                            suggestions.add(mapping.getPrimaryAlias());
                            break;
                        }
                    }
                }
            }

            return suggestions;
        } else {
            String subCommand = split[0];
            CommandMapping mapping = get(subCommand);
            String passedArguments = Joiner.on(" ").join(Arrays.copyOfRange(split, 1, split.length));

            if (mapping != null) {
                return mapping.getCallable().getSuggestions(passedArguments, locals);
            } else {
                return Collections.emptyList();
            }
        }
    }

    @Override
    public SettableDescription getDescription() {
        return description;
    }

    @Override
    public boolean testPermission(CommandLocals locals) {
        for (CommandMapping mapping : getCommands()) {
            if (mapping.getCallable().testPermission(locals)) {
                return true;
            }
        }

        return false;
    }

}

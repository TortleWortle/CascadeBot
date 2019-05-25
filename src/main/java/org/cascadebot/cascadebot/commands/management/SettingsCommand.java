/*
 * Copyright (c) 2019 CascadeBot. All rights reserved.
 * Licensed under the MIT license.
 */

package org.cascadebot.cascadebot.commands.management;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import org.cascadebot.cascadebot.commandmeta.Argument;
import org.cascadebot.cascadebot.commandmeta.ArgumentType;
import org.cascadebot.cascadebot.commandmeta.CommandContext;
import org.cascadebot.cascadebot.commandmeta.ICommandMain;
import org.cascadebot.cascadebot.commandmeta.Module;
import org.cascadebot.cascadebot.data.objects.GuildSettingsCore;
import org.cascadebot.cascadebot.data.objects.Setting;
import org.cascadebot.cascadebot.permissions.CascadePermission;
import org.cascadebot.cascadebot.utils.PasteUtils;
import org.cascadebot.cascadebot.utils.Table;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public class SettingsCommand implements ICommandMain {

    @Override
    public void onCommand(Member sender, CommandContext context) {
        Field field;
        if (context.getArgs().length == 0 || context.getArg(0).equalsIgnoreCase("list")) {
            Table.TableBuilder tableBuilder = new Table.TableBuilder("Setting", "Current value");
            GuildSettingsCore.VALUES
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .map(Map.Entry::getValue)
                    .forEach((f) -> {
                        try {
                            tableBuilder.addRow(f.getName(), String.valueOf(f.get(context.getSettings())));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    });
            PasteUtils.pasteIfLong(tableBuilder.build().toString(), 2000, context::reply);
        } else if ((field = GuildSettingsCore.VALUES.get(context.getArg(0).toLowerCase())) != null) {
            try {
                Setting settingAnnotation = field.getAnnotation(Setting.class);
                if (settingAnnotation != null) {
                    if (!context.getData().getEnabledFlags().containsAll(Arrays.asList(settingAnnotation.flagRequired()))) {
                        settingAnnotation.niceName();
                        context.getTypedMessaging().replyDanger(
                                "You cannot edit this setting! You need the: %s flag to do this!",
                                settingAnnotation.niceName()
                        );
                        return;
                    }
                }

                String value = context.getArg(1);
                if (field.getType() == boolean.class) {
                    boolean booleanValue = Boolean.valueOf(value);
                    value = String.valueOf(booleanValue);
                    field.setBoolean(context.getSettings(), booleanValue);
                } else if (field.getType() == String.class) {
                    field.set(context.getSettings(), value);
                } else {
                    return;
                }
                context.getTypedMessaging().replySuccess("Setting `%s` has been set to a value of `%s`", field.getName(), value);
            } catch (IllegalAccessException e) {
                context.getTypedMessaging().replyException("Could not access that setting!", e);
            }
        } else {
            context.getTypedMessaging().replyDanger("Cannot find that field!");
        }
    }

    @Override
    public String command() {
        return "settings";
    }

    @Override
    public String description() {
        return "Allows users to change settings for the guild";
    }

    @Override
    public Set<Argument> getUndefinedArguments() {
        return Set.of(
                Argument.of("list", "Lists the current settings for the guild", ArgumentType.COMMAND),
                Argument.of("setting", "", ArgumentType.REQUIRED, Set.of(
                        Argument.of("value", "The value for the setting", ArgumentType.REQUIRED)
                )));
    }

    @Override
    public CascadePermission getPermission() {
        return CascadePermission.of("Settings command", "settings", false, Permission.MANAGE_SERVER);
    }

    @Override
    public Module getModule() {
        return Module.MANAGEMENT;
    }

}

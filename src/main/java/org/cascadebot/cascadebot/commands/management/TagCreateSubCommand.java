/*
 * Copyright (c) 2019 CascadeBot. All rights reserved.
 * Licensed under the MIT license.
 */

package org.cascadebot.cascadebot.commands.management;

import net.dv8tion.jda.api.entities.Member;
import org.cascadebot.cascadebot.commandmeta.CommandContext;
import org.cascadebot.cascadebot.commandmeta.ISubCommand;
import org.cascadebot.cascadebot.data.objects.Tag;
import org.cascadebot.cascadebot.permissions.CascadePermission;

public class TagCreateSubCommand implements ISubCommand {

    @Override
    public void onCommand(Member sender, CommandContext context) {
        if (context.getArgs().length < 2) {
            context.getUIMessaging().replyUsage();
            return;
        }

        // Warn if the original argument contains uppercase letters
        boolean warnUppercase = !context.getArg(0).equals(context.getArg(0).toLowerCase());
        String tagName = context.getArg(0).toLowerCase();

        if (context.getCoreSettings().hasTag(tagName)) {
            context.getTypedMessaging().replyDanger(context.i18n("commands.tag.create.tag_already_exists", tagName));
            return;
        }

        String message = context.i18n("commands.tag.create.successfully_created_tag", tagName);

        if (warnUppercase) {
            message += "\n\n" + context.i18n("commands.tag.create.warn_uppercase");
        }

        context.getCoreSettings().addTag(context.getArg(0), new Tag(context.getMessage(1), "tag"));
        context.getTypedMessaging().replySuccess(message);
    }

    @Override
    public String command() {
        return "create";
    }

    @Override
    public String parent() {
        return "tag";
    }

    @Override
    public CascadePermission getPermission() {
        return CascadePermission.of("tag.create", false);
    }

}

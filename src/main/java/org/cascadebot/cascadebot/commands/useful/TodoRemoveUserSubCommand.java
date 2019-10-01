package org.cascadebot.cascadebot.commands.useful;

import net.dv8tion.jda.api.entities.Member;
import org.cascadebot.cascadebot.commandmeta.CommandContext;
import org.cascadebot.cascadebot.commandmeta.ISubCommand;
import org.cascadebot.cascadebot.data.objects.TodoList;
import org.cascadebot.cascadebot.permissions.CascadePermission;
import org.cascadebot.cascadebot.utils.DiscordUtils;

public class TodoRemoveUserSubCommand implements ISubCommand {

    @Override
    public void onCommand(Member sender, CommandContext context) {
        if (context.getArgs().length < 2) {
            context.getUIMessaging().replyUsage();
            return;
        }

        TodoList todoList = context.getData().getGuildSettingsUseful().getTodoList(context.getArg(0));

        if (todoList == null) {
            context.getTypedMessaging().replyDanger("Todo list " + context.getArg(0) + " doesn't exist");
            return;
        }

        if (todoList.getOwnerId() != context.getMember().getIdLong()) {
            context.getTypedMessaging().replyDanger("Only the owner of a todo list can remove users from the list!");
            return;
        }

        Member target = DiscordUtils.getMember(context.getGuild(), context.getArg(1));

        if (target == null) {
            context.getTypedMessaging().replyDanger("Couldn't find user `" + context.getArg(1) + "`");
            return;
        }

        todoList.removeEditUser(target);

        context.getTypedMessaging().replySuccess("Removed user " + target.getUser().getAsTag() + " to the todo list " + context.getArg(0));

    }

    @Override
    public String command() {
        return "removeuser";
    }

    @Override
    public CascadePermission getPermission() {
        return CascadePermission.of("todo.remove.user", true);
    }

    @Override
    public String parent() {
        return "toto";
    }

}

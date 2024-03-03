package tw.xserver.loader.util;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import tw.xserver.loader.base.Loader;

import javax.annotation.Nullable;

public class GlobalUtil {
    public static User getUserById(Long id) {
        return Loader.jdaBot.retrieveUserById(id).complete();
    }

    @Nullable
    public static String getExtensionByName(String fileName) {
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return null;
    }

    public static String getNickOrName(User user, Guild guild) {
        Member member;
        if ((member = guild.retrieveMemberById(user.getIdLong()).complete()) == null || member.getNickname() == null) {
            return user.getName();
        }

        return member.getNickname() + " (" + user.getName() + ')';
    }

    public static boolean checkCommand(SlashCommandInteractionEvent event, String name) {
        // correct: return 0
        return (!event.getName().equals(name));
    }
}

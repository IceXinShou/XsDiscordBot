package tw.xserver.loader.cli;

import asg.cliche.Command;
import asg.cliche.Param;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;

import static net.dv8tion.jda.internal.utils.Checks.notNull;

public class PrivateChannelCLI extends RootCLI {

    private final User user;

    public PrivateChannelCLI(User user) {
        this.user = user;
    }

    @Command(name = "dm", description = "direct message to the user")
    public void dm(
            @Param(name = "content", description = "message content") String content
    ) throws Exception {
        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        notNull(privateChannel, "Private Channel");

        privateChannel.sendMessage(content).queue();
    }
}

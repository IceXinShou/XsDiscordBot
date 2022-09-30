import com.xs.loader.PluginEvent;
import com.xs.loader.util.BasicUtil;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.PermissionERROR.permissionCheck;
import static com.xs.loader.util.SlashCommandOption.*;
import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends PluginEvent {

    public Map<String, Object> config = new HashMap<>();
    public Map<String, String> lang;
    private final String[] LANG_DEFAULT = {"en_US", "zh_TW"};
    private final String[] LANG_PARAMETERS_DEFAULT = {
            "REGISTER_NAME", "REGISTER_OPTION_MEMBER_YOU_CHOOSE", "REGISTER_OPTION_TIME_DAY",
            "REGISTER_OPTION_REASON", "NO_PERMISSION", "PERMISSION_DENIED", "SUCCESS", "UNKNOWN_ERROR"
    };

    FileGetter getter;
    BasicUtil util;

    final String TAG = "[Ban]";
    final String PATH_FOLDER_NAME = "Ban";

    @Override
    public void initLoad() {
        getter = new FileGetter(TAG, PATH_FOLDER_NAME, Main.class.getClassLoader());
        util = new BasicUtil(TAG);
        loadConfigFile();
        loadVariables();
        loadLang();
        util.println("Loaded");
    }

    @Override
    public void unload() {
        util.println("UnLoaded");
    }

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("ban", lang.get("REGISTER_NAME")).addOptions(
                        new OptionData(USER, USER_TAG, lang.get("REGISTER_OPTION_MEMBER_YOU_CHOOSE"), true),
                        new OptionData(INTEGER, DAYS, lang.get("REGISTER_OPTION_TIME_DAY")),
                        new OptionData(STRING, REASON, lang.get("REGISTER_OPTION_REASON"))
                )
        };
    }

    @Override
    public void loadConfigFile() {
        config = getter.readYml("config.yml", "plugins/" + PATH_FOLDER_NAME);
        util.println("Setting File Loaded Successfully!");
    }

    @Override
    public void loadVariables() {

    }

    @Override
    public void loadLang() {
        // expert files
        getter.exportLang(LANG_DEFAULT, LANG_PARAMETERS_DEFAULT);
        lang = getter.getLangFileData((String) config.get("Lang"));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ban")) return;
        if (!permissionCheck(BAN_MEMBERS, event))
            return;

        Member selfMember = event.getGuild().getSelfMember();
        Member member = event.getOption(USER_TAG).getAsMember();
        String reason = event.getOption(REASON) == null ? "null" : event.getOption(REASON).getAsString();

        if (!selfMember.hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("NO_PERMISSION"), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(member)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("PERMISSION_DENIED"), 0xFF0000)).queue();
            return;
        }

        int delDays = 0;
        OptionMapping option = event.getOption(DAYS);
        if (option != null)
            delDays = (int) Math.max(0, Math.min(7, option.getAsLong()));

        String userName = member.getEffectiveName();
        event.getGuild().ban(member.getUser(), delDays).reason(reason).queue(
                success -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("SUCCESS") + ' ' + userName, 0xffb1b3)).queue();
                },
                error -> {
                    event.getHook().editOriginalEmbeds(createEmbed(lang.get("UNKNOWN_ERROR"), 0xFF0000)).queue();
                }
        );
    }
}
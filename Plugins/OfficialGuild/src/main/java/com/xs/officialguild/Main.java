package com.xs.officialguild;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.json.JSONObject;

import java.util.Map;

import static com.xs.loader.MainLoader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.UrlDataGetter.getData;
import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "OG";
    private final String PATH_FOLDER_NAME = "./plugins/OfficialGuild";
    private final long ownGuildID = 858672865355890708L;
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final JsonFileManager manager = new JsonFileManager(PATH_FOLDER_NAME + "/data/userNames.json", TAG, true);
    private Category authCategory;

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, this.getClass());

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("invite", "invite a member who hasn't been invited")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .addOptions(
                                new OptionData(USER, "user", "user", true)
                                        .setDescriptionLocalizations(lang.get("register;options;user"))
                        )
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(KICK_MEMBERS))
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("invite")) return;
        if (!event.getMember().hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale local = event.getUserLocale();
    }

    @Override
    public void onReady(ReadyEvent event) {
        authCategory = jdaBot.getGuildById(858672865355890708L).getCategoryById(858672866597142539L);
        if (authCategory == null) {
            logger.warn("CANNOT FOUND Auth Category!");
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != ownGuildID) return;

        DiscordLocale local = event.getMember().getGuild().getLocale();
        User user = event.getUser();
        Channel newChannel = authCategory.createTextChannel("驗證").complete();
        if (manager.getObj().has(user.getId())) {
            // joined before
            JSONObject data = new JSONObject(getData(manager.getObj().getJSONObject(user.getId()).getString("MC_UUID")));
            if (data.has("errorMessage")) {
                // TODO:
            } else {
                String mcName = data.getString("name");
                
            }

        }

    }
}
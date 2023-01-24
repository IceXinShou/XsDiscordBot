package com.xs.dynamicvc;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Map;

import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "DynamicVC";
    private final String PATH_FOLDER_NAME = "plugins/DynamicVC";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content

    public Main() {
        super(true);
    }

    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadConfigFile();
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void unload() {
        super.unload();
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        LangGetter langGetter = new LangGetter(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT);

        // expert files
        langGetter.exportDefaultLang();
        lang = langGetter.readLangFileData();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("dynamicvc", "commands about dynamic voice chat ")
                        .setNameLocalizations(lang.get("register;cmd"))
                        .setDescriptionLocalizations(lang.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("create", "create a dynamic voice chat")
                                .setNameLocalizations(lang.get(""))
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the first channel be detected", true),
                                        new OptionData(STRING, "name", "name (%guild_name%, %user%, %user_name%, %user_tag%, %nickname%)", true),
                                        new OptionData(CHANNEL, "category", "category"),
                                        new OptionData(INTEGER, "bitrate", "bitrate"),
                                        new OptionData(INTEGER, "limit", "member limit in the chat")
                                ),
                        new SubcommandData("remove", "remove dynamic voice chat(s)")
                                .setNameLocalizations(lang.get(""))
                )
        };

    }
    public SubcommandData[] subGuildCommands() {
        return new SubcommandData[]{
                new SubcommandData("newroom", "創建自動化房間").addOptions(
                        new OptionData(CHANNEL, "detectchannel", "偵測頻道", true),
                        new OptionData(STRING, "voicename", "語音名稱(可包含空白鍵, %guild_name%, %user%, %user_name%, %user_tag%, 或 %nickname%)", true),
                        new OptionData(STRING, "textname", "文字名稱(不可包含空白鍵, %guild_name%, %user%, %user_name%, %user_tag%, 或 %nickname%)"),
                        new OptionData(CHANNEL, "voicecategory", "語音頻道目錄"),
                        new OptionData(CHANNEL, "textcategory", "文字頻道目錄"),
                        new OptionData(INTEGER, "voicebitrate", "語音位元率 (kbps)"),
                        new OptionData(INTEGER, "memberlimit", "語音人數限制 (1~99)")
                ),
                new SubcommandData("removeroom", "移除自動化房間")
                        .addOption(CHANNEL, "detectchannel", "偵測頻道", true),
        };
    }

    @Override
    public void loadConfigFile() {
        JSONObject config = new JSONObject(getter.readYml("config.yml", PATH_FOLDER_NAME));
        logger.log("Setting File Loaded Successfully");
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

    }
}
package com.xs.dynamicvc;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangGetter;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xs.loader.MainLoader.ROOT_PATH;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL;

public class Main extends PluginEvent {
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "DynamicVC";
    private final String PATH_FOLDER_NAME = "./plugins/DynamicVC";
    private Map<String, Map<DiscordLocale, String>> lang; // Label, Local, Content
    private final HashSet<Long> trackedChannel = new HashSet<>();
    private final HashSet<TrackedChannel> originChannel = new HashSet<>();

    public Main() {
        super(true);
    }


    @Override
    public void initLoad() {
        super.initLoad();
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class.getClassLoader());
        loadLang();
        loadData();
        logger.log("Loaded");
    }

    private void loadData() {
        File folder = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME + "/Data");
        folder.mkdirs();

        for (File i : folder.listFiles()) {

            JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/" + i.getName(), TAG, false);

            for (Object j : fileManager.getAry()) {
                JSONObject obj = (JSONObject) j;
                originChannel.add(new TrackedChannel(
                                Long.parseLong(i.getName().substring(0, i.getName().length() - 5)),
                                obj.getLong("category"),
                                obj.getString("name"),
                                obj.getInt("bitrate"),
                                obj.getInt("limit")
                        )
                );
            }
        }
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
                        new SubcommandData("createbychannel", "create a dynamic voice chat by channel")
                                .setNameLocalizations(lang.get("register;subcommand;create;cmd"))
                                .setDescriptionLocalizations(lang.get("register;subcommand;create;description"))
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
                                                .setDescriptionLocalizations(lang.get("register;subcommand;create;options;detect"))
                                )
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dynamicvc")) return;
        if (!event.getSubcommandName().equals("createbychannel")) return;
        DiscordLocale local = event.getUserLocale();
        try {
            long guildID = event.getGuild().getIdLong();
            long category;
            String name;
            int bitrate;
            int limit;


            VoiceChannel channel = event.getOption("detect").getAsChannel().asVoiceChannel();
            category = channel.getParentCategoryIdLong();
            name = channel.getName();
            bitrate = channel.getBitrate();
            limit = channel.getUserLimit();

            JSONObject object = new JSONObject();
            object.put("category", category);
            object.put("name", name);
            object.put("bitrate", bitrate);
            object.put("limit", limit);

            JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/" + guildID + ".json", TAG, false);
            fileManager.getAry().put(object);
            fileManager.save();

            trackedChannel.add(channel.getIdLong());

            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;success").get(local), 0x00FFFF)).queue();
        } catch (Exception e) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.get("runtime;errors;unknown").get(local), 0xFF0000)).queue();
            logger.warn(e.getMessage());
        }

    }


    @Override
    public void onGuildReady(GuildReadyEvent event) {
        for (TrackedChannel i : originChannel.stream()
                .filter(i -> event.getGuild().getIdLong() == i.guildID).collect(Collectors.toList())) {
            trackedChannel.addAll(event.getGuild().getCategoryById(i.categoryID).getVoiceChannels().stream()
                    .filter(j -> j.getName().equals(i.name)).map(ISnowflake::getIdLong).collect(Collectors.toList()));
        }
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannelUnion joinChannel = event.getChannelJoined();
        if (joinChannel != null && trackedChannel.contains(joinChannel.getIdLong())) {
            if (joinChannel.getMembers().size() == 1) { // copy a new channel
                joinChannel.createCopy().queue(i -> {
                    trackedChannel.add(i.getIdLong());
                });
            }
        }


        AudioChannelUnion leftChannel = event.getChannelLeft();
        if (leftChannel != null && trackedChannel.contains(leftChannel.getIdLong())) {
            if (leftChannel.getMembers().size() == 0) { // remove channel
                trackedChannel.remove(leftChannel.getIdLong());
                leftChannel.delete().queue();
            }
        }
    }
}
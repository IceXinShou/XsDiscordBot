package com.xs.dynamicvc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonFileManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static com.xs.loader.base.Loader.ROOT_PATH;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;
import static net.dv8tion.jda.api.interactions.commands.OptionType.CHANNEL;

public class Main extends Event {
    private static final String TAG = "DynamicVC";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/DynamicVC";
    private final HashSet<Long> trackedChannel = new HashSet<>();
    private final HashSet<TrackedChannel> originChannel = new HashSet<>();
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content

    public Main() {
        super(true);
    }


    @Override
    public void initLoad() {

        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
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
                JsonObject obj = (JsonObject) j;
                originChannel.add(new TrackedChannel(
                                Long.parseLong(i.getName().substring(0, i.getName().length() - 5)),
                                obj.get("category").getAsLong(),
                                obj.get("name").getAsString(),
                                obj.get("bitrate").getAsInt(),
                                obj.get("limit").getAsInt()
                        )
                );
            }
        }
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("dynamicvc", "commands about dynamic voice chat ")
                        .setNameLocalizations(langMap.get("register;cmd"))
                        .setDescriptionLocalizations(langMap.get("register;description"))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
                        .addSubcommands(
                        new SubcommandData("createbychannel", "create a dynamic voice chat by channel name")
                                .setNameLocalizations(langMap.get("register;subcommand;create;cmd"))
                                .setDescriptionLocalizations(langMap.get("register;subcommand;create;description"))
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
                                                .setDescriptionLocalizations(langMap.get("register;subcommand;create;options;detect"))
                                ),

                        new SubcommandData("removebychannel", "remove a dynamic voice chat by channel name")
                                .setNameLocalizations(langMap.get("register;subcommand;remove;cmd"))
                                .setDescriptionLocalizations(langMap.get("register;subcommand;remove;description"))
                                .addOptions(
                                        new OptionData(CHANNEL, "detect", "the channel be detected", true)
                                                .setDescriptionLocalizations(langMap.get("register;subcommand;remove;options;detect"))
                                )
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("dynamicvc")) return;
        if (event.getSubcommandName() == null) return;

        DiscordLocale local = event.getUserLocale();
        long guildID = event.getGuild().getIdLong();
        switch (event.getSubcommandName()) {
            case "createbychannel": {
                try {
                    long category;
                    String name;
                    int bitrate;
                    int limit;

                    VoiceChannel channel = event.getOption("detect").getAsChannel().asVoiceChannel();
                    category = channel.getParentCategoryIdLong();
                    name = channel.getName();
                    bitrate = channel.getBitrate();
                    limit = channel.getUserLimit();

                    JsonObject object = new JsonObject();
                    object.addProperty("category", category);
                    object.addProperty("name", name);
                    object.addProperty("bitrate", bitrate);
                    object.addProperty("limit", limit);

                    JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/" + guildID + ".json", TAG, false);
                    fileManager.getAry().add(object);
                    fileManager.save();

                    trackedChannel.add(channel.getIdLong());

                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;success", local), 0x00FFFF)).queue();
                } catch (Exception e) {
                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;errors;unknown", local), 0xFF0000)).queue();
                    logger.warn(Arrays.toString(e.getStackTrace()));
                    logger.warn(e.getMessage());
                }

                break;
            }

            case "removebychannel": {
                JsonFileManager fileManager = new JsonFileManager(PATH_FOLDER_NAME + "/Data/" + guildID + ".json", TAG, false);
                VoiceChannel channel = event.getOption("detect").getAsChannel().asVoiceChannel();

                JsonObject targetObject = new JsonObject();
                targetObject.addProperty("category", channel.getParentCategoryIdLong());
                targetObject.addProperty("name", channel.getName());
                targetObject.addProperty("limit", channel.getUserLimit());
                targetObject.addProperty("bitrate", channel.getBitrate());

                JsonArray array = fileManager.getAry();

                boolean removed = false;
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    if (obj.equals(targetObject)) {
                        array.remove(i);
                        removed = true;
                    }
                }

                if (removed) {
                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;remove_success", local), 0x00FFFF)).queue();
                } else {
                    event.getHook().editOriginalEmbeds(createEmbed(langManager.get("runtime;no_remove_success", local), 0x00FFFF)).queue();
                }

                break;
            }
        }
    }


    @Override
    public void onGuildReady(GuildReadyEvent event) {
        for (TrackedChannel i : originChannel.stream()
                .filter(i -> event.getGuild().getIdLong() == i.guildID).collect(Collectors.toList())) {

            Category category = event.getGuild().getCategoryById(i.categoryID);
            if (category == null) continue;

            trackedChannel.addAll(category.getVoiceChannels().stream()
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
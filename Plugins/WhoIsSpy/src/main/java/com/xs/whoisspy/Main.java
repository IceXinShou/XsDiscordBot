package com.xs.whoisspy;

import com.xs.loader.PluginEvent;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.util.FileGetter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class Main extends PluginEvent {
    private LangManager langManager;
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private FileGetter getter;
    private Logger logger;
    private static final String TAG = "WhoIsSpy";
    private final String PATH_FOLDER_NAME = "plugins/WhoIsSpy";
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private boolean start = false;
    private Message message;
    private String problem;
    private String spy_problem;
    private int spyCount;
    private int whiteCount;
    private Category category = null;
    private final List<Member> users = new ArrayList<>();
    private final List<Member> admins = new ArrayList<>();
    private WhoIsSpy game;

    public Main() {
        super(true);
    }

    private void reset() {
        message = null;
        problem = "";
        spy_problem = "";
        spyCount = 1;
        whiteCount = 0;
        category = null;
        users.clear();
        admins.clear();
        game = null;
        start = false;
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
        langManager = new LangManager(TAG, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN, this.getClass());

        langMap = langManager.getMap();
    }

    @Override
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("whoisspy", "開始誰是臥底遊戲").addOptions(
                        new OptionData(STRING, "problem", "題目", true),
                        new OptionData(STRING, "spy_problem", "臥底題目", true),
                        new OptionData(INTEGER, "spy", "臥底人數"),
                        new OptionData(INTEGER, "white", "白板人數"),
                        new OptionData(CHANNEL, "category", "開始目錄")
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("whoisspy")) return;
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        reset();

        event.getChannel().sendMessageEmbeds(createEmbed("誰是臥底遊戲~", String.format("加入人數: %d\n" +
                                "裁判人數: %d", users.size(), admins.size())
                        , 0x00FFFF))
                .addActionRow(
                        new ButtonImpl("join", "加入遊戲", ButtonStyle.SUCCESS, false, null),
                        new ButtonImpl("leave", "退出遊戲", ButtonStyle.DANGER, false, null),
                        new ButtonImpl("admin", "擔任關主", ButtonStyle.SECONDARY, false, null),
                        new ButtonImpl("start", "開始遊戲", ButtonStyle.PRIMARY, false, null)
                ).queue(i -> message = i);
        event.getHook().sendMessageEmbeds(createEmbed("已發送訊息!", 0x00FFFF)).queue();
        category = event.getOption("category") != null ? event.getOption("category").getAsChannel().asCategory() : null;
        spyCount = event.getOption("spy") != null ? event.getOption("spy").getAsInt() : 1;
        whiteCount = event.getOption("white") != null ? event.getOption("white").getAsInt() : 0;
        problem = event.getOption("problem").getAsString();
        spy_problem = event.getOption("spy_problem").getAsString();

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        switch (args[0]) {
            case "join": {
                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始，目前無法加入", 0xFF0000)).queue();
                    return;
                }
                if (users.contains(event.getMember())) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你已經加入!", 0xFF0000)).queue();
                } else {
                    users.add(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入", 0x448844)).queue();
                }
                updateMessage();
                break;
            }
            case "leave": {
                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始，目前無法退出", 0xFF0000)).queue();
                    return;
                }
                if (users.contains(event.getMember())) {
                    users.remove(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已退出", 0x448844)).queue();
                } else {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你並未加入!", 0xFF0000)).queue();
                }
                updateMessage();
                break;
            }
            case "admin": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }
                if (start) {
                    if (admins.contains(event.getMember())) {
                        admins.remove(event.getMember());
                        game.removeMember(event.getMember());
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判", 0x448844)).queue();
                    } else {
                        admins.add(event.getMember());
                        game.addMember(event.getMember());
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                    }

                    updateMessage();
                    return;
                }
                if (users.contains(event.getMember())) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("請退出遊戲後再試一次!", 0xFF0000)).queue();
                    return;
                }

                if (admins.contains(event.getMember())) {
                    admins.remove(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判!", 0x448844)).queue();
                } else {
                    admins.add(event.getMember());
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                }
                updateMessage();
                break;
            }
            case "start": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始了", 0xFF0000)).queue();
                    return;
                }
                event.getInteraction().deferEdit().queue();
                message.editMessageEmbeds(createEmbed("遊戲已開始!", 0xFF0000)).setComponents().queue();
                start = true;
                game = new WhoIsSpy(
                        event.getGuild(),
                        category,
                        users,
                        admins,
                        spyCount,
                        whiteCount,
                        problem,
                        spy_problem
                );
                break;
            }

            case "getrole": {
                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getRole(event.getMember().getIdLong()), 0x00FFFF)).queue();
                break;
            }

            case "getallrole": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getAllRole(), 0x00FFFF)).queue();
                break;
            }

            case "end": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                game.endGame();
                reset();
                event.getInteraction().deferEdit().queue();
                break;
            }

            case "delete": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.getGuild().getTextChannelById(args[1]).delete().queue();
                event.getInteraction().deferEdit().queue();
                break;
            }

            case "startvote": {
                if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                game.startVote();
                break;
            }
        }
    }

    private final Map<Long, Integer> voteData = new HashMap<>();


//    @Override
//    public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
//        String[] args = event.getComponentId().split(":");
//
//        switch (args[0]) {
//            case "vote": {
//                long id = Long.parseLong(args[1]);
//                voteData.put(id, voteData.getOrDefault(id, 1));
//                break;
//            }
//        }
//    }

    public void updateMessage() {
        message.editMessageEmbeds(
                createEmbed("誰是臥底遊戲~", String.format("遊玩人數: %d\n" +
                                "關主人數: %d", users.size(), admins.size())
                        , 0x00FFFF)
        ).queue();
    }
}
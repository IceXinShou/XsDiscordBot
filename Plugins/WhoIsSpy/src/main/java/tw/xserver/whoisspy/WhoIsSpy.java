package tw.xserver.whoisspy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.plugin.Event;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class WhoIsSpy extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(WhoIsSpy.class);
    private static final String PATH_FOLDER_NAME = "plugins/WhoIsSpy";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private final List<Member> users = new ArrayList<>();
    private final List<Member> admins = new ArrayList<>();
    private final Map<Long, Integer> voteData = new HashMap<>();
    private boolean start = false;
    private Message message;
    private String problem;
    private String spy_problem;
    private int spyCount;
    private int whiteCount;
    private Category category = null;
    private GameManager game;

    public WhoIsSpy() {
        super(true);

        reloadAll();
        LOGGER.info("loaded WhoIsSpy");
    }

    @Override
    public void unload() {
        LOGGER.info("unLoaded WhoIsSpy");
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
    public CommandData[] guildCommands() {
        return new CommandData[]{
                new CommandDataImpl("who-is-spy", "開始誰是臥底遊戲").addOptions(
                        new OptionData(STRING, "problem", "題目", true),
                        new OptionData(STRING, "spy_problem", "臥底題目", true),
                        new OptionData(INTEGER, "spy", "臥底人數"),
                        new OptionData(INTEGER, "white", "白板人數"),
                        new OptionData(CHANNEL, "category", "開始目錄")
                )
        };
    }

    @Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent event) {
        if (checkCommand(event, "who-is-spy")) return;

        Member member = event.getMember();
        notNull(member, "Member Executor");

        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
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
        category = event.getOption("category", null, OptionMapping::getAsChannel).asCategory();
        spyCount = event.getOption("spy", 1, OptionMapping::getAsInt);
        whiteCount = event.getOption("white", 0, OptionMapping::getAsInt);
        problem = event.getOption("problem", null, OptionMapping::getAsString);
        spy_problem = event.getOption("spy_problem", null, OptionMapping::getAsString);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");

        Guild guild = event.getGuild();
        Member member = event.getMember();
        notNull(member, "Member Executor");
        notNull(guild, "Guild");

        switch (args[0]) {
            case "join": {
                if (start) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("遊戲已經開始，目前無法加入", 0xFF0000)).queue();
                    return;
                }

                if (users.contains(member)) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你已經加入!", 0xFF0000)).queue();
                } else {
                    users.add(member);
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

                if (users.contains(member)) {
                    users.remove(member);
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已退出", 0x448844)).queue();
                } else {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("你並未加入!", 0xFF0000)).queue();
                }
                updateMessage();
                break;
            }

            case "admin": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }
                if (start) {

                    if (admins.contains(member)) {
                        admins.remove(member);
                        game.removeMember(member);
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判", 0x448844)).queue();
                    } else {
                        admins.add(member);
                        game.addMember(member);
                        event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                    }

                    updateMessage();
                    return;
                }
                if (users.contains(member)) {
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("請退出遊戲後再試一次!", 0xFF0000)).queue();
                    return;
                }

                if (admins.contains(member)) {
                    admins.remove(member);
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已經退出裁判!", 0x448844)).queue();
                } else {
                    admins.add(member);
                    event.getInteraction().deferReply(true).addEmbeds(createEmbed("已加入裁判", 0x448844)).queue();
                }
                updateMessage();
                break;
            }

            case "start": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
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
                game = new GameManager(
                        guild,
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
                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getRole(member.getIdLong()), 0x00FFFF)).queue();
                break;
            }

            case "getallrole": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                event.getInteraction().deferReply(true).addEmbeds(createEmbed(game.getAllRole(), 0x00FFFF)).queue();
                break;
            }

            case "end": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                game.endGame();
                reset();
                event.getInteraction().deferEdit().queue();
                break;
            }

            case "delete": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                TextChannel channel = guild.getTextChannelById(args[1]);
                if (channel != null)
                    channel.delete().queue();

                event.getInteraction().deferEdit().queue();
                break;
            }

            case "startvote": {
                if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                    event.deferEdit().queue();
                    return;
                }

                game.startVote();
                break;
            }
        }
    }


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
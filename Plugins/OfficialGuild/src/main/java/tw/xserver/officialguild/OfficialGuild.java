package tw.xserver.officialguild;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tw.xserver.loader.lang.LangManager;
import tw.xserver.loader.plugin.Event;
import tw.xserver.loader.util.FileGetter;
import tw.xserver.loader.util.json.JsonObjFileManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import static net.dv8tion.jda.api.interactions.DiscordLocale.CHINESE_TAIWAN;
import static tw.xserver.loader.base.Loader.ROOT_PATH;
import static tw.xserver.loader.base.Loader.jdaBot;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.UrlDataGetter.getData;

public class OfficialGuild extends Event {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfficialGuild.class);
    private static final String PATH_FOLDER_NAME = "plugins/OfficialGuild";
    private static final File FOLDER = new File(ROOT_PATH + '/' + PATH_FOLDER_NAME);
    private Language lang;

    private static final long OWN_GUILD_ID = 858672865355890708L;
    private static final long LOG_CHANNEL_ID = 858672865816346634L;
    private final Map<Long, UserStepData> stepData = new HashMap<>();
    private final List<Long> JOINED_ROLE_ID = Arrays.asList(858672865385119755L, 858701368457953360L, 858707058530451476L, 858703314606751764L);
    private final List<Long> AUTHED_ROLE_ID = Arrays.asList(858672865385119757L, 858704345448841226L);
    private final ScheduledExecutorService executorService;
    private Guild ownGuild;
    private JsonObjFileManager manager;
    private static final Long GOD_ROLE_ID = 1157315773769982023L;
    private static final Long APEX_ROLE_ID = 1157316094550347859L;
    private static final Long RAIL_ROLE_ID = 1157317688721420318L;
    private Role godRole;
    private Role apexRole;
    private Role railRole;

    public OfficialGuild() {
        super(true);

        reloadAll();
        executorService = Executors.newScheduledThreadPool(1);
        LOGGER.info("loaded OfficialGuild");
    }

    @Override
    public void unload() {
        executorService.shutdown();
        LOGGER.info("unLoaded OfficialGuild");
    }

    @Override
    public void reloadConfigFile() {
        getter = new FileGetter(FOLDER, OfficialGuild.class);

        if (new File(PATH_FOLDER_NAME + "/data").mkdirs()) {
            LOGGER.info("default data folder created");
        }

        manager = new JsonObjFileManager(PATH_FOLDER_NAME + "/data/userNames.json");
    }

    @Override
    public void reloadLang() {
        try {
            lang = new LangManager<>(getter, PATH_FOLDER_NAME, CHINESE_TAIWAN, Language.class).get();
        } catch (IOException | IllegalAccessException | InstantiationException | InvocationTargetException |
                 NoSuchMethodException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommandData[] guildCommands() {
        return null;
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        ownGuild = jdaBot.getGuildById(OWN_GUILD_ID);
        if (ownGuild == null) {
            LOGGER.warn("CANNOT FOUND Main Guild!");
        }

        godRole = ownGuild.getRoleById(GOD_ROLE_ID);
        apexRole = ownGuild.getRoleById(APEX_ROLE_ID);
        railRole = ownGuild.getRoleById(RAIL_ROLE_ID);

//        TextChannel channel = ownGuild.getTextChannelById(858672865444626439L);
//        Message message = channel.retrieveMessageById(1157603306345070612L).complete();
//        List<ItemComponent> components = message.getActionRows().get(0).getComponents();
//        components.add(Button.of(ButtonStyle.PRIMARY, "xs:og:role:1", "原神", Emoji.fromUnicode("💎")));
//        components.add(Button.of(ButtonStyle.PRIMARY, "xs:og:role:2", "APEX", Emoji.fromUnicode("🔫")));
//        components.add(Button.of(ButtonStyle.PRIMARY, "xs:og:role:3", "星鐵", Emoji.fromUnicode("🎇")));
//        message.editMessageComponents(ActionRow.of(components)).queue();

//        ownGuild.upsertCommand(
//                Commands.slash("create_firstjoin", "if you dont know what it is, please not to touch!")
//                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(ADMINISTRATOR))
//        ).queue();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!event.isFromType(ChannelType.PRIVATE)) return;
        if (event.getAuthor().getIdLong() != 1179666947022008371L) return;

        String messageMon = event.getMessage().getContentRaw();
        if (messageMon.length() != 2) return;
        event.getChannel().sendMessage("請稍等 ... ").queue(initialMsg -> {
            new Thread(() -> {
                StringBuilder builder = new StringBuilder();
                builder.append("```yml\n");
                try {
                    for (int i = 1; i <= 30; i++) {
                        Connection.Response rsp = Jsoup
                                .connect(String.format("https://travel.wutai.gov.tw/Travel/QuotaByDate/HYCDEMO/2024-%s-%02d", messageMon, i))
                                .referrer("https://travel.wutai.gov.tw/")
                                .execute();

                        JsonObject obj = JsonParser.parseString(rsp.body()).getAsJsonArray().get(1).getAsJsonObject();
                        builder.append(String.format("%s/%02d used: '%3s', paid: '%3s'\n",
                                messageMon, i, obj.get("used").getAsString(), obj.get("paid").getAsString()));
                        Thread.sleep(150);
                    }
                } catch (Exception e) {
                    LOGGER.error("custom error", e);
                    initialMsg.editMessage("處理時發生錯誤").queue();
                    return;
                }
                builder.append("```");

                initialMsg.editMessage(builder.toString()).queue();
            }).start();
        });
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild().getIdLong() != OWN_GUILD_ID) return;
        Member member = event.getMember();
        for (long i : JOINED_ROLE_ID) {
            Role role = ownGuild.getRoleById(i);
            if (role == null) continue;

            ownGuild.addRoleToMember(member, role).queue();
        }
    }

//    @Override
//    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
//        switch (event.getName()) {
//            case "create_firstjoin": {
//                Button button = new ButtonImpl("xs:og:create", "開始驗證", SUCCESS, false, null);
//                EmbedBuilder builder = new EmbedBuilder()
//                        .setTitle("正式加入原之序前，需要您的暱稱等資訊，方便其他成員認識您")
//                        .setDescription("原之序並不會要求您提供敏感資訊")
//                        .setThumbnail("https://i.imgur.com/6ivsnRr.png")
//                        .setFooter("公告")
//                        .setTimestamp(OffsetDateTime.now())
//                        .setColor(0x00FFFF);
//                event.getMessageChannel().sendMessageEmbeds(builder.build()).setActionRow(button).queue();
//                break;
//            }
//        }
//    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] args = event.getComponentId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("og")) return;

        switch (args[2]) {
            case "create": {
                startAuth(event);
                break;
            }

            case "chi": {
                createChineseInput(event);
                break;
            }

            case "eng": {
                createEnglishInput(event);
                break;
            }

            case "verify": {
                verifyButton(event);
                break;
            }

            case "role": {
                Member member = event.getMember();
                switch (args[3]) {
                    case "1": {
                        if (member.getRoles().contains(godRole)) {
                            ownGuild.removeRoleFromMember(member, godRole).queue();
                        } else {
                            ownGuild.addRoleToMember(member, godRole).queue();
                        }
                        break;
                    }

                    case "2": {
                        if (member.getRoles().contains(apexRole)) {
                            ownGuild.removeRoleFromMember(member, apexRole).queue();
                        } else {
                            ownGuild.addRoleToMember(member, apexRole).queue();
                        }
                        break;
                    }

                    case "3": {
                        if (member.getRoles().contains(railRole)) {
                            ownGuild.removeRoleFromMember(member, railRole).queue();
                        } else {
                            ownGuild.addRoleToMember(member, railRole).queue();
                        }
                        break;
                    }
                }

                event.deferEdit().queue();
                break;
            }
        }
    }


    private void startAuth(ButtonInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        User user = event.getUser();
        UserStepData step;
        if (stepData.containsKey(user.getIdLong())) {
            LOGGER.info("start get");
            step = stepData.get(user.getIdLong());
            LOGGER.info("get success");
        } else {
            step = new UserStepData();
            stepData.put(user.getIdLong(), step);
        }

        step.hook = event
                .deferReply(true)
                .setEmbeds(createEmbed(lang.runtime.steps.updating.get(locale), 0x777700))
                .complete();

        step.updateEmbed();
    }

    private void createChineseInput(ButtonInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        TextInput chiInp = TextInput.create("chi", lang.runtime.steps.chi.label.get(locale), TextInputStyle.SHORT)
                .setPlaceholder(lang.runtime.steps.chi.placeholder.get(locale))
                .setMinLength(2)
                .setMaxLength(2)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_chi", lang.runtime.steps.chi.title.get(locale))
                        .addComponents(ActionRow.of(chiInp))
                        .build()
        ).queue();
    }

    private void createEnglishInput(ButtonInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        TextInput engInp = TextInput.create("eng", lang.runtime.steps.eng.org_label.get(locale), TextInputStyle.SHORT)
                .setPlaceholder(lang.runtime.steps.eng.org_placeholder.get(locale))
                .setMinLength(1)
                .setMaxLength(50)
                .build();

        TextInput mcInp = TextInput.create("mcid", lang.runtime.steps.eng.mc_label.get(locale), TextInputStyle.SHORT)
                .setPlaceholder(lang.runtime.steps.eng.mc_placeholder.get(locale))
                .setMinLength(1)
                .setMaxLength(48)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_eng", lang.runtime.steps.eng.title.get(locale))
                        .addComponents(ActionRow.of(engInp), ActionRow.of(mcInp))
                        .build()
        ).queue();
    }

    private void verifyButton(ButtonInteractionEvent event) {
        UserStepData step = stepData.get(event.getUser().getIdLong());
        manager.add(event.getUser().getId(), step.getObj());
        manager.save();
        event.deferEdit().queue();
        Member member = event.getMember();
        if (ownGuild == null || member == null) return;

        for (long i : AUTHED_ROLE_ID) {
            Role role = ownGuild.getRoleById(i);
            if (role == null) continue;

            ownGuild.addRoleToMember(member, role).queue();
        }

        if (event.getGuild().getSelfMember().canInteract(member)) {
            member.modifyNickname(step.getNick()).queue();
        }

        LOGGER.info("new member: " + step.getNick());

        TextChannel logChannel = ownGuild.getTextChannelById(LOG_CHANNEL_ID);
        if (logChannel != null) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("歡迎加入")
                    .setFooter("成員誕生")
                    .setTimestamp(OffsetDateTime.now())
                    .setColor(0xFFCCDD);
            if (!step.mc_uuid.isEmpty()) {
                builder
                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""),
                                "https://namemc.com/profile/" + step.mc_uuid,
                                member.getEffectiveAvatarUrl()
                        )
                        .setImage("https://mc-heads.net/body/" + step.mc_uuid)
                        .setThumbnail("https://mc-heads.net/avatar/" + step.mc_uuid);
            } else {
                builder
                        .setAuthor(member.getEffectiveName() + (member.getNickname() != null ?
                                        (" (" + member.getUser().getName() + ')') : ""), null,
                                member.getEffectiveAvatarUrl()
                        );
            }

            event.getHook().deleteOriginal().queue();
            logChannel.sendMessageEmbeds(builder.build()).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String[] args = event.getModalId().split(":");
        if (!args[0].equals("xs") || !args[1].equals("og")) return;
        switch (args[2]) {
            case "set_chi": {
                getChineseNameByForm(event);
                break;
            }

            case "set_eng": {
                getEnglishNameByForm(event);
                break;
            }
        }
    }

    private void getChineseNameByForm(ModalInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        ModalMapping chiInp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((chiInp = event.getValue("chi")) == null) return;
        if (!Pattern.matches("^[一-龥]+$", chiInp.getAsString())) { // \\u4E00-\\u9fa5
            event.deferReply(true)
                    .setEmbeds(createEmbed(lang.runtime.errors.wrong_type_chi.get(locale), 0xFF0000))
                    .queue();
        } else {
            step.chineseName = chiInp.getAsString();
            step.updateEmbed();
            event.deferEdit().queue();
        }
    }

    private void getEnglishNameByForm(ModalInteractionEvent event) {
        DiscordLocale locale = event.getUserLocale();
        ModalMapping engInp, mcid_inp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((engInp = event.getValue("eng")) == null) return;
        if ((mcid_inp = event.getValue("mcid")) == null) return;

        if (!mcid_inp.getAsString().isEmpty()) {
            String uuid = getUUIDByName(mcid_inp.getAsString());
            if (uuid == null) {
                event.deferReply(true).setEmbeds(
                        createEmbed(lang.runtime.errors.cannot_found_mc_acc.get(locale)
                                .replace("%name%", mcid_inp.getAsString()), 0xFF0000
                        )
                ).queue();
                return;
            }

            step.mc_uuid = uuid;
        } else {
            // reset mc uuid
            step.mc_uuid = "";
        }

        step.englishName = engInp.getAsString();
        step.updateEmbed();
        event.deferEdit().queue();
    }

    @Nullable
    private String getUUIDByName(String mcName) {
        final String check_url = "https://api.mojang.com/users/profiles/minecraft/";

        String respond = getData(check_url + mcName);
        if (respond == null) return null;

        JsonObject data = JsonParser.parseString(respond).getAsJsonObject();
        if (data.has("id")) return data.get("id").getAsString();

        return null;
    }
}
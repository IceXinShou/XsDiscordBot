package com.xs.officialguild;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xs.loader.lang.LangManager;
import com.xs.loader.logger.Logger;
import com.xs.loader.plugin.Event;
import com.xs.loader.util.FileGetter;
import com.xs.loader.util.JsonObjFileManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import static com.xs.loader.base.Loader.jdaBot;
import static com.xs.loader.util.EmbedCreator.createEmbed;
import static com.xs.loader.util.UrlDataGetter.getData;

public class Main extends Event {
    private static final String TAG = "OG";
    private final String[] LANG_DEFAULT = {"en-US", "zh-TW"};
    private final String PATH_FOLDER_NAME = "plugins/OfficialGuild";
    private final long OWN_GUILD_ID = 858672865355890708L;
    private final long LOG_CHANNEL_ID = 858672865816346634L;
    private final Map<Long, UserStepData> stepData = new HashMap<>();
    private final List<Long> JOINED_ROLE_ID = Arrays.asList(858672865385119755L, 858701368457953360L, 858707058530451476L, 858703314606751764L);
    private final List<Long> AUTHED_ROLE_ID = Arrays.asList(858672865385119757L, 858704345448841226L);
    private final ScheduledExecutorService executorService;
    private LangManager langManager;
    private FileGetter getter;
    private Logger logger;
    private Guild ownGuild;
    private Map<String, Map<DiscordLocale, String>> langMap; // Label, Local, Content
    private JsonObjFileManager manager;

    private final Long GOD_ROLE_ID = 1157315773769982023L;
    private final Long APEX_ROLE_ID = 1157316094550347859L;
    private final Long RAIL_ROLE_ID = 1157317688721420318L;
    private Role godRole;
    private Role apexRole;
    private Role railRole;

    public Main() {
        super(true);

        executorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void initLoad() {
        logger = new Logger(TAG);
        getter = new FileGetter(logger, PATH_FOLDER_NAME, Main.class);
        new File(PATH_FOLDER_NAME + "/data").mkdirs();
        manager = new JsonObjFileManager(PATH_FOLDER_NAME + "/data/userNames.json", TAG);
        loadLang();
        logger.log("Loaded");
    }

    @Override
    public void reload() {
        loadLang();
    }

    @Override
    public void unload() {
        logger.log("UnLoaded");
        executorService.shutdown();
    }

    @Override
    public void loadLang() {
        langManager = new LangManager(logger, getter, PATH_FOLDER_NAME, LANG_DEFAULT, DiscordLocale.CHINESE_TAIWAN);
    }

    @Override
    public CommandData[] guildCommands() {
        return null;
    }

    @Override
    public void onReady(ReadyEvent event) {
        ownGuild = jdaBot.getGuildById(OWN_GUILD_ID);
        if (ownGuild == null) {
            logger.warn("CANNOT FOUND Main Guild!");
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
        DiscordLocale local = event.getUserLocale();
        User user = event.getUser();
        UserStepData step;
        if (stepData.containsKey(user.getIdLong())) {
            logger.log("start get");
            step = stepData.get(user.getIdLong());
            logger.log("get success");
        } else {
            step = new UserStepData();
            stepData.put(user.getIdLong(), step);
        }

        step.hook = event
                .deferReply(true)
                .setEmbeds(createEmbed(langManager.get("runtime;steps;updating", local), 0x777700))
                .complete();

        step.updateEmbed();
    }

    private void createChineseInput(ButtonInteractionEvent event) {
        DiscordLocale local = event.getUserLocale();
        TextInput chiInp = TextInput.create("chi", langManager.get("runtime;steps;chi;label", local), TextInputStyle.SHORT)
                .setPlaceholder(langManager.get("runtime;steps;chi;placeholder", local))
                .setMinLength(2)
                .setMaxLength(2)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_chi", langManager.get("runtime;steps;chi;title", local))
                        .addComponents(ActionRow.of(chiInp))
                        .build()
        ).queue();
    }

    private void createEnglishInput(ButtonInteractionEvent event) {
        DiscordLocale local = event.getUserLocale();
        TextInput engInp = TextInput.create("eng", langManager.get("runtime;steps;eng;org_label", local), TextInputStyle.SHORT)
                .setPlaceholder(langManager.get("runtime;steps;eng;org_placeholder", local))
                .setMinLength(1)
                .setMaxLength(50)
                .build();

        TextInput mcInp = TextInput.create("mcid", langManager.get("runtime;steps;eng;mc_label", local), TextInputStyle.SHORT)
                .setPlaceholder(langManager.get("runtime;steps;eng;mc_placeholder", local))
                .setMinLength(1)
                .setMaxLength(48)
                .setRequired(false)
                .build();

        event.replyModal(
                Modal.create("xs:og:set_eng", langManager.get("runtime;steps;eng;title", local))
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

        logger.log("new member: " + step.getNick());

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
        DiscordLocale local = event.getUserLocale();
        ModalMapping chiInp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((chiInp = event.getValue("chi")) == null) return;
        if (!Pattern.matches("^[一-龥]+$", chiInp.getAsString())) { // \\u4E00-\\u9fa5
            event.deferReply(true)
                    .setEmbeds(createEmbed(langManager.get("runtime;errors;wrong_type_chi", local), 0xFF0000))
                    .queue();
        } else {
            step.chineseName = chiInp.getAsString();
            step.updateEmbed();
            event.deferEdit().queue();
        }
    }

    private void getEnglishNameByForm(ModalInteractionEvent event) {
        DiscordLocale local = event.getUserLocale();
        ModalMapping engInp, mcid_inp;
        UserStepData step = stepData.get(event.getUser().getIdLong());
        if ((engInp = event.getValue("eng")) == null) return;
        if ((mcid_inp = event.getValue("mcid")) == null) return;

        if (!mcid_inp.getAsString().isEmpty()) {
            String uuid = getUUIDByName(mcid_inp.getAsString());
            if (uuid == null) {
                event.deferReply(true).setEmbeds(
                        createEmbed(langManager.get("runtime;errors;cannot_found_mc_acc", local)
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
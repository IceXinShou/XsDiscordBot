package tw.xserver.general.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.concurrent.TimeUnit;

import static net.dv8tion.jda.api.Permission.BAN_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.*;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class Ban {
    private final Language.Ban_t lang;

    public Ban(Language.Ban_t lang) {
        this.lang = lang;
    }

    public SlashCommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("ban", "ban a member from your server")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.options.member.name)
                                        .setDescriptionLocalizations(lang.register.options.member.description),
                                new OptionData(INTEGER, "days", "day")
                                        .setNameLocalizations(lang.register.options.day.name)
                                        .setDescriptionLocalizations(lang.register.options.day.description),
                                new OptionData(STRING, "reason", "reason")
                                        .setNameLocalizations(lang.register.options.reason.name)
                                        .setDescriptionLocalizations(lang.register.options.reason.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(BAN_MEMBERS))
        };
    }


    public void process(SlashCommandInteractionEvent event) {
        if (checkCommand(event, "ban")) return;

        Guild guild = event.getGuild();
        Member runMember = event.getMember();

        notNull(guild, "Guild");
        notNull(runMember, "Member Executor");

        if (!runMember.hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale locale = event.getUserLocale();
        Member selfMember = guild.getSelfMember();
        Member targetMember = event.getOption("member", null, OptionMapping::getAsMember);
        String reason = event.getOption("reason", "null", OptionMapping::getAsString);
        int delDays = Math.max(0, event.getOption("days", 0, OptionMapping::getAsInt));

        if (targetMember == null) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_user.get(locale), 0xFF0000)).queue();
            return;
        }


        if (!selfMember.hasPermission(BAN_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_permission.get(locale), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(targetMember)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.permission_denied.get(locale), 0xFF0000)).queue();
            return;
        }

        guild.ban(targetMember, delDays, TimeUnit.DAYS).reason(reason).queue(
                success -> event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.done.get(locale) + ' ' + targetMember.getEffectiveName(), 0xffb1b3)).queue(),
                error -> event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.unknown.get(locale), 0xFF0000)).queue()
        );
    }
}

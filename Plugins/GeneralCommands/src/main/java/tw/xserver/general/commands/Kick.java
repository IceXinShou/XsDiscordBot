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

import static net.dv8tion.jda.api.Permission.KICK_MEMBERS;
import static net.dv8tion.jda.api.interactions.commands.OptionType.STRING;
import static net.dv8tion.jda.api.interactions.commands.OptionType.USER;
import static net.dv8tion.jda.internal.utils.Checks.notNull;
import static tw.xserver.loader.util.EmbedCreator.createEmbed;
import static tw.xserver.loader.util.GlobalUtil.checkCommand;

public class Kick {
    private final Language.Kick_t lang;

    public Kick(Language.Kick_t lang) {
        this.lang = lang;
    }

    public SlashCommandData[] guildCommands() {
        return new SlashCommandData[]{
                Commands.slash("kick", "kick a member from your server")
                        .setNameLocalizations(lang.register.name)
                        .setDescriptionLocalizations(lang.register.description)
                        .addOptions(
                                new OptionData(USER, "member", "member", true)
                                        .setNameLocalizations(lang.register.options.member.name)
                                        .setDescriptionLocalizations(lang.register.options.member.description),
                                new OptionData(STRING, "reason", "reason")
                                        .setNameLocalizations(lang.register.options.reason.name)
                                        .setDescriptionLocalizations(lang.register.options.reason.description))
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(KICK_MEMBERS))
        };
    }

    public void process(SlashCommandInteractionEvent event) {
        if (checkCommand(event, "kick")) return;

        Guild guild = event.getGuild();
        Member runMember = event.getMember();

        notNull(guild, "Guild");
        notNull(runMember, "Member Executor");

        if (!runMember.hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed("你沒有權限", 0xFF0000)).queue();
            return;
        }

        DiscordLocale locale = event.getUserLocale();
        Member selfMember = guild.getSelfMember();
        Member targetMember = event.getOption("member", null, OptionMapping::getAsMember);
        String reason = event.getOption("reason", "null", OptionMapping::getAsString);

        if (targetMember == null) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_user.get(locale), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.hasPermission(KICK_MEMBERS)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.no_permission.get(locale), 0xFF0000)).queue();
            return;
        }

        if (!selfMember.canInteract(targetMember)) {
            event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.permission_denied.get(locale), 0xFF0000)).queue();
            return;
        }

        guild.kick(targetMember).reason(reason).queue(
                success -> event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.successes.done.get(locale) + ' ' + targetMember.getEffectiveName(), 0xffd2c5)).queue(),
                error -> event.getHook().editOriginalEmbeds(createEmbed(lang.runtime.errors.unknown.get(locale), 0xFF0000)).queue()
        );
    }
}

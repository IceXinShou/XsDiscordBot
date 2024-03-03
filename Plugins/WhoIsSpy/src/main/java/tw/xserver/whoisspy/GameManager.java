package tw.xserver.whoisspy;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.*;

import static tw.xserver.loader.util.EmbedCreator.createEmbed;

public class GameManager {
    private final Guild guild;
    private final List<Member> users;
    private final List<Member> admins;
    private final int spy;
    private final int white;
    private final String problem;
    private final String spy_problem;
    private final List<Member> spys = new ArrayList<>();
    private final List<Member> whites = new ArrayList<>();
    private final Map<Long, String> data = new HashMap<>();
    private TextChannel channel;

    GameManager(Guild guild, Category category, List<Member> users, List<Member> admins, int spy, int white, String problem, String spy_problem) {
        this.guild = guild;
        this.users = users;
        this.admins = admins;
        this.spy = spy;
        this.white = white;
        this.problem = problem;
        this.spy_problem = spy_problem;
        Random rand = new Random();
        category.createTextChannel("誰是臥底").queue(i -> {
            channel = i;
            for (PermissionOverride j : channel.getPermissionContainer().getRolePermissionOverrides()) {
                j.delete().queue();
            }
            channel.upsertPermissionOverride(guild.getPublicRole()).reset().queue();
            channel.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.VIEW_CHANNEL).queue();

            for (int j = 0; j < spy; j++) {
                int index = rand.nextInt(users.size());
                Member member = users.get(index);
                channel.upsertPermissionOverride(member).setAllowed(Permission.VIEW_CHANNEL).queue();
                spys.add(member);
                data.put(member.getIdLong(), spy_problem);
                users.remove(index);
            }

            for (int j = 0; j < white; j++) {
                int index = rand.nextInt(users.size());
                Member member = users.get(index);
                channel.upsertPermissionOverride(member).setAllowed(Permission.VIEW_CHANNEL).queue();
                whites.add(member);
                data.put(member.getIdLong(), "-");
                users.remove(index);
            }


            for (Member j : users) {
                channel.upsertPermissionOverride(j).setAllowed(Permission.VIEW_CHANNEL).queue();
                data.put(j.getIdLong(), problem);
            }

            for (Member j : admins) {
                channel.upsertPermissionOverride(j).setAllowed(Permission.VIEW_CHANNEL).queue();
                data.put(j.getIdLong(), "[關主]");
            }

            channel.sendMessageEmbeds(createEmbed("按此按鈕得到自己的題目", 0x00FFFF))
                    .setActionRow(
                            new ButtonImpl("getrole", "取得自己的題目", ButtonStyle.SUCCESS, false, null),
                            //new ButtonImpl("startvote", "開始投票", ButtonStyle.SUCCESS, false, null),
                            new ButtonImpl("getallrole", "取得所有人的題目", ButtonStyle.PRIMARY, false, null),
                            new ButtonImpl("end", "結算並結束遊戲", ButtonStyle.DANGER, false, null)
                    )
                    .queue();
        });
    }

    String getRole(long id) {
        return data.get(id);
    }

    void addMember(Member member) {
        channel.upsertPermissionOverride(member).setAllowed(Permission.VIEW_CHANNEL).queue();
    }

    void removeMember(Member member) {
        remove(member);
        channel.upsertPermissionOverride(member).reset().queue();
    }

    void remove(Member member) {
        if (users.contains(member)) {
            users.remove(member);
        } else spys.remove(member);
    }

    String getAllRole() {
        StringBuilder innocentName = new StringBuilder();
        StringBuilder spyName = new StringBuilder();
        StringBuilder whiteName = new StringBuilder();

        for (Member i : users) {
            innocentName.append(i.getEffectiveName()).append('\n');
        }
        for (Member i : spys) {
            spyName.append(i.getEffectiveName()).append('\n');
        }
        for (Member i : whites) {
            whiteName.append(i.getEffectiveName()).append('\n');
        }
        return String.format("平民題目: %s\n" +
                "臥底題目: %s\n\n" +
                "平民: \n%s\n" +
                "臥底: \n%s\n" +
                "白板: \n%s", problem, spy_problem, innocentName, spyName, whiteName);
    }

    void endGame() {
        channel.sendMessageEmbeds(createEmbed("遊戲結束!", getAllRole(), 0x00FFFF))
                .addActionRow(
                        new ButtonImpl("delete:" + channel.getId(), "刪除頻道", ButtonStyle.DANGER, false, null)
                ).queue();
    }

    public void startVote() {
        List<Button> buttons = new ArrayList<>();
        for (Member i : users) {
            buttons.add(Button.of(ButtonStyle.SUCCESS, "vote:" + i.getId(), i.getEffectiveName()));
        }

        for (Member i : spys) {
            buttons.add(Button.of(ButtonStyle.SUCCESS, "vote:" + i.getId(), i.getEffectiveName()));
        }

        Collections.shuffle(buttons);

//        options.add(SelectOption.of("棄票", "vote:null"));
        channel.sendMessageEmbeds(createEmbed("請選出你所認為的臥底: ", 0x00FFFF)).addActionRow(
                buttons
//                SelectMenu.create("vote").addOptions(options).setMaxValues(1).setMinValues(1).build()
        ).addActionRow().queue();
    }
}
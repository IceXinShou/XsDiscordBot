package com.xs.officialguild;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.Arrays;
import java.util.List;

import static com.xs.loader.util.EmbedCreator.createEmbed;
import static net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle.*;

public class UserStepData {
    public TextChannel channel = null;
    public long messageID = 0L;
    public String chineseName = "";
    public String englishName = "";
    public String mc_uuid = "";

    /**
     * @return <code>true</code> if chinese name set
     */
    public boolean chi() {
        return !chineseName.equals("");
    }

    /**
     * @return <code>true</code> if english name set
     */
    public boolean eng() {
        return !englishName.equals("") || !mc_uuid.equals("");
    }

    /**
     * @return <code>true</code> if all set
     */
    public boolean verify() {
        return !chi() || !eng();
    }

    public void updateButton() {
        channel.editMessageEmbedsById(messageID, createEmbed("UPDATED", 0x00FFFF)).setActionRow(getSettingButton()).queue();
    }


    List<Button> getSettingButton() {
        return Arrays.asList(
                new ButtonImpl("xs:og:chi", "中文暱稱", chi() ? SUCCESS : DANGER, false, null),
                new ButtonImpl("xs:og:eng", "英文暱稱", eng() ? SUCCESS : DANGER, false, null),
                new ButtonImpl("xs:og:verify", "同意許可", PRIMARY, verify(), null)
        );
    }
}

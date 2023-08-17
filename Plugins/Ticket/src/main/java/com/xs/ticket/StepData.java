package com.xs.ticket;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

public class StepData {
    public String author;
    public String authorIconURL;
    public String title;
    public String description;
    public int color;
    public String reasonTitle;
    public List<Long> adminIDs = new ArrayList<>();
    public String btnContent;
    public Emoji btnEmoji;
    public ButtonStyle btnStyle;

    public StepData() {
        author = "Ticket 服務";
        authorIconURL = "https://img.lovepik.com/free-png/20211116/lovepik-customer-service-personnel-icon-png-image_400960955_wh1200.png";
        title = "\uD83D\uDEE0 聯絡我們";
        description = "✨ 點擊下方 **[按鈕]**，並提供所遭遇的問題，我們盡快給予答覆！ ✨";
        color = 0x00FFFF;
        reasonTitle = "有任何可以幫助的問題嗎~";
        btnContent = "聯絡我們";
        btnEmoji = Emoji.fromUnicode("✉");
        btnStyle = ButtonStyle.SUCCESS;
    }
}
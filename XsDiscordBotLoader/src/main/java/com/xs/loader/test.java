package com.xs.loader;

import kotlin.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class test {
    @SuppressWarnings("ALL")
    public static void main(String[] args) {
        List<Pair<String, String>> data = new ArrayList<>();
        Collections.addAll(data,
                new Pair<>("apologize for", "道歉"),
                new Pair<>("apology for", "道歉"),
                new Pair<>("apparently", "顯然"),
                new Pair<>("apparent", "明顯的"),
                new Pair<>("obvious", "明顯的 (o_s)"),
                new Pair<>("expression", "詞句"),
                new Pair<>("plenty of", "很多"),
                new Pair<>("avoid Ving", "避開"),
                new Pair<>("expert", "專家"),
                new Pair<>("ancient", "古老的"),
                new Pair<>("peacefully", "和平地"),
                new Pair<>("peace", "平靜"),
                new Pair<>("influence (v)", "影響"),
                new Pair<>("affect (v)", "影響"),
                new Pair<>("influence on (n)", "影響"),
                new Pair<>("effect (n)", "影響"),
                new Pair<>("formal", "正式的"),
                new Pair<>("significant for", "重要的 (adj)"),
                new Pair<>("significance to", "重要性 (n)"),
                new Pair<>("resident", "居民"),
                new Pair<>("anyway", "無論如何"),
                new Pair<>("anyhow", "無論如何"),
                new Pair<>("deny", "否定"),
                new Pair<>("admit", "肯定"),
                new Pair<>("society", "社會"),
                new Pair<>("usage", "用法"),
                new Pair<>("bump into", "撞上"),
                new Pair<>("in a hurry", "匆忙地"),
                new Pair<>("point out", "指出"),
                new Pair<>("more or less", "或多或少"),
                new Pair<>("build up", "增加"),
                new Pair<>("grocery", "食品雜貨"),
                new Pair<>("weird", "奇怪的"),
                new Pair<>("tough", "困難的"),
                new Pair<>("advice", "意見"),
                new Pair<>("advise", "建議"),
                new Pair<>("distribute", "分發"),
                new Pair<>("solution", "解決辦法"),
                new Pair<>("solve", "解決"),
                new Pair<>("celebrity", "名人"),
                new Pair<>("support", "支持"),
                new Pair<>("activity", "活動"),
                new Pair<>("prevent", "防止"),
                new Pair<>("odd", "古怪的"),
                new Pair<>("educate", "教導"),
                new Pair<>("education", "教育"),
                new Pair<>("invest", "投資"),
                new Pair<>("investment", "投資"),
                new Pair<>("issue", "議題"),
                new Pair<>("assist", "幫忙"),
                new Pair<>("assistant", "助理"),
                new Pair<>("eventually", "最後"),
                new Pair<>("eventual", "最終的"),
                new Pair<>("treasure (v)", "珍惜"),
                new Pair<>("treasure (n)", "寶藏")
        );

        Scanner sc = new Scanner(System.in);

        while (true) {
            // Collections.shuffle(data);

            for (Pair<String, String> i : data) {
                System.out.print(i.component1());
                sc.nextLine();
                System.out.println(i.component2());
            }
        }
    }
}
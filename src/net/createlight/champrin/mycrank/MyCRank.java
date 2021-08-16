package net.createlight.champrin.mycrank;


import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.*;

public class MyCRank extends PluginBase implements Listener {

    private String format;//name-record 执行一次addRankData自动排序 获得的List即为有序的排名
    private String tip;
    private String formTitle;

    private LinkedHashMap<String, Integer> formIdMap = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> formIdMap2 = new LinkedHashMap<>();
    private int formId = 20210804;

    private static MyCRank instance;

    public static MyCRank getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        if (!new File(this.getDataFolder() + "/config.yml").exists()) {
            this.saveResource("config.yml", false);
        }
        Config config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        this.format = config.getString("each-number-format");
        this.tip = config.getString("tip-on-title");
        this.formTitle = config.getString("windowForm-title");
        this.getLogger().info("§fMyCRank-用于我的游戏插件中给玩家每次游戏所得分数加入总排名榜，如果用我的游戏插件，这是必装插件！");
        this.getLogger().info("§7MyCRank-It is used in my game plugin to add the score of each game to the total ranking list. If you use my game plugin, it is a required plugin!");
        this.getLogger().info("§d加载完毕。。。§e|作者：Champrin");
        this.getLogger().info("§7Loading complete...§e|Author:Champrin");
    }

    public Config getConfig(String pluginName, String rankName) {
        return new Config(this.getDataFolder() + "/" + pluginName + "/" + rankName + ".yml", Config.YAML);
    }

    public boolean existRank(String pluginName, String rankName) {
        return new File(this.getDataFolder() + "/" + pluginName + "/" + rankName + ".yml").exists();
    }

    public void createRank(String pluginName, String rankName) {
        Config config = getConfig(pluginName, rankName);
        config.set("rank", new ArrayList<>(Arrays.asList("player-000", "player-000", "player-000", "player-000", "player-000", "player-000", "player-000", "player-000", "player-000", "player-000")));
        config.save();
    }

//遍历原排行榜 从下标i=0开始比较 一有符合条件的(高或低) 那么让i至size-1的元素后退一位
//因为是从头开始，且一遇到player-000或null的就直接插入，那么就保证了每次添加一个新数据，最终结果都是按条件(高或低)来排序的
    public void addRankDataFromHigh(String pluginName, String rankName, String playerName, int record) {
        Config config = getConfig(pluginName, rankName);
        ArrayList<String> rankList = (ArrayList<String>) config.getStringList("rank");
        int size = rankList.size();
        for (int i = 0; i < size; ++i) {
            if (rankList.get(i).equals("player-000")) {
                rankList.set(i, playerName + "-" + record);
                break;
            }
            String m = rankList.get(i);
            String[] ms = m.split("-");
            if (Integer.parseInt(ms[1]) < record) {
                rankList.add("player-000");
                for (int j = size - 1; j >= i; j--) {
                    String a = rankList.get(j);
                    rankList.set(j + 1, a);
                }
                rankList.set(i, playerName + "-" + record);
                break;
            }
        }
        config.set("rank", rankList);
        config.save();
    }

    public void addRankDataFromLow(String pluginName, String rankName, String playerName, int record) {
        Config config = getConfig(pluginName, rankName);
        List<String> rankList = config.getStringList("rank");
        int size = rankList.size();
        for (int i = 0; i < size; ++i) {
            if (rankList.get(i).equals("player-000")) {
                rankList.set(i, playerName + "-" + record);
                break;
            }
            String m = rankList.get(i);
            String[] ms = m.split("-");
            if (Integer.parseInt(ms[1]) > record) {
                rankList.add("player-000");
                for (int j = size - 1; j >= i; j--) {
                    String a = rankList.get(j);
                    rankList.set(j + 1, a);
                }
                rankList.set(i, playerName + "-" + record);
                break;
            }
        }
        config.set("rank", rankList);
        config.save();
    }

    public void deleteRank(String pluginName, String rankName) {
        new File(this.getDataFolder() + "/" + pluginName + "/" + rankName + ".yml").delete();
    }

    public String getRank(String pluginName, String rankName) {
        return getRank(pluginName, rankName, 999);
    }

    public String getRank(String pluginName, String rankName, int num) {
        Config config = getConfig(pluginName, rankName);
        List<String> rankList = config.getStringList("rank");
        StringBuilder rank = new StringBuilder(tip);
        num = Math.min(rankList.size(), num);
        for (int i = 0; i < num; ++i) {
            if (rankList.get(i) == null) break;
            String m = rankList.get(i);
            String[] ms = m.split("-");
            rank.append("\n").append(format.replaceAll("%NO%", i + 1 + "").replaceAll("%NAME%", ms[0]).replaceAll("%RECORD%", ms[1]));
        }
        return rank.toString();
    }

    public void sendRankMainForm(String pluginName, Player player) {
        if (!formIdMap.containsKey(pluginName)) {
            formIdMap.put(pluginName, formId);
            formIdMap2.put(formId, pluginName);
            this.formId += 1;
        }
        FormWindowSimple window = new FormWindowSimple(formTitle.replaceAll("%NAME%", pluginName), "");
        File folder = new File(this.getDataFolder() + "/" + pluginName + "/");
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                window.addButton(new ElementButton(file.getName().substring(0,file.getName().lastIndexOf("."))));
            }
        }
        player.showFormWindow(window, formIdMap.get(pluginName));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (formIdMap.containsValue(event.getFormID())) {
            FormResponseSimple response = (FormResponseSimple) event.getResponse();
            if (response == null) return;
            int clickedButtonId = response.getClickedButtonId();
            if (clickedButtonId == -1) return;
            String pluginName = formIdMap2.get(event.getFormID());
            File folder = new File(this.getDataFolder() + "/"+pluginName+ "/");
            File[] files = folder.listFiles();
            if (files != null) {
                String name = files[clickedButtonId].getName().substring(0,files[clickedButtonId].getName().lastIndexOf("."));
                System.out.println(name);
                FormWindowSimple window = new FormWindowSimple(formTitle.replaceAll("%NAME%", name), getRank(pluginName, name));
                player.showFormWindow(window);
            }
        }
    }
}
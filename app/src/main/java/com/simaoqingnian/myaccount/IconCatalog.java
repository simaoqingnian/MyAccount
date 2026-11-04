package com.simaoqingnian.myaccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Offline vector icons shared by built-in and personal categories. */
public final class IconCatalog {
    public static final class Item {
        public final String id, name;
        Item(String id, String name) { this.id = id; this.name = name; }
    }

    public static final List<Item> ALL;
    static {
        String[][] definitions = {
            {"food","餐具"},{"bus","公交"},{"bag","购物袋"},{"basket","购物篮"},
            {"home","房屋"},{"bolt","闪电"},{"game","游戏"},{"heart","爱心"},
            {"book","书本"},{"plane","飞机"},{"gift","礼物"},{"wallet","钱包"},
            {"grid","方格"},{"coffee","咖啡"},{"fruit","水果"},{"cookie","饼干"},
            {"vegetable","蔬菜"},{"bowl","饭碗"},{"takeout","外卖"},{"shirt","衣服"},
            {"lipstick","口红"},{"scissors","剪刀"},{"phone","手机"},{"wifi","网络"},
            {"subscription","订阅"},{"laptop","电脑"},{"car","汽车"},{"fuel","加油"},
            {"parking","停车"},{"taxi","出租车"},{"paw","宠物"},{"baby","宝宝"},
            {"ball","球类"},{"dumbbell","哑铃"},{"shield","保障"},{"bank","银行"},
            {"tools","工具"},{"handHeart","爱心捐赠"},{"receipt","票据"},{"package","快递"},
            {"refund","退款"},{"percent","利率"},{"coins","钱币"},{"recycle","循环"},
            {"redpacket","红包"},{"briefcase","公文包"},{"pension","养老"},{"ticket","票券"},
            {"flower","鲜花"},{"cake","蛋糕"},{"music","音乐"},{"camera","相机"},
            {"watch","手表"},{"train","火车"},{"bike","自行车"},{"ship","轮船"},
            {"umbrella","雨伞"},{"sun","太阳"},{"moon","月亮"},{"star","星星"},
            {"diamond","钻石"},{"ring","戒指"},{"brush","画笔"},{"bed","床铺"},
            {"sofa","沙发"},{"laundry","洗衣"},{"water","水滴"},{"tree","树木"},
            {"building","楼宇"},{"key","钥匙"},{"medical","药箱"},{"globe","地球"}
        };
        List<Item> items = new ArrayList<>();
        for (String[] d : definitions) items.add(new Item(d[0], d[1]));
        ALL = Collections.unmodifiableList(items);
    }

    public static Item get(String id) {
        for (Item item : ALL) if (item.id.equals(id)) return item;
        throw new IllegalArgumentException("请选择图标库中的图标");
    }

    public static List<Item> unused(Ledger.Catalog catalog, String editingId) {
        java.util.Set<String> used = new java.util.HashSet<>();
        for (Ledger.Category c : catalog.all) if (!c.id.equals(editingId)) used.add(c.icon);
        List<Item> result = new ArrayList<>();
        for (Item item : ALL) if (!used.contains(item.id)) result.add(item);
        return result;
    }

    private IconCatalog() {}
}

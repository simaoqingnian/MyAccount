package com.simaoqingnian.myaccount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selection order is a local UI preference, separate from financial records. */
public final class RecentCategories {
    public static List<String> select(List<String> previous, String selected) {
        Set<String> ids = new LinkedHashSet<>();
        if (selected != null && !selected.isEmpty()) ids.add(selected);
        for (String id : previous) {
            if (id != null && !id.isEmpty()) ids.add(id);
            if (ids.size() >= 200) break;
        }
        return new ArrayList<>(ids);
    }

    public static List<Ledger.Category> order(Ledger.Catalog catalog, String type, List<String> recent) {
        Map<String, Ledger.Category> remaining = new LinkedHashMap<>();
        for (Ledger.Category c : catalog.ofType(type)) remaining.put(c.id, c);
        List<Ledger.Category> result = new ArrayList<>();
        for (String id : recent) {
            Ledger.Category c = remaining.remove(id);
            if (c != null) result.add(c);
        }
        result.addAll(remaining.values());
        return result;
    }

    public static Ledger.Category first(Ledger.Catalog catalog, String type, List<String> recent) {
        List<Ledger.Category> ordered = order(catalog, type, recent);
        if (ordered.isEmpty()) throw new IllegalArgumentException("没有可用的记账项目");
        return ordered.get(0);
    }

    private RecentCategories() {}
}

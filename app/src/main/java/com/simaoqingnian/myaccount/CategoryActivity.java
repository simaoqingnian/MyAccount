package com.simaoqingnian.myaccount;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Personal categories are shared by entry, reports, budget and backups. */
public final class CategoryActivity extends Activity {
    private LedgerStore store;
    private Ledger.Data data = Ledger.Data.empty();
    private String type = "expense";
    private LinearLayout root;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new LedgerStore(this);
        if (state != null) type = state.getString("type", "expense");
        root = Ui.column(this);
        root.setBackgroundColor(Ui.PAPER);
        setContentView(root);
        Ui.insets(this, root);
    }

    @Override protected void onResume() { super.onResume(); reload(); }

    private void reload() {
        LedgerStore.IO.execute(() -> {
            try {
                Ledger.Data latest = store.read();
                runOnUiThread(() -> { if (!isDestroyed()) { data = latest; render(); } });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void render() {
        root.removeAllViews();
        LinearLayout header = Ui.row(this);
        Ui.pad(header, 14, 6, 14, 6);
        header.addView(Ui.smallAction(this, "返回", this::finish));
        Ui.weight(header, Ui.bold(this, "项目与图标", 21, Ui.INK));
        header.addView(Ui.smallAction(this, "＋ 新建", () -> showEditor(this, store, data, type, null, c -> reload())));
        root.addView(header);
        LinearLayout types = Ui.row(this);
        Ui.pad(types, 20, 4, 20, 4);
        for (String value : new String[]{"expense", "income"}) {
            Ui.weight(types, Ui.action(this, value.equals("expense") ? "支出项目" : "收入项目", type.equals(value), () -> { type = value; render(); }));
        }
        root.addView(types);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = Ui.column(this);
        Ui.pad(content, 22, 12, 22, 24);
        content.addView(Ui.text(this, "点击自定义项目可改名、换图标。\n已有账目或预算的项目会保留，避免历史记录失效。", 12, Ui.MUTED));
        Ui.space(content, 14);
        for (Ledger.Category c : data.categories(type)) {
            LinearLayout row = Ui.row(this);
            Ui.pad(row, 0, 10, 0, 10);
            Ui.Icon icon = new Ui.Icon(this, c.icon, Ui.GREEN);
            icon.setBackground(Ui.background(Ui.SAGE, 12, this));
            Ui.pad(icon, 9, 9, 9, 9);
            row.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 42)));
            TextView name = Ui.bold(this, c.name, 14, Ui.INK);
            Ui.pad(name, 12, 0, 8, 0);
            Ui.weight(row, name);
            row.addView(Ui.text(this, c.custom() ? "自定义  ›" : "预置", 11, Ui.MUTED));
            if (c.custom()) {
                row.setFocusable(true);
                row.setOnClickListener(v -> showEditor(this, store, data, type, c, saved -> reload()));
            }
            content.addView(row);
            Ui.divider(content);
        }
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    public static void showEditor(Activity owner, LedgerStore store, Ledger.Data data, String type,
                                  Ledger.Category existing, Consumer<Ledger.Category> saved) {
        LinearLayout form = Ui.column(owner);
        Ui.pad(form, 22, 8, 22, 12);
        form.addView(Ui.text(owner, (type.equals("expense") ? "支出" : "收入") + "项目 · 名称最多 12 个字符", 12, Ui.MUTED));
        Ui.space(form, 12);
        EditText name = Ui.input(owner, "例如：猫咪口粮、午间咖啡");
        name.setContentDescription("自定义项目名称");
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(24)});
        if (existing != null) name.setText(existing.name);
        form.addView(name);
        List<IconCatalog.Item> spare = IconCatalog.unused(data.catalog, existing == null ? "" : existing.id);
        String[] selected = {existing != null ? existing.icon : spare.isEmpty() ? "star" : spare.get(0).id};
        LinearLayout preview = Ui.row(owner);
        Ui.pad(preview, 0, 14, 0, 10);
        Runnable updatePreview = () -> {
            preview.removeAllViews();
            Ui.Icon icon = new Ui.Icon(owner, selected[0], Ui.GREEN);
            icon.setBackground(Ui.background(Ui.SAGE, 13, owner));
            Ui.pad(icon, 11, 11, 11, 11);
            preview.addView(icon, new LinearLayout.LayoutParams(Ui.dp(owner, 48), Ui.dp(owner, 48)));
            TextView caption = Ui.text(owner, "  " + IconCatalog.get(selected[0]).name + "    点击换图标  ›", 13, Ui.GREEN);
            Ui.weight(preview, caption);
        };
        updatePreview.run();
        preview.setOnClickListener(v -> pickIcon(owner, data.catalog, existing == null ? "" : existing.id,
                selected[0], icon -> { selected[0] = icon; updatePreview.run(); }));
        preview.setFocusable(true);
        preview.setContentDescription("选择项目图标");
        form.addView(preview);
        form.addView(Ui.text(owner, "图标库共 " + IconCatalog.ALL.size() + " 个图标，优先展示未使用的图标。", 11, Ui.MUTED));
        TextView error = Ui.text(owner, "", 12, Ui.DANGER);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Ui.space(form, 12);
        form.addView(error);
        AlertDialog.Builder builder = new AlertDialog.Builder(owner).setTitle(existing == null ? "新建自定义项目" : "编辑自定义项目")
                .setView(form).setNegativeButton("取消", null).setPositiveButton("保存", null);
        if (existing != null) builder.setNeutralButton("删除", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                final Ledger.Category category;
                try {
                    String id = existing == null ? "custom-" + UUID.randomUUID() : existing.id;
                    category = new Ledger.Category(id, name.getText().toString(), type, selected[0], existing == null ? "#8BBAB3" : existing.color);
                } catch (Exception e) { error.setText(e.getMessage()); return; }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                LedgerStore.IO.execute(() -> {
                    try {
                        store.saveCategory(category);
                        owner.runOnUiThread(() -> { if (!owner.isDestroyed()) { dialog.dismiss(); saved.accept(category); } });
                    } catch (Exception e) {
                        owner.runOnUiThread(() -> { if (!owner.isDestroyed()) { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); error.setText(e.getMessage()); } });
                    }
                });
            });
            if (existing != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(button ->
                new AlertDialog.Builder(owner).setTitle("删除项目“" + existing.name + "”？")
                    .setMessage("只有没有关联账目和分类预算的自定义项目可以删除。")
                    .setNegativeButton("取消", null).setPositiveButton("删除", (d, w) -> {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(false);
                        LedgerStore.IO.execute(() -> {
                            try {
                                store.deleteCategory(existing.id);
                                owner.runOnUiThread(() -> { if (!owner.isDestroyed()) { dialog.dismiss(); saved.accept(null); } });
                            } catch (Exception e) {
                                owner.runOnUiThread(() -> { if (!owner.isDestroyed()) { dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true); error.setText(e.getMessage()); } });
                            }
                        });
                    }).show());
        });
        dialog.show();
    }

    private static void pickIcon(Activity owner, Ledger.Catalog catalog, String editingId,
                                 String selected, Consumer<String> choose) {
        LinearLayout content = Ui.column(owner);
        Ui.pad(content, 14, 0, 14, 10);
        List<IconCatalog.Item> spare = IconCatalog.unused(catalog, editingId);
        boolean[] onlySpare = {!spare.isEmpty()};
        LinearLayout tabs = Ui.row(owner);
        content.addView(tabs);
        ScrollView scroll = new ScrollView(owner);
        GridLayout grid = new GridLayout(owner);
        grid.setColumnCount(4);
        scroll.addView(grid);
        content.addView(scroll, new LinearLayout.LayoutParams(-1, Ui.dp(owner, 340)));
        AlertDialog picker = new AlertDialog.Builder(owner).setTitle("选择项目图标").setView(content).setNegativeButton("取消", null).create();
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            tabs.removeAllViews();
            Ui.weight(tabs, Ui.action(owner, "未使用 (" + spare.size() + ")", onlySpare[0], () -> { onlySpare[0] = true; render[0].run(); }));
            Ui.weight(tabs, Ui.action(owner, "全部 (" + IconCatalog.ALL.size() + ")", !onlySpare[0], () -> { onlySpare[0] = false; render[0].run(); }));
            grid.removeAllViews();
            List<IconCatalog.Item> items = onlySpare[0] ? spare : IconCatalog.ALL;
            int index = 0;
            for (IconCatalog.Item item : items) {
                LinearLayout cell = Ui.column(owner);
                cell.setGravity(Gravity.CENTER);
                Ui.Icon icon = new Ui.Icon(owner, item.id, item.id.equals(selected) ? Ui.WHITE : Ui.GREEN);
                icon.setBackground(Ui.background(item.id.equals(selected) ? Ui.GREEN : Ui.SAGE, 12, owner));
                Ui.pad(icon, 9, 9, 9, 9);
                cell.addView(icon, new LinearLayout.LayoutParams(Ui.dp(owner, 42), Ui.dp(owner, 42)));
                Ui.space(cell, 5);
                cell.addView(Ui.text(owner, item.name, 10, Ui.INK));
                cell.setContentDescription(item.name + (item.id.equals(selected) ? "，已选择" : ""));
                cell.setFocusable(true);
                cell.setOnClickListener(v -> { picker.dismiss(); choose.accept(item.id); });
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(GridLayout.spec(index / 4), GridLayout.spec(index % 4, 1, 1f));
                p.width = 0; p.height = Ui.dp(owner, 78);
                grid.addView(cell, p);
                index++;
            }
            scroll.scrollTo(0, 0);
        };
        render[0].run();
        picker.show();
    }

    @Override protected void onSaveInstanceState(Bundle out) { out.putString("type", type); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { LedgerStore.IO.execute(store::close); super.onDestroy(); }
}

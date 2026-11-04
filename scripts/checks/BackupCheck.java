import com.simaoqingnian.myaccount.BackupCodec;
import com.simaoqingnian.myaccount.Ledger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.json.JSONArray;
import org.json.JSONObject;

/** Uses a cached host JSON implementation; no Android runtime or device required. */
public final class BackupCheck {
    private static int checks;
    private interface Checked { void run() throws Exception; }
    private static void expect(boolean ok, String name) { if (!ok) throw new AssertionError(name); checks++; }
    private static void rejects(Checked action, String name) throws Exception {
        try { action.run(); } catch (Exception expected) { checks++; return; }
        throw new AssertionError(name);
    }
    public static void main(String[] args) throws Exception {
        Ledger.Data demo = Ledger.demo();
        JSONObject legacy = new JSONObject(BackupCodec.encode(demo));
        legacy.put("schemaVersion", 1);
        legacy.getJSONObject("data").remove("customCategories");
        Ledger.Data migrated = BackupCodec.decode(legacy.toString());
        expect(migrated.entries.size() == demo.entries.size(), "schema 1 records retained");
        expect(migrated.budgets.size() == demo.budgets.size(), "schema 1 budgets retained");
        expect(migrated.catalog.custom.isEmpty(), "schema 1 has no personal categories");
        expect(Ledger.total(migrated.entries,"expense") == Ledger.total(demo.entries,"expense"), "schema 1 exact amounts");
        Ledger.Category c = new Ledger.Category("custom-dog", "狗粮", "expense", "paw", "#AAC0A4");
        Ledger.Catalog catalog = new Ledger.Catalog(Collections.singletonList(c));
        String now = Instant.now().toString();
        Ledger.Entry e = new Ledger.Entry("my-entry", "expense", c.id, 12345, Ledger.today().toString(), "大袋狗粮", now, now, catalog);
        Ledger.Budget budget = new Ledger.Budget(50000L, Collections.singletonMap(c.id, 25000L), catalog);
        Ledger.Data personal = new Ledger.Data(Collections.singletonList(e), Collections.singletonMap(Ledger.today().toString().substring(0,7),budget), true, Collections.singletonList(c));
        String encoded = BackupCodec.encode(personal);
        Ledger.Data restored = BackupCodec.decode(encoded);
        expect(new JSONObject(encoded).getInt("schemaVersion") == 2, "schema 2 export");
        expect(restored.category(c.id).name.equals(c.name) && restored.category(c.id).icon.equals(c.icon), "custom name and icon round trip");
        expect(restored.entries.get(0).amount == 12345 && restored.entries.get(0).note.equals(e.note), "custom entry round trip");
        expect(restored.budgets.values().iterator().next().limits.get(c.id) == 25000, "custom budget round trip");
        expect(restored.hide, "hidden preference round trip");
        expect(Ledger.noteSuggestions(restored,"expense",c.id).equals(Arrays.asList(e.note)), "restored notes available");
        expect(BackupCodec.decode(legacy.toString()).catalog.custom.isEmpty(), "catalog does not leak across restores");
        JSONObject unknown = new JSONObject(encoded); unknown.put("schemaVersion", 3);
        rejects(() -> BackupCodec.decode(unknown.toString()), "future backup version rejected");
        JSONObject missing = new JSONObject(encoded); missing.getJSONObject("data").remove("customCategories");
        rejects(() -> BackupCodec.decode(missing.toString()), "missing schema 2 catalog rejected");
        JSONObject decimal = new JSONObject(encoded); decimal.getJSONObject("data").getJSONArray("transactions").getJSONObject(0).put("amountMinor", 123.45);
        rejects(() -> BackupCodec.decode(decimal.toString()), "fractional cents rejected");
        JSONObject orphan = new JSONObject(encoded); orphan.getJSONObject("data").put("customCategories", new JSONArray());
        rejects(() -> BackupCodec.decode(orphan.toString()), "orphan entry rejected");
        JSONObject duplicate = new JSONObject(encoded); JSONArray list = duplicate.getJSONObject("data").getJSONArray("customCategories"); list.put(new JSONObject(list.getJSONObject(0).toString()));
        rejects(() -> BackupCodec.decode(duplicate.toString()), "duplicate category rejected");
        JSONObject collision = new JSONObject(encoded); collision.getJSONObject("data").getJSONArray("customCategories").getJSONObject(0).put("name", "餐饮");
        rejects(() -> BackupCodec.decode(collision.toString()), "built-in category collision rejected");
        JSONObject wrongIcon = new JSONObject(encoded); wrongIcon.getJSONObject("data").getJSONArray("customCategories").getJSONObject(0).put("icon", "missing-icon");
        rejects(() -> BackupCodec.decode(wrongIcon.toString()), "unknown icon rejected");
        JSONObject wrongType = new JSONObject(encoded); wrongType.getJSONObject("data").getJSONArray("customCategories").getJSONObject(0).put("type", "income");
        rejects(() -> BackupCodec.decode(wrongType.toString()), "category type mismatch rejected");
        JSONObject duplicateEntry = new JSONObject(encoded); JSONArray records = duplicateEntry.getJSONObject("data").getJSONArray("transactions"); records.put(new JSONObject(records.getJSONObject(0).toString()));
        rejects(() -> BackupCodec.decode(duplicateEntry.toString()), "duplicate entry rejected");
        rejects(() -> BackupCodec.decode("not-json"), "invalid JSON rejected");
        System.out.println("Backup checks passed: " + checks);
    }
}

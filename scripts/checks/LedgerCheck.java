import com.simaoqingnian.myaccount.Ledger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Host-side finance checks. Does not connect to or install anything on a device. */
public final class LedgerCheck {
    private static int checks;
    private static void expect(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void rejects(Runnable action,String label){try{action.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError(label);}
    public static void main(String[] args){
        expect(Ledger.parseAmount("0.10")+Ledger.parseAmount("0.20")==30,"exact cents");
        expect(Ledger.parseAmount("999999999.99")==Ledger.MAX_MINOR,"maximum amount");
        for(String input:new String[]{"0","-1","1.234","1e3","NaN","1000000000.00"})rejects(()->Ledger.parseAmount(input),"reject "+input);
        rejects(()->Ledger.parseDate("2026-02-30"),"impossible date");
        rejects(()->Ledger.parseDate(Ledger.today().plusDays(1).toString()),"future date");
        Ledger.Data demo=Ledger.demo();YearMonth month=YearMonth.from(Ledger.today());
        expect(Ledger.total(Ledger.month(demo,month),"expense")==231000,"current expense");
        expect(Ledger.total(Ledger.month(demo,month),"income")==1200000,"current income");
        Ledger.Report year=Ledger.report(demo,"year",Ledger.today(),"expense");
        long sum=0;for(Ledger.Bucket b:year.buckets)sum+=b.total;
        expect(sum==year.total,"year equals months");expect(year.elapsed==month.getMonthValue(),"current year divisor");
        expect(Ledger.report(demo,"month",Ledger.today(),"expense").elapsed==Ledger.today().getDayOfMonth(),"month divisor");
        int pastYear=Ledger.today().getYear()-1;
        Ledger.Report historical=Ledger.report(demo,"year",LocalDate.of(pastYear,1,1),"expense");expect(historical.elapsed==12,"full past year");
        expect(Ledger.groups(historical.entries,"expense").stream().mapToLong(g->g.total).sum()==historical.total,"category total");
        expect(Ledger.report(demo,"year",LocalDate.of(pastYear-3,1,1),"expense").total==0,"empty year");
        LocalDate jan=LocalDate.of(Ledger.today().getYear(),1,1);String now=Instant.now().toString();
        Ledger.Entry a=new Ledger.Entry("a","expense","food",100,jan.minusDays(1).toString(),"cross-year",now,now);
        Ledger.Entry b=new Ledger.Entry("b","expense","food",250,jan.toString(),"cross-year",now,now);
        Ledger.Data cross=new Ledger.Data(Arrays.asList(a,b),Collections.emptyMap(),false);Ledger.Report week=Ledger.report(cross,"week",jan,"expense");
        long expected=week.start.isAfter(jan.minusDays(1))?250:350;expect(week.total==expected,"week includes previous year when in same week");
        Map<String,Long> limits=new LinkedHashMap<>();limits.put("food",101L);rejects(()->new Ledger.Budget(100L,limits),"budget allocation");rejects(()->new Ledger.Budget(null,limits),"budget without total");
        Ledger.Entry formula=new Ledger.Entry("formula","expense","food",100,jan.toString(),"=SUM(1,2)\n\"quoted\"",now,now);
        String csv=Ledger.csv(Collections.singletonList(formula));expect(csv.startsWith("\uFEFF"),"CSV BOM");expect(csv.contains("\"'=SUM(1,2)\n\"\"quoted\"\"\""),"CSV formula and quoting");
        expect(Ledger.filter(Ledger.month(demo,month),"expense","food","咖啡",null).size()==1,"search intersection");
        System.out.println("Ledger checks passed: "+checks);
    }
}

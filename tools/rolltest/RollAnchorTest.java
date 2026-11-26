import com.feiyuaq.niuma.Cycle;
import com.feiyuaq.niuma.D;
import com.feiyuaq.niuma.Prefs;

import java.util.Calendar;

/** 纯 JVM 校验 Cycle.rollAnchor：只前移整数个周期，相位（今天第几天）必须前后一致 */
public class RollAnchorTest {

    static int fails = 0;

    static void check(String name, String anchor, String today, String wantAnchor, int mode, String cycle) {
        Prefs p = new Prefs();
        p.mode = mode;
        p.anchor = anchor;
        if (mode == Prefs.MODE_THREE) p.threeCycle = cycle;
        else p.customCycle = cycle;
        Calendar t = D.parse(today);
        int idxBefore = Cycle.index(p, t);
        boolean moved = Cycle.rollAnchor(p, t);
        int idxAfter = Cycle.index(p, t);
        boolean ok = p.anchor.equals(wantAnchor) && idxBefore == idxAfter;
        if (!ok) fails++;
        System.out.println((ok ? "PASS " : "FAIL ") + name
                + " | anchor " + anchor + " -> " + p.anchor + " (want " + wantAnchor + ")"
                + " | today " + today + " | idx " + idxBefore + " -> " + idxAfter
                + " | moved=" + moved);
    }

    public static void main(String[] args) {
        String apn = "W,480,960,-1,-1|W,480,960,-1,-1|W,960,1440,-1,-1|W,960,1440,-1,-1"
                + "|R|W,0,480,-1,-1|W,0,480,-1,-1|R";   // 8 天
        String c16 = "W,480,1200,-1,-1|W,480,1200,-1,-1|R|W,480,1200,-1,-1|R|W,960,1920,-1,-1|R|R"
                + "|W,480,1200,-1,-1|W,480,1200,-1,-1|R|W,480,1200,-1,-1|R|W,960,1920,-1,-1|R|R"; // 16 天

        // APN 8 天
        check("APN 未满一轮(7天) 不动", "2026-11-01", "2026-11-08", "2026-11-01", Prefs.MODE_THREE, apn);
        check("APN 正好一轮(8天) 前移到今天", "2026-11-01", "2026-11-09", "2026-11-09", Prefs.MODE_THREE, apn);
        check("APN 一轮半(14天) 前移1轮", "2026-11-01", "2026-11-15", "2026-11-09", Prefs.MODE_THREE, apn);
        check("APN 25天 前移3轮", "2026-11-01", "2026-11-26", "2026-11-25", Prefs.MODE_THREE, apn);
        check("APN 第1天在未来 不动", "2026-12-01", "2026-11-26", "2026-12-01", Prefs.MODE_THREE, apn);

        // 自定义 16 天
        check("16天 未满一轮(5天) 不动", "2026-11-01", "2026-11-06", "2026-11-01", Prefs.MODE_CYCLE, c16);
        check("16天 56天 前移3轮", "2026-10-01", "2026-11-26", "2026-11-18", Prefs.MODE_CYCLE, c16);
        check("16天 32天 正好2轮", "2026-10-01", "2026-11-02", "2026-11-02", Prefs.MODE_CYCLE, c16);

        // 长白班不动
        check("长白班 不动", "2026-01-01", "2026-11-26", "2026-01-01", Prefs.MODE_FIXED, apn);

        System.out.println(fails == 0 ? "ALL PASS" : (fails + " FAILED"));
    }
}

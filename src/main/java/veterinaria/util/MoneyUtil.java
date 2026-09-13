package veterinaria.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class MoneyUtil {

    private static final Locale LOCALE_AR = new Locale("es", "AR");
    private static final Locale LOCALE_US = Locale.US;

    private static final DecimalFormat DF_STD;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(LOCALE_US);
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator('\0');
        DF_STD = new DecimalFormat("0.00", symbols);
        DF_STD.setGroupingUsed(false);
    }

    private MoneyUtil() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Monto vacío");
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("Monto vacío");
        }

        s = s.replace("$", "");
        s = s.replace("AR$", "");
        s = s.replace(" ", "");

        if (s.contains(",") && s.contains(".")) {
            s = s.replace(".", "");
            s = s.replace(",", ".");
        } else if (s.contains(",")) {
            s = s.replace(",", ".");
        }

        BigDecimal bd;
        try {
            bd = new BigDecimal(s);
        } catch (NumberFormatException ex) {
            try {
                NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_AR);
                Number n = nf.parse(raw);
                bd = BigDecimal.valueOf(n.doubleValue());
            } catch (ParseException pe) {
                throw new IllegalArgumentException("Monto inválido: " + raw);
            }
        }

        return bd.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(Double value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal percent(String rawPercent) {
        BigDecimal p = parse(rawPercent);
        return p.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    }

    public static String format(BigDecimal value) {
        BigDecimal v = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_AR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }

    public static String formatStandard(BigDecimal value) {
        BigDecimal v = (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
        return DF_STD.format(v);
    }

    public static BigDecimal scale(BigDecimal value, int decimals, RoundingMode mode) {
        if (decimals < 0) decimals = 0;
        if (mode == null) mode = RoundingMode.HALF_UP;
        BigDecimal v = (value == null ? BigDecimal.ZERO : value);
        return v.setScale(decimals, mode);
    }

    public static String format(BigDecimal value, int decimals, RoundingMode mode) {
        BigDecimal v = scale(value, decimals, mode);
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_AR);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        return nf.format(v);
    }
}

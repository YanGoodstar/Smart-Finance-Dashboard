package com.smartfinance.dashboard.module.importjob;

import java.util.Set;
import java.util.Locale;

public final class ImportSourceTypes {

    public static final String ALIPAY_CSV = "ALIPAY_CSV";
    public static final String WECHAT_CSV = "WECHAT_CSV";

    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of(ALIPAY_CSV, WECHAT_CSV);

    private ImportSourceTypes() {
    }

    public static boolean isSupported(String sourceType) {
        if (sourceType == null) {
            return false;
        }
        return SUPPORTED_SOURCE_TYPES.contains(sourceType.trim().toUpperCase(Locale.ROOT));
    }
}

package com.quantfund.data.validator;

import com.quantfund.common.entity.NavHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 数据质量校验器
 */
public class DataValidator {

    private static final Logger log = LoggerFactory.getLogger(DataValidator.class);

    /** 允许的最大连续缺失交易日 */
    private static final int MAX_MISSING_DAYS = 3;

    /** 单日净值最大变动百分比（超过视为异常值） */
    private static final BigDecimal MAX_DAILY_CHANGE = new BigDecimal("0.15");

    /**
     * 校验净值数据的质量和完整性
     */
    public ValidationResult validateNavData(List<NavHistory> navList) {
        ValidationResult result = new ValidationResult();

        if (navList == null || navList.isEmpty()) {
            result.addError("净值数据为空");
            return result;
        }

        // 1. 检查日期排序和连续性
        checkDateContinuity(navList, result);

        // 2. 检查净值合理性
        checkNavReasonable(navList, result);

        // 3. 检查缺失值
        checkMissingValues(navList, result);

        // 4. 检查重复
        checkDuplicates(navList, result);

        log.info("Validation completed: {} errors, {} warnings for {} records",
                result.errorCount(), result.warningCount(), navList.size());

        return result;
    }

    private void checkDateContinuity(List<NavHistory> navList, ValidationResult result) {
        for (int i = 1; i < navList.size(); i++) {
            LocalDate prev = navList.get(i - 1).getNavDate();
            LocalDate curr = navList.get(i).getNavDate();

            if (curr.isBefore(prev)) {
                result.addError(String.format("日期乱序: %s 在 %s 之前", curr, prev));
            }

            long daysBetween = ChronoUnit.DAYS.between(prev, curr);
            // 跳过周末（周五->周一 = 3天正常）
            if (daysBetween > 10) {
                result.addWarning(String.format("日期间隔过大: %s 到 %s (%d天)", prev, curr, daysBetween));
            }
        }
    }

    private void checkNavReasonable(List<NavHistory> navList, ValidationResult result) {
        for (NavHistory nav : navList) {
            if (nav.getUnitNav() == null) continue;

            // 净值必须为正
            if (nav.getUnitNav().compareTo(BigDecimal.ZERO) <= 0) {
                result.addError(String.format("%s: 净值为负/零: %s", nav.getNavDate(), nav.getUnitNav()));
            }

            // 单日收益异常检测
            if (nav.getDailyReturn() != null) {
                BigDecimal absReturn = nav.getDailyReturn().abs();
                if (absReturn.compareTo(MAX_DAILY_CHANGE) > 0) {
                    result.addWarning(String.format("%s: 单日收益异常: %s%%",
                            nav.getNavDate(), nav.getDailyReturn().multiply(new BigDecimal("100"))));
                }
            }
        }
    }

    private void checkMissingValues(List<NavHistory> navList, ValidationResult result) {
        for (NavHistory nav : navList) {
            if (nav.getUnitNav() == null && nav.getAccNav() == null) {
                result.addWarning(String.format("%s: 单位净值和累计净值均为null", nav.getNavDate()));
            }
        }
    }

    private void checkDuplicates(List<NavHistory> navList, ValidationResult result) {
        for (int i = 1; i < navList.size(); i++) {
            if (navList.get(i).getNavDate().equals(navList.get(i - 1).getNavDate())) {
                result.addError(String.format("重复日期: %s", navList.get(i).getNavDate()));
            }
        }
    }

    /**
     * 判断是否为交易日（A股：周一至周五，排除法定节假日）
     * 简化版：仅排除周末
     */
    public static boolean isWeekday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }
}

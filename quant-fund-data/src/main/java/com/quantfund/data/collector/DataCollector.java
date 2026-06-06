package com.quantfund.data.collector;

import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;

import java.time.LocalDate;
import java.util.List;

/**
 * 数据采集器接口 —— 统一不同数据源的实现
 */
public interface DataCollector {

    /** 采集全量基金列表 */
    List<Fund> fetchFundList() throws Exception;

    /** 采集单只基金的净值历史 */
    List<NavHistory> fetchNavHistory(String fundCode, LocalDate start, LocalDate end) throws Exception;

    /** 采集基金持仓（季度报告） */
    String fetchFundHoldings(String fundCode, String reportDate) throws Exception;

    /** 采集基金经理信息 */
    String fetchManagerInfo(String fundCode) throws Exception;

    /** 采集基准指数日行情 */
    String fetchIndexDaily(String indexCode, LocalDate start, LocalDate end) throws Exception;

    /** 数据源名称 */
    String getSourceName();

    /** 测试连通性 */
    boolean testConnection();
}

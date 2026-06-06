package com.quantfund.data.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 东方财富数据采集器 —— 直连东方财富/天天基金HTTP接口
 *
 * 接口来源（通过AKShare及抓包逆向工程确定）：
 * - 基金列表: fund.eastmoney.com 的rankhandler接口
 * - 净值历史: api.fund.eastmoney.com/f10/lsjz
 * - 基金详情: fund.eastmoney.com/pingzhongdata/{code}.js
 */
public class EastMoneyCollector implements DataCollector {

    private static final Logger log = LoggerFactory.getLogger(EastMoneyCollector.class);

    private static final String SOURCE_NAME = "eastmoney";

    // 东方财富基金列表JS（返回全量基金数组，格式：var r = [["000001","HXCZHH","华夏成长混合",...],...]）
    private static final String FUND_LIST_URL =
            "http://fund.eastmoney.com/js/fundcode_search.js";

    // 天天基金净值历史接口
    private static final String NAV_HISTORY_URL =
            "http://api.fund.eastmoney.com/f10/lsjz?fundCode=%s&pageIndex=%d&pageSize=100&startDate=%s&endDate=%s";

    // 基金详情JS数据
    private static final String FUND_DETAIL_URL =
            "http://fund.eastmoney.com/pingzhongdata/%s.js";

    // 上海/深圳指数行情（新浪接口）
    private static final String INDEX_DAILY_URL =
            "http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=240&ma=no&datalen=500";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EastMoneyCollector() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .addHeader("Referer", "http://fund.eastmoney.com/")
                            .build();
                    return chain.proceed(req);
                })
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    @Override
    public boolean testConnection() {
        try {
            Request req = new Request.Builder().url("http://fund.eastmoney.com/").build();
            try (Response resp = httpClient.newCall(req).execute()) {
                return resp.isSuccessful();
            }
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }

    // ============ 基金列表 ============

    @Override
    public List<Fund> fetchFundList() throws Exception {
        List<Fund> funds = new ArrayList<>();

        Request req = new Request.Builder()
                .url(FUND_LIST_URL)
                .addHeader("Referer", "http://fund.eastmoney.com/")
                .build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new RuntimeException("Failed to fetch fund list: " + resp.code());
            }
            String body = resp.body().string();

            // 格式：var r = [["000001","HXCZHH","华夏成长混合","混合型-灵活","..."]];
            // 提取 [...] 部分
            int start = body.indexOf('[');
            int end = body.lastIndexOf(']');
            if (start < 0 || end < 0) {
                log.warn("Could not parse fund list from response (len={})", body.length());
                return funds;
            }

            String arrayStr = body.substring(start, end + 1);
            // 使用 Jackson 解析 JSON 数组
            var array = objectMapper.readValue(arrayStr, List.class);

            for (Object item : array) {
                List<String> fields = (List<String>) item;
                if (fields.size() < 3) continue;

                Fund fund = new Fund();
                fund.setFundCode(fields.get(0));
                fund.setFundName(fields.get(2));
                fund.setFundShortName(fields.get(2));
                fund.setDataSource(SOURCE_NAME);
                fund.setIsActive(true);

                // 类型映射
                if (fields.size() >= 4) {
                    String rawType = fields.get(3);
                    fund.setFundType(mapFundType(rawType));
                    fund.setFundSubtype(rawType);
                } else {
                    fund.setFundType("开放式");
                }

                // 判断ETF/指数
                String name = fund.getFundName();
                fund.setIsEtf(name.contains("ETF") || name.contains("交易型"));
                fund.setIsIndex(name.contains("指数"));

                funds.add(fund);
            }
        }

        log.info("Fetched {} funds from {}", funds.size(), FUND_LIST_URL);

        // 尝试补充部分基金的详细信息
        enrichSampleFunds(funds);

        return funds;
    }

    private void enrichSampleFunds(List<Fund> funds) {
        // 采样前20只基金获取基金公司等信息，避免请求过多
        int count = 0;
        for (Fund fund : funds) {
            if (count >= 20) break;
            try {
                Fund detail = fetchFundDetail(fund.getFundCode());
                if (detail != null && detail.getManagementCompany() != null) {
                    fund.setManagementCompany(detail.getManagementCompany());
                    fund.setCustodianBank(detail.getCustodianBank());
                    fund.setInceptionDate(detail.getInceptionDate());
                    fund.setManagementFee(detail.getManagementFee());
                    fund.setBenchmark(detail.getBenchmark());
                    count++;
                }
                Thread.sleep(100); // 请求间隔
            } catch (Exception e) {
                // skip individual fund detail failures
            }
        }
        log.info("Enriched {} fund details", count);
    }

    /**
     * 获取单只基金的详细信息（费率、基金经理、规模等）
     */
    public Fund fetchFundDetail(String fundCode) throws Exception {
        String url = String.format(FUND_DETAIL_URL, fundCode);
        Request req = new Request.Builder().url(url).build();

        try (Response resp = httpClient.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return null;
            String jsContent = resp.body().string();

            Fund fund = new Fund();
            fund.setFundCode(fundCode);
            fund.setDataSource(SOURCE_NAME);

            // 从JS变量中提取数据
            // var fS_name = "易方达蓝筹精选混合";
            fund.setFundName(extractJsVar(jsContent, "fS_name"));
            fund.setFundShortName(extractJsVar(jsContent, "fS_shortName"));
            fund.setFundType(extractJsVar(jsContent, "fS_typename"));

            // var isETF = "1";  var isIndexFund = "0";
            fund.setIsEtf("1".equals(extractJsVar(jsContent, "isETF")));
            fund.setIsIndex("1".equals(extractJsVar(jsContent, "isIndexFund")));

            // 费率：var fund_managersRate = "1.50";  管理费
            String mgmtFee = extractJsVar(jsContent, "fund_managersRate");
            if (!mgmtFee.isEmpty()) fund.setManagementFee(new BigDecimal(mgmtFee));

            // 规模：var fund_sourceRate = "xxx";
            String size = extractJsVar(jsContent, "fund_sumSize");
            if (!size.isEmpty()) fund.setFundSize(new BigDecimal(size.replace(",", "")));

            // 基金公司
            fund.setManagementCompany(extractJsVar(jsContent, "fS_companyname"));
            fund.setCustodianBank(extractJsVar(jsContent, "fS_custodianname"));

            // 成立日期
            String inception = extractJsVar(jsContent, "fS_clrq");
            if (!inception.isEmpty()) {
                fund.setInceptionDate(LocalDate.parse(inception, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            }

            return fund;
        }
    }

    // ============ 净值历史 ============

    @Override
    public List<NavHistory> fetchNavHistory(String fundCode, LocalDate start, LocalDate end) throws Exception {
        List<NavHistory> result = new ArrayList<>();
        int pageIndex = 1;
        boolean hasMore = true;
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (hasMore) {
            String url = String.format(NAV_HISTORY_URL, fundCode, pageIndex,
                    start.format(df), end.format(df));
            Request req = new Request.Builder().url(url).build();

            try (Response resp = httpClient.newCall(req).execute()) {
                if (!resp.isSuccessful() || resp.body() == null) break;
                String body = resp.body().string();
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.get("Data");
                if (data == null) break;

                JsonNode lsjzList = data.get("LSJZList");
                if (lsjzList == null || !lsjzList.isArray() || lsjzList.size() == 0) {
                    hasMore = false;
                    break;
                }

                for (JsonNode item : lsjzList) {
                    NavHistory nav = new NavHistory();
                    // 从funds表中找到fund_id（此处先设null，入库时由service层填充）
                    // nav.setFundId(fundId);

                    String dateStr = item.get("FSRQ").asText();
                    nav.setNavDate(LocalDate.parse(dateStr, df));

                    String unitNav = item.get("DWJZ").asText();
                    if (!unitNav.isEmpty()) nav.setUnitNav(new BigDecimal(unitNav));

                    String accNav = item.get("LJJZ").asText();
                    if (!accNav.isEmpty()) nav.setAccNav(new BigDecimal(accNav));

                    // 日增长率
                    String dailyGrowth = item.get("JZZZL").asText();
                    if (!dailyGrowth.isEmpty()) {
                        nav.setDailyReturn(new BigDecimal(dailyGrowth).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
                    }

                    nav.setDataSource(SOURCE_NAME);
                    result.add(nav);
                }

                // 判断是否还有更多页
                int totalCount = root.get("TotalCount") != null ? root.get("TotalCount").asInt() : 0;
                hasMore = (pageIndex * 100) < totalCount;
                pageIndex++;

                // 防护：最多获取5000条（约20年交易日）
                if (pageIndex > 50) hasMore = false;
            }
        }

        log.info("Fetched {} nav records for fund {} ({} - {})", result.size(), fundCode, start, end);
        return result;
    }

    // ============ 基金持仓 ============

    @Override
    public String fetchFundHoldings(String fundCode, String reportDate) throws Exception {
        // 持仓数据可从天天基金持仓页面解析：
        // http://fundf10.eastmoney.com/ccmx_{fundCode}.html
        String url = String.format("http://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code=%s&topline=10", fundCode);
        Request req = new Request.Builder().url(url).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    @Override
    public String fetchManagerInfo(String fundCode) throws Exception {
        String url = String.format("http://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jijinjingli&code=%s", fundCode);
        Request req = new Request.Builder().url(url).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    @Override
    public String fetchIndexDaily(String indexCode, LocalDate start, LocalDate end) throws Exception {
        String url = String.format(INDEX_DAILY_URL, indexCode);
        Request req = new Request.Builder().url(url).build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return resp.body() != null ? resp.body().string() : "";
        }
    }

    // ============ 工具方法 ============

    private String extractJsVar(String jsContent, String varName) {
        Pattern p = Pattern.compile("var\\s+" + varName + "\\s*=\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(jsContent);
        return m.find() ? m.group(1) : "";
    }

    private String mapFundType(String rawType) {
        if (rawType == null || rawType.isEmpty()) return "开放式";
        return switch (rawType) {
            case "ETF", "ETF联接" -> "ETF";
            case "LOF" -> "LOF";
            case "货币型", "货币" -> "货币";
            case "封闭式" -> "封闭式";
            case "QDII" -> "QDII";
            case "FOF" -> "FOF";
            default -> "开放式";
        };
    }
}

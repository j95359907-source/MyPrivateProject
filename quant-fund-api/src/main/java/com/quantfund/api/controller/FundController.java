package com.quantfund.api.controller;

import com.quantfund.api.dto.*;
import com.quantfund.api.service.FundService;
import com.quantfund.common.entity.Fund;
import com.quantfund.common.entity.NavHistory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/funds")
@Tag(name = "基金数据", description = "基金查询、筛选、详情接口")
public class FundController {

    @Autowired
    private FundService fundService;

    // ========== 基金列表与搜索 ==========

    @GetMapping
    @Operation(summary = "基金列表", description = "分页查询基金列表，支持类型筛选和关键词搜索")
    public ResponseEntity<PageResponse<FundDto>> listFunds(
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Fund> fundPage = fundService.searchFunds(fundType, keyword, page, size);
        Page<FundDto> dtoPage = fundPage.map(FundDto::fromEntity);
        return ResponseEntity.ok(PageResponse.of(dtoPage));
    }

    @GetMapping("/etf")
    @Operation(summary = "ETF列表", description = "获取所有ETF基金")
    public ResponseEntity<List<FundDto>> listEtfFunds() {
        List<Fund> funds = fundService.getEtfFunds();
        return ResponseEntity.ok(funds.stream().map(FundDto::fromEntity).toList());
    }

    // ========== 基金详情 ==========

    @GetMapping("/{fundCode}")
    @Operation(summary = "基金详情", description = "获取指定基金的详细信息")
    public ResponseEntity<FundDetailDto> getFundDetail(@PathVariable String fundCode) {
        Fund fund = fundService.getByFundCode(fundCode);
        if (fund == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(FundDetailDto.fromEntity(fund));
    }

    // ========== 净值数据 ==========

    @GetMapping("/{fundCode}/nav")
    @Operation(summary = "净值历史", description = "获取基金净值历史数据")
    public ResponseEntity<List<NavHistoryDto>> getNavHistory(
            @PathVariable String fundCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start == null) start = LocalDate.now().minusYears(1);
        if (end == null) end = LocalDate.now();

        List<NavHistory> navList = fundService.getNavHistory(fundCode, start, end);
        return ResponseEntity.ok(navList.stream().map(NavHistoryDto::fromEntity).toList());
    }

    // ========== 筛选排名 ==========

    @GetMapping("/screener")
    @Operation(summary = "基金筛选", description = "多维度筛选基金：收益率、夏普比率、最大回撤等")
    public ResponseEntity<List<FundScreenerDto>> screenFunds(
            @RequestParam(required = false) String fundType,
            @RequestParam(required = false) @Parameter(description = "最低1年收益(%)") BigDecimal minReturn1y,
            @RequestParam(required = false) @Parameter(description = "最大1年回撤(%)") BigDecimal maxDrawdown1y,
            @RequestParam(required = false) @Parameter(description = "最低夏普比率") BigDecimal minSharpe,
            @RequestParam(required = false) @Parameter(description = "排序字段") String sortBy,
            @RequestParam(defaultValue = "20") int topN) {

        List<FundScreenerDto> results = fundService.screenFunds(
                fundType, minReturn1y, maxDrawdown1y, minSharpe, sortBy, topN);
        return ResponseEntity.ok(results);
    }

    // ========== 辅助 ==========

    @GetMapping("/companies")
    @Operation(summary = "基金公司列表")
    public ResponseEntity<List<String>> listCompanies() {
        return ResponseEntity.ok(fundService.getDistinctCompanies());
    }

    @GetMapping("/types")
    @Operation(summary = "基金类型列表")
    public ResponseEntity<List<String>> listTypes() {
        return ResponseEntity.ok(fundService.getDistinctTypes());
    }

    @GetMapping("/stats")
    @Operation(summary = "基金统计")
    public ResponseEntity<FundStatsDto> getStats() {
        return ResponseEntity.ok(fundService.getStats());
    }
}

package com.quantfund.trading.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 消息通知服务 —— 交易信号/告警推送
 *
 * 支持渠道：控制台日志 / 钉钉机器人 / 邮件
 * 后续可扩展：Server酱微信推送 / Telegram Bot
 */
public class NotifyService {

    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

    private NotifyChannel channel = NotifyChannel.CONSOLE;
    private String dingTalkWebhook;
    private String emailRecipient;

    public enum NotifyChannel { CONSOLE, DINGTALK, EMAIL }

    public void setDingTalk(String webhook) {
        this.dingTalkWebhook = webhook;
        this.channel = NotifyChannel.DINGTALK;
    }

    public void setEmail(String recipient) {
        this.emailRecipient = recipient;
        this.channel = NotifyChannel.EMAIL;
    }

    /** 发送交易信号通知 */
    public void sendTradeSignals(List<TradeSignal> signals) {
        if (signals.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("📊 量化交易信号\n");
        sb.append("═══════════════\n");

        for (TradeSignal s : signals) {
            sb.append(String.format("• %s %s %s 权重%.1f%% 置信度%.0f%%\n",
                    s.type(), s.fundCode(), s.fundName(), s.weight() * 100, s.confidence() * 100));
            if (s.reason() != null) sb.append("  理由: ").append(s.reason()).append("\n");
        }

        send(sb.toString());
    }

    /** 发送订单执行通知 */
    public void sendOrderExecution(String orderId, String fundCode, String type,
                                    double amount, String status) {
        String emoji = "EXECUTED".equals(status) ? "✅" : "❌";
        String msg = String.format("%s 订单%s: %s %s ¥%.2f",
                emoji, status, type, fundCode, amount);
        send(msg);
    }

    /** 发送风险告警 */
    public void sendAlert(String title, String message) {
        send("🚨 " + title + "\n" + message);
    }

    private void send(String message) {
        switch (channel) {
            case CONSOLE -> log.info("[通知] {}", message);
            case DINGTALK -> sendDingTalk(message);
            case EMAIL -> sendEmail(message);
        }
    }

    private void sendDingTalk(String message) {
        if (dingTalkWebhook == null) return;
        log.info("[钉钉] {}", message);
        // 实际实现：OkHttp POST JSON到webhook
        // {"msgtype":"text","text":{"content":"消息内容"}}
    }

    private void sendEmail(String message) {
        if (emailRecipient == null) return;
        log.info("[邮件] {} → {}", emailRecipient, message);
    }

    // --- 数据类型 ---

    public record TradeSignal(String fundCode, String fundName, String type,
                               double weight, double confidence, String reason) {}
}

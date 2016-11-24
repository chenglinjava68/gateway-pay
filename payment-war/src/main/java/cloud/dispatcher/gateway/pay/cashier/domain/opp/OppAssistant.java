package cloud.dispatcher.gateway.pay.cashier.domain.opp;

import java.util.Map;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;

public interface OppAssistant {

    public Map<String, String> buildPayingRequestParamsMap(
           PayingEntity entity, long logId, String clientIP);

    public Map<String, String> parsePayingNotifyParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    public Map<String, String> parsePayingReturnParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    public Map<String, String> buildRefundRequestParamsMap(
           PayingEntity paying, RefundEntity refund, long logId);

    public Map<String, String> parseRefundNotifyParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    public boolean commitRefundRequest(Map<String, String> params);
}

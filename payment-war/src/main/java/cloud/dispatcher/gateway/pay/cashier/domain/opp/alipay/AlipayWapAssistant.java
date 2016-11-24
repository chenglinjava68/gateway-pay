package cloud.dispatcher.gateway.pay.cashier.domain.opp.alipay;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class AlipayWapAssistant extends AlipayAssistant {

    private static String ALIPAY_GATEWAY_TARGET_URI = "http://wappaygw.alipay.com/service/rest.htm";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> buildPayingRequestParamsMap(PayingEntity entity, long logId,
            String clientIP) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);
        try {
            paramsMap.put("partner", oppAuth.get("alipay").get("partner"));
            paramsMap.put("service", "alipay.wap.trade.create.direct");
            paramsMap.put("_input_charset", "UTF-8");
            paramsMap.put("v", "2.0");
            paramsMap.put("sec_id", "MD5");
            paramsMap.put("format", "xml");
            paramsMap.put("req_id", entity.getId().substring(0, 3) + logId);
            paramsMap.put("req_data", buildRequestData(entity, logId));

            String post1 = convertRequestParamMap(paramsMap);
            paramsMap.put("sign", sign(post1, oppAuth.get("alipay").get("key"), "MD5", "UTF-8"));

            String requestToken = parseRequestToken(URLDecoder.decode(sendHttpRequest(
                    ALIPAY_GATEWAY_TARGET_URI, paramsMap, "UTF-8"), "UTF-8"));
            if (StringUtils.isEmpty(requestToken)) {return Collections.EMPTY_MAP;}

            paramsMap.remove("req_id");

            paramsMap.put("req_data", "<auth_and_execute_req><request_token>" + requestToken +
                    "</request_token></auth_and_execute_req>");
            paramsMap.put("service", "alipay.wap.auth.authAndExecute");

            String post2 = convertRequestParamMap(paramsMap);
            paramsMap.put("sign", sign(post2, oppAuth.get("alipay").get("key"), "MD5", "UTF-8"));
            return paramsMap;
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
    
    @SuppressWarnings("unchecked")
    private String buildRequestData(PayingEntity entity, long logId) {
        Map<String, String> item = JsonUtil.decode(entity.getAppOrderItem(), Map.class);

        StringBuffer buffer = new StringBuffer("<direct_trade_create_req>");

        buffer.append("<out_trade_no>").append(String.valueOf(entity.getId())
                .substring(3)).append("</out_trade_no>");

        buffer.append("<subject>").append(item.get("name")).append("</subject>");
        
        String expiredTime = expiredTimeMap.get(entity.getAppOrderSource());
        if (expiredTime != null) {
            buffer.append("<pay_expire>").append(expiredTime).append("</pay_expire>");
        }

        double price = new BigDecimal(entity.getAppOrderFee()).divide(new BigDecimal(100),
                2, BigDecimal.ROUND_HALF_UP).doubleValue();
        buffer.append("<total_fee>").append(price).append("</total_fee>");

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);
        String sellerAccount = oppAuth.get("alipay").get("seller_email");
        buffer.append("<seller_account_name>").append(sellerAccount);
        buffer.append("</seller_account_name>");

        buffer.append("<call_back_url>").append(buildPayingReturnURL(
                entity.getId(), logId)).append("</call_back_url>");

        buffer.append("<notify_url>").append(buildPayingNotifyURL(
                entity.getId(), logId)).append("</notify_url>");

        return buffer.append("</direct_trade_create_req>").toString();
    }
    
    @SuppressWarnings("unchecked")
    private String parseRequestToken(String response) {
        String[] splited = response.split("&");
        Map<String, String> paramsMap = new HashMap<String, String>();
        for (int i = 0; i < splited.length; i++) {
            int pos = splited[i].indexOf("=");
            int len = splited[i].length();
            String key = splited[i].substring(0, pos);
            String val = splited[i].substring(pos + 1, len);
            paramsMap.put(key, val);
        }

        if (StringUtils.isEmpty(paramsMap.get("res_data")))
            return "";

        try {
            Document document = DocumentHelper.parseText(
                    paramsMap.get("res_data"));
            List<Element> childs = document.getRootElement().elements();
            for (Element child : childs) {
                if (child.getName().equals("request_token")) {
                    return child.getStringValue();
                }
            }
            return "";
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
    
    @Override
    public Map<String, String> parsePayingNotifyParamsMap(Map<String, String> params,
            Map<String, Map<String, String>> oppAuth) {
        checkNotifyParamsMap(params, oppAuth.get("alipay"));
        try {
            Map<String, String> result = new HashMap<String, String>();
            Document document = DocumentHelper.parseText(params.get("notify_data"));
            String status = document.selectSingleNode("//notify/trade_status").getText();
            if (status.equals("TRADE_FINISHED") || status.equals("TRADE_SUCCESS")) {
                String tradeNo = document.selectSingleNode("//notify/trade_no").getText();
                String account = document.selectSingleNode("//notify/buyer_id").getText() +
                        ":" + document.selectSingleNode("//notify/buyer_email").getText();
                result.put("tradeNo", tradeNo);
                result.put("account", account);
            }
            return result;
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    @Override
    public Map<String, String> parsePayingReturnParamsMap(Map<String, String> params, 
            Map<String, Map<String, String>> oppAuth) {
        checkReturnParamsMap(params, oppAuth.get("alipay"));
        Map<String, String> result = new HashMap<String, String>();
        if (params.get("result").equals("success")) {
            result.put("tradeNo", params.get("trade_no"));
        }
        return result;
    }

    @Override
    protected void checkNotifyParamsMap(Map<String, String> params, Map<String, String> oppAuth) {
        String sign = StringUtils.isEmpty(params.get("sign")) ? "" : params.get("sign");
        StringBuffer converted = new StringBuffer();
        converted.append("service=").append(params.get("service")).append("&");
        converted.append("v=").append(params.get("v")).append("&");
        converted.append("sec_id=").append(params.get("sec_id")).append("&");
        converted.append("notify_data=").append(params.get("notify_data"));
        if (!sign(converted.toString(), oppAuth.get("key"), "MD5", "UTF-8").equals(sign)) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
        
        try {
            Document document = DocumentHelper.parseText(params.get("notify_data"));
            String notifyId = document.selectSingleNode("//notify/notify_id").getText();
            if (StringUtils.isNotEmpty(notifyId) && !checkNotifyOnRemote(
                    notifyId, oppAuth.get("partner"))) {
                throw new IllegalArgumentException(
                        "Illegal notify params, params = [" + params + "]");
            }
        } catch (DocumentException error) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
    }
    
    @Override
    protected void checkReturnParamsMap(Map<String, String> params, Map<String, String> oppAuth) {
        String sign = StringUtils.isEmpty(params.get("sign")) ? "" : params.get("sign");
        String converted = convertRequestParamMap(params);
        if (!sign(converted, oppAuth.get("key"), "MD5", "UTF-8").equals(sign)) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
    }
}

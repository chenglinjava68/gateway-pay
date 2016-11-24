package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin;

import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.utils.CodeUtil;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class WeixinWapAssistant extends WeixinAssistant {

    @Getter @Setter private JsApiTicketHolder simpleJsApiTicketHolder;

    @Getter @Setter private String domain;

    @Override
    public Map<String, String> buildPayingRequestParamsMap(PayingEntity entity, long logId, String clientIP) {
        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
            entity.getOppGatewayAuth(), Map.class);

        Map<String, String> paramsMap = new HashMap<String, String>();

        String noncestr = CodeUtil.md5Hex(entity.getId() + String.valueOf(logId), "UTF-8");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("wxmpay"));
        if (StringUtils.isEmpty(accessToken)) {
            accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("wxmpay"), "");
        }

        String jsApiTicket = simpleJsApiTicketHolder.getJsApiTicket(accessToken);
        if (StringUtils.isEmpty(jsApiTicket)) {
            try {
                jsApiTicket = simpleJsApiTicketHolder.getJsApiTicket(accessToken, "");
            } catch (Exception error) {
                accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("wxmpay"), "");
                jsApiTicket = simpleJsApiTicketHolder.getJsApiTicket(accessToken, "");
            }
        }

        Map<String, String> configParamsMap = new HashMap<String, String>();
        configParamsMap.put("jsapi_ticket", jsApiTicket);
        configParamsMap.put("noncestr", noncestr);
        configParamsMap.put("timestamp", timestamp);
        configParamsMap.put("url", domain + "/wap/paying/redirect/3/" + entity.getId());

        paramsMap.put("config_app_id", oppAuth.get("wxmpay").get("app_id"));
        paramsMap.put("config_timestamp", timestamp);
        paramsMap.put("config_noncestr", noncestr);
        paramsMap.put("config_signature", CodeUtil.shaHex(
                convertRequestParamMap(configParamsMap), "UTF-8"));

        Map<String, String> pack1 = buildPayingPackageParamsMap(entity, logId, clientIP);
        String sign = CodeUtil.md5Hex(convertRequestParamMap(pack1) + "&key=" +
                oppAuth.get("wxmpay").get("partkey"), "UTF-8").toUpperCase();

//        Map<String, String> pack2 = new HashMap<String, String>();
//        try {
//            List<String> keys = new ArrayList<String>(pack1.keySet());
//            for (int i = 0; i < keys.size(); i++) {
//                String key = keys.get(i);
//                pack2.put(key, URLEncoder.encode(pack1.get(key), "GBK").replace("+", "%20"));
//            }
//        } catch (UnsupportedEncodingException error) {
//            throw new RuntimeException("", error);
//        }

        String packageString = convertRequestParamMap(pack1) + "&sign=" + sign;

        Map<String, String> wxpayParamsMap = new HashMap<String, String>();
        wxpayParamsMap.put("appkey", oppAuth.get("wxmpay").get("appkey"));
        wxpayParamsMap.put("appid", oppAuth.get("wxmpay").get("app_id"));
        wxpayParamsMap.put("package", packageString);
        wxpayParamsMap.put("timestamp", timestamp);
        wxpayParamsMap.put("noncestr", noncestr);

        paramsMap.put("wxpay_timestamp", timestamp);
        paramsMap.put("wxpay_package", packageString);
        paramsMap.put("wxpay_noncestr", noncestr);
        paramsMap.put("wxpay_sign_type", "SHA1");
        paramsMap.put("wxpay_sign_text", CodeUtil.shaHex(
                convertRequestParamMap(wxpayParamsMap), "UTF-8"));

        return paramsMap;
    }

    protected Map<String, String> buildPayingPackageParamsMap(PayingEntity entity,
                                                              long logId, String clientIP) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("input_charset", "GBK");
        params.put("fee_type", "1");
        params.put("bank_type", "WX");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        params.put("time_expire", dateFormat.format(
                entity.getAppOrderExpire()));

        params.put("total_fee", String.valueOf(entity.getAppOrderFee()));
        params.put("spbill_create_ip", clientIP);
        params.put("out_trade_no", String.valueOf(
                entity.getId()).substring(3));

        Map<String, Map<String, String>> auth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);
        params.put("partner", auth.get("wxmpay").get("partner"));

        Map<String, String> item = JsonUtil.decode(
                entity.getAppOrderItem(), Map.class);
        params.put("body", item.get("name"));

        params.put("notify_url", buildPayingNotifyURL(entity.getId(), logId));
        return params;
    }

    /**
     * ����֧��ƽ̨�ڻص�ʱ�ύ��֧��������ص�����MAP
     *
     * @return ֧��ƽ̨���صĲ�����ˮ��
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> parsePayingNotifyParamsMap(Map<String, String> params,
                                                          Map<String, Map<String, String>> oppAuth) {
        checkPayingNotifyParamsMap(params, oppAuth.get("wxmpay"));
        Map<String, String> member = JsonUtil.decode(params.get("__POSTDATA__"), Map.class);
        Map<String, String> result = new HashMap<String, String>();
        if (params.get("trade_state").equals("0") &&
                params.get("trade_mode").equals("1")) {
            String account = member.get("OpenId");
            String tradeNo = params.get("transaction_id");
            result.put("account", account);
            result.put("tradeNo", tradeNo);
        }
        return result;
    }

    /**
     * ����֧��ƽ̨�ڻ���ʱ�ύ��֧��������ص�����MAP
     *
     * @return ֧��ƽ̨���صĲ�����ˮ��
     */
    @Override
    public Map<String, String> parsePayingReturnParamsMap(
            Map<String, String> params, Map<String, Map<String, String>> oppAuth) {
        return null;
    }

    /**
     * ��֤���յ���֧��֪ͨ����MAP�е�Ԫ���Ƿ�δ���۸�
     *
     * @return Map�е�Ԫ���Ƿ�δ���۸�
     */
    protected void checkPayingNotifyParamsMap(Map<String, String> params, Map<String, String> oppAuth) {
        String signature = CodeUtil.md5Hex(convertRequestParamMap(params) +
                "&key=" + oppAuth.get("partkey"), "gbk");

        if (!signature.equalsIgnoreCase(params.get("sign"))) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }

        checkNotifyOnRemote(params.get("notify_id"), oppAuth, params);
    }

    /**
     * ����notifyId�Լ�partner����ͨ��HTTP�����ڲƸ�ͨ
     * ��ȡATN���
     *
     * @return ֧�������ص�ATN���
     */
    @SuppressWarnings("unchecked")
    protected void checkNotifyOnRemote(String notifyId, Map<String, String> oppAuth,
                                       Map<String, String> params) {
        StringBuffer buffer = new StringBuffer(WEIXIN_NOTIFY_TARGET_URI);
        buffer.append("?partner=").append(oppAuth.get("partner"));
        buffer.append("&notify_id=").append(notifyId).append("&");
        String sign = CodeUtil.md5Hex("notify_id=" + notifyId +
                "&partner=" + oppAuth.get("partner") + "&key=" + oppAuth.get("partkey"), "GBK");
        buffer.append("&sign=").append(sign);
        Map<String, String> notify = new HashMap<String, String>();
        try {
            Document document = DocumentHelper.parseText(
                    sendHttpRequest(buffer.toString()));
            List<Element> childs = document.getRootElement().elements();
            for (Element child : childs) {
                notify.put(child.getName(), child.getStringValue());
            }

            if (StringUtils.isEmpty(notify.get("trade_state")) ||
                    !notify.get("trade_state").equals(params.get("trade_state"))) {
                throw new IllegalArgumentException(
                        "Illegal notify params, params = [" + notify + "]");
            }

            if (StringUtils.isEmpty(notify.get("trade_mode")) ||
                    !notify.get("trade_mode").equals(params.get("trade_mode"))) {
                throw new IllegalArgumentException(
                        "Illegal notify params, params = [" + notify + "]");
            }
        } catch (DocumentException error) {
            throw new RuntimeException("", error);
        }
    }

    /**
     * ͨ��RefundEntity������ύ��֧��ƽ̨�Ĳ���MAP
     *
     * @return �ύ��֧��ƽ̨�Ĳ���MAP
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> buildRefundRequestParamsMap(PayingEntity paying,
                                                           RefundEntity refund, long logId) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                paying.getOppGatewayAuth(), Map.class);

        paramsMap.put("refund_fee", String.valueOf(refund.getAppOrderFee()));
        paramsMap.put("input_charset", "GBK");
        paramsMap.put("total_fee", String.valueOf(paying.getAppOrderFee()));
        paramsMap.put("partner", oppAuth.get("wxmpay").get("partner"));
        paramsMap.put("transaction_id", paying.getOppGatewayCode());
        paramsMap.put("op_user_passwd", oppAuth.get("wxmpay").get("passwd"));
        paramsMap.put("out_refund_no", CodeUtil.md5Hex(
                paying.getId() + String.valueOf(refund.getId()), "GBK"));
        paramsMap.put("op_user_id", oppAuth.get("wxmpay").get("partner"));
        paramsMap.put("sign_type", "MD5");
        paramsMap.put("service_version", "1.1");

        String sign = CodeUtil.md5Hex(convertRequestParamMap(paramsMap) +
                "&key=" + oppAuth.get("wxmpay").get("partkey"), "gbk").toUpperCase();

        paramsMap.put("sign", sign);
        paramsMap.put("__REFUNDID__", String.valueOf(refund.getId()));
        paramsMap.put("__PAYINGID__", paying.getId());
        paramsMap.put("__LOGGERID__", String.valueOf(logId));

        return paramsMap;
    }

    /**
     * ִ���ύ�˿�����Ĳ���, ���ص�ֻ���ύ�Ƿ�ɹ�
     *
     * @param params �˿����MAP
     */
    @Override
    public boolean commitRefundRequest(Map<String, String> params) {
        String payingId = params.get("__PAYINGID__");
        String refundId = params.get("__REFUNDID__");
        String loggerId = params.get("__LOGGERID__");
        params.remove("__PAYINGID__");
        params.remove("__REFUNDID__");
        params.remove("__LOGGERID__");

        WeixinHttpsClient client = new WeixinHttpsClient();
        client.setCertPemFile(new File(certFileRootDirectory + "/tenpay.pem"));
        client.setCertPfxFile(new File(certFileRootDirectory + "/" + params.get("partner") + ".pfx"));
        client.setCertPfxPasswd(params.get("partner"));
        String response = client.doRequest("POST", WEIXIN_REFUND_TARGET_URI,
                convertRequestParamMap(params) + "&sign=" + params.get("sign"));

        NoticeEntity entity = new NoticeEntity();
        entity.setNotifyTime(System.currentTimeMillis() + 5000);
        entity.setRequestURI(buildRefundNotifyURL(payingId, Long.valueOf(refundId),
                Long.valueOf(loggerId)));
        entity.setContent(response);
        simpleNotifierExecutor.register(entity);
        return true;
    }

    /**
     * ����֧��ƽ̨�ڻص�ʱ�ύ���˿����ص�����MAP
     *
     * @return ֧��ƽ̨���صĲ�����ˮ��
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> parseRefundNotifyParamsMap(Map<String, String> params,
                                                          Map<String, Map<String, String>> oppAuth) {
        checkRefundNotifyParamsMap(params, oppAuth.get("wxmpay"));
        if (!params.get("retcode").equals("0")) {
            return Collections.EMPTY_MAP;
        }

        String status = params.get("refund_status");
        if (status.equals("4") || status.equals("8") || status.equals("9") ||
                status.equals("10") || status.equals("11")) {
            return params;
        } else {
            return Collections.EMPTY_MAP;
        }
    }

    /**
     * ��֤���յ����˿�֪ͨ����MAP�е�Ԫ���Ƿ�δ���۸�
     *
     * @return Map�е�Ԫ���Ƿ�δ���۸�
     */
    protected void checkRefundNotifyParamsMap(Map<String, String> params, Map<String, String> oppAuth) {
        String signature = CodeUtil.md5Hex(convertRequestParamMap(params) +
                "&key=" + oppAuth.get("partkey"), "gbk");

        if (!signature.equalsIgnoreCase(params.get("sign"))) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
    }
}

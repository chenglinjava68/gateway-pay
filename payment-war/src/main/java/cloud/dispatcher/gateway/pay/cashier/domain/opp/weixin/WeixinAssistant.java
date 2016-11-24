package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import cloud.dispatcher.gateway.pay.cashier.domain.notifier.bean.entity.NoticeEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.notifier.exectuor.NotifierExecutor;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.AbstractAssistant;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.utils.CodeUtil;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class WeixinAssistant extends AbstractAssistant {

    protected static String WEIXIN_REFUND_TARGET_URI = "https://mch.tenpay.com/refundapi/gateway/refund.xml";

    protected static String WEIXIN_PREPAY_TARGET_URI = "https://api.weixin.qq.com/pay/genprepay";

    protected static String WEIXIN_NOTIFY_TARGET_URI = "https://gw.tenpay.com/gateway/verifynotifyid.xml";

    @Getter @Setter protected AccessTokenHolder simpleAccessTokenHolder;
    
    @Getter @Setter protected String certFileRootDirectory;

    @Getter @Setter protected NotifierExecutor simpleNotifierExecutor;

    protected WeixinAssistant() {}
    
    /**
     * ��������Mapת����key1=val1&key2=val2��ʽ���ַ���
     *
     * @return PACKAGE���ݴ�
     */
    protected String convertRequestParamMap(Map<String, String> params) {
        Map<String, String> request = requestParamMapFilter(params);
        List<String> keys = new ArrayList<String>(request.keySet());
        Collections.sort(keys);

        String converted = "";

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            converted = converted + key + "=" + request.get(key) + "&";
        }
        return converted.substring(0, converted.lastIndexOf("&"));
    }

    /**
     * ���˲���Map�еĿ�ֵ��ǩ����صĲ���
     */
    protected Map<String, String> requestParamMapFilter(Map<String, String> params) {
        Map<String, String> result = new HashMap<String, String>();
        if (params == null || params.size() == 0)
            return result;

        for (String key : params.keySet()) {
            String value = params.get(key);
            if (StringUtils.isEmpty(value) || key.equals("sign") ||
                    key.equals("__POSTDATA__") ||
                    key.equals("__PAYINGID__") ||
                    key.equals("__REFUNDID__") ||
                    key.equals("__LOGGERID__")) {
                continue;
            }
            result.put(key, value);
        }

        return result;
    }

    /**
     * ���ݷ��ؽ���ж�AccessToken�Ƿ����
     */
    protected boolean isAccessTokenExpired(Map<String, String> params) {
        String error = String.valueOf(params.get("errcode"));
        if (StringUtils.isEmpty(error)) {
            return false;
        }
        return error.equals("42001") || error.equals("40001") ? true : false;
    }
    
    /**
     * ͨ��PayingEntity������ύ��֧��ƽ̨�Ĳ���MAP
     *
     * �˴����ص���Ԥ֧���������ɺ�����
     *
     * @return �ύ��֧��ƽ̨�Ĳ���MAP
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> buildPayingRequestParamsMap(PayingEntity entity, 
            long logId, String clientIP) {
        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);

        String accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("weixin"));
        if (StringUtils.isEmpty(accessToken)) {
            accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("weixin"), "");
        }

        Map<String, String> prepayParamsMap = buildPrePayRequestParamsMap(
                entity, logId, clientIP);

        Map<String, String> result = commitPrePayRequest(JsonUtil.encode(
                prepayParamsMap), accessToken);

        if (isAccessTokenExpired(result)) {
            accessToken = simpleAccessTokenHolder.getAccessToken(oppAuth.get("weixin"));
            result = commitPrePayRequest(JsonUtil.encode(prepayParamsMap), accessToken);
        }

        String prepayId = result.get("prepayid");

        if (StringUtils.isEmpty(prepayId))
            throw new RuntimeException("Create prepayId failed: " + result);

        Map<String, String> payingParamsMap = new HashMap<String, String>();
        payingParamsMap.put("package", "Sign=WXPay");
        payingParamsMap.put("appkey", oppAuth.get("weixin").get("appkey"));
        payingParamsMap.put("timestamp", prepayParamsMap.get("timestamp"));
        payingParamsMap.put("partnerid", oppAuth.get("weixin").get("partner"));
        payingParamsMap.put("appid", oppAuth.get("weixin").get("app_id"));
        payingParamsMap.put("prepayid", prepayId);
        payingParamsMap.put("noncestr", prepayParamsMap.get("noncestr"));
        payingParamsMap.put("sign", CodeUtil.shaHex(convertRequestParamMap(payingParamsMap), "gbk"));
        payingParamsMap.remove("appkey");

        return payingParamsMap;
    }
    
    /**
     * ����Ԥ֧������ʱ����Ĳ���MAP
     *
     * @return Ԥ֧����������MAP
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String> buildPrePayRequestParamsMap(PayingEntity entity, 
            long logId, String clientIP) {
        SortedMap<String, String> paramsMap = new TreeMap<String, String>();
        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);

        Map<String, String> pack1 = buildPayingPackageParamsMap(entity, logId, clientIP);

        String sign = CodeUtil.md5Hex(convertRequestParamMap(pack1) + "&key=" +
                oppAuth.get("weixin").get("partkey"), "gbk").toUpperCase();

        Map<String, String> pack2 = new HashMap<String, String>();
        try {
            List<String> keys = new ArrayList<String>(pack1.keySet());
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                pack2.put(key, URLEncoder.encode(pack1.get(key), "GBK").replace("+", "%20"));
            }
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException("", error);
        }

        paramsMap.put("appid", oppAuth.get("weixin").get("app_id"));
        paramsMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        paramsMap.put("noncestr", CodeUtil.md5Hex(entity.getId() + String.valueOf(logId), "GBK"));
        paramsMap.put("appkey", oppAuth.get("weixin").get("appkey"));
        paramsMap.put("traceid", entity.getId());
        paramsMap.put("package", convertRequestParamMap(pack2) + "&sign=" + sign);
        paramsMap.put("app_signature", CodeUtil.shaHex(convertRequestParamMap(
               paramsMap), "GBK"));
        paramsMap.put("sign_method", "sha1");
        paramsMap.remove("appkey");

        return paramsMap;
    }
    
    /**
     * ����΢��Ԥ֧����������Ҫ��PACKAGE��ص�����MAP
     *
     * @return PACKAGE��ص�����MAP
     */
    @SuppressWarnings("unchecked")
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
        params.put("partner", auth.get("weixin").get("partner"));
        
        Map<String, String> item = JsonUtil.decode(
                entity.getAppOrderItem(), Map.class);
        params.put("body", item.get("name"));

        params.put("notify_url", buildPayingNotifyURL(entity.getId(), logId));
        return params;
    }
    
    /**
     * �ύԤ֧�����󲢷���Ԥ֧������ID�������Ϣ
     *
     * @return Ԥ֧������ID�������Ϣ
     */
    @SuppressWarnings("unchecked")
    protected Map<String, String> commitPrePayRequest(String param, String accessToken) {
        String url = WEIXIN_PREPAY_TARGET_URI + "?access_token=" + accessToken;
        return JsonUtil.decode(sendHttpRequest(url, param, "GBK"), Map.class);
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
        checkPayingNotifyParamsMap(params, oppAuth.get("weixin"));
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
        paramsMap.put("partner", oppAuth.get("weixin").get("partner"));
        paramsMap.put("transaction_id", paying.getOppGatewayCode());
        paramsMap.put("op_user_passwd", oppAuth.get("weixin").get("passwd"));
        paramsMap.put("out_refund_no", CodeUtil.md5Hex(
                paying.getId() + String.valueOf(refund.getId()), "GBK"));
        paramsMap.put("op_user_id", oppAuth.get("weixin").get("partner"));
        paramsMap.put("sign_type", "MD5");
        paramsMap.put("service_version", "1.1");

        String sign = CodeUtil.md5Hex(convertRequestParamMap(paramsMap) +
                "&key=" + oppAuth.get("weixin").get("partkey"), "gbk").toUpperCase();

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
        checkRefundNotifyParamsMap(params, oppAuth.get("weixin"));
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

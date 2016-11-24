package cloud.dispatcher.gateway.pay.cashier.domain.opp.alipay;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang.StringUtils;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.AbstractAssistant;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class AlipayAssistant extends AbstractAssistant {

    private static String ALIPAY_GATEWAY_TARGET_URI = "https://mapi.alipay.com/gateway.do";
    
    @Getter @Setter protected Map<String, String> expiredTimeMap;

    protected AlipayAssistant() {}

    /**
     * ���˲���Map�еĿ�ֵ��ǩ����صĲ���(sign, sign_type)
     */
    protected Map<String, String> requestParamMapFilter(Map<String, String> params) {
        Map<String, String> result = new HashMap<String, String>();
        if (params == null || params.size() == 0)
            return result;

        for (String key : params.keySet()) {
            String value = params.get(key);
            if (StringUtils.isEmpty(value) || key.equalsIgnoreCase("sign")
                    || key.equalsIgnoreCase("sign_type")) {
                continue;
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * ��֧������MAP���ݲ�֧ͬ��ƽ̨�Ĺ���ƴ�ӳ��ַ���, ��������֧������ǩ��
     *
     * @return ֧�������ַ���
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
        return converted.substring(0, converted.length() - 1);
    }

    protected String convertRequestParamMapWithQuotes(Map<String, String> params) {
        Map<String, String> request = requestParamMapFilter(params);
        List<String> keys = new ArrayList<String>(request.keySet());
        Collections.sort(keys);

        String converted = "";

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            converted = converted + key + "=\"" + request.get(key) + "\"&";
        }
        return converted.substring(0, converted.length() - 1);
    }

    /**
     * ͨ��PayingEntity������ύ��֧��ƽ̨�Ĳ���MAP
     *
     * @return �ύ��֧��ƽ̨�Ĳ���MAP
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String> buildPayingRequestParamsMap(PayingEntity entity, 
            long logId, String clientIP) {
        Map<String, String> paramsMap = new HashMap<String, String>();

        Date expiredDate = entity.getAppOrderExpire();
        Date currentDate = new Date();
        if (expiredDate.getTime() - currentDate.getTime() <= 0) {
            return Collections.EMPTY_MAP;
        }

        Map<String, Map<String, String>> oppAuth = JsonUtil.decode(
                entity.getOppGatewayAuth(), Map.class);

        paramsMap.put("service", "create_direct_pay_by_user");
        paramsMap.put("payment_type", "1");
        paramsMap.put("_input_charset", "GBK");

        double price = new BigDecimal(entity.getAppOrderFee()).divide(new BigDecimal(100),
                2, BigDecimal.ROUND_HALF_UP).doubleValue();
        paramsMap.put("total_fee", String.valueOf(price));
        
        String expiredTime = expiredTimeMap.get(entity.getAppOrderSource());
        if (expiredTime != null) {
            paramsMap.put("it_b_pay", expiredTime);
        }

        paramsMap.put("notify_url", buildPayingNotifyURL(entity.getId(), logId));
        paramsMap.put("return_url", buildPayingReturnURL(entity.getId(), logId));
        paramsMap.put("out_trade_no", String.valueOf(entity.getId()).substring(3));
        paramsMap.put("partner", oppAuth.get("alipay").get("partner"));
        paramsMap.put("seller_email", oppAuth.get("alipay").get("seller_email"));

        Map<String, String> item = JsonUtil.decode(entity.getAppOrderItem(), Map.class);
        String itemDesc = item.get("desc");
        paramsMap.put("body", !StringUtils.isEmpty(itemDesc) ? itemDesc : "");
        String itemName = item.get("name");
        paramsMap.put("subject", !StringUtils.isEmpty(itemName) ? itemName : "");

        paramsMap.put("sign_type", "MD5");
        paramsMap.put("sign", sign(convertRequestParamMap(paramsMap),
                oppAuth.get("alipay").get("key"), "MD5", "gbk"));

        return paramsMap;
    }
    
    /**
     * ����֧��ƽ̨�ڻص�ʱ�ύ��֧��������ص�����MAP
     *
     * @return ֧��ƽ̨���صĲ�����ˮ��
     */
    @Override
    public Map<String, String> parsePayingNotifyParamsMap(Map<String, String> params, 
            Map<String, Map<String, String>> oppAuth) {
        checkNotifyParamsMap(params, oppAuth.get("alipay"));
        try {
            Map<String, String> result = new HashMap<String, String>();
            String status = new String(params.get("trade_status").getBytes("ISO-8859-1"), "GBK");
            if (status.equals("TRADE_FINISHED") || status.equals("TRADE_SUCCESS")) {
                String tradeNo = new String(params.get("trade_no").getBytes("ISO-8859-1"),"GBK");
                String account = new String(params.get("buyer_id").getBytes("ISO-8859-1"),"GBK") +
                        ":" + new String(params.get("buyer_email").getBytes("ISO-8859-1"),"GBK");
                result.put("tradeNo", tradeNo);
                result.put("account", account);
            }
            return result;
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }
    }
    
    /**
     * ����֧��ƽ̨�ڻ���ʱ�ύ��֧��������ص�����MAP
     *
     * @return ֧��ƽ̨���صĲ�����ˮ��
     */
    @Override
    public Map<String, String> parsePayingReturnParamsMap(Map<String, String> params, 
            Map<String, Map<String, String>> oppAuth) {
        checkReturnParamsMap(params, oppAuth.get("alipay"));
        try {
            Map<String, String> result = new HashMap<String, String>();
            String status = new String(params.get("trade_status").getBytes("ISO-8859-1"), "GBK");
            if (status.equals("TRADE_FINISHED") || status.equals("TRADE_SUCCESS")) {
                String tradeNo = new String(params.get("trade_no").getBytes("ISO-8859-1"),"GBK");
                String account = new String(params.get("buyer_id").getBytes("ISO-8859-1"),"GBK") +
                        ":" + new String(params.get("buyer_email").getBytes("ISO-8859-1"),"GBK");
                result.put("tradeNo", tradeNo);
                result.put("account", account);
            }
            return result;
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
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

        paramsMap.put("service", "refund_fastpay_by_platform_nopwd");
        paramsMap.put("batch_num", "1");
        paramsMap.put("_input_charset", "GBK");

        SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        paramsMap.put("refund_date", simpleDateFormat1.format(refund.getCreatedAt()));

        String batchNo = paying.getId().substring(0, 3) + String.valueOf(refund.getId());
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyyMMdd");
        paramsMap.put("batch_no", simpleDateFormat2.format(refund.getCreatedAt()) + batchNo);

        double price = new BigDecimal(refund.getAppOrderFee()).divide(new BigDecimal(100),
                2, BigDecimal.ROUND_HALF_UP).doubleValue();
        paramsMap.put("detail_data", paying.getOppGatewayCode() + "^" + String.valueOf(
                price) + "^Э���˿�");

        paramsMap.put("partner", oppAuth.get("alipay").get("partner"));
        paramsMap.put("seller_email", oppAuth.get("alipay").get(
                "seller_email"));

        paramsMap.put("notify_url", buildRefundNotifyURL(paying.getId(),
                refund.getId(), logId));

        paramsMap.put("sign_type", "MD5");
        paramsMap.put("sign", sign(convertRequestParamMap(paramsMap),
                oppAuth.get("alipay").get("key"), "MD5", "gbk"));

        return paramsMap;
    }
    
    /**
     * ִ���ύ�˿�����Ĳ���, ���������Ƿ��ύ�ɹ�
     *
     * @param params �˿����MAP
     */
    @Override
    public boolean commitRefundRequest(Map<String, String> params) {
        String result = sendHttpRequest(ALIPAY_GATEWAY_TARGET_URI, params, "GBK");
        return result.indexOf(
                "<is_success>T</is_success>") != -1 ? true : false;
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
        checkNotifyParamsMap(params, oppAuth.get("alipay"));
        return (params.get("result_details").matches("^\\d+\\^\\d+.\\d+\\^SUCCESS$"))
                ? params : Collections.EMPTY_MAP;
    }
    
    /**
     * ����notifyId�Լ�partner����ͨ��HTTP������֧����
     * ��ȡATN���
     *
     * @return ֧�������ص�ATN���
     */
    protected boolean checkNotifyOnRemote(String notifyId, String partner) {
        StringBuffer buffer = new StringBuffer(ALIPAY_GATEWAY_TARGET_URI);
        buffer.append("?service=notify_verify&partner=");
        buffer.append(partner + "&notify_id=" + notifyId);
        return sendHttpRequest(buffer.toString()).equals("true");
    }
    
    /**
     * ��֤���յ���֧��֪ͨ����Map�е�Ԫ���Ƿ�δ���۸�
     *
     * @return MAP�е�Ԫ���Ƿ�δ���۸�
     */
    protected void checkNotifyParamsMap(Map<String, String> params, 
            Map<String, String> oppAuth) {
        String sign = StringUtils.isEmpty(params.get("sign")) ? "" : params.get("sign");
        String converted = convertRequestParamMap(params);
        if (!sign(converted, oppAuth.get("key"), "MD5", "gbk").equals(sign)) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
        
        if (StringUtils.isNotEmpty(params.get("notify_id")) && !checkNotifyOnRemote(
                params.get("notify_id"), oppAuth.get("partner"))) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
    }
    
    /**
     * ��֤���յ���֧����������Map�е�Ԫ���Ƿ�δ���۸�
     *
     * @return MAP�е�Ԫ���Ƿ�δ���۸�
     */
    protected void checkReturnParamsMap(Map<String, String> params, 
            Map<String, String> oppAuth) {
        String sign = StringUtils.isEmpty(params.get("sign")) ? "" : params.get("sign");
        String converted = convertRequestParamMap(params);
        if (!sign(converted, oppAuth.get("key"), "MD5", "gbk").equals(sign)) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
        
        if (StringUtils.isNotEmpty(params.get("notify_id")) && !checkNotifyOnRemote(
                params.get("notify_id"), oppAuth.get("partner"))) {
            throw new IllegalArgumentException(
                    "Illegal notify params, params = [" + params + "]");
        }
    }
    
    
}

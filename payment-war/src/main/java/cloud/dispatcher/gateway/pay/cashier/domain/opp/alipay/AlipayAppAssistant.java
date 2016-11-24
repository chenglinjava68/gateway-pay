package cloud.dispatcher.gateway.pay.cashier.domain.opp.alipay;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;
import cloud.dispatcher.gateway.pay.cashier.utils.RSAUtil;
import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AlipayAppAssistant extends AlipayAssistant {

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

        paramsMap.put("service", "mobile.securitypay.pay");
        paramsMap.put("payment_type", "1");
        paramsMap.put("_input_charset", "utf-8");

        double price = new BigDecimal(entity.getAppOrderFee()).divide(new BigDecimal(100),
                2, BigDecimal.ROUND_HALF_UP).doubleValue();
        paramsMap.put("total_fee", String.valueOf(price));

        String expiredTime = expiredTimeMap.get(entity.getAppOrderSource());
        if (expiredTime != null) {
            paramsMap.put("it_b_pay", expiredTime);
        }

        paramsMap.put("notify_url", buildPayingNotifyURL(entity.getId(), logId));
        paramsMap.put("out_trade_no", String.valueOf(entity.getId()).substring(3));
        paramsMap.put("partner", oppAuth.get("alipay").get("partner"));
        paramsMap.put("seller_id", oppAuth.get("alipay").get("seller_email"));

        Map<String, String> item = JsonUtil.decode(entity.getAppOrderItem(), Map.class);
        String itemName = item.get("name");
        paramsMap.put("subject", !StringUtils.isEmpty(itemName) ? itemName : "������Ʒ����");
        String itemDesc = item.get("desc");
        paramsMap.put("body", !StringUtils.isEmpty(itemDesc) ? itemDesc : "������Ʒ����");

        String converted = convertRequestParamMapWithQuotes(paramsMap);

        try {
            String encoded = URLEncoder.encode(RSAUtil.encrypt(converted,
                    oppAuth.get("alipay").get("rsa_pri_key"), "UTF-8"), "UTF-8");
            paramsMap.put("sign", encoded);
        } catch (UnsupportedEncodingException error) {
            throw new RuntimeException(error);
        }

        paramsMap.put("sign_type", "RSA");

        paramsMap.put("converted", converted + "&sign_type=\"RSA\"&sign=\"" +
                paramsMap.get("sign") + "\"");

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
     * ��֤���յ���֧��֪ͨ����Map�е�Ԫ���Ƿ�δ���۸�
     *
     * @return MAP�е�Ԫ���Ƿ�δ���۸�
     */
    protected void checkNotifyParamsMap(Map<String, String> params,
            Map<String, String> oppAuth) {
        String sign = StringUtils.isEmpty(params.get("sign")) ? "" : params.get("sign");
        String converted = convertRequestParamMap(params);
        if (!RSAUtil.verify(converted, sign, oppAuth.get("rsa_pub_key"), "utf-8")) {
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

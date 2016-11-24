package cloud.dispatcher.gateway.pay.cashier.domain.opp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity.RefundEntity;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.CodeUtil;

public abstract class AbstractAssistant implements OppAssistant {

    @Getter @Setter protected HttpClientFactory httpClientFactory;

    @Getter @Setter protected String oppPayingNotifyURL;

    @Getter @Setter protected String oppPayingReturnURL;

    @Getter @Setter protected String oppRefundNotifyURL;

    protected String sendHttpRequest(String requestURI) {
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpGet request = new HttpGet(requestURI);
        try {
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    protected String sendHttpRequest(String requestURI,
            String params, String charset) {
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpPost request = new HttpPost(requestURI);
        try {
            request.setEntity(new StringEntity(params, charset));
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    protected String sendHttpRequest(String requestURI,
            Map<String, String> params, String charset) {
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpPost request = new HttpPost(requestURI);
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        for (String key : params.keySet()) {
            String val = params.get(key);
            pairs.add(new BasicNameValuePair(key, val));
        }

        try {
            request.setEntity(new UrlEncodedFormEntity(pairs, charset));
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    protected String sign(String content, String key, String signType,
            String charset) {
        if (!signType.equalsIgnoreCase("MD5") &&
            !signType.equalsIgnoreCase("SHA")) {
            throw new IllegalArgumentException(
                    "Wrong parameter: signType = [" + signType + "]");
        }
        if (signType.equalsIgnoreCase("MD5")) {
            return CodeUtil.md5Hex(content + key, charset);
        } else {
            return CodeUtil.shaHex(content + key, charset);
        }
    }

    protected String buildPayingNotifyURL(String id, long logId) {
        StringBuffer buffer = new StringBuffer(oppPayingNotifyURL);
        buffer.append("/").append(id).append("/").append(logId);
        return buffer.toString();
    }

    protected String buildPayingReturnURL(String id, long logId) {
        StringBuffer buffer = new StringBuffer(oppPayingReturnURL);
        buffer.append("/").append(id).append("/").append(logId);
        return buffer.toString();
    }

    protected String buildRefundNotifyURL(String payingId, long refundId,
            long logId) {
        StringBuffer buffer = new StringBuffer(oppRefundNotifyURL);
        buffer.append("/").append(payingId).append("/");
        buffer.append(refundId).append("/").append(logId);
        return buffer.toString();
    }

    @Override
    public abstract Map<String, String> buildPayingRequestParamsMap(
           PayingEntity entity, long logId, String clientIP);

    @Override
    public abstract Map<String, String> parsePayingNotifyParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    @Override
    public abstract Map<String, String> parsePayingReturnParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    @Override
    public abstract Map<String, String> buildRefundRequestParamsMap(
           PayingEntity paying, RefundEntity refund, long logId);

    @Override
    public abstract Map<String, String> parseRefundNotifyParamsMap(
           Map<String, String> params,
           Map<String, Map<String, String>> oppAuth);

    @Override
    public abstract boolean commitRefundRequest(Map<String, String> params);
}

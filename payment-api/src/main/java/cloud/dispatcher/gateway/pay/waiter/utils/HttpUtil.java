package cloud.dispatcher.gateway.pay.waiter.utils;

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

import cloud.dispatcher.gateway.pay.waiter.http.HttpClientFactory;

public class HttpUtil {

    @Getter @Setter private HttpClientFactory httpClientFactory;

    /**
     * ģ���ύHTTPՈ��, �����������ַ����Ӧ����
     *
     * @return �����ַ����Ӧ����
     */
    private String sendHttpRequest(String requestURI) {
        HttpClient client = httpClientFactory.getSimpleHttpClient();
        HttpGet request = new HttpGet(requestURI);
        try {
            HttpResponse response = client.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    /**
     * ģ���ύHTTPՈ��, �����������ַ����Ӧ����
     *
     * @return �����ַ����Ӧ����
     */
    private String sendHttpRequest(String requestURI,
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

    /**
     * ģ���ύHTTPՈ��, �����������ַ����Ӧ����
     *
     * @return �����ַ����Ӧ����
     */
    private String sendHttpRequest(String requestURI,
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
}

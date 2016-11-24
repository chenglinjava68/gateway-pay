package cloud.dispatcher.gateway.pay.cashier.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import net.rubyeye.xmemcached.XMemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;

public class ViewUtil {

    private static final String OPENAPI_URI_GET_HEADER_WEB = "";

    private static final String OPENAPI_URI_GET_FOOTER_WEB = "";

    @Getter @Setter private XMemcachedClient xMemcachedClient;

    @Getter @Setter private String domain;

    @Getter @Setter private String appId;

    @Getter @Setter private String token;

    @Getter @Setter private String cache;

    private static final String HEADER_WEB_MEMCACHE_KEY_PREFIX = "HTML_WEB_HEADER_";

    private static final String FOOTER_WEB_MEMCACHE_KEY_PREFIX = "HTML_WEB_FOOTER_";

    @Getter @Setter private HttpClientFactory httpClientFactory;

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewUtil.class);

    public static String getJsonResponse(boolean success, String message, String redirect) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("redirect", redirect);
        paramsMap.put("success", success);
        paramsMap.put("message", message);
        return JsonUtil.encode(paramsMap);
    }

    public static String getJsonResponse(boolean success) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("success", success);
        return JsonUtil.encode(paramsMap);
    }

    public static String getJsonResponse(boolean success, String message) {
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        paramsMap.put("success", success);
        paramsMap.put("message", message);
        return JsonUtil.encode(paramsMap);
    }

    @SuppressWarnings("unchecked")
    public String getWebHeaderHTML() {
        String html = "";
        try {
            html = xMemcachedClient.get(HEADER_WEB_MEMCACHE_KEY_PREFIX + cache);
            if (!StringUtils.isEmpty(html))
                return html;
            HttpUriRequest request = new HttpGet(OPENAPI_URI_GET_HEADER_WEB +
                    "client_id=" + appId + "&client_secret=" + token);
            HttpResponse response = httpClientFactory.getSimpleHttpClient()
                    .execute(request);
            String result = EntityUtils.toString(response.getEntity());
            if (!StringUtils.isEmpty(result)) {
                Map<String, String> content = JsonUtil.decode(result, Map.class);
                if (content.get("code") != null && content.get("code").equals("1")) {
                    html = content.get("html");
                    xMemcachedClient.add(HEADER_WEB_MEMCACHE_KEY_PREFIX + cache,
                            12 * 3600, html);
                }
            }
            return html;
        } catch (Exception error) {
            LOGGER.error("", error);
            return html;
        }
    }

    @SuppressWarnings("unchecked")
    public String getWebFooterHTML() {
        String html = "";
        try {
            html = xMemcachedClient.get(FOOTER_WEB_MEMCACHE_KEY_PREFIX + cache);
            if (!StringUtils.isEmpty(html))
                return html;
            HttpUriRequest request = new HttpGet(OPENAPI_URI_GET_FOOTER_WEB +
                    "client_id=" + appId + "&client_secret=" + token);
            HttpResponse response = httpClientFactory.getSimpleHttpClient()
                    .execute(request);
            String result = EntityUtils.toString(response.getEntity());
            if (!StringUtils.isEmpty(result)) {
                Map<String, String> content = JsonUtil.decode(result, Map.class);
                if (content.get("code") != null && content.get("code").equals("1")) {
                    html = content.get("content");
                    xMemcachedClient.add(FOOTER_WEB_MEMCACHE_KEY_PREFIX + cache,
                            12 * 3600, html);
                }
            }
            return html;
        } catch (Exception error) {
            LOGGER.error("", error);
            return html;
        }
    }
}

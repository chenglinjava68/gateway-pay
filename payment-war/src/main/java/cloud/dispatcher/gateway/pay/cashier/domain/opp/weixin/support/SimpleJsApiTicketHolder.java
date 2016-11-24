package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin.support;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin.JsApiTicketHolder;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import net.rubyeye.xmemcached.XMemcachedClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Map;

public class SimpleJsApiTicketHolder implements JsApiTicketHolder {

    private static final String TICKET_MEMCACHE_KEY_PREFIX = "PAYMENT_WEIXIN_JSAPITICKET_";

    private static final String TICKET_BUILDER_REQUEST_URI = "https://api.weixin.qq.com" +
            "/cgi-bin/ticket/getticket?type=jsapi&access_token=";

    @Getter @Setter protected HttpClientFactory httpClientFactory;

    @Getter @Setter private XMemcachedClient xMemcachedClient;

    @Override
    public String getJsApiTicket(String accessToken, String jsApiTicket) {
        String jsApiTicketFromMemory = getJsApiTicketFromMemory(accessToken);
        if (StringUtils.isNotEmpty(jsApiTicketFromMemory) &&
                !jsApiTicketFromMemory.equals(jsApiTicket)) {
            return jsApiTicketFromMemory;
        }
        synchronized (this) {
            jsApiTicketFromMemory = getJsApiTicketFromRemote(accessToken);
            if (StringUtils.isNotEmpty(jsApiTicketFromMemory) &&
                    !jsApiTicketFromMemory.equals(accessToken)) {
                return jsApiTicketFromMemory;
            }
            String jsApiTicketFromRemote = "";
            try {
                jsApiTicketFromRemote = getJsApiTicketFromRemote(accessToken);
            } catch (Exception error) {
                throw new RuntimeException("", error);
            }
            setJsApiTicketInMemory(accessToken, jsApiTicketFromRemote);
            return jsApiTicketFromRemote;
        }
    }

    @Override
    public String getJsApiTicket(String accessToken) {
        return getJsApiTicketFromMemory(accessToken);
    }

    @SuppressWarnings("unchecked")
    private String getJsApiTicketFromRemote(String accessToken) {
        DefaultHttpClient client = httpClientFactory.getSimpleHttpClient();
        String requestURI = TICKET_BUILDER_REQUEST_URI + accessToken;

        try {
            String response = EntityUtils.toString(client.execute(
                    new HttpGet(requestURI)).getEntity());
            Map<String, String> result = JsonUtil.decode(response, Map.class);
            if (!StringUtils.isEmpty(result.get("ticket"))) {
                return result.get("ticket");
            } else {
                throw new RuntimeException("[SimpleJsApiTicketHolder] " +
                        "Generate failed, result = [" + result + "]");
            }
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    private String getJsApiTicketFromMemory(String accessToken) {
        try {
            return xMemcachedClient.get(TICKET_MEMCACHE_KEY_PREFIX + accessToken.hashCode());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private void setJsApiTicketInMemory(String accessToken, String jsApiTicket) {
        try {
            xMemcachedClient.add(TICKET_MEMCACHE_KEY_PREFIX + accessToken.hashCode(), 7000, jsApiTicket);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}

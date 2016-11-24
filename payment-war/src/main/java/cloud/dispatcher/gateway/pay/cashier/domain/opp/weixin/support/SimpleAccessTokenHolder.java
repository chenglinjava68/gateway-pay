package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin.support;

import java.io.IOException;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import net.rubyeye.xmemcached.XMemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin.AccessTokenHolder;
import cloud.dispatcher.gateway.pay.cashier.http.HttpClientFactory;
import cloud.dispatcher.gateway.pay.cashier.utils.JsonUtil;

public class SimpleAccessTokenHolder implements AccessTokenHolder {

    private static final String TOKEN_MEMCACHE_KEY_PREFIX = "PAYMENT_WEIXIN_ACCESSTOKEN_";

    private static final String TOKEN_BUILDER_REQUEST_URI = "https://api.weixin.qq.com" +
            "/cgi-bin/token?grant_type=client_credential&";

    @Getter @Setter protected HttpClientFactory httpClientFactory;

    @Getter @Setter private XMemcachedClient xMemcachedClient;

    /**
     * ��ȡ֧���˺Ŷ�Ӧ��AccessToken
     *
     * Ĭ�ϴӻ����л�ȡ, ����ӻ����л�ȡ����AccessToken������е�һ
     * ��, ��ǿ�ƴ�Զ�˷��������л�ȡ��ˢ�»���
     *
     * @param accessToken ��һ�λ�ȡ����AccessToken
     *
     * @return AccessToken
     */
    @Override
    public String getAccessToken(Map<String, String> params, String accessToken) {
        String partner = params.get("partner");
        String accessTokenFromMemory = getAccessTokenFromMemory(partner);
        if (StringUtils.isNotEmpty(accessTokenFromMemory) &&
                !accessTokenFromMemory.equals(accessToken)) {
            return accessTokenFromMemory;
        }
        synchronized (this) {
            accessTokenFromMemory = getAccessTokenFromMemory(partner);
            if (StringUtils.isNotEmpty(accessTokenFromMemory) &&
                    !accessTokenFromMemory.equals(accessToken)) {
                return accessTokenFromMemory;
            }
            String accessTokenFromRemote = getAccessTokenFromRemote(params);
            setAccessTokenInMemory(partner, accessTokenFromRemote);
            return accessTokenFromRemote;
        }
    }

    /**
     * ��ȡ֧���˺Ŷ�Ӧ��AccessToken
     *
     * Ĭ�ϴӻ����л�ȡ, ���������û���˺Ŷ�Ӧ��AccessToken���Զ��
     * �������Ͻ��л�ȡ
     *
     * @param params ��֤��Ϣ
     *
     * @return AccessToken
     */
    @Override
    public String getAccessToken(Map<String, String> params) {
        String partner = params.get("partner");
        return getAccessTokenFromMemory(partner);
    }

    private String getAccessTokenFromMemory(String partner) {
        try {
            return xMemcachedClient.get(TOKEN_MEMCACHE_KEY_PREFIX + partner);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    @SuppressWarnings("unchecked")
    private String getAccessTokenFromRemote(Map<String, String> params) {
        String requestURI = TOKEN_BUILDER_REQUEST_URI + "&secret=" + params.get(
                "secret") + "&appid=" + params.get("app_id");

        DefaultHttpClient client = httpClientFactory.getSimpleHttpClient();

        try {
            String response = EntityUtils.toString(client.execute(
                    new HttpGet(requestURI)).getEntity());
            Map<String, String> result = JsonUtil.decode(response, Map.class);
            if (!StringUtils.isEmpty(result.get("access_token"))) {
                return result.get("access_token");
            } else {
                throw new RuntimeException("[SimpleAccessTokenHolder] " +
                        "Generate failed, result = [" + result + "]");
            }
        } catch (IOException error) {
            throw new RuntimeException(error);
        }
    }

    private void setAccessTokenInMemory(String partner, String token) {
        try {
            xMemcachedClient.add(TOKEN_MEMCACHE_KEY_PREFIX + partner, 7000, token);
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}

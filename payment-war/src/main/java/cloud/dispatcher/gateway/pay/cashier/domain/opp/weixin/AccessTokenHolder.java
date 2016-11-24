package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin;

import java.util.Map;

public interface AccessTokenHolder {

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
    public String getAccessToken(Map<String, String> params, String accessToken);

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
    public String getAccessToken(Map<String, String> params);
}

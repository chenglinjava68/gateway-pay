package cloud.dispatcher.gateway.pay.cashier.domain.opp.weixin;

public interface JsApiTicketHolder {

    /**
     * ��ȡ֧���˺Ŷ�Ӧ��JsApiTicket
     *
     * Ĭ�ϴӻ����л�ȡ, ����ӻ����л�ȡ����JsApiTicket������е�һ
     * ��, ��ǿ�ƴ�Զ�˷��������л�ȡ��ˢ�»���
     *
     * @return JsApiTicket
     */
    public String getJsApiTicket(String accessToken, String jsApiTicket);

    /**
     * ��ȡ֧���˺Ŷ�Ӧ��JsApiTicket
     *
     * Ĭ�ϴӻ����л�ȡ, ����ӻ����л�ȡ����JsApiTicket������е�һ
     * ��, ��ǿ�ƴ�Զ�˷��������л�ȡ��ˢ�»���
     *
     * @return JsApiTicket
     */
    public String getJsApiTicket(String accessToken);
}

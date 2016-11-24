package cloud.dispatcher.gateway.pay.cashier.domain.opp.enums;

public enum OppTypeEnum {

    ALIPAY(1), WEIXIN(2), WXMPAY(3);

    private OppTypeEnum(int value) {this.value = value;}

    private int value;

    public int value() {
        return value;
    }

    public static OppTypeEnum valueOf(int value) {
        switch (value) {
            case 1:
                return ALIPAY;
            case 2:
                return WEIXIN;
            case 3:
                return WXMPAY;
        }
        throw new IllegalArgumentException(
                "Wrong parameter: value = [" + value + "]");
    }
}

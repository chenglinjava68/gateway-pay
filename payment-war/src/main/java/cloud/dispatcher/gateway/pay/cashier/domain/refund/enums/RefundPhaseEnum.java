package cloud.dispatcher.gateway.pay.cashier.domain.refund.enums;

public enum RefundPhaseEnum {

    NOTEXIST(0), CREATED(1), OPERATING(2), FAILED(3), SUCCEED(4);

    private RefundPhaseEnum(int value) {this.value = value;}

    private int value;

    public int value() {
        return value;
    }

    public static RefundPhaseEnum valueOf(int value) {
        switch (value) {
            case 0:
                return NOTEXIST;
            case 1:
                return CREATED;
            case 2:
                return OPERATING;
            case 3:
                return FAILED;
            case 4:
                return SUCCEED;
        }
        throw new IllegalArgumentException(
                "Wrong parameter: value = [" + value + "]");
    }
}

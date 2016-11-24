package cloud.dispatcher.gateway.pay.cashier.domain.paying.enums;

public enum PayingPhaseEnum {

    NOTEXIST(0), CREATED(1), OPERATING(2), FAILED(3), SUCCEED(4), FINISHED(5);

    private PayingPhaseEnum(int value) {this.value = value;}

    private int value;

    public int value() {
        return value;
    }

    public static PayingPhaseEnum valueOf(int value) {
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
            case 5:
                return FINISHED;
        }
        throw new IllegalArgumentException(
                "Wrong parameter: value = [" + value + "]");
    }
}

package cloud.dispatcher.gateway.pay.cashier.domain.logger.enums;

public enum LoggerResultEnum {

    DEFAULT(0), FAILED(1), SUCCEED(2);

    private LoggerResultEnum(int value) {this.value = value;}

    private int value;

    public int value() {
        return value;
    }

    public static LoggerResultEnum valueOf(int value) {
        switch (value) {
            case 0:
                return DEFAULT;
            case 1:
                return FAILED;
            case 2:
                return SUCCEED;
        }
        throw new IllegalArgumentException(
                "Wrong parameter: value = [" + value + "]");
    }
}

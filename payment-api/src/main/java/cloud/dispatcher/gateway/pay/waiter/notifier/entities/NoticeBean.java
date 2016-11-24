package cloud.dispatcher.gateway.pay.waiter.notifier.entities;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class NoticeBean implements Serializable {

    private static final long serialVersionUID = 7109188240826146292L;

    @Getter @Setter private String requestURI;

    @Getter @Setter private boolean result;

    @Getter @Setter private String oppType;

    @Getter @Setter private String oppCode;

    @Getter @Setter private long notifyTime;

    @Getter @Setter private String payingId;

    @Getter @Setter private String refundId;

    @Getter @Setter private String loggerId;
}

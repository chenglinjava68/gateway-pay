package cloud.dispatcher.gateway.pay.waiter.refund.entities;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class RefundParamBean implements Serializable {

    private static final long serialVersionUID = 2109187240826146293L;

    @Setter @Getter private String payindId;

    @Setter @Getter private int fee;

    @Setter @Getter private String reason;

    @Setter @Getter private String notify;
}

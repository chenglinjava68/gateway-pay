package cloud.dispatcher.gateway.pay.cashier.domain.refund.bean.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class RefundEntity implements Serializable {

    private static final long serialVersionUID = 7109187240826146298L;

    @Getter @Setter private String tableName;

    @Getter @Setter private int status;

    @Getter @Setter private long id;

    @Getter @Setter private String usrAccount;

    @Getter @Setter private String usrPayment;

    @Getter @Setter private long usrUid;

    @Getter @Setter private String payingId;

    @Getter @Setter private Date createdAt;

    @Getter @Setter private Date updatedAt;

    @Getter @Setter private String oppGatewayFeedback;

    @Getter @Setter private int appOrderFee;

    @Getter @Setter private String appOrderSource;

    @Getter @Setter private String appOrderNumber;

    @Getter @Setter private String appOrderNotify;

    @Getter @Setter private int oppGatewayType;

    @Getter @Setter private String oppGatewayCode;
}

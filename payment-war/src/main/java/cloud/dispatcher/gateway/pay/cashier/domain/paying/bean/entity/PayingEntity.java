package cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class PayingEntity implements Serializable {

    private static final long serialVersionUID = 7109187240826146295L;

    @Getter @Setter private String tableName;

    @Getter @Setter private int status;

    @Getter @Setter private String id;

    @Getter @Setter private String usrPayment;

    @Getter @Setter private String usrAccount;

    @Getter @Setter private long usrUid;

    @Getter @Setter private Date createdAt;

    @Getter @Setter private Date updatedAt;

    @Getter @Setter private String oppGatewayFeedback;

    @Getter @Setter private int oppGatewayType;

    @Getter @Setter private String oppGatewayAuth;

    @Getter @Setter private String oppGatewayCode;

    @Getter @Setter private int appOrderFee;

    @Getter @Setter private String appOrderItem;

    @Getter @Setter private Date appOrderExpire;

    @Getter @Setter private String appOrderSource;

    @Getter @Setter private String appOrderNumber;

    @Getter @Setter private String appOrderNotify;

    @Getter @Setter private String appOrderReturn;
}

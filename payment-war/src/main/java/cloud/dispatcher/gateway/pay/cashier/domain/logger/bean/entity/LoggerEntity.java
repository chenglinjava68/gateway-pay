package cloud.dispatcher.gateway.pay.cashier.domain.logger.bean.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class LoggerEntity implements Serializable {

    private static final long serialVersionUID = 7109187240826146292L;

    @Getter @Setter private String tableName;

    @Getter @Setter private int status;

    @Getter @Setter private long id;

    @Getter @Setter private String payingId;

    @Getter @Setter private long refundId;

    @Getter @Setter private Date createdAt;

    @Getter @Setter private Date updatedAt;

    @Getter @Setter private int oppFee;

    @Getter @Setter private String oppCode;

    @Getter @Setter private int oppType;

    @Getter @Setter private String oppCommit;

    @Getter @Setter private String oppReturn;
    
    @Getter @Setter private String oppNotify;
}

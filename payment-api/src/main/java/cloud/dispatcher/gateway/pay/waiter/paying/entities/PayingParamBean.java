package cloud.dispatcher.gateway.pay.waiter.paying.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class PayingParamBean implements Serializable {

    private static final long serialVersionUID = 2109187240826146292L;

    @Getter @Setter private long uid;

    @Getter @Setter private String username;

    @Getter @Setter private String orderNum;

    @Getter @Setter private String itemName;

    @Getter @Setter private String itemDesc;

    @Getter @Setter private String itemLink;

    @Getter @Setter private String notifyURL;

    @Getter @Setter private String returnURL;

    @Getter @Setter private int fee;

    @Getter @Setter private Date expire;

    @Getter @Setter private Map<String, String> alipayAccount;

    @Getter @Setter private Map<String, String> weixinAccount;

    @Getter @Setter private Map<String, String> wxmpayAccount;
}

package cloud.dispatcher.gateway.pay.cashier.web.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ViewHelp {

    @Getter @Setter private String scriptRootURI;

    @Getter @Setter private String cssRootURI;

    @Getter @Setter private String imgRootURI;

    @Getter @Setter private String domain;

    @Getter @Setter private String versionNumber;
}

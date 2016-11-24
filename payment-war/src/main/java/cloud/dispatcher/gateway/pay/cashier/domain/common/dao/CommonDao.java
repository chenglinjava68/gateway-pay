package cloud.dispatcher.gateway.pay.cashier.domain.common.dao;

import lombok.Getter;
import lombok.Setter;
import net.rubyeye.xmemcached.XMemcachedClient;

import org.apache.commons.lang.StringUtils;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

public class CommonDao extends SqlMapClientDaoSupport {
    
    @Getter @Setter protected XMemcachedClient xMemcachedClient;

    protected int parseTableNameSuffix(String value) {
        if (StringUtils.isNotEmpty(value) && value.length() == 35) {
            return Integer.valueOf(value.substring(0, 3));
        } else {
            throw new IllegalArgumentException(
                    "Wrong parameter: value = [" + value + "]");
        }
    }
}

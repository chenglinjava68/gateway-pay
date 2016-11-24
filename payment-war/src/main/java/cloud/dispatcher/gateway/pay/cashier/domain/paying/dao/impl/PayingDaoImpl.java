package cloud.dispatcher.gateway.pay.cashier.domain.paying.dao.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import cloud.dispatcher.gateway.pay.cashier.domain.common.dao.CommonDao;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.bean.entity.PayingEntity;
import cloud.dispatcher.gateway.pay.cashier.domain.paying.dao.PayingDao;

public class PayingDaoImpl extends CommonDao implements PayingDao {

    private static final String TABLE_PAYMENT_PAYING_NAME_PREFIX = "payment_paying";

    /**
     * ����֧�������Ķ�����(PrimaryKey)
     *
     * 1.����UUID�������е�"-"���滻�ɿ��ַ�; 2.ȡUUID��һλ���ַ���ת��ΪASCII��
     * 3.�����һλ�ַ�Ϊ����(0-9, ASCII�� >= 048 && <= 057)��ֱ�������λǰ��0
     *   �����һλ�ַ�Ϊ��ĸ(a-f, ASCII�� >= 097 && <= 102)��ֱ�����һλǰ��0
     *
     * �����һλΪ������ͨ��+1������letter��0-9ת��Ϊ1-10, �����һλΪ��ĸ��ͨ��
     * -1������letter��10-15ת��Ϊ11-16, ǰ�õ�3λ���ֿ���ֱ�����ڷֱ����
     */
    private String buildPrimaryKey() {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "").toLowerCase();
        int letter = (int)uuid.charAt(0);
        uuid = String.valueOf((letter >= 48 && letter <= 57) ?
                (letter - 47) : (letter - 86)) + uuid;
        return uuid.length() == 33 ? "00" + uuid : "0" + uuid;
    }

    /**
     * ����֧�������Ż�ȡ�������ڱ�ı���
     */
    private String buildTableName(String id) {
        return TABLE_PAYMENT_PAYING_NAME_PREFIX + String.valueOf(
                parseTableNameSuffix(id));
    }

    /**
     * ����֧��������Ϣ
     *
     * ���ύ֧��ƽ̨ǰ����֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.OPERATING
     */
    @Override
    public void updateOnCommit(PayingEntity entity) {
        entity.setTableName(buildTableName(entity.getId()));
        this.getSqlMapClientTemplate().update(
                "PayingDao.updateOnCommit", entity);
    }

    /**
     * ����֧��������Ϣ
     *
     * ��֧��ƽ̨�ص������֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.FAILED �� PayingPhaseEnum.SUCCEED
     */
    @Override
    public void updateOnNotify(PayingEntity entity) {
        entity.setTableName(buildTableName(entity.getId()));
        this.getSqlMapClientTemplate().update(
                "PayingDao.updateOnNotify", entity);
    }
    
    /**
     * ����֧��������Ϣ
     *
     * ��֧��ƽ̨�ص������֧���������������, ����֧��������״̬Ϊ
     * PayingPhaseEnum.FAILED �� PayingPhaseEnum.SUCCEED
     */
    @Override
    public void updateOnReturn(PayingEntity entity) {
        entity.setTableName(buildTableName(entity.getId()));
        this.getSqlMapClientTemplate().update(
                "PayingDao.updateOnReturn", entity);
    }

    /**
     * ����ID��ȡ֧����������
     *
     * ��ȡ����PayingEntity������, status��oppGatewayType��Ҫ���ݶ�
     * Ӧ��ö������PayingPhaseEnum��OppTypeEnum����ת��
     *
     * @return ֧����������
     */
    @Override
    public PayingEntity select(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tableName", buildTableName(id));
        params.put("id", id);
        return (PayingEntity) this.getSqlMapClientTemplate().queryForObject(
                "PayingDao.select", params);
    }

    /**
     * ����в���֧����������
     *
     * Id��DAO����UUID����, �����в��ص���setId����; ����֧������ʱ
     * , statusΪ1(PayingPhaseEnum.REGISTED), ֧����ʽΪ0(δѡ��)
     *
     * @return ֧����������
     */
    @Override
    public String insert(PayingEntity entity) {
        String primaryKey = buildPrimaryKey();
        entity.setId(primaryKey);
        entity.setTableName(buildTableName(primaryKey));
        this.getSqlMapClientTemplate().insert("PayingDao.insert", entity);
        return primaryKey;
    }
}

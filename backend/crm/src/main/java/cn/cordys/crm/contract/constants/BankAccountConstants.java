package cn.cordys.crm.contract.constants;

public enum BankAccountConstants {

    NAME("name", "账户名称", "Account name"),
    TYPE("type", "收款方式", "Payment type"),
    OPENING_BANK("openingBank", "开户行", "Opening bank"),
    BANK_ACCOUNT("bankAccount", "银行账号", "Bank account"),
    ACCOUNT_HOLDER("accountHolder", "户名", "Account holder"),
    QRCODE("qrcode", "收款二维码", "QR code"),
    REMARK("remark", "备注", "Remark");

    private final String key;
    private final String ch;
    private final String us;

    BankAccountConstants(String key, String ch, String us) {
        this.key = key;
        this.ch = ch;
        this.us = us;
    }

    public String getKey() {
        return key;
    }

    public String getCh() {
        return ch;
    }

    public String getUs() {
        return us;
    }

    public String getId() {
        if (this == BankAccountConstants.NAME) {
            // name 和其他字段的 businessKey 冲突
            return "bank_account_" + getKey();
        } else {
            // 其他字段不冲突，不处理，避免影响历史数据
            return getKey();
        }
    }
}

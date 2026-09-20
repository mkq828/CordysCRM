package cn.cordys.crm.contract.constants;

/**
 * 收款账户的收款方式。
 *
 * @author song-cc-rock
 */
public enum BankAccountTypeEnum {

    BANK_CARD("BANK_CARD", "银行卡", "Bank card"),
    WECHAT("WECHAT", "微信", "WeChat"),
    ALIPAY("ALIPAY", "支付宝", "Alipay");

    private final String value;
    private final String ch;
    private final String us;

    BankAccountTypeEnum(String value, String ch, String us) {
        this.value = value;
        this.ch = ch;
        this.us = us;
    }

    public String getValue() {
        return value;
    }

    public String getCh() {
        return ch;
    }

    public String getUs() {
        return us;
    }
}

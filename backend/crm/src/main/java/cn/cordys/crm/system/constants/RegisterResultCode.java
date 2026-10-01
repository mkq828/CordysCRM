package cn.cordys.crm.system.constants;

import cn.cordys.common.exception.IResultCode;

/**
 * 注册模块业务返回码
 */
public enum RegisterResultCode implements IResultCode {

    PHONE_EXIST(101100, "register.phone.exist"),
    ID_CARD_EXIST(101101, "register.id_card.exist"),
    CREDIT_CODE_EXIST(101102, "register.credit_code.exist"),
    APPLICATION_NOT_FOUND(101103, "register.application.not.found"),
    ALREADY_PROCESSED(101104, "register.application.already.processed"),
    REGISTER_TYPE_INVALID(101105, "register.type.invalid"),
    LICENSE_REQUIRED(101106, "register.license.required"),
    ACCOUNT_NOT_OPENED(101107, "register.account.not.opened"),
    AGREEMENT_NOT_AGREED(101108, "register.agreement.not.agreed"),
    REGISTER_REJECTED(101109, "register.rejected.login");

    private final int code;
    private final String message;

    RegisterResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return getTranslationMessage(this.message);
    }
}

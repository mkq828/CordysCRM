import { BankAccountTypeEnum } from '@lib/shared/enums/bankAccountEnum';
import type { SaveBankAccountParams } from '@lib/shared/models/contract';

const initBankAccountForm: SaveBankAccountParams = {
  id: '',
  name: '',
  type: BankAccountTypeEnum.BANK_CARD,
  openingBank: '',
  bankAccount: '',
  accountHolder: '',
  qrcode: '',
  remark: '',
};

export default initBankAccountForm;

import { defineStore } from 'pinia';
import dayjs from 'dayjs';

import { LicenseInfo } from '@lib/shared/models/system/authorizedManagement';
import { TenantSubscription } from '@lib/shared/models/system/business';

import { getSubscription } from '@/api/modules';

let licenseValidationPromise: Promise<boolean> | null = null;

// 租户订阅(tenant_plan)映射为 license 兼容结构：付费版(PRO/ENTERPRISE)视为企业版，BASIC/试用视为无
function toLicenseInfo(sub: TenantSubscription): LicenseInfo {
  const paid = ['PRO', 'ENTERPRISE'].includes(sub.version || '');
  let status: string | null = null;
  if (paid) {
    status = sub.status === 'ACTIVE' ? 'valid' : 'expired';
  }
  return {
    status,
    corporation: '',
    expired: sub.expireTime ? dayjs(sub.expireTime).format('YYYY-MM-DD') : '',
    product: '',
    edition: sub.version || '',
    licenseVersion: '',
    count: 0,
  };
}

const useLicenseStore = defineStore('license', {
  persist: {
    paths: ['licenseInfo', 'expiredDuring', 'expiredDays'],
  },
  state: (): {
    licenseInfo: LicenseInfo | null;
    expiredDuring: boolean;
    expiredDays: number;
  } => ({
    licenseInfo: null,
    expiredDuring: false,
    expiredDays: 0,
  }),
  actions: {
    setLicenseInfo(info: LicenseInfo) {
      this.licenseInfo = info;
    },
    removeLicenseStatus() {
      if (this.licenseInfo) {
        this.licenseInfo.status = null;
      }
    },
    hasLicense() {
      return this.licenseInfo?.status === 'valid';
    },
    isEnterpriseVersion() {
      return ['valid', 'expired'].includes(this.licenseInfo?.status || '');
    },
    getExpirationTime(resTime: string) {
      if (!this.isEnterpriseVersion()) {
        this.expiredDuring = false;
        this.expiredDays = 0;
        return;
      }

      if (!resTime && !this.hasLicense() && this.isEnterpriseVersion()) {
        this.expiredDuring = true;
        this.expiredDays = 0;
        return;
      }

      const today = dayjs().startOf('day'); // 今天的开始时间
      const expirationDate = dayjs(resTime).startOf('day'); // license到期日的开始时间

      // 计算天数差（不包含当天）
      const daysDifference = expirationDate.diff(today, 'day');
      if (daysDifference < 0) {
        // 已过期情况
        const daysExpired = Math.abs(daysDifference);
        this.expiredDays = -daysExpired; // 负数表示已过期天数
        this.expiredDuring = daysExpired <= 30; // 过期后只显示30天
      } else if (daysDifference === 0) {
        // 今天到期（显示"剩余1天"）
        this.expiredDays = 1;
        this.expiredDuring = true;
      } else if (daysDifference <= 29) {
        // 1-29天后到期（显示剩余天数+1）
        this.expiredDays = daysDifference + 1;
        this.expiredDuring = true;
      } else if (daysDifference === 30) {
        // 30天后到期（显示"剩余30天"）
        this.expiredDays = daysDifference + 1;
        this.expiredDuring = false;
      } else {
        // 31+天后到期
        this.expiredDays = daysDifference + 1;
        this.expiredDuring = false;
      }
    },
    // license校验
    async getValidateLicense(): Promise<boolean> {
      try {
        const result = await getSubscription();
        if (!result || !result.version) {
          this.removeLicenseStatus();
          return true;
        }
        const info = toLicenseInfo(result);
        this.setLicenseInfo(info);
        this.getExpirationTime(info.expired);
        return true;
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        return false;
      }
    },
    // 请求失败时清空缓存，后续可重新校验
    async ensureLicenseValidated() {
      if (!licenseValidationPromise) {
        licenseValidationPromise = this.getValidateLicense();
      }
      const validated = await licenseValidationPromise;
      if (!validated) {
        licenseValidationPromise = null;
      }
    },
    resetLicenseValidation() {
      licenseValidationPromise = null;
    },
  },
});

export default useLicenseStore;

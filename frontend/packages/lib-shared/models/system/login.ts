export interface LoginParams {
  username: string;
  password: string;
  authenticate: string;
  loginAddress?: string;
  platform: 'WEB' | 'MOBILE'; // 平台,WEB\MOBILE
  captchaId?: string;
  captchaCode?: string;
}

// 图形验证码
export interface CaptchaResult {
  captchaId: string;
  captchaImage: string; // base64 data URI
}

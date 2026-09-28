// 版本（套餐版本，可配置）
export interface Edition {
  id: string;
  code: string;
  name: string;
  yearPrice?: number;
  firstYearPrice?: number;
  softLimit?: number;
  validityDays?: number;
  sort?: number;
  status?: number;
}

// 功能（可配置功能点，含全局开关）
export interface Feature {
  id: string;
  featureCode: string;
  name: string;
  category?: string;
  enable?: boolean;
}

// 版本保存参数（含功能归属）
export interface EditionSaveParams {
  id?: string;
  code: string;
  name: string;
  yearPrice?: number;
  firstYearPrice?: number;
  softLimit?: number;
  validityDays?: number;
  sort?: number;
  status?: number;
  featureIds?: string[];
}

// 功能保存参数
export interface FeatureSaveParams {
  id?: string;
  featureCode: string;
  name: string;
  category?: string;
  enable?: boolean;
}

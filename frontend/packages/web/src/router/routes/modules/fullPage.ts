import { FullPageEnum, WorkbenchRouteEnum } from '@/enums/routeEnum';

import { FULL_PAGE_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const FullPage: AppRouteRecordRaw = {
  path: '/fullPage',
  name: FullPageEnum.FULL_PAGE,
  redirect: '/fullPage/fullExportQuotation',
  component: FULL_PAGE_LAYOUT,
  meta: {
    hideInMenu: true,
    permissions: ['DASHBOARD:READ', 'OPPORTUNITY_QUOTATION:READ'],
  },
  children: [
    {
      path: 'fullExportQuotation',
      name: FullPageEnum.FULL_PAGE_EXPORT_QUOTATION,
      component: () => import('@/views/opportunity/components/quotation/exportQuotationPdf.vue'),
      meta: {
        permissions: ['OPPORTUNITY_QUOTATION:READ'],
      },
    },
    {
      path: 'fullExportOrder',
      name: FullPageEnum.FULL_PAGE_EXPORT_ORDER,
      component: () => import('@/views/order/order/components/exportPdf.vue'),
      meta: {
        permissions: ['ORDER:READ'],
      },
    },
    {
      path: 'dmProfileGuide',
      name: WorkbenchRouteEnum.WORKBENCH_DM_PROFILE_GUIDE,
      component: () => import('@/views/workbench/dmProfile/guide/index.vue'),
      meta: {
        locale: 'menu.workbench.dmProfileGuide',
        permissions: [],
        hideInMenu: true,
      },
    },
  ],
};

export default FullPage;

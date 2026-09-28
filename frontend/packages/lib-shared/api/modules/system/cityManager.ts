import type { CordysAxios } from '@lib/shared/api/http/Axios';

import {
  cityManagerAddUrl,
  cityManagerAssignUrl,
  cityManagerDisableUrl,
  cityManagerListUrl,
  cityManagerMyOrgsUrl,
  cityManagerOrgOptionsUrl,
  cityManagerPerformanceOverviewUrl,
  cityManagerReassignUrl,
} from '@lib/shared/api/requrls/system/cityManager';
import type { CommonList } from '@lib/shared/models/common';
import type {
  CityManagerAddParams,
  CityManagerAssignParams,
  CityManagerItem,
  CityManagerOrgItem,
  CityManagerPageParams,
  CityManagerPerformanceOverview,
  CityManagerPerformanceParams,
  CityManagerReassignParams,
} from '@lib/shared/models/system/cityManager';

export default function useCityManagerApi(CDR: CordysAxios) {
  function add(data: CityManagerAddParams) {
    return CDR.post({ url: cityManagerAddUrl, data });
  }
  function pageList(data: CityManagerPageParams) {
    return CDR.post<CommonList<CityManagerItem>>({ url: cityManagerListUrl, data }, { ignoreCancelToken: true });
  }
  function disable(id: string) {
    return CDR.get({ url: `${cityManagerDisableUrl}/${id}` });
  }
  function assign(data: CityManagerAssignParams) {
    return CDR.post({ url: cityManagerAssignUrl, data });
  }
  function reassign(data: CityManagerReassignParams) {
    return CDR.post({ url: cityManagerReassignUrl, data });
  }
  function orgOptions() {
    return CDR.get<CityManagerOrgItem[]>({ url: cityManagerOrgOptionsUrl });
  }
  function myOrgs() {
    return CDR.get<CityManagerOrgItem[]>({ url: cityManagerMyOrgsUrl });
  }
  function performanceOverview(data: CityManagerPerformanceParams) {
    return CDR.post<CityManagerPerformanceOverview>({ url: cityManagerPerformanceOverviewUrl, data });
  }

  return {
    add,
    pageList,
    disable,
    assign,
    reassign,
    orgOptions,
    myOrgs,
    performanceOverview,
  };
}

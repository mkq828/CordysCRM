import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  editionDeleteUrl,
  editionFeatureDeleteUrl,
  editionFeatureListUrl,
  editionFeatureSaveUrl,
  editionListUrl,
  editionMappingUrl,
  editionOptionsUrl,
  editionSaveUrl,
} from '@lib/shared/api/requrls/system/edition';
import type { Edition, EditionSaveParams, Feature, FeatureSaveParams } from '@lib/shared/models/system/edition';

export default function useEditionApi(CDR: CordysAxios) {
  // 版本-列表（全量）
  function listEditions() {
    return CDR.get<Edition[]>({ url: editionListUrl });
  }

  // 版本-启用列表（开通/续费下拉）
  function listEditionOptions() {
    return CDR.get<Edition[]>({ url: editionOptionsUrl });
  }

  // 版本-保存（新增/编辑，含功能归属）
  function saveEdition(data: EditionSaveParams) {
    return CDR.post({ url: editionSaveUrl, data });
  }

  // 版本-删除
  function deleteEdition(id: string) {
    return CDR.post({ url: `${editionDeleteUrl}/${id}` });
  }

  // 功能-列表（全量）
  function listFeatures() {
    return CDR.get<Feature[]>({ url: editionFeatureListUrl });
  }

  // 功能-保存（新增/编辑）
  function saveFeature(data: FeatureSaveParams) {
    return CDR.post({ url: editionFeatureSaveUrl, data });
  }

  // 功能-删除
  function deleteFeature(id: string) {
    return CDR.post({ url: `${editionFeatureDeleteUrl}/${id}` });
  }

  // 版本-功能归属查询（返回功能ID集合）
  function listFeatureIdsByEditionId(editionId: string) {
    return CDR.get<string[]>({ url: `${editionMappingUrl}/${editionId}` });
  }

  return {
    listEditions,
    listEditionOptions,
    saveEdition,
    deleteEdition,
    listFeatures,
    saveFeature,
    deleteFeature,
    listFeatureIdsByEditionId,
  };
}

<template>
  <div class="h-full py-[16px]">
    <MsMinderEditor
      v-model:activeExtraKey="activeExtraKey"
      v-model:extra-visible="extraVisible"
      v-model:loading="loading"
      v-model:import-json="importJson"
      :tags="[]"
      :replaceable-tags="replaceableTags"
      :insert-node="insertNode"
      :priority-disable-check="priorityDisableCheck"
      :after-tag-edit="afterTagEdit"
      :extract-content-tab-list="extractContentTabList"
      :can-show-enter-node="canShowEnterNode"
      :insert-sibling-menus="insertSiblingMenus"
      :insert-son-menus="insertSonMenus"
      :can-show-paste-menu="!stopPaste()"
      :can-show-more-menu="canShowMoreMenu()"
      :can-show-priority-menu="canShowPriorityMenu()"
      :priority-tooltip="t('caseManagement.caseReview.caseLevel')"
      single-tag
      tag-enable
      sequence-enable
      @content-change="handleContentChange"
      @node-select="handleNodeSelect"
      @action="handleAction"
      @before-exec-command="handleBeforeExecCommand"
      @save="handleMinderSave"
      @float-menu-close="handleBaseInfoCancel"
    >
      <template #extractMenu>
        <a-tooltip v-if="showDetailMenu" :content="t('common.detail')">
          <MsButton
            type="icon"
            class="ms-minder-node-float-menu-icon-button"
            :class="[extraVisible ? 'ms-minder-node-float-menu-icon-button--focus' : '']"
            @click="toggleDetail"
          >
            <MsIcon type="icon-icon_describe_outlined" class="text-[var(--color-text-4)]" />
          </MsButton>
        </a-tooltip>
      </template>
      <template #extractTabContent>
        <baseInfo
          v-if="activeExtraKey === 'baseInfo'"
          ref="baseInfoRef"
          :loading="baseInfoLoading"
          :active-case="activeCase"
          @init-template="(id) => (templateId = id)"
          @cancel="handleBaseInfoCancel"
        />
        <attachment
          v-else-if="activeExtraKey === 'attachment'"
          v-model:model-value="fileList"
          :active-case="activeCase"
          @upload-success="initCaseDetail"
        />
        <caseCommentList v-else-if="activeExtraKey === 'comments'" :active-case="activeCase" />
        <bugList v-else :active-case="activeCase" />
      </template>
    </MsMinderEditor>
  </div>
</template>

<script setup lang="ts">
  import { Message } from '@arco-design/web-vue';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import { FormItem } from '@/components/pure/ms-form-create/types';
  import MsMinderEditor from '@/components/pure/ms-minder-editor/minderEditor.vue';
  import type { MinderJson, MinderJsonNode, MinderJsonNodeData } from '@/components/pure/ms-minder-editor/props';
  import { MsFileItem } from '@/components/pure/ms-upload/types';
  import attachment from './attachment.vue';
  import baseInfo from './basInfo.vue';
  import bugList from './bugList.vue';
  import caseCommentList from './commentList.vue';

  import {
    checkFileIsUpdateRequest,
    getCaseDetail,
    getCaseMinder,
    getCaseMinderTree,
    saveCaseMinder,
  } from '@/api/modules/case-management/featureCase';
  import { useI18n } from '@/hooks/useI18n';
  import useAppStore from '@/store/modules/app';
  import useFeatureCaseStore from '@/store/modules/case/featureCase';
  import useMinderStore from '@/store/modules/components/minder-editor/index';
  import { MinderCustomEvent } from '@/store/modules/components/minder-editor/types';
  import { filterTree, getGenerateId, mapTree } from '@/utils';

  import {
    FeatureCaseMinderEditType,
    FeatureCaseMinderStepItem,
    FeatureCaseMinderUpdateParams,
  } from '@/models/caseManagement/featureCase';
  import { MoveMode, TableQueryParams } from '@/models/common';
  import { MinderEventName } from '@/enums/minderEnum';

  import useMinderBaseApi from './useMinderBaseApi';
  import { convertToFile, initFormCreate } from '@/views/case-management/caseManagementFeature/components/utils';

  const props = defineProps<{
    moduleId: string;
    moduleName: string;
    modulesCount: Record<string, number>; // 模块数量
  }>();
  const emit = defineEmits<{
    (e: 'save'): void;
  }>();

  const appStore = useAppStore();
  const { t } = useI18n();
  const minderStore = useMinderStore();

  const {
    caseTag,
    moduleTag,
    stepTag,
    textDescTag,
    prerequisiteTag,
    remarkTag,
    caseOffspringTags,
    insertSiblingMenus,
    insertSonMenus,
    insertNode,
    handleBeforeExecCommand,
    stopPaste,
    checkNodeCanShowMenu,
    canShowMoreMenu,
    canShowPriorityMenu,
    handleContentChange,
    replaceableTags,
    priorityDisableCheck,
  } = useMinderBaseApi();
  const importJson = ref<MinderJson>({
    root: {} as MinderJsonNode,
    template: 'default',
    treePath: [],
  });
  const caseTree = ref<MinderJsonNode[]>([]);
  const loading = ref(false);
  const tempMinderParams = ref<FeatureCaseMinderUpdateParams>({
    projectId: appStore.currentProjectId,
    versionId: '',
    updateCaseList: [],
    updateModuleList: [],
    deleteResourceList: [],
    additionalNodeList: [],
  });
  const templateId = ref('');

  /**
   * 初始化用例模块树
   */
  async function initCaseTree() {
    try {
      loading.value = true;
      const res = await getCaseMinderTree({
        projectId: appStore.currentProjectId,
        moduleId: props.moduleId === 'all' ? '' : props.moduleId,
      });
      caseTree.value = mapTree<MinderJsonNode>(res, (e) => ({
        ...e,
        data: {
          id: e.id,
          text: e.name,
          resource: props.modulesCount[e.id] !== undefined ? [moduleTag] : e.data?.resource,
          expandState: e.level === 1 ? 'expand' : 'collapse',
          count: props.modulesCount[e.id],
          isNew: false,
          changed: false,
        },
        children:
          props.modulesCount[e.id] > 0 && !e.children?.length
            ? [
                {
                  data: {
                    id: 'fakeNode',
                    text: 'fakeNode',
                    resource: ['fakeNode'],
                    isNew: false,
                    changed: false,
                  },
                },
              ]
            : e.children,
      }));
      importJson.value.root = {
        children: caseTree.value,
        data: {
          id: 'NONE',
          text: t('ms.minders.allModule'),
          resource: [moduleTag],
          disabled: true,
        },
      };
      window.minder.importJson(importJson.value);
      if (props.moduleId !== 'all') {
        nextTick(() => {
          minderStore.dispatchEvent(MinderEventName.ENTER_NODE, undefined, undefined, undefined, [
            window.minder.getNodeById(props.moduleId),
          ]);
        });
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    initCaseTree();
  });

  const baseInfoRef = ref<InstanceType<typeof baseInfo>>();

  /**
   * 解析用例节点信息
   * @param node 用例节点
   */
  function getCaseNodeInfo(node: MinderJsonNode) {
    let textStep: MinderJsonNode | undefined; // 文本描述
    let prerequisiteNode: MinderJsonNode | undefined; // 前置条件
    let remarkNode: MinderJsonNode | undefined; // 备注
    const stepNodes: MinderJsonNode[] = []; // 步骤描述
    node.children?.forEach((item) => {
      if (item.data?.resource?.includes(textDescTag)) {
        textStep = item;
      } else if (item.data?.resource?.includes(stepTag)) {
        stepNodes.push(item);
      } else if (item.data?.resource?.includes(prerequisiteTag)) {
        prerequisiteNode = item;
      } else if (item.data?.resource?.includes(remarkTag)) {
        remarkNode = item;
      }
    });
    const steps: FeatureCaseMinderStepItem[] = stepNodes.map((child, i) => {
      return {
        id: child.data?.id || getGenerateId(),
        num: i,
        desc: child.data?.text || '',
        result: child.children?.[0].data?.text || '',
      };
    });
    return {
      prerequisite: prerequisiteNode?.data?.text || '',
      caseEditType: steps.length > 0 ? 'STEP' : ('TEXT' as FeatureCaseMinderEditType),
      steps: JSON.stringify(steps),
      textDescription: textStep?.data?.text || '',
      expectedResult: textStep?.children?.[0]?.data?.text || '',
      description: remarkNode?.data?.text || '',
    };
  }

  /**
   * 获取节点的移动信息
   * @param node 节点
   * @param parent 父节点
   */
  function getNodeMoveInfo(nodeIndex: number, parent?: MinderJsonNode): { moveMode: MoveMode; targetId?: string } {
    const moveMode = nodeIndex === 0 ? 'BEFORE' : 'AFTER'; // 除了第一个以外，其他都是在目标节点后面插入
    return {
      moveMode,
      targetId:
        moveMode === 'BEFORE'
          ? parent?.children?.[1]?.data?.id
          : parent?.children?.[(nodeIndex || parent.children.length - 1) - 1]?.data?.id,
    };
  }

  const baseInfoLoading = ref(false);

  const formRules = ref<FormItem[]>([]);

  const extraVisible = ref<boolean>(false);
  const activeCase = ref<Record<string, any>>({});
  const extractContentTabList = computed(() => {
    const fullTabList = [
      {
        label: t('common.baseInfo'),
        value: 'baseInfo',
      },
      {
        label: t('caseManagement.featureCase.attachment'),
        value: 'attachment',
      },
      {
        value: 'comments',
        label: t('caseManagement.featureCase.comments'),
      },
      {
        value: 'bug',
        label: t('caseManagement.featureCase.bug'),
      },
    ];
    if (!activeCase.value.isNew) {
      return fullTabList;
    }
    return fullTabList.filter((item) => item.value === 'baseInfo');
  });
  const activeExtraKey = ref<'baseInfo' | 'attachment' | 'comments' | 'bug'>('baseInfo');

  const fileList = ref<MsFileItem[]>([]);
  const checkUpdateFileIds = ref<string[]>([]);

  const getListFunParams = ref<TableQueryParams>({
    combine: {
      hiddenIds: [],
    },
  });

  // 监视文件列表处理关联和本地文件
  watch(
    () => fileList.value,
    (val) => {
      if (val) {
        getListFunParams.value.combine.hiddenIds = fileList.value.filter((item) => !item.local).map((item) => item.uid);
      }
    },
    { deep: true }
  );

  /**
   * 初始化用例详情
   * @param data 节点数据/用例数据
   */
  async function initCaseDetail(data?: MinderJsonNodeData | Record<string, any>) {
    try {
      baseInfoLoading.value = true;
      const res = await getCaseDetail(data?.id || activeCase.value.id);
      activeCase.value = res;
      const fileIds = (res.attachments || []).map((item: any) => item.id) || [];
      if (fileIds.length) {
        checkUpdateFileIds.value = await checkFileIsUpdateRequest(fileIds);
      }
      if (res.attachments) {
        // 处理文件列表
        fileList.value = res.attachments
          .map((fileInfo: any) => {
            return {
              ...fileInfo,
              name: fileInfo.fileName,
              isUpdateFlag: checkUpdateFileIds.value.includes(fileInfo.id),
            };
          })
          .map((fileInfo: any) => {
            return convertToFile(fileInfo);
          });
      }
      formRules.value = initFormCreate(res.customFields, ['FUNCTIONAL_CASE:READ+UPDATE']);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      baseInfoLoading.value = false;
    }
  }

  function resetExtractInfo() {
    activeCase.value = {};
    fileList.value = [];
  }

  function handleBaseInfoCancel() {
    extraVisible.value = false;
    resetExtractInfo();
  }

  /**
   * 切换用例详情显示
   */
  async function toggleDetail(val?: boolean) {
    extraVisible.value = val !== undefined ? val : !extraVisible.value;
    const node: MinderJsonNode = window.minder.getSelectedNode();
    const { data } = node;
    if (extraVisible.value) {
      if (data?.resource && data.resource.includes(caseTag)) {
        activeExtraKey.value = 'baseInfo';
        resetExtractInfo();
        if (data.isNew === false) {
          // 非新用例节点才能加载详情
          initCaseDetail(data);
        } else {
          activeCase.value = {
            id: data.id,
            name: data.text,
            isNew: true,
          };
        }
      }
    }
  }

  const showDetailMenu = ref(false);
  const canShowEnterNode = ref(false);
  /**
   * 处理脑图节点激活/点击
   * @param node 被激活/点击的节点
   */
  async function handleNodeSelect(node: MinderJsonNode) {
    checkNodeCanShowMenu(node);
    const { data } = node;
    if (
      data?.resource?.includes(moduleTag) &&
      (node.children || []).length > 0 &&
      node.type !== 'root' &&
      !data.isNew
    ) {
      // 模块节点且有子节点且非根节点且非新建节点，可展示进入节点菜单
      canShowEnterNode.value = true;
    } else {
      canShowEnterNode.value = false;
    }
    if (data?.resource && data.resource.includes(caseTag)) {
      // 用例节点才展示详情按钮
      showDetailMenu.value = true;
      if (extraVisible.value) {
        toggleDetail(true);
      }
    } else if (data?.resource?.includes(moduleTag) && data.count > 0 && data.isLoaded !== true) {
      // 模块节点且有用例且未加载过用例数据
      try {
        loading.value = true;
        showDetailMenu.value = false;
        extraVisible.value = false;
        const res = await getCaseMinder({
          projectId: appStore.currentProjectId,
          moduleId: data.id,
        });
        const fakeNode = node.children?.find((e) => e.data?.id === undefined); // 移除占位的虚拟节点
        if (fakeNode) {
          window.minder.removeNode(fakeNode);
        }
        if ((!res || res.length === 0) && node.children?.length) {
          // 如果模块下没有用例且有别的模块节点，正常展开
          node.expand();
          node.renderTree();
          window.minder.layout();
          return;
        }
        // TODO:递归渲染存在的子节点
        res.forEach((e) => {
          // 用例节点
          const child = window.minder.createNode(
            {
              ...e.data,
              isNew: false,
            },
            node
          );
          child.render();
          e.children?.forEach((item) => {
            // 前置/步骤/备注节点
            const grandChild = window.minder.createNode(
              {
                ...item.data,
                isNew: false,
              },
              child
            );
            grandChild.render();
            item.children?.forEach((subItem) => {
              // 预期结果节点
              const greatGrandChild = window.minder.createNode(
                {
                  ...subItem.data,
                  isNew: false,
                },
                grandChild
              );
              greatGrandChild.render();
            });
          });
          child.expand();
          child.renderTree();
        });
        node.expand();
        node.renderTree();
        window.minder.layout();
        window.minder.execCommand('camera', node, 100);
        if (node.data) {
          node.data.isLoaded = true;
        }
        // 加载完用例数据后，更新当前importJson数据
        importJson.value = window.minder.exportJson();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    } else {
      // 文本节点或已加载过用例数据的模块节点
      extraVisible.value = false;
      showDetailMenu.value = false;
      resetExtractInfo();
      if (node.children && node.children.length > 0) {
        node.expand();
        node.renderTree();
        window.minder.layout();
      }
    }
  }

  /**
   * 标签编辑后，如果将标签修改为模块，则删除已添加的优先级
   * @param node 选中节点
   * @param tag 更改后的标签
   */
  function afterTagEdit(nodes: MinderJsonNode[], tag: string) {
    nodes.forEach((node, index) => {
      if (tag === moduleTag && node.data) {
        // 排除是从用例节点切换到模块节点的数据
        tempMinderParams.value.updateCaseList = tempMinderParams.value.updateCaseList.filter(
          (e) => e.id !== node.data?.id
        );
        node.data.isNew = true;
        window.minder.execCommand('priority');
        if (index === nodes.length - 1) {
          nextTick(() => {
            handleNodeSelect(node);
          });
        }
      } else if (node.data?.resource?.includes(caseTag)) {
        // 排除是从模块节点切换到用例节点的数据
        tempMinderParams.value.updateModuleList = tempMinderParams.value.updateModuleList.filter(
          (e) => e.id !== node.data?.id
        );
        node.data.isNew = true;
        if (index === nodes.length - 1) {
          nextTick(() => {
            handleNodeSelect(node);
          });
        }
      } else if (node.data?.resource?.some((e) => caseOffspringTags.includes(e))) {
        // 用例子孙节点更新，标记用例节点变化
        if (node.parent?.data?.resource?.includes(caseTag)) {
          node.parent.data.changed = true;
        } else if (node.parent?.parent?.data?.resource?.includes(caseTag)) {
          // 期望结果是第三层节点
          node.parent.parent.data.changed = true;
        }
      }
    });
  }

  /**
   * 处理脑图节点操作
   * @param event 脑图事件对象
   */
  function handleAction(event: MinderCustomEvent) {
    const { nodes, name } = event;
    if (nodes && nodes.length > 0) {
      switch (name) {
        case MinderEventName.DELETE_NODE:
        case MinderEventName.CUT_NODE:
          // TODO:循环优化
          nodes.forEach((node) => {
            if (!caseOffspringTags.some((e) => node.data?.resource?.includes(e))) {
              // 非用例下的子孙节点的移除，才加入删除资源队列
              tempMinderParams.value.deleteResourceList.push({
                id: node.data?.id || getGenerateId(),
                type: node.data?.resource?.[0] || moduleTag,
              });
            }
            if (node.data?.resource?.includes(caseTag)) {
              // 删除用例节点
              tempMinderParams.value.updateCaseList = tempMinderParams.value.updateCaseList.filter(
                (e) => e.id !== node.data?.id
              );
            } else if (node.data?.resource?.includes(moduleTag)) {
              // 删除模块节点
              tempMinderParams.value.updateModuleList = tempMinderParams.value.updateModuleList.filter(
                (e) => e.id !== node.data?.id
              );
            } else if (!node.data?.resource) {
              // 删除文本节点
              tempMinderParams.value.additionalNodeList = tempMinderParams.value.additionalNodeList.filter(
                (e) => e.id !== node.data?.id
              );
            }
          });
          break;
        default:
          break;
      }
    }
  }

  /**
   * 生成脑图保存的入参
   */
  function makeMinderParams(fullJson: MinderJson): FeatureCaseMinderUpdateParams {
    filterTree(fullJson.root.children, (node, nodeIndex, parent) => {
      if (node.data.isNew !== false || node.data.changed === true) {
        if (node.data.resource?.includes(moduleTag)) {
          // 处理模块节点
          tempMinderParams.value.updateModuleList.push({
            id: node.data.id,
            name: node.data.text,
            parentId: parent?.data.id || 'NONE',
            type: node.data.isNew !== false ? 'ADD' : 'UPDATE',
            ...getNodeMoveInfo(nodeIndex, parent as MinderJsonNode),
          });
        } else if (node.data.resource?.includes(caseTag)) {
          // 处理用例节点
          const caseNodeInfo = getCaseNodeInfo(node as MinderJsonNode);
          const caseBaseInfo = baseInfoRef.value?.makeParams();
          tempMinderParams.value.updateCaseList.push({
            id: node.data.id,
            moduleId: parent?.data.id || '',
            type: node.data.isNew !== false ? 'ADD' : 'UPDATE',
            templateId: templateId.value,
            tags: caseBaseInfo?.tags || [],
            customFields: caseBaseInfo?.customFields || [],
            name: caseBaseInfo?.name || node.data.text,
            ...getNodeMoveInfo(nodeIndex, parent as MinderJsonNode),
            ...caseNodeInfo,
          });
          return false; // 用例的子孙节点已经处理过，跳过
        } else if (!node.data.resource || node.data.resource.length === 0) {
          // 处理文本节点
          tempMinderParams.value.additionalNodeList.push({
            id: node.data.id,
            parentId: parent?.data.id || 'NONE',
            type: node.data.isNew !== false ? 'ADD' : 'UPDATE',
            name: node.data.text,
            ...getNodeMoveInfo(nodeIndex, parent as MinderJsonNode),
          });
        }
      }

      return true;
    });
    return tempMinderParams.value;
  }

  /**
   * 处理脑图保存
   * @param fullJson 脑图导出的完整数据
   * @param callback 保存成功回调
   */
  async function handleMinderSave(fullJson: MinderJson, callback: () => void) {
    try {
      loading.value = true;
      await saveCaseMinder(makeMinderParams(fullJson));
      extraVisible.value = false;
      Message.success(t('common.saveSuccess'));
      tempMinderParams.value = {
        projectId: appStore.currentProjectId,
        versionId: '',
        updateCaseList: [],
        updateModuleList: [],
        deleteResourceList: [],
        additionalNodeList: [],
      };
      emit('save');
      callback();
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  }

  const featureCaseStore = useFeatureCaseStore();

  watch(
    () => featureCaseStore.modulesCount,
    () => {
      initCaseTree();
    },
    {
      deep: true,
    }
  );
</script>

<style lang="less" scoped></style>

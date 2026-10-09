// 短视频模板库（1:1 移植 printfilm 的全量 27 款风格模板）。
// 前端以本静态目录为唯一真相源：templateKey 与 printfilm 模板 id 对齐，生成时原样传给后端；
// 封面走本地 import，分类/比例/文案与 printfilm 保持一致。
import coverAnim3d from '@/assets/video-templates/covers/anim_3d.png';
import coverBrandClean from '@/assets/video-templates/covers/brand_clean.png';
import coverChalkWhiteboard from '@/assets/video-templates/covers/chalk_whiteboard.png';
import coverCyberNeon from '@/assets/video-templates/covers/cyber_neon.png';
import coverDocuWarm from '@/assets/video-templates/covers/docu_warm.png';
import coverEpicFantasy from '@/assets/video-templates/covers/epic_fantasy.png';
import coverFilmCinematic from '@/assets/video-templates/covers/film_cinematic.png';
import coverHuokeDouyinHook from '@/assets/video-templates/covers/huoke_douyin_hook.png';
import coverHuokeReviewFacts from '@/assets/video-templates/covers/huoke_review_facts.png';
import coverHuokeSoftInvite from '@/assets/video-templates/covers/huoke_soft_invite.png';
import coverHuokeXhsRecommend from '@/assets/video-templates/covers/huoke_xhs_recommend.png';
import coverInkGuofeng from '@/assets/video-templates/covers/ink_guofeng.png';
import coverKidsFlat from '@/assets/video-templates/covers/kids_flat.png';
import coverLiveCinematic from '@/assets/video-templates/covers/live_cinematic.png';
import coverLivePerson from '@/assets/video-templates/covers/live_person.png';
import coverLiveProductDesk from '@/assets/video-templates/covers/live_product_desk.png';
import coverLiveStreetInterview from '@/assets/video-templates/covers/live_street_interview.png';
import coverMagazineCollage from '@/assets/video-templates/covers/magazine_collage.png';
import coverNoirThriller from '@/assets/video-templates/covers/noir_thriller.png';
import coverOpensourceLiveWork from '@/assets/video-templates/covers/opensource_live_work.png';
import coverOpensourceShowcase from '@/assets/video-templates/covers/opensource_showcase.png';
import coverPhotoRealism from '@/assets/video-templates/covers/photo_realism.png';
import coverPixelRetro from '@/assets/video-templates/covers/pixel_retro.png';
import coverPortraitStory from '@/assets/video-templates/covers/portrait_story.png';
import coverRetroVhs from '@/assets/video-templates/covers/retro_vhs.png';
import coverSoftAnime from '@/assets/video-templates/covers/soft_anime.png';
import coverVoxPapercut from '@/assets/video-templates/covers/vox_papercut.png';

export interface VideoTemplateItem {
  templateKey: string;
  name: string;
  description: string;
  category: string[];
  ratio: string;
  sortOrder: number;
  cover: string;
}

export const VIDEO_TEMPLATES: VideoTemplateItem[] = [
  // 获客模板排最前：面向帮助企业做短视频获客，默认优先展示
  {
    templateKey: 'huoke_douyin_hook',
    name: '获客·抖音钩子',
    description: '竖屏口播节奏：前 3 秒钩子→场景画面→1–2 个可核验体验→询单/下单号召。适合投放获客。',
    category: ['科普', '获客', '商业'],
    ratio: '9:16',
    sortOrder: -4,
    cover: coverHuokeDouyinHook,
  },
  {
    templateKey: 'huoke_xhs_recommend',
    name: '获客·小红书安利',
    description: '竖屏闺蜜安利结构：钩子标题→第一印象→分点真实体验→推荐给谁。封面信息量高、口语化。',
    category: ['科普', '获客', '商业'],
    ratio: '9:16',
    sortOrder: -3,
    cover: coverHuokeXhsRecommend,
  },
  {
    templateKey: 'huoke_review_facts',
    name: '获客·口碑拆解',
    description: '横屏客观详实：总体评价→环境/服务→推荐项+理由→性价比→适合谁。帮别人做决策，不抒情。',
    category: ['科普', '获客', '商业'],
    ratio: '16:9',
    sortOrder: -2,
    cover: coverHuokeReviewFacts,
  },
  {
    templateKey: 'huoke_soft_invite',
    name: '获客·熟人轻推',
    description: '竖屏生活化短片：一句真实感受→一个具体细节→一句轻推荐。克制、不像广告，适合转发给熟人。',
    category: ['科普', '获客', '商业'],
    ratio: '9:16',
    sortOrder: -1,
    cover: coverHuokeSoftInvite,
  },
  {
    templateKey: 'opensource_showcase',
    name: '开源项目展示',
    description: '按项目内容动态规划：人物操作系统界面与真实使用场景，适合开源工具与平台介绍。',
    category: ['开源', '图文', '商业'],
    ratio: '9:16',
    sortOrder: 1,
    cover: coverOpensourceShowcase,
  },
  {
    templateKey: 'opensource_live_work',
    name: '真人工作场景',
    description: '真人写实工位操作：侧脸/过肩操作系统，适合开源工具与产品工作流科普。',
    category: ['开源', '真人感', '写实感'],
    ratio: '16:9',
    sortOrder: 2,
    cover: coverOpensourceLiveWork,
  },
  {
    templateKey: 'live_street_interview',
    name: '真人街访口播',
    description: '街头/通勤场景的真人出镜口播感，适合观点、体验与轻访谈科普。',
    category: ['真人感', '纪录片'],
    ratio: '9:16',
    sortOrder: 3,
    cover: coverLiveStreetInterview,
  },
  {
    templateKey: 'live_product_desk',
    name: '真人桌面演示',
    description: '桌面俯拍/斜俯写实：真人双手演示产品或笔记本流程，适合工具评测与教程。',
    category: ['真人感', '写实感', '商业'],
    ratio: '16:9',
    sortOrder: 4,
    cover: coverLiveProductDesk,
  },
  {
    templateKey: 'portrait_story',
    name: '竖屏图文故事',
    description: '竖屏插画叙事，电影感构图，适合历史人文短片。',
    category: ['图文', '电影感', '故事'],
    ratio: '9:16',
    sortOrder: 5,
    cover: coverPortraitStory,
  },
  {
    templateKey: 'anim_3d',
    name: '3D 动画',
    description: '电影级三维动画质感，圆润造型与柔和体积光，适合科普讲解与故事短片。',
    category: ['3D', '动漫', '科普'],
    ratio: '16:9',
    sortOrder: 5,
    cover: coverAnim3d,
  },
  {
    templateKey: 'live_cinematic',
    name: '真人电影感',
    description: '真人实拍电影质感，戏剧光影与浅景深，适合叙事短片。',
    category: ['电影感', '真人感'],
    ratio: '16:9',
    sortOrder: 6,
    cover: coverLiveCinematic,
  },
  {
    templateKey: 'live_person',
    name: '真人感叙事',
    description: '生活化真人出镜感，适合人物故事、口播与纪实短片。',
    category: ['真人感', '故事'],
    ratio: '9:16',
    sortOrder: 7,
    cover: coverLivePerson,
  },
  {
    templateKey: 'photo_realism',
    name: '写实摄影',
    description: '照片级写实质感，适合产品、风光与纪实科普。',
    category: ['写实感', '摄影'],
    ratio: '16:9',
    sortOrder: 7,
    cover: coverPhotoRealism,
  },
  {
    templateKey: 'film_cinematic',
    name: '电影感胶片',
    description: '宽银幕胶片质感与戏剧光影，适合叙事短片与氛围故事。',
    category: ['电影感', '胶片'],
    ratio: '16:9',
    sortOrder: 8,
    cover: coverFilmCinematic,
  },
  {
    templateKey: 'noir_thriller',
    name: '黑色悬疑',
    description: '高对比光影与冷调氛围，适合悬疑、案件与暗夜叙事。',
    category: ['悬疑', '电影感'],
    ratio: '16:9',
    sortOrder: 9,
    cover: coverNoirThriller,
  },
  {
    templateKey: 'vox_papercut',
    name: 'Vox剪纸科普',
    description: '低饱和扁平剪纸，以人物操作电脑/系统界面为主画面，适合硬核科普与产品讲解。',
    category: ['科普', '剪纸'],
    ratio: '16:9',
    sortOrder: 10,
    cover: coverVoxPapercut,
  },
  {
    templateKey: 'docu_warm',
    name: '温暖纪实',
    description: '纪实插画气质与暖色调，适合人物故事与人文纪录短片。',
    category: ['纪录片', '电影感'],
    ratio: '16:9',
    sortOrder: 12,
    cover: coverDocuWarm,
  },
  {
    templateKey: 'kids_flat',
    name: '儿童绘本扁平',
    description: '柔和配色与圆润造型，适合儿童科普与故事。',
    category: ['儿童', '绘本'],
    ratio: '16:9',
    sortOrder: 20,
    cover: coverKidsFlat,
  },
  {
    templateKey: 'soft_anime',
    name: '柔光动漫',
    description: '日系柔光赛璐璐，适合青春故事与情感短片。',
    category: ['动漫', '故事'],
    ratio: '16:9',
    sortOrder: 22,
    cover: coverSoftAnime,
  },
  {
    templateKey: 'chalk_whiteboard',
    name: '粉笔白板手绘',
    description: '黑板粉笔讲解风，突出人物操作系统/画流程图的课堂演示。',
    category: ['科普', '手绘'],
    ratio: '16:9',
    sortOrder: 30,
    cover: coverChalkWhiteboard,
  },
  {
    templateKey: 'cyber_neon',
    name: '赛博霓虹',
    description: '霓虹夜城与未来感，适合科技、都市与科幻话题。',
    category: ['科幻', '赛博'],
    ratio: '16:9',
    sortOrder: 35,
    cover: coverCyberNeon,
  },
  {
    templateKey: 'epic_fantasy',
    name: '奇幻史诗',
    description: '宏大场景与奇幻光影，适合神话、冒险与世界观短片。',
    category: ['奇幻', '电影感'],
    ratio: '16:9',
    sortOrder: 38,
    cover: coverEpicFantasy,
  },
  {
    templateKey: 'magazine_collage',
    name: '杂志拼贴',
    description: '剪报拼贴与印刷纹理，适合文化话题与品牌故事。',
    category: ['商业', '拼贴'],
    ratio: '9:16',
    sortOrder: 40,
    cover: coverMagazineCollage,
  },
  {
    templateKey: 'brand_clean',
    name: '极简品牌',
    description: '干净色块与强留白，适合产品解说与品牌短片。',
    category: ['商业', '极简'],
    ratio: '9:16',
    sortOrder: 42,
    cover: coverBrandClean,
  },
  {
    templateKey: 'pixel_retro',
    name: '像素复古科普',
    description: '8-bit/16-bit 像素风，适合科技史与游戏化讲解。',
    category: ['复古', '像素'],
    ratio: '16:9',
    sortOrder: 50,
    cover: coverPixelRetro,
  },
  {
    templateKey: 'retro_vhs',
    name: '复古 VHS',
    description: '磁带录像与扫描线质感，适合怀旧故事与年代感内容。',
    category: ['复古', '电影感'],
    ratio: '16:9',
    sortOrder: 52,
    cover: coverRetroVhs,
  },
  {
    templateKey: 'ink_guofeng',
    name: '水墨国风',
    description: '水墨留白与写意笔触，适合历史与文化短故事。',
    category: ['国风', '水墨'],
    ratio: '9:16',
    sortOrder: 60,
    cover: coverInkGuofeng,
  },
];

// 分类排序（与 printfilm lib/categories.ts 一致）
export const VIDEO_CATEGORY_ORDER = [
  '开源',
  '科普',
  '获客',
  '纪录片',
  '写实感',
  '真人感',
  '电影感',
  '儿童',
  '动漫',
  '国风',
  '科幻',
  '奇幻',
  '悬疑',
  '商业',
  '复古',
  '图文',
];

export interface Inspiration {
  title: string;
  theme: string;
  script: string;
}

// 灵感示例池（与 printfilm CreateProjectPage INSPIRATION_POOL 一致）
export const INSPIRATION_POOL: Inspiration[] = [
  {
    title: '工厂探厂短视频',
    theme: '把一家小工厂的拳头产品做成抖音获客口播：前3秒钩子、生产场景、1-2个可核验的工艺点、询单号召。只写已知事实。',
    script:
      '这家厂子不大，但一个产品做了很多年。\n\n' +
      '车间不算豪华，工序清楚，原材料都摆在明处。\n\n' +
      '我看了他们主打那一款，用料和做工按我自己的标准来：扎实、不花哨。\n\n' +
      '有合作意向的话，按他们现有规格和报价自己核实，别听绝对化承诺。',
  },
  {
    title: '产品种草一篇笔记',
    theme: '小红书获客安利：钩子标题、第一印象、分点真实体验、推荐给谁。卖点只作客观陈述。',
    script:
      '本来只是随手试用，结果用下来很顺。\n\n' +
      '第一印象是包装干净、说明清楚。\n\n' +
      '我重点试了核心功能，效果如实说；操作简单，上手快。\n\n' +
      '更适合追求省心的人；预算有限的可以先看基础版再决定。',
  },
  {
    title: '客户案例拆解',
    theme: '点评式获客讲解：总体评价、服务流程、推荐项带理由、性价比、适合谁。没写的价格不要猜。',
    script:
      '总体感受：流程清楚、响应及时，适合第一次合作的人。\n\n' +
      '对接顺畅，负责人会主动说明怎么推进。\n\n' +
      '推荐他们的核心服务，理由是我们实际用过、步骤好懂。\n\n' +
      '报价以官方公示为准。团队一起用比一个人更划算。',
  },
  {
    title: '熟人圈轻推荐',
    theme: '朋友圈获客短片：一句真实感受、一个具体细节、一句轻推荐。克制，不像广告。',
    script:
      '最近接触了一家本地服务，比想象中靠谱。\n\n' +
      '他们做事情有条理，进度看得见。\n\n' +
      '你们要是有需要，可以自己去问问。',
  },
  {
    title: '黑洞是如何形成的',
    theme: '黑洞是如何形成的？用通俗方式讲清恒星坍缩、事件视界与时空弯曲，面向中学生。',
    script:
      '夜空里最神秘的天体之一，就是黑洞。\n\n' +
      '当一颗足够大的恒星燃料烧尽，核心会在引力下剧烈坍缩，密度高到连光都逃不出去，事件视界就此诞生。\n\n' +
      '它不是宇宙吸尘器，而是时空被严重弯曲的区域。靠近它，时间流逝也会变得奇怪。\n\n' +
      '记住：质量够大、坍缩够猛，黑洞就会出现。下一次看科普新闻，你就能分清传说与科学。',
  },
  {
    title: '为什么天空是蓝色的',
    theme: '为什么天空是蓝色的？用瑞利散射解释阳光、空气分子与傍晚火烧云，适合科普入门。',
    script:
      '抬头一看，白天的天空常常是蓝的，这是巧合吗？\n\n' +
      '阳光看起来发白，其实包含多种颜色。空气分子对蓝光散射更强，蓝光更容易被「弹」向四面八方，于是我们眼里的天空偏蓝。\n\n' +
      '早晚太阳更低，光穿过更厚大气，蓝光散得更干净，剩下的红橙光就染红了天边。\n\n' +
      '所以天空的颜色，是光与空气的一场合作。',
  },
  {
    title: 'AI 如何改变生活',
    theme: 'AI 如何改变生活：从推荐、语音助手到医疗影像，讲清便利与需要警惕的偏见。',
    script:
      '打开手机，推荐视频、导航路线、语音助手——人工智能已经悄悄进了日常。\n\n' +
      '它擅长从海量数据里找规律：帮医生看影像线索，帮工厂预判故障，也帮你把搜索变成对话。\n\n' +
      '但 AI 不是魔法。数据有偏见，模型会犯错，隐私也需要边界。真正有用的，是把 AI 当工具，而不是当权威。\n\n' +
      '理解它能做什么、不能做什么，你才能用得更聪明。',
  },
  {
    title: '火星上的一天',
    theme: '火星上的一天长什么样？对比地球日长、气温、沙尘与人类基地想象，做成场景化科普。',
    script:
      '想象你在火星醒来：太阳更远更小，天空偏奶油色，一天大约 24 小时 39 分钟。\n\n' +
      '白天可能「温暖」到零下，夜晚更冷。薄薄的二氧化碳大气留不住热量，沙尘暴偶尔遮天。\n\n' +
      '科学家仍在规划基地：要防辐射、造氧气、种食物。火星不是第二地球，却是最近的外太空课堂。\n\n' +
      '了解火星的一天，就是在预习人类下一次远行。',
  },
  {
    title: '梦的力量',
    theme: '梦的力量：睡眠周期、快速眼动与记忆整理，用故事讲清做梦如何帮助大脑「复盘」。',
    script:
      '你睡着以后，大脑并没有下班。\n\n' +
      '进入快速眼动睡眠时，大脑像在回放白天的片段，拼接成奇幻的梦。科学家认为，这有助于整理记忆、调节情绪。\n\n' +
      '睡不够，注意力和创造力都会打折；规律睡眠，则像给大脑做夜间维护。\n\n' +
      '下次做怪梦，别只觉得荒唐——那可能是大脑在加班学习。',
  },
  {
    title: '一只流浪猫的春天',
    theme: '一只流浪猫的春天：用拟人旁白讲城市生态、投喂边界与人宠共处，温暖向科普叙事。',
    script:
      '春天来了，巷口的橘猫开始换毛，也开始寻找更安全的角落。\n\n' +
      '城市里的流浪动物，靠的是残存野性与人类不经意的善意。科学投喂、绝育与尊重距离，比冲动更重要。\n\n' +
      '它们不是风景，也不是麻烦，而是城市生态的一部分。\n\n' +
      '这个春天，愿每一只猫，都能遇见更稳妥的明天。',
  },
  {
    title: '光合作用的秘密',
    theme: '光合作用的秘密：叶子如何把阳光变成糖，讲清叶绿体、能量转化与地球氧气来源。',
    script:
      '绿叶不只是装饰，它们是地球上最安静的化工厂。\n\n' +
      '叶绿体抓住阳光，把水和二氧化碳做成糖，并释放氧气。没有这个过程，绝大多数食物链都会断裂。\n\n' +
      '你呼吸的氧气、餐桌上的米饭和蔬菜，都间接来自这场光的魔法。\n\n' +
      '看懂光合作用，就看懂了生命运转的底层账本。',
  },
  {
    title: '地震来了怎么办',
    theme: '地震来了怎么办：用场景教学讲清震前准备、避险姿势与谣言辨别，实用安全科普。',
    script:
      '地面突然晃起来，第一反应往往是慌。\n\n' +
      '正确做法是就近伏地、掩护、抓稳，远离窗户与高架物；不要一窝蜂挤电梯。提前准备应急包，比临时抱佛脚更管用。\n\n' +
      '震后还要警惕余震和谣言。权威信息、互助秩序，才是真正的安全感。\n\n' +
      '懂一点地震知识，关键时刻就能多一分从容。',
  },
  {
    title: '咖啡因如何提神',
    theme: '咖啡因如何提神：腺苷受体、耐受与睡眠代价，帮上班族科学喝咖啡。',
    script:
      '困的时候来杯咖啡，真的是「叫醒大脑」吗？\n\n' +
      '咖啡因会抢占腺苷的位置，让你暂时感觉不困。但它不能替代睡眠，下午喝太多，夜里更容易翻来覆去。\n\n' +
      '逐渐耐受后，同样一杯效果会变弱。更聪明的用法是：需要专注时再用，并给睡眠留窗口。\n\n' +
      '提神可以靠咖啡，恢复还是得靠睡。',
  },
  {
    title: '塑料去哪了',
    theme: '塑料去哪了：微塑料、海洋环流与可替代材料，做成环保主题科普短片。',
    script:
      '扔掉的塑料袋，真的消失了吗？\n\n' +
      '多数只是被撕成更小的碎片。微塑料进入河流与海洋，再进入食物链，最终可能回到我们的餐桌。\n\n' +
      '减少一次性塑料、做好分类、推动更好的材料，比事后清理便宜得多。\n\n' +
      '问「塑料去哪了」，其实是在问：我们愿为未来留什么。',
  },
  {
    title: '疫苗为什么有效',
    theme: '疫苗为什么有效：用「预演」比喻讲清抗原、抗体与群体免疫，澄清常见误解。',
    script:
      '疫苗不是药，更像给免疫系统发的预告片。\n\n' +
      '它让身体提前认识病原体的关键特征，真正遇到时能更快调动抗体。这不是改写基因，而是训练记忆。\n\n' +
      '足够多人获得保护，传播链会被削弱，这就是群体免疫的意义。\n\n' +
      '科学接种，是把自己和身边人一起放进更安全的网络。',
  },
  {
    title: '潮汐的涨落',
    theme: '潮汐为何涨落：月球引力、离心效应与大潮小潮，海边场景化讲解。',
    script:
      '海边的人最懂：水位会按时涨、按时退。\n\n' +
      '主要推手是月球的引力，太阳也来凑热闹。地球、月球、太阳排成一线时，潮差更大，就是大潮。\n\n' +
      '潮汐还影响航行、发电甚至生物节律。抬头看月亮，脚下的海也在回应。\n\n' +
      '涨落之间，是天地引力的可见痕迹。',
  },
];

/** 按分类整理模板：热门推荐 = sort_order 升序前 8，其余按 VIDEO_CATEGORY_ORDER 过滤 */
export function videoTemplateCategories(templates: VideoTemplateItem[]): string[] {
  const found = new Set<string>();
  templates.forEach((tpl) => {
    tpl.category.forEach((c) => {
      if (VIDEO_CATEGORY_ORDER.includes(c)) {
        found.add(c);
      }
    });
  });
  return ['全部', '热门推荐', ...VIDEO_CATEGORY_ORDER.filter((c) => found.has(c))];
}

/** 从内容第一行推导项目名称（与 printfilm deriveTitle 一致） */
export function deriveTitle(text: string): string {
  const line = text
    .trim()
    .split(/\n/)[0]
    .replace(/["""'']/g, '')
    .replace(/[。！？!？:：].*$/, '')
    .trim();
  if (!line) {
    return '未命名作品';
  }
  return line.slice(0, 18);
}

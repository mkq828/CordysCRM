package cn.cordys.crm.ai.drama.service;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * 漫剧内置画面风格：风格 ID、展示名、生图提示词片段。
 * 与前端 {@code views/drama/templates.ts} 的 IMAGE_STYLE_IDS 对齐（移植 printfilm image_styles.py）。
 */
public final class DramaImageStyles {

    private DramaImageStyles() {
    }

    /** 内置风格 ID（与前端保持一致） */
    private static final Set<String> IDS = Set.of(
            "retro-sci-fi-atompunk",
            "palace-intrigue-cold",
            "domestic-suspense-cold",
            "ancient-romance-soft",
            "ancient-chinese-mythology",
            "japanese-youth-film",
            "japanese-daily-natural",
            "korean-urban-soft",
            "chinese-urban-realistic",
            "wuxia-realistic-photo",
            "90s-realistic-film",
            "retro-narrative-film",
            "american-retro-hollywood",
            "neon-cyberpunk-film",
            "90s-rural-china-film",
            "cgi-3d-animation",
            "ghibli-handdrawn-anime",
            "tezuka-era-cartoon",
            "shanghai-animation",
            "pixel-art",
            "shadow-puppet-illustration");

    /** 风格展示名 */
    private static final Map<String, String> LABELS = Map.ofEntries(
            Map.entry("retro-sci-fi-atompunk", "复古科幻原子朋克"),
            Map.entry("palace-intrigue-cold", "宫斗权谋冷峻"),
            Map.entry("domestic-suspense-cold", "国产悬疑冷调"),
            Map.entry("ancient-romance-soft", "古偶唯美柔光"),
            Map.entry("ancient-chinese-mythology", "中国古代神话史诗"),
            Map.entry("japanese-youth-film", "日式青春胶片"),
            Map.entry("japanese-daily-natural", "日式生活自然"),
            Map.entry("korean-urban-soft", "韩剧都市柔光"),
            Map.entry("chinese-urban-realistic", "国产都市写实"),
            Map.entry("wuxia-realistic-photo", "武侠江湖写实摄影"),
            Map.entry("90s-realistic-film", "90年代写实电影"),
            Map.entry("retro-narrative-film", "复古叙事电影"),
            Map.entry("american-retro-hollywood", "美式复古好莱坞"),
            Map.entry("neon-cyberpunk-film", "霓虹赛博电影"),
            Map.entry("90s-rural-china-film", "90年代中国农村电影"),
            Map.entry("cgi-3d-animation", "3D 动画"),
            Map.entry("ghibli-handdrawn-anime", "宫崎骏气质手绘"),
            Map.entry("tezuka-era-cartoon", "手冢治虫时代卡通画风"),
            Map.entry("shanghai-animation", "上美画风"),
            Map.entry("pixel-art", "像素风"),
            Map.entry("shadow-puppet-illustration", "皮影戏插画"));

    /** 风格 → 生图提示词片段 */
    private static final Map<String, String> PROMPTS = Map.ofEntries(
            Map.entry("retro-sci-fi-atompunk",
                    "复古科幻原子朋克风格，1950年代未来主义美学，流线型金属与原子能符号，霓虹高光，金属质感，高对比色彩，轻微胶片颗粒"),
            Map.entry("palace-intrigue-cold",
                    "中国宫廷权谋题材冷峻风格，低饱和暗调，克制光影，庄重构图，华贵但压抑的宫廷氛围，硬朗轮廓，戏剧化侧光"),
            Map.entry("domestic-suspense-cold",
                    "国产悬疑影视冷调风格，偏青灰色调，低调光，阴影浓重，写实摄影质感，紧张压抑氛围，细节丰富"),
            Map.entry("ancient-romance-soft",
                    "中国古代偶像剧唯美柔光风格，梦幻柔焦，暖色薄纱光晕，精致古装妆造，背景虚化，浪漫飘逸氛围"),
            Map.entry("ancient-chinese-mythology",
                    "中国古代神话史诗风格，上古洪荒气质，苍茫山河与云雾神光，青铜礼器与粗纻麻衣质感，水墨青绿与矿物颜料色调，庄严神圣，史诗大场面，电影级光影，忌现代偶像剧柔光与甜宠滤镜"),
            Map.entry("japanese-youth-film",
                    "日式青春题材胶片摄影风格，柯达胶片色调，自然阳光，浅景深，细腻颗粒，青涩真挚的日常氛围"),
            Map.entry("japanese-daily-natural",
                    "日式生活纪录片自然光影风格，柔和自然光，低对比，真实日常场景，安静治愈，轻微胶片质感"),
            Map.entry("korean-urban-soft", "韩剧都市题材柔光风格，暖色滤镜，通透肤质，都市背景虚化，浪漫温柔灯光氛围"),
            Map.entry("chinese-urban-realistic",
                    "国产都市现实题材写实摄影风格，自然光，真实生活场景，中性色调，细节锐利，无过度美化"),
            Map.entry("wuxia-realistic-photo",
                    "中国武侠江湖题材写实摄影风格，自然光影，真实地形与服饰质感，动态构图，江湖氛围，电影级景深"),
            Map.entry("90s-realistic-film",
                    "1990年代写实电影风格，胶片质感，自然肤色，时代感服装与环境，柔和对比，怀旧色调"),
            Map.entry("retro-narrative-film",
                    "复古叙事电影风格，经典电影构图，胶片色彩分级，富有故事感的场景调度，电影级布光"),
            Map.entry("american-retro-hollywood",
                    "美式复古好莱坞黄金年代风格，高对比布光，暖调彩色或经典黑白，明星质感，华丽景深"),
            Map.entry("neon-cyberpunk-film",
                    "霓虹赛博朋克电影风格，蓝紫霓虹灯光，雨夜反射，高对比，未来都市，烟雾与全息感光效"),
            Map.entry("90s-rural-china-film",
                    "1990年代中国农村题材电影风格，自然光，土黄与绿色调，粗糙真实质感，乡土生活氛围"),
            Map.entry("cgi-3d-animation",
                    "电影级三维 CGI 动画风格，皮克斯/梦工厂气质，圆润造型与清晰轮廓，柔和体积光与次表面散射，干净材质与饱和配色，浅景深，非写实摄影、非日系赛璐璐平面、非剪纸扁平"),
            Map.entry("ghibli-handdrawn-anime",
                    "宫崎骏风格手绘动画质感，写实人体比例与朴素真实的五官，平滑干净的上色与清晰线条，柔和自然光，生活化场景与服饰，非照片写实、非真人摄影、非3D CGI、非像素风、非剪纸扁平"),
            Map.entry("tezuka-era-cartoon", "手冢治虫时代经典日式卡通画风，简洁线条，复古动画平涂着色，怀旧动画质感"),
            Map.entry("shanghai-animation",
                    "上海美术电影制片厂经典动画画风，中国民族绘画韵味，水彩与工笔结合，诗意唯美，传统色彩"),
            Map.entry("pixel-art", "像素艺术风格，清晰像素块，复古游戏美学，有限色板，8-bit 或 16-bit 质感"),
            Map.entry("shadow-puppet-illustration",
                    "中国皮影戏插画画风，剪影轮廓，镂空纹理，暖色背光，民间艺术装饰性，层叠投影效果"));

    /** 非写实画风（国漫/二次元/动画/插画/像素/皮影）：角色参考图只做五官特征参考，不强制真人照片质感 */
    private static final Set<String> NON_PHOTOREAL_STYLES = Set.of(
            "cgi-3d-animation",
            "ghibli-handdrawn-anime",
            "tezuka-era-cartoon",
            "shanghai-animation",
            "pixel-art",
            "shadow-puppet-illustration");

    /** 是否为内置风格 ID */
    public static boolean isValid(String styleId) {
        return StringUtils.isNotBlank(styleId) && IDS.contains(styleId.trim());
    }

    /** 是否为「明确写实」画风：非空且不在非写实清单内。空（未选风格）默认按半写实国漫处理，返回 false */
    public static boolean isExplicitPhotoreal(String styleId) {
        return StringUtils.isNotBlank(styleId) && !NON_PHOTOREAL_STYLES.contains(styleId.trim());
    }

    /** 展示名，未知 ID 原样返回 */
    public static String label(String styleId) {
        if (StringUtils.isBlank(styleId)) {
            return "";
        }
        return LABELS.getOrDefault(styleId.trim(), styleId.trim());
    }

    /** 默认画面风格（未选风格时）：现代半写实 CG，像真人但非照片，规避 Seedance 真人脸风控 */
    private static final String DEFAULT_STYLE_PROMPT =
            "现代半写实 CG 数字绘画，写实人体比例与真实面部结构，五官精致立体、眉目清晰，"
                    + "细腻平滑的皮肤质感与自然肤色，柔和立体光影，画面干净现代、细节丰富，"
                    + "非照片写实、非真人摄影、无皮肤毛孔特写、无相机景深与镜头光斑";

    /** 生图提示词片段：空返回默认半写实国漫；未知 ID 返回空串 */
    public static String prompt(String styleId) {
        if (StringUtils.isBlank(styleId)) {
            return DEFAULT_STYLE_PROMPT;
        }
        return PROMPTS.getOrDefault(styleId.trim(), "");
    }
}

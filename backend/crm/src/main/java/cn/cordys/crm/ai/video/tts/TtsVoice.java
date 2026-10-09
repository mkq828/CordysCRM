package cn.cordys.crm.ai.video.tts;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS 音色选项：value 为音色 ID（传给合成接口），label 为前端下拉展示名。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TtsVoice {

    private String value;

    private String label;

    /** 音色性别：female/male，前端卡片「女声/男声」标识用；可空 */
    private String gender;
}

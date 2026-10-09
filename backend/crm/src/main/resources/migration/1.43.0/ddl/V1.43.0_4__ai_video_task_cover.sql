ALTER TABLE ai_video_task ADD COLUMN cover_attachment_id VARCHAR(64) NULL COMMENT '封面附件ID(可更换/用首镜，空则回退模板封面)' AFTER attachment_id;

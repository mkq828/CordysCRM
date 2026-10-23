ALTER TABLE ai_knowledge_doc
    ADD COLUMN sort_no INT NULL COMMENT '排序号，填数字按升序排前，NULL 排最后' AFTER error_msg,
    ADD COLUMN attachment_id VARCHAR(64) NULL COMMENT '原文件附件ID（sys_attachment.id），用于在线查看' AFTER sort_no;

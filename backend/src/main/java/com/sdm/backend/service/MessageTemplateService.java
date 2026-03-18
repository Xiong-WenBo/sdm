package com.sdm.backend.service;

import com.sdm.backend.entity.MessageTemplate;
import com.sdm.backend.mapper.MessageTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MessageTemplateService {

    @Autowired
    private MessageTemplateMapper messageTemplateMapper;

    public List<MessageTemplate> findAll() {
        return messageTemplateMapper.findAll();
    }

    public List<MessageTemplate> findEnabledTemplates() {
        return messageTemplateMapper.findByEnabled(1);
    }

    public List<MessageTemplate> findByType(String type) {
        return messageTemplateMapper.findByType(type);
    }

    public MessageTemplate findByTemplateCode(String templateCode) {
        return messageTemplateMapper.findByTemplateCode(templateCode);
    }

    public MessageTemplate findById(Long id) {
        return messageTemplateMapper.findById(id);
    }

    @Transactional
    public int insert(MessageTemplate template) {
        return messageTemplateMapper.insert(template);
    }

    @Transactional
    public int update(MessageTemplate template) {
        return messageTemplateMapper.update(template);
    }

    @Transactional
    public int deleteById(Long id) {
        return messageTemplateMapper.deleteById(id);
    }

    @Transactional
    public int updateEnabled(Long id, Integer enabled) {
        return messageTemplateMapper.updateEnabled(id, enabled);
    }

    /**
     * 渲染消息模板
     * @param template 模板对象
     * @param params 参数 Map
     * @return 渲染后的消息内容 [title, content]
     */
    public String[] renderTemplate(MessageTemplate template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }

        String title = renderText(template.getTitleTemplate(), params);
        String content = renderText(template.getContentTemplate(), params);

        return new String[]{title, content};
    }

    /**
     * 渲染文本中的占位符
     * @param text 模板文本
     * @param params 参数 Map
     * @return 渲染后的文本
     */
    private String renderText(String text, Map<String, Object> params) {
        if (text == null || params == null || params.isEmpty()) {
            return text;
        }

        // 匹配 {key} 格式的占位符
        Pattern pattern = Pattern.compile("\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(text);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 使用模板代码发送消息
     * @param templateCode 模板代码
     * @param params 参数
     * @return 渲染后的标题和内容
     */
    public String[] renderByTemplateCode(String templateCode, Map<String, Object> params) {
        MessageTemplate template = findByTemplateCode(templateCode);
        if (template == null || template.getEnabled() != 1) {
            return null;
        }
        return renderTemplate(template, params);
    }
}

package com.sdm.backend.controller;

import com.sdm.backend.annotation.Log;
import com.sdm.backend.dto.Result;
import com.sdm.backend.entity.MessageTemplate;
import com.sdm.backend.service.MessageTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/message/template")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class MessageTemplateController {

    @Autowired
    private MessageTemplateService messageTemplateService;

    @GetMapping("/list")
    public ResponseEntity<Result<List<MessageTemplate>>> getTemplateList(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer enabled
    ) {
        List<MessageTemplate> templates;
        
        if (type != null && !type.isEmpty()) {
            templates = messageTemplateService.findByType(type);
        } else if (enabled != null) {
            templates = messageTemplateService.findEnabledTemplates();
        } else {
            templates = messageTemplateService.findAll();
        }
        
        return ResponseEntity.ok(Result.success(templates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<MessageTemplate>> getTemplateById(@PathVariable Long id) {
        MessageTemplate template = messageTemplateService.findById(id);
        if (template == null) {
            return ResponseEntity.ok(Result.error(404, "模板不存在"));
        }
        return ResponseEntity.ok(Result.success(template));
    }

    @GetMapping("/code/{templateCode}")
    public ResponseEntity<Result<MessageTemplate>> getTemplateByCode(@PathVariable String templateCode) {
        MessageTemplate template = messageTemplateService.findByTemplateCode(templateCode);
        if (template == null) {
            return ResponseEntity.ok(Result.error(404, "模板不存在"));
        }
        return ResponseEntity.ok(Result.success(template));
    }

    @PostMapping
    @Log(module = "MESSAGE_TEMPLATE", operation = "CREATE", description = "创建消息模板")
    public ResponseEntity<Result<Void>> createTemplate(@RequestBody MessageTemplate template) {
        if (template.getTemplateCode() == null || template.getTemplateCode().isEmpty()) {
            return ResponseEntity.ok(Result.error(400, "模板编码不能为空"));
        }
        
        // 检查模板编码是否已存在
        MessageTemplate existing = messageTemplateService.findByTemplateCode(template.getTemplateCode());
        if (existing != null) {
            return ResponseEntity.ok(Result.error(400, "模板编码已存在"));
        }
        
        messageTemplateService.insert(template);
        return ResponseEntity.ok(Result.success(null, "模板创建成功"));
    }

    @PutMapping("/{id}")
    @Log(module = "MESSAGE_TEMPLATE", operation = "UPDATE", description = "更新消息模板")
    public ResponseEntity<Result<Void>> updateTemplate(@PathVariable Long id, @RequestBody MessageTemplate template) {
        MessageTemplate existing = messageTemplateService.findById(id);
        if (existing == null) {
            return ResponseEntity.ok(Result.error(404, "模板不存在"));
        }
        
        template.setId(id);
        messageTemplateService.update(template);
        return ResponseEntity.ok(Result.success(null, "模板更新成功"));
    }

    @DeleteMapping("/{id}")
    @Log(module = "MESSAGE_TEMPLATE", operation = "DELETE", description = "删除消息模板")
    public ResponseEntity<Result<Void>> deleteTemplate(@PathVariable Long id) {
        MessageTemplate template = messageTemplateService.findById(id);
        if (template == null) {
            return ResponseEntity.ok(Result.error(404, "模板不存在"));
        }
        
        messageTemplateService.deleteById(id);
        return ResponseEntity.ok(Result.success(null, "模板删除成功"));
    }

    @PutMapping("/{id}/enabled")
    @Log(module = "MESSAGE_TEMPLATE", operation = "UPDATE", description = "修改模板启用状态")
    public ResponseEntity<Result<Void>> updateTemplateEnabled(
            @PathVariable Long id,
            @RequestParam Integer enabled
    ) {
        MessageTemplate template = messageTemplateService.findById(id);
        if (template == null) {
            return ResponseEntity.ok(Result.error(404, "模板不存在"));
        }
        
        messageTemplateService.updateEnabled(id, enabled);
        return ResponseEntity.ok(Result.success(null, "状态更新成功"));
    }

    @PostMapping("/render")
    public ResponseEntity<Result<Map<String, String>>> renderTemplate(
            @RequestBody Map<String, Object> params
    ) {
        try {
            String templateCode = (String) params.get("templateCode");
            Map<String, Object> templateParams = (Map<String, Object>) params.get("templateParams");
            
            String[] rendered = messageTemplateService.renderByTemplateCode(templateCode, templateParams);
            if (rendered == null) {
                return ResponseEntity.ok(Result.error(404, "模板不存在或已禁用"));
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("title", rendered[0]);
            result.put("content", rendered[1]);
            
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error(500, "渲染失败：" + e.getMessage()));
        }
    }
}

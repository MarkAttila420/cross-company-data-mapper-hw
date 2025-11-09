package com.example.mapping_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.mapping_service.model.FieldMapping;
import com.example.mapping_service.model.MappingRequest;
import com.example.mapping_service.model.MappingResponse;
import com.example.mapping_service.model.TransformRequest;
import com.example.mapping_service.model.TransformResponse;
import com.example.mapping_service.service.MappingService;
import com.example.mapping_service.service.GeminiAIService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/mapping")
public class MappingController {

    @Autowired
    private MappingService mappingService;

    @Autowired
    private GeminiAIService geminiAIService;

    // Simple in-memory templates store for the POC
    private final List<MappingResponse> templates = Collections.synchronizedList(new ArrayList<>());

    @PostMapping("/suggest")
    public ResponseEntity<MappingResponse> suggest(@RequestBody MappingRequest request) {
        try {
            List<FieldMapping> mappings = geminiAIService.generateMappings(request.getSourceFormat(), request.getTargetFormat());
            MappingResponse resp = new MappingResponse(mappings);
            // store template for demo purposes
            templates.add(resp);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/transform")
    public ResponseEntity<TransformResponse> transform(@RequestBody TransformRequest request) {
        try {
            // apply mappings to source data
            var transformed = mappingService.applyMappings(request.getSourceData(), request.getMappings());
            TransformResponse resp = new TransformResponse(transformed);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/templates")
    public ResponseEntity<List<MappingResponse>> getTemplates() {
        return ResponseEntity.ok(new ArrayList<>(templates));
    }
}
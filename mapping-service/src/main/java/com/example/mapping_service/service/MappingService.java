package com.example.mapping_service.service;

import com.example.mapping_service.model.FieldMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MappingService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Apply a list of field mappings to the provided source data and return a transformed map.
	 */
	public Map<String, Object> applyMappings(Map<String, Object> sourceData, List<FieldMapping> mappings) {
		Map<String, Object> result = new HashMap<>();

		if (mappings == null || mappings.isEmpty() || sourceData == null) {
			return result;
		}

		for (FieldMapping fm : mappings) {
			try {
				Object raw = getValueByPath(sourceData, fm.getSourcePath());
				if (raw == null) continue;

				Object transformed = applyTransformation(raw, fm.getTransformationType(), fm.getTargetPath());

				// If split_name and targetPath refers to a parent (not ending with firstName/lastName),
				// apply both fields under that target path.
				if ("split_name".equalsIgnoreCase(fm.getTransformationType()) &&
						!(fm.getTargetPath().endsWith("firstName") || fm.getTargetPath().endsWith("lastName"))) {
					// transformed expected to be a Map with firstName/lastName
					if (transformed instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> nameParts = (Map<String, Object>) transformed;
						for (Map.Entry<String, Object> e : nameParts.entrySet()) {
							setValueByPath(result, fm.getTargetPath() + "." + e.getKey(), e.getValue());
						}
					}
				} else {
					setValueByPath(result, fm.getTargetPath(), transformed);
				}
			} catch (Exception e) {
				// For POC, swallow and continue; in production log/propagate as needed
				e.printStackTrace();
			}
		}

		return result;
	}

	private Object applyTransformation(Object raw, String transformationType, String targetPath) {
		if (transformationType == null || transformationType.isEmpty() || "none".equalsIgnoreCase(transformationType)) {
			return raw;
		}

		String t = transformationType.toLowerCase(Locale.ROOT);

		switch (t) {
			case "date_format":
				if (raw instanceof String) {
					return convertDateFormat((String) raw);
				}
				return raw;
			case "split_name":
				if (raw instanceof String) {
					return splitName((String) raw, targetPath);
				}
				return raw;
			case "phone_format":
				if (raw instanceof String) {
					return convertPhone((String) raw);
				}
				return raw;
			default:
				// unknown transformation, return raw
				return raw;
		}
	}

	private String convertDateFormat(String input) {
		// Try yyyy-MM-dd -> dd/MM/yyyy
		try {
			SimpleDateFormat src = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat dst = new SimpleDateFormat("dd/MM/yyyy");
			Date d = src.parse(input);
			return dst.format(d);
		} catch (ParseException ignored) {
		}

		// If input already in dd/MM/yyyy, return as-is
		return input;
	}

	private Map<String, Object> splitName(String input, String targetPath) {
		// Basic split on whitespace. For Hungarian names like "Nagy János" assume Last First
		String[] parts = input.trim().split("\\s+", 2);
		String last = parts.length > 0 ? parts[0] : "";
		String first = parts.length > 1 ? parts[1] : "";

		Map<String, Object> m = new HashMap<>();
		m.put("firstName", first);
		m.put("lastName", last);
		return m;
	}

	private String convertPhone(String input) {
		// Example: +36301234567 -> 06301234567
		String s = input.trim();
		if (s.startsWith("+36")) {
			s = s.substring(3);
			if (!s.startsWith("0")) {
				s = "0" + s;
			}
			return s;
		}

		// If it starts with country code like +, try to remove leading + and country code
		if (s.startsWith("+")) {
			// remove + and keep rest
			s = s.substring(1);
			// ensure leading 0
			if (!s.startsWith("0")) s = "0" + s;
			return s;
		}

		return s;
	}

	@SuppressWarnings("unchecked")
	private Object getValueByPath(Map<String, Object> data, String path) {
		if (data == null || path == null || path.isEmpty()) return null;
		String[] parts = path.split("\\.");
		Object current = data;
		for (String p : parts) {
			if (!(current instanceof Map)) return null;
			Map<String, Object> m = (Map<String, Object>) current;
			if (!m.containsKey(p)) return null;
			current = m.get(p);
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private void setValueByPath(Map<String, Object> data, String path, Object value) {
		if (data == null || path == null || path.isEmpty()) return;
		String[] parts = path.split("\\.");
		Map<String, Object> current = data;
		for (int i = 0; i < parts.length; i++) {
			String p = parts[i];
			if (i == parts.length - 1) {
				current.put(p, value);
				return;
			}
			Object next = current.get(p);
			if (!(next instanceof Map)) {
				Map<String, Object> nm = new HashMap<>();
				current.put(p, nm);
				current = nm;
			} else {
				current = (Map<String, Object>) next;
			}
		}
	}
}

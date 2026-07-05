package com.back.job.dto;

import java.util.List;

public record BatchCreateResponse(String batchId, List<String> jobIds) {
}

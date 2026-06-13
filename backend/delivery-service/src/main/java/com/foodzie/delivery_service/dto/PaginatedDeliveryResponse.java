package com.foodzie.delivery_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated response for deliveries or earnings history
 */
@Data
@Builder
public class PaginatedDeliveryResponse {
    private List<DeliveryHistoryResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isFirst;
    private boolean isLast;
}

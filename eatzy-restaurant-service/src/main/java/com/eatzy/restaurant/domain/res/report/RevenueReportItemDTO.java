package com.eatzy.restaurant.domain.res.report;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportItemDTO {
    private LocalDate date;
    private BigDecimal foodRevenue;      // Doanh thu từ món ăn (subtotal)
    private BigDecimal deliveryFee;      // Phí giao hàng
    private BigDecimal discountAmount;   // Số tiền giảm giá
    private BigDecimal commissionAmount; // Hoa hồng platform
    private BigDecimal netRevenue;       // Doanh thu thực nhận
    private Integer totalOrders;         // Số đơn hàng
}

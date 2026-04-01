package com.eatzy.restaurant.designpattern.strategy;

import java.math.BigDecimal;

/**
 * ★ DESIGN PATTERN #4: Strategy Pattern
 * 
 * Interface cho phep doi thuat toan tinh hoa hong (Commission) lien hoat luc runtime.
 * Moi loai nha hang (Standard, Premium, Free-trial) se co 1 cach tinh hoa hong rieng.
 */
public interface CommissionStrategy {
    /**
     * Tinh phi hoa hong nen tang thu tu nha hang dua tren tong doanh thu.
     * 
     * @param totalRevenue Tong doanh thu cua don hang
     * @return So tien hoa hong phai tra cho nen tang
     */
    BigDecimal calculateCommission(BigDecimal totalRevenue);

    /**
     * Ten cua Strategy (de log va hien thi)
     */
    String getStrategyName();
}

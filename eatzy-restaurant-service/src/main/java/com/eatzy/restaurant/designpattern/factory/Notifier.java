package com.eatzy.restaurant.designpattern.factory;

/**
 * ★ DESIGN PATTERN #2: Factory Method Pattern
 * 
 * Interface chung cho tat ca cac kenh gui thong bao.
 * Factory se quyet dinh tra ve EmailNotifier hay SmsNotifier dua tren cau hinh.
 */
public interface Notifier {
    /**
     * Gui thong bao toi nguoi nhan.
     * 
     * @param to      Dia chi nguoi nhan (email hoac so dien thoai)
     * @param subject Tieu de thong bao
     * @param body    Noi dung thong bao
     */
    void send(String to, String subject, String body);

    /**
     * Ten kenh gui thong bao (EMAIL, SMS, PUSH)
     */
    String getChannel();
}

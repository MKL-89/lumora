package com.lumora.sms.service.impl;

/** Kenyan phone numbers written in different ways: "0712 345 678", "+254712345678", "712345678". */
final class Phones {

    private Phones() {
    }

    /** The subscriber digits only, so every way of writing one number compares equal. */
    static String normalise(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("254")) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    static boolean same(String a, String b) {
        return a != null && b != null && !normalise(a).isEmpty() && normalise(a).equals(normalise(b));
    }
}

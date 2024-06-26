/**
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 * <p>
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.wso2.openbanking.accelerator.consent.extensions.util;

import java.time.LocalDate;

/**
 * This enum represents the different types of periods that can be used in the application.
 * Each enum value is associated with a string representation and a method to calculate the divisor based
 * on the period type.
 * The divisor is used to convert other time units to this period type.
 */
public enum PeriodicTypesEnum {

    DAY("Day"),

    WEEK("Week"),

    FORTNIGHT("Fortnight"),

    MONTH("Month"),

    HALF_YEAR("Half-Year"),

    YEAR("Year");

    private String value;

    PeriodicTypesEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    /**
     * Returns the divisor based on the period type.
     *
     * @return the divisor based on the period type
     */
    public int getDivisor() {
        switch (this) {
            case DAY:
                return 1;
            case WEEK:
                return 7;
            case FORTNIGHT:
                return 14;
            case MONTH:
                return LocalDate.now().lengthOfMonth();
            case HALF_YEAR:
                return LocalDate.now().isLeapYear() ? 181 : 180;
            case YEAR:
                return LocalDate.now().isLeapYear() ? 366 : 365;
            default:
                throw new IllegalArgumentException("Invalid PeriodType");
        }
    }
}

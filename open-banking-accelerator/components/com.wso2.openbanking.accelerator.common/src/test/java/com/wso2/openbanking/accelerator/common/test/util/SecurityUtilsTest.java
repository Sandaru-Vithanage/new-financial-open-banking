/**
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wso2.openbanking.accelerator.common.test.util;

import com.wso2.openbanking.accelerator.common.util.SecurityUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests Common Security Utils.
 */
public class SecurityUtilsTest {

    @Test
    public void testSanitizeString() {
        String sanitizedString = SecurityUtils.sanitizeString("tests\nsanitizing");
        Assert.assertFalse(sanitizedString.contains("\n"));
    }

    @Test
    public void testSanitizeStringList() {
        List<String> sanitizedList = new ArrayList<>();
        sanitizedList.add("tests\nsanitizing");
        sanitizedList.add("tests\nsan\nitizing");
        sanitizedList.add("tests\nsanitizing\n");

        Assert.assertFalse(SecurityUtils.sanitize(sanitizedList).stream().anyMatch(s -> s.contains("\n")));
    }

    @Test
    public void testSanitizeStringSet() {
        Set<String> sanitizedList = new HashSet<>();
        sanitizedList.add("tests\nsanitizing");
        sanitizedList.add("tests\nsanitizingtext");
        sanitizedList.add("tests\nsanitizingwords");

        Assert.assertFalse(SecurityUtils.sanitize(sanitizedList).stream().anyMatch(s -> s.contains("\n")));
    }

    @Test
    public void testContainSpecialChars() {
        Assert.assertTrue(SecurityUtils.containSpecialChars("tests&sanitizing"));
    }
}

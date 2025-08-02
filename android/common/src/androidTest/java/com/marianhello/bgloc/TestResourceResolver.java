package com.anwar1909.bgloc;

import com.anwar1909.bgloc.test.TestConstants;

public class TestResourceResolver extends ResourceResolver {

    public TestResourceResolver() {

    }

    public String getAuthority() {
        return TestConstants.Authority;
    }
}

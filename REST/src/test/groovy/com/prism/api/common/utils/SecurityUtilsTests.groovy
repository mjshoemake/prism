package com.prism.api.common.utils

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SecurityUtilsTests {

	@Test
	void testPasswordEncrypt() {
		String hash = SecurityUtils.encryptPassword("password1")
		println "Hash: $hash"
	}

}

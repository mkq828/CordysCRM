package cn.cordys.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SecretCipher 加解密行为测试：随机 IV、幂等、明文惰性兼容、密文篡改检测。
 */
class SecretCipherTest {

    private static final String MASTER_KEY = "cordys.crypto.master-key";

    @BeforeEach
    void setUp() {
        // 注入测试主密钥，触发真正的 AES-GCM 加密路径
        System.setProperty(MASTER_KEY, "test-master-key-for-secret-cipher");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(MASTER_KEY);
    }

    @Test
    void roundTrip() {
        String plain = "sk-1234567890abcdef";
        String encrypted = SecretCipher.encrypt(plain);
        assertTrue(SecretCipher.isEncrypted(encrypted));
        assertEquals(plain, SecretCipher.decrypt(encrypted));
    }

    @Test
    void randomIvProducesDifferentCiphertext() {
        String plain = "sk-same-plaintext";
        assertNotEquals(SecretCipher.encrypt(plain), SecretCipher.encrypt(plain));
    }

    @Test
    void encryptIsIdempotent() {
        String encrypted = SecretCipher.encrypt("sk-idempotent");
        assertEquals(encrypted, SecretCipher.encrypt(encrypted));
    }

    @Test
    void decryptPassesThroughLegacyPlaintext() {
        String legacy = "legacy-plain-secret";
        assertFalse(SecretCipher.isEncrypted(legacy));
        assertEquals(legacy, SecretCipher.decrypt(legacy));
    }

    @Test
    void nullAndEmptyHandling() {
        assertNull(SecretCipher.encrypt(null));
        assertNull(SecretCipher.encrypt(""));
        assertNull(SecretCipher.decrypt(null));
        assertNull(SecretCipher.decrypt(""));
    }

    @Test
    void tamperedCiphertextFails() {
        String encrypted = SecretCipher.encrypt("sk-tamper");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";
        assertThrows(RuntimeException.class, () -> SecretCipher.decrypt(tampered));
    }
}

import hopper.pfm.insect.core.domain.EmailPolicy
import hopper.pfm.insect.core.domain.NamePolicy
import hopper.pfm.insect.core.domain.PasswordPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    @Test fun passwordNeedsAllRules() {
        assertTrue(PasswordPolicy.check("Passw0rdX").valid)
        assertFalse(PasswordPolicy.check("short1A").valid)
        assertFalse(PasswordPolicy.check("alllowercase1").valid)
        assertFalse(PasswordPolicy.check("ALLUPPERCASE1").valid)
        assertFalse(PasswordPolicy.check("NoDigitsHere").valid)
    }

    @Test fun passwordLimitIsInBytesNotCharacters() {
        // 40 characters of 2 bytes each = 80 bytes > 72
        val long = "Aa1" + "é".repeat(40)
        assertFalse(PasswordPolicy.check(long).withinMaxBytes)
        assertTrue(PasswordPolicy.check("Aa1" + "é".repeat(30)).withinMaxBytes)
    }

    @Test fun emailShape() {
        assertTrue(EmailPolicy.isValid(" ban@example.com "))
        assertFalse(EmailPolicy.isValid("ban@"))
        assertFalse(EmailPolicy.isValid("ban example@x.com"))
    }

    @Test fun nameLength() {
        assertTrue(NamePolicy.isValid("A"))
        assertFalse(NamePolicy.isValid("   "))
        assertFalse(NamePolicy.isValid("x".repeat(51)))
    }
}

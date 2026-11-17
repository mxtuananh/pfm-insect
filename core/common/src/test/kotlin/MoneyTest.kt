import hopper.pfm.insect.core.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun formatsDong() {
        assertEquals("45.000", Money.format(45_000, "VND"))
        assertEquals("1.250.000", Money.format(1_250_000, "VND"))
        assertEquals("0", Money.format(0, "VND"))
        assertEquals("45.000 ₫", Money.formatWithSymbol(45_000, "VND"))
    }

    @Test fun formatsCents() {
        assertEquals("12.34", Money.format(1_234, "USD"))
        assertEquals("1,234.05", Money.format(123_405, "USD"))
        assertEquals("0.07", Money.format(7, "USD"))
        assertEquals("$12.34", Money.formatWithSymbol(1_234, "USD"))
    }

    @Test fun parsesDong() {
        assertEquals(45_000L, Money.parse("45.000", "VND"))
        assertEquals(45_000L, Money.parse("45000", "VND"))
        assertNull(Money.parse("", "VND"))
        assertNull(Money.parse("abc", "VND"))
    }

    @Test fun parsesCents() {
        assertEquals(1_234L, Money.parse("12.34", "USD"))
        assertEquals(1_200L, Money.parse("12", "USD"))
        assertEquals(1_250L, Money.parse("12.5", "USD"))
        assertNull(Money.parse("12.345", "USD"))
        assertNull(Money.parse("1.2.3", "USD"))
    }

    @Test fun rejectsTooLarge() {
        assertEquals(Money.MAX_AMOUNT, Money.parse("9999999999999", "VND"))
        assertNull(Money.parse("10000000000000", "VND"))
    }

    @Test fun roundTrip() {
        for (amount in listOf(1L, 999L, 1_000L, 123_456_789L)) {
            assertEquals(amount, Money.parse(Money.format(amount, "VND"), "VND"))
        }
    }
}

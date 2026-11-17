import hopper.pfm.insect.core.model.AppVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {
    private fun v(text: String) = AppVersion.parseOrNull(text)!!

    @Test fun comparesNumerically() {
        assertTrue(v("1.10.0") > v("1.9.9"))
        assertTrue(v("2.0") > v("1.99.99"))
    }

    @Test fun missingPartsAreZero() {
        assertEquals(v("1.2"), v("1.2.0"))
        assertEquals(v("1.2").hashCode(), v("1.2.0").hashCode())
    }

    @Test fun ignoresSuffix() {
        assertEquals(v("1.2.3"), v("1.2.3-debug"))
    }

    @Test fun rejectsGarbage() {
        assertNull(AppVersion.parseOrNull(null))
        assertNull(AppVersion.parseOrNull(""))
        assertNull(AppVersion.parseOrNull("dev"))
    }
}

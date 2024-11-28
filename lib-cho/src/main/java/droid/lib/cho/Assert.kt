package droid.lib.cho

object Assert {
    fun fail(message: String?) {
        if (message == null) {
            throw AssertionError()
        }
        throw AssertionError(message)
    }

    @JvmOverloads
    fun assertTrue(condition: Boolean, message: String? = null) {
        if (!condition) {
            fail(message)
        }
    }

    @JvmOverloads
    fun assertFalse(condition: Boolean, message: String? = null) {
        assertTrue(!condition, message)
    }
}

package me.weishu.kernelsu.data.model

import me.weishu.kernelsu.Natives
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateInfoTest {
    @Test
    fun invalidNamespaceFallsBackWhenExporting() {
        val json = TemplateInfo(namespace = Int.MAX_VALUE).toJSON()

        assertEquals(
            Natives.Profile.Namespace.INHERITED.name,
            json.getString("namespace"),
        )
    }
}

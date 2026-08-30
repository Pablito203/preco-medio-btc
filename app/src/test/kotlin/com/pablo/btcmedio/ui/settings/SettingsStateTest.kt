package com.pablo.btcmedio.ui.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStateTest {

    @Test
    fun voz_exige_reconhecimento_e_nano_prontos() {
        assertTrue(SettingsState(nano = NanoStatus.PRONTO, speechAvailable = true).voiceEnabled)
    }

    @Test
    fun sem_nano_a_voz_fica_indisponivel_mesmo_com_reconhecimento() {
        listOf(
            NanoStatus.VERIFICANDO,
            NanoStatus.PRECISA_BAIXAR,
            NanoStatus.BAIXANDO,
            NanoStatus.NAO_SUPORTADO,
        ).forEach { status ->
            assertFalse(
                "com Nano em $status a voz não pode ser oferecida",
                SettingsState(nano = status, speechAvailable = true).voiceEnabled,
            )
        }
    }

    @Test
    fun sem_reconhecimento_a_voz_fica_indisponivel_mesmo_com_nano() {
        assertFalse(SettingsState(nano = NanoStatus.PRONTO, speechAvailable = false).voiceEnabled)
    }
}

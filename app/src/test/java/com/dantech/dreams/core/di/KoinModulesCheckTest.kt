package com.dantech.dreams.core.di

import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.verify.verify

class KoinModulesCheckTest : KoinTest {

    @Test
    fun `dataModule resolves`() {
        // dataModule has no Android-context dependency, so it can be verified on JVM.
        dataModule.verify()
    }

    @Test
    fun `appModule and featureModule are well-formed`() {
        koinApplication {
            modules(appModule, featureModule)
        }
    }
}

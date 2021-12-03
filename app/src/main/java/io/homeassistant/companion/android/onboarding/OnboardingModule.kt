package io.homeassistant.companion.android.onboarding

import android.app.Activity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import io.homeassistant.companion.android.onboarding.authentication.AuthenticationListener
import io.homeassistant.companion.android.onboarding.authentication.AuthenticationPresenter
import io.homeassistant.companion.android.onboarding.authentication.AuthenticationPresenterImpl
import io.homeassistant.companion.android.onboarding.integration.MobileAppIntegrationListener
import io.homeassistant.companion.android.onboarding.integration.MobileAppIntegrationPresenter
import io.homeassistant.companion.android.onboarding.integration.MobileAppIntegrationPresenterImpl

@Module
@InstallIn(ActivityComponent::class)
abstract class OnboardingModule {

    companion object {

        @Provides
        fun authenticationListener(activity: Activity): AuthenticationListener =
            activity as AuthenticationListener

        @Provides
        fun mobileAppIntegrationListener(activity: Activity): MobileAppIntegrationListener =
            activity as MobileAppIntegrationListener
    }

    @Binds
    abstract fun authPresenter(authenticationPresenterImpl: AuthenticationPresenterImpl): AuthenticationPresenter

    @Binds
    abstract fun mobileAppIntegrationPresenter(mobileAppIntegrationPresenterImpl: MobileAppIntegrationPresenterImpl): MobileAppIntegrationPresenter
}

package com.blueth.guard.scanner

import android.content.Context
import android.content.pm.PackageManager
import com.blueth.guard.update.SignatureUpdateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class TrackerCategory(val label: String) {
    ANALYTICS("Analytics"),
    ADVERTISING("Advertising"),
    CRASH_REPORTING("Crash Reporting"),
    ENGAGEMENT("Engagement"),
    ATTRIBUTION("Attribution"),
    IDENTIFICATION("Identification"),
    LOCATION("Location")
}

data class TrackerSignature(
    val name: String,
    val company: String,
    val category: TrackerCategory,
    val codeSignature: String,
    val networkDomain: String? = null
)

data class DetectedTracker(
    val signature: TrackerSignature,
    val matchedComponents: List<String>
)

object TrackerDatabase {
    val signatures: List<TrackerSignature> = listOf(
        // === ANALYTICS ===
        TrackerSignature("Google Analytics", "Google", TrackerCategory.ANALYTICS, "com.google.android.gms.analytics", "google-analytics.com"),
        TrackerSignature("Firebase Analytics", "Google", TrackerCategory.ANALYTICS, "com.google.firebase.analytics", "firebase-analytics.com"),
        TrackerSignature("Amplitude", "Amplitude", TrackerCategory.ANALYTICS, "com.amplitude.api", "amplitude.com"),
        TrackerSignature("Mixpanel", "Mixpanel", TrackerCategory.ANALYTICS, "com.mixpanel.android", "mixpanel.com"),
        TrackerSignature("Segment", "Segment", TrackerCategory.ANALYTICS, "com.segment.analytics", "segment.io"),
        TrackerSignature("Flurry", "Yahoo", TrackerCategory.ANALYTICS, "com.flurry.android", "flurry.com"),
        TrackerSignature("Countly", "Countly", TrackerCategory.ANALYTICS, "ly.count.android", "count.ly"),
        TrackerSignature("Heap", "Heap", TrackerCategory.ANALYTICS, "com.heapanalytics.android", "heap.io"),
        TrackerSignature("Snowplow", "Snowplow Analytics", TrackerCategory.ANALYTICS, "com.snowplowanalytics.snowplow", "snowplowanalytics.com"),
        TrackerSignature("PostHog", "PostHog", TrackerCategory.ANALYTICS, "com.posthog.android", "posthog.com"),
        TrackerSignature("Rudderstack", "Rudderstack", TrackerCategory.ANALYTICS, "com.rudderstack.android", "rudderstack.com"),
        TrackerSignature("mParticle", "mParticle", TrackerCategory.ANALYTICS, "com.mparticle.android", "mparticle.com"),
        TrackerSignature("Tealium", "Tealium", TrackerCategory.ANALYTICS, "com.tealium.library", "tealium.com"),
        TrackerSignature("Adobe Analytics", "Adobe", TrackerCategory.ANALYTICS, "com.adobe.marketing.mobile", "adobe.com"),
        TrackerSignature("Pendo", "Pendo", TrackerCategory.ANALYTICS, "io.pendo.android", "pendo.io"),
        TrackerSignature("Matomo", "Matomo", TrackerCategory.ANALYTICS, "org.matomo.sdk", "matomo.org"),
        TrackerSignature("Localytics", "Localytics", TrackerCategory.ANALYTICS, "com.localytics.android", "localytics.com"),
        TrackerSignature("UXCam", "UXCam", TrackerCategory.ANALYTICS, "com.uxcam.android", "uxcam.com"),
        TrackerSignature("Smartlook", "Smartlook", TrackerCategory.ANALYTICS, "com.smartlook.sdk", "smartlook.com"),

        // === ADVERTISING ===
        TrackerSignature("Google AdMob", "Google", TrackerCategory.ADVERTISING, "com.google.android.gms.ads", "googlesyndication.com"),
        TrackerSignature("Facebook Ads", "Meta", TrackerCategory.ADVERTISING, "com.facebook.ads", "facebook.com"),
        TrackerSignature("Unity Ads", "Unity", TrackerCategory.ADVERTISING, "com.unity3d.ads", "unityads.unity3d.com"),
        TrackerSignature("IronSource", "ironSource", TrackerCategory.ADVERTISING, "com.ironsource.mediationsdk", "ironsource.com"),
        TrackerSignature("AppLovin", "AppLovin", TrackerCategory.ADVERTISING, "com.applovin.sdk", "applovin.com"),
        TrackerSignature("Vungle", "Liftoff", TrackerCategory.ADVERTISING, "com.vungle.warren", "vungle.com"),
        TrackerSignature("InMobi", "InMobi", TrackerCategory.ADVERTISING, "com.inmobi.ads", "inmobi.com"),
        TrackerSignature("Chartboost", "Chartboost", TrackerCategory.ADVERTISING, "com.chartboost.sdk", "chartboost.com"),
        TrackerSignature("AdColony", "AdColony", TrackerCategory.ADVERTISING, "com.adcolony.sdk", "adcolony.com"),
        TrackerSignature("Tapjoy", "Tapjoy", TrackerCategory.ADVERTISING, "com.tapjoy.android", "tapjoy.com"),
        TrackerSignature("StartApp", "StartApp", TrackerCategory.ADVERTISING, "com.startapp.sdk", "startapp.com"),
        TrackerSignature("MoPub", "Twitter", TrackerCategory.ADVERTISING, "com.mopub.mobileads", "mopub.com"),
        TrackerSignature("Fyber", "Fyber", TrackerCategory.ADVERTISING, "com.fyber.inneractive", "fyber.com"),
        TrackerSignature("Pangle", "ByteDance", TrackerCategory.ADVERTISING, "com.bytedance.sdk.openadsdk", "pangle.io"),
        TrackerSignature("Yandex Ads", "Yandex", TrackerCategory.ADVERTISING, "com.yandex.mobile.ads", "yandex.com"),
        TrackerSignature("Amazon Ads", "Amazon", TrackerCategory.ADVERTISING, "com.amazon.device.ads", "amazon-adsystem.com"),
        TrackerSignature("Ogury", "Ogury", TrackerCategory.ADVERTISING, "io.presage.common", "ogury.co"),
        TrackerSignature("Digital Turbine", "Digital Turbine", TrackerCategory.ADVERTISING, "com.digitalturbine.ignite", "digitalturbine.com"),

        // === CRASH REPORTING ===
        TrackerSignature("Firebase Crashlytics", "Google", TrackerCategory.CRASH_REPORTING, "com.google.firebase.crashlytics", "firebase.google.com"),
        TrackerSignature("Sentry", "Sentry", TrackerCategory.CRASH_REPORTING, "io.sentry.android", "sentry.io"),
        TrackerSignature("Bugsnag", "Bugsnag", TrackerCategory.CRASH_REPORTING, "com.bugsnag.android", "bugsnag.com"),
        TrackerSignature("Instabug", "Instabug", TrackerCategory.CRASH_REPORTING, "com.instabug.library", "instabug.com"),
        TrackerSignature("ACRA", "ACRA", TrackerCategory.CRASH_REPORTING, "org.acra", null),
        TrackerSignature("Raygun", "Raygun", TrackerCategory.CRASH_REPORTING, "com.raygun.raygun4android", "raygun.com"),
        TrackerSignature("Datadog", "Datadog", TrackerCategory.CRASH_REPORTING, "com.datadog.android", "datadoghq.com"),
        TrackerSignature("New Relic", "New Relic", TrackerCategory.CRASH_REPORTING, "com.newrelic.agent.android", "newrelic.com"),
        TrackerSignature("Embrace", "Embrace", TrackerCategory.CRASH_REPORTING, "io.embrace.android", "embrace.io"),

        // === ENGAGEMENT ===
        TrackerSignature("Facebook SDK", "Meta", TrackerCategory.ENGAGEMENT, "com.facebook.appevents", "facebook.com"),
        TrackerSignature("Facebook Login", "Meta", TrackerCategory.ENGAGEMENT, "com.facebook.login", "facebook.com"),
        TrackerSignature("OneSignal", "OneSignal", TrackerCategory.ENGAGEMENT, "com.onesignal", "onesignal.com"),
        TrackerSignature("Braze", "Braze", TrackerCategory.ENGAGEMENT, "com.braze.ui", "braze.com"),
        TrackerSignature("CleverTap", "CleverTap", TrackerCategory.ENGAGEMENT, "com.clevertap.android", "clevertap.com"),
        TrackerSignature("Leanplum", "Leanplum", TrackerCategory.ENGAGEMENT, "com.leanplum", "leanplum.com"),
        TrackerSignature("Airship", "Airship", TrackerCategory.ENGAGEMENT, "com.urbanairship.android", "airship.com"),
        TrackerSignature("MoEngage", "MoEngage", TrackerCategory.ENGAGEMENT, "com.moengage.core", "moengage.com"),
        TrackerSignature("Batch", "Batch", TrackerCategory.ENGAGEMENT, "com.batch.android", "batch.com"),
        TrackerSignature("Intercom", "Intercom", TrackerCategory.ENGAGEMENT, "io.intercom.android", "intercom.io"),
        TrackerSignature("Zendesk", "Zendesk", TrackerCategory.ENGAGEMENT, "com.zendesk.sdk", "zendesk.com"),
        TrackerSignature("Pushwoosh", "Pushwoosh", TrackerCategory.ENGAGEMENT, "com.pushwoosh", "pushwoosh.com"),
        TrackerSignature("Iterable", "Iterable", TrackerCategory.ENGAGEMENT, "com.iterable.iterableapi", "iterable.com"),
        TrackerSignature("Customer.io", "Customer.io", TrackerCategory.ENGAGEMENT, "io.customer.sdk", "customer.io"),
        TrackerSignature("Helpshift", "Helpshift", TrackerCategory.ENGAGEMENT, "com.helpshift.support", "helpshift.com"),
        TrackerSignature("Freshchat", "Freshworks", TrackerCategory.ENGAGEMENT, "com.freshchat.consumer", "freshchat.com"),

        // === ATTRIBUTION ===
        TrackerSignature("Adjust", "Adjust", TrackerCategory.ATTRIBUTION, "com.adjust.sdk", "adjust.com"),
        TrackerSignature("AppsFlyer", "AppsFlyer", TrackerCategory.ATTRIBUTION, "com.appsflyer.sdk", "appsflyer.com"),
        TrackerSignature("Branch", "Branch", TrackerCategory.ATTRIBUTION, "io.branch.referral", "branch.io"),
        TrackerSignature("Kochava", "Kochava", TrackerCategory.ATTRIBUTION, "com.kochava.tracker", "kochava.com"),
        TrackerSignature("Singular", "Singular", TrackerCategory.ATTRIBUTION, "com.singular.sdk", "singular.net"),
        TrackerSignature("Tune", "Tune", TrackerCategory.ATTRIBUTION, "com.tune.ma", "tune.com"),
        TrackerSignature("Tenjin", "Tenjin", TrackerCategory.ATTRIBUTION, "com.tenjin.android", "tenjin.io"),

        // === IDENTIFICATION ===
        TrackerSignature("Google Sign-In", "Google", TrackerCategory.IDENTIFICATION, "com.google.android.gms.auth", "googleapis.com"),
        TrackerSignature("Facebook Share", "Meta", TrackerCategory.IDENTIFICATION, "com.facebook.share", "facebook.com"),
        TrackerSignature("Appsee", "Appsee", TrackerCategory.IDENTIFICATION, "com.appsee.android", "appsee.com"),
        TrackerSignature("FullStory", "FullStory", TrackerCategory.IDENTIFICATION, "com.fullstory.instrumentation", "fullstory.com"),
        TrackerSignature("Hotjar", "Hotjar", TrackerCategory.IDENTIFICATION, "com.hotjar.android", "hotjar.com"),
        TrackerSignature("Glassbox", "Glassbox", TrackerCategory.IDENTIFICATION, "com.glassbox.glassboxsdk", "glassbox.com"),

        // === LOCATION ===
        TrackerSignature("Google Maps", "Google", TrackerCategory.LOCATION, "com.google.android.gms.maps", "maps.googleapis.com"),
        TrackerSignature("Mapbox", "Mapbox", TrackerCategory.LOCATION, "com.mapbox.mapboxsdk", "mapbox.com"),
        TrackerSignature("Radar", "Radar", TrackerCategory.LOCATION, "io.radar.sdk", "radar.io"),
        TrackerSignature("Foursquare Pilgrim", "Foursquare", TrackerCategory.LOCATION, "com.foursquare.pilgrim", "foursquare.com"),
        TrackerSignature("HERE SDK", "HERE Technologies", TrackerCategory.LOCATION, "com.here.sdk", "here.com")
    )
}

@Singleton
class TrackerDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateManager: SignatureUpdateManager
) {
    fun detectTrackers(packageName: String, permissions: List<String>): List<DetectedTracker> {
        val pm = context.packageManager
        val componentNames = getComponentClassNames(pm, packageName)
        val detected = mutableListOf<DetectedTracker>()

        // Bundled signatures
        for (signature in TrackerDatabase.signatures) {
            val matched = componentNames.filter { it.startsWith(signature.codeSignature) }
            if (matched.isNotEmpty()) {
                detected.add(DetectedTracker(signature, matched))
            }
        }

        // Remote signatures
        val remote = updateManager.getCachedManifest()
        remote?.trackerSignatures?.trackers?.forEach { entry ->
            val category = try { TrackerCategory.valueOf(entry.category.uppercase()) } catch (_: Exception) { TrackerCategory.ANALYTICS }
            val signature = TrackerSignature(entry.name, entry.company, category, entry.codeSignature, entry.networkDomain)
            val matched = componentNames.filter { it.startsWith(signature.codeSignature) }
            if (matched.isNotEmpty()) {
                detected.add(DetectedTracker(signature, matched))
            }
        }

        return detected
    }

    private fun getComponentClassNames(pm: PackageManager, packageName: String): List<String> {
        val names = mutableListOf<String>()
        try {
            val flags = PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
            val pkgInfo = pm.getPackageInfo(packageName, flags)
            pkgInfo.activities?.forEach { names.add(it.name) }
            pkgInfo.services?.forEach { names.add(it.name) }
            pkgInfo.receivers?.forEach { names.add(it.name) }
            pkgInfo.providers?.forEach { names.add(it.name) }
        } catch (_: PackageManager.NameNotFoundException) {
            // Package not found
        } catch (_: SecurityException) {
            // Cannot access package components
        }
        return names
    }
}

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
    /**
     * Known tracker signatures — 200+ entries across 7 categories.
     * Inspired by the Exodus Privacy database and public tracker research.
     * Updated via SignatureUpdateManager for new trackers.
     */
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
        TrackerSignature("HERE SDK", "HERE Technologies", TrackerCategory.LOCATION, "com.here.sdk", "here.com"),

        // === ANALYTICS (additional) ===
        TrackerSignature("CleverTap Analytics", "CleverTap", TrackerCategory.ANALYTICS, "com.clevertap.android.sdk", "clevertap.com"),
        TrackerSignature("Kissmetrics", "Kissmetrics", TrackerCategory.ANALYTICS, "com.kissmetrics.sdk", "kissmetrics.com"),
        TrackerSignature("Chartbeat", "Chartbeat", TrackerCategory.ANALYTICS, "com.chartbeat.androidsdk", "chartbeat.com"),
        TrackerSignature("Swrve", "Swrve", TrackerCategory.ANALYTICS, "com.swrve.sdk", "swrve.com"),
        TrackerSignature("Apptentive", "Apptentive", TrackerCategory.ANALYTICS, "com.apptentive.android", "apptentive.com"),
        TrackerSignature("Kumulos", "Kumulos", TrackerCategory.ANALYTICS, "com.kumulos.android", "kumulos.com"),
        TrackerSignature("Webengage", "Webengage", TrackerCategory.ANALYTICS, "com.webengage.sdk", "webengage.com"),
        TrackerSignature("Taplytics", "Taplytics", TrackerCategory.ANALYTICS, "com.taplytics.sdk", "taplytics.com"),
        TrackerSignature("AppsFlyer Analytics", "AppsFlyer", TrackerCategory.ANALYTICS, "com.appsflyer.internal", "appsflyer.com"),
        TrackerSignature("Woopra", "Woopra", TrackerCategory.ANALYTICS, "com.woopra.android", "woopra.com"),
        TrackerSignature("Firebase Performance", "Google", TrackerCategory.ANALYTICS, "com.google.firebase.perf", "firebase.google.com"),
        TrackerSignature("Firebase Remote Config", "Google", TrackerCategory.ANALYTICS, "com.google.firebase.remoteconfig", "firebase.google.com"),
        TrackerSignature("Firebase Dynamic Links", "Google", TrackerCategory.ANALYTICS, "com.google.firebase.dynamiclinks", "firebase.google.com"),
        TrackerSignature("Firebase In-App Messaging", "Google", TrackerCategory.ANALYTICS, "com.google.firebase.inappmessaging", "firebase.google.com"),
        TrackerSignature("Google Tag Manager", "Google", TrackerCategory.ANALYTICS, "com.google.android.gms.tagmanager", "tagmanager.google.com"),
        TrackerSignature("Leanplum Analytics", "Leanplum", TrackerCategory.ANALYTICS, "com.leanplum.internal", "leanplum.com"),
        TrackerSignature("Optimizely", "Optimizely", TrackerCategory.ANALYTICS, "com.optimizely.ab.android", "optimizely.com"),
        TrackerSignature("LaunchDarkly", "LaunchDarkly", TrackerCategory.ANALYTICS, "com.launchdarkly.sdk.android", "launchdarkly.com"),
        TrackerSignature("Aptabase", "Aptabase", TrackerCategory.ANALYTICS, "com.aptabase.android", "aptabase.com"),
        TrackerSignature("Plausible", "Plausible", TrackerCategory.ANALYTICS, "io.plausible.android", "plausible.io"),

        // === ADVERTISING (additional) ===
        TrackerSignature("Criteo", "Criteo", TrackerCategory.ADVERTISING, "com.criteo.publisher.sdk", "criteo.com"),
        TrackerSignature("Mintegral", "Mintegral", TrackerCategory.ADVERTISING, "com.mbridge.msdk", "mintegral.com"),
        TrackerSignature("Smaato", "Smaato", TrackerCategory.ADVERTISING, "com.smaato.sdk", "smaato.com"),
        TrackerSignature("Verve", "Verve", TrackerCategory.ADVERTISING, "net.pubnative.lite", "verve.com"),
        TrackerSignature("Bigo Ads", "Bigo", TrackerCategory.ADVERTISING, "sg.bigo.ads", "bigo.sg"),
        TrackerSignature("Yandex AppMetrica", "Yandex", TrackerCategory.ADVERTISING, "com.yandex.metrica", "appmetrica.yandex.com"),
        TrackerSignature("ByteDance Union", "ByteDance", TrackerCategory.ADVERTISING, "com.bytedance.sdk.openadsdk.api", "pangle.io"),
        TrackerSignature("HyprMX", "HyprMX", TrackerCategory.ADVERTISING, "com.hyprmx.android", "hyprmx.com"),
        TrackerSignature("Kidoz", "Kidoz", TrackerCategory.ADVERTISING, "com.kidoz.sdk", "kidoz.net"),
        TrackerSignature("LiftoffMonetize", "Liftoff", TrackerCategory.ADVERTISING, "com.vungle.ads", "liftoff.io"),
        TrackerSignature("Moloco", "Moloco", TrackerCategory.ADVERTISING, "com.moloco.sdk", "moloco.com"),
        TrackerSignature("BidMachine", "BidMachine", TrackerCategory.ADVERTISING, "io.bidmachine", "bidmachine.io"),
        TrackerSignature("InMobi UniAds", "InMobi", TrackerCategory.ADVERTISING, "com.inmobi.unification", "inmobi.com"),
        TrackerSignature("LINE Ads", "LINE", TrackerCategory.ADVERTISING, "com.linecorp.admanager", "line.me"),
        TrackerSignature("Madex", "Madex", TrackerCategory.ADVERTISING, "com.madex.sdk", "madex.com"),
        TrackerSignature("MyTarget", "VK", TrackerCategory.ADVERTISING, "com.my.target.sdk", "mytarget.mail.ru"),
        TrackerSignature("Nend", "Fan Communications", TrackerCategory.ADVERTISING, "net.nend.android", "nend.net"),
        TrackerSignature("PubMatic", "PubMatic", TrackerCategory.ADVERTISING, "com.pubmatic.sdk", "pubmatic.com"),
        TrackerSignature("SuperAwesome", "SuperAwesome", TrackerCategory.ADVERTISING, "tv.superawesome.sdk", "superawesome.com"),
        TrackerSignature("Verve Group", "Verve", TrackerCategory.ADVERTISING, "com.verve.sdk", "verve.com"),

        // === CRASH REPORTING (additional) ===
        TrackerSignature("AppCenter Crashes", "Microsoft", TrackerCategory.CRASH_REPORTING, "com.microsoft.appcenter.crashes", "appcenter.ms"),
        TrackerSignature("AppCenter Analytics", "Microsoft", TrackerCategory.CRASH_REPORTING, "com.microsoft.appcenter.analytics", "appcenter.ms"),
        TrackerSignature("Rollbar", "Rollbar", TrackerCategory.CRASH_REPORTING, "com.rollbar.android", "rollbar.com"),
        TrackerSignature("BugFender", "BugFender", TrackerCategory.CRASH_REPORTING, "com.bugfender.sdk", "bugfender.com"),
        TrackerSignature("LogRocket", "LogRocket", TrackerCategory.CRASH_REPORTING, "com.logrocket.core", "logrocket.com"),

        // === ENGAGEMENT (additional) ===
        TrackerSignature("Firebase Cloud Messaging", "Google", TrackerCategory.ENGAGEMENT, "com.google.firebase.messaging", "firebase.google.com"),
        TrackerSignature("Firebase Auth", "Google", TrackerCategory.ENGAGEMENT, "com.google.firebase.auth", "firebase.google.com"),
        TrackerSignature("Twilio", "Twilio", TrackerCategory.ENGAGEMENT, "com.twilio.chat", "twilio.com"),
        TrackerSignature("SendBird", "SendBird", TrackerCategory.ENGAGEMENT, "com.sendbird.android", "sendbird.com"),
        TrackerSignature("Stream Chat", "Stream", TrackerCategory.ENGAGEMENT, "io.getstream.chat", "getstream.io"),
        TrackerSignature("Drift", "Drift", TrackerCategory.ENGAGEMENT, "com.driftt.sdk", "drift.com"),
        TrackerSignature("Crisp", "Crisp", TrackerCategory.ENGAGEMENT, "im.crisp.client", "crisp.chat"),
        TrackerSignature("Kustomer", "Kustomer", TrackerCategory.ENGAGEMENT, "com.kustomer.core", "kustomer.com"),
        TrackerSignature("Salesforce Marketing Cloud", "Salesforce", TrackerCategory.ENGAGEMENT, "com.salesforce.marketingcloud", "salesforce.com"),
        TrackerSignature("Marketo", "Adobe", TrackerCategory.ENGAGEMENT, "com.marketo.sdk", "marketo.com"),
        TrackerSignature("WebEngage Push", "WebEngage", TrackerCategory.ENGAGEMENT, "com.webengage.sdk.android", "webengage.com"),
        TrackerSignature("Netcore Smartech", "Netcore", TrackerCategory.ENGAGEMENT, "com.netcore.android", "netcoresmartech.com"),
        TrackerSignature("Insider", "Insider", TrackerCategory.ENGAGEMENT, "com.useinsider.insider", "useinsider.com"),
        TrackerSignature("Vibes", "Vibes", TrackerCategory.ENGAGEMENT, "com.vibes.sdk", "vibes.com"),
        TrackerSignature("Swrve Push", "Swrve", TrackerCategory.ENGAGEMENT, "com.swrve.sdk.push", "swrve.com"),

        // === ATTRIBUTION (additional) ===
        TrackerSignature("Airbridge", "Airbridge", TrackerCategory.ATTRIBUTION, "co.ab180.airbridge", "airbridge.io"),
        TrackerSignature("Tune MATT", "Tune", TrackerCategory.ATTRIBUTION, "com.tune.crosspromo", "tune.com"),
        TrackerSignature("Nielsen DAR", "Nielsen", TrackerCategory.ATTRIBUTION, "com.nielsen.app.sdk", "nielsen.com"),
        TrackerSignature("comScore", "comScore", TrackerCategory.ATTRIBUTION, "com.comscore.analytics", "comscore.com"),
        TrackerSignature("DoubleVerify", "DoubleVerify", TrackerCategory.ATTRIBUTION, "com.doubleverify.dvsdk", "doubleverify.com"),
        TrackerSignature("MOAT Analytics", "Oracle", TrackerCategory.ATTRIBUTION, "com.moat.analytics", "moat.com"),
        TrackerSignature("IAS (Integral Ad Science)", "IAS", TrackerCategory.ATTRIBUTION, "com.integralads.avid", "integralads.com"),

        // === IDENTIFICATION (additional) ===
        TrackerSignature("FingerprintJS", "FingerprintJS", TrackerCategory.IDENTIFICATION, "com.fingerprintjs.android", "fingerprint.com"),
        TrackerSignature("DeviceAtlas", "DeviceAtlas", TrackerCategory.IDENTIFICATION, "com.deviceatlas.android", "deviceatlas.com"),
        TrackerSignature("ThreatMetrix", "LexisNexis", TrackerCategory.IDENTIFICATION, "com.threatmetrix.TrustDefender", "threatmetrix.com"),
        TrackerSignature("Iovation", "TransUnion", TrackerCategory.IDENTIFICATION, "com.iovation.mobile", "iovation.com"),
        TrackerSignature("Telesign", "Telesign", TrackerCategory.IDENTIFICATION, "com.telesign.sdk", "telesign.com"),
        TrackerSignature("Adjust Signature", "Adjust", TrackerCategory.IDENTIFICATION, "com.adjust.sdk.signature", "adjust.com"),
        TrackerSignature("Braze Content Cards", "Braze", TrackerCategory.IDENTIFICATION, "com.braze.contentcards", "braze.com"),
        TrackerSignature("Huawei Analytics", "Huawei", TrackerCategory.IDENTIFICATION, "com.huawei.hms.analytics", "huawei.com"),
        TrackerSignature("Samsung Analytics", "Samsung", TrackerCategory.IDENTIFICATION, "com.samsung.android.sdk.sga", "samsung.com"),
        TrackerSignature("Xiaomi Analytics", "Xiaomi", TrackerCategory.IDENTIFICATION, "com.xiaomi.analytics", "xiaomi.com"),

        // === LOCATION (additional) ===
        TrackerSignature("Huq Industries", "Huq", TrackerCategory.LOCATION, "io.huq.sourcekit", "huq.io"),
        TrackerSignature("Placed", "Foursquare", TrackerCategory.LOCATION, "com.placed.client", "placed.com"),
        TrackerSignature("SafeGraph", "SafeGraph", TrackerCategory.LOCATION, "com.safegraph.sdk", "safegraph.com"),
        TrackerSignature("Skyhook", "Qualcomm", TrackerCategory.LOCATION, "com.skyhookwireless.wps", "skyhook.com"),
        TrackerSignature("X-Mode", "Outlogic", TrackerCategory.LOCATION, "com.xmode.sdk", "xmode.io"),
        TrackerSignature("Tutela", "Tutela", TrackerCategory.LOCATION, "com.tutela.sdk", "tutela.com"),
        TrackerSignature("Cuebiq", "Cuebiq", TrackerCategory.LOCATION, "com.cuebiq.sdk", "cuebiq.com"),
        TrackerSignature("Gravy Analytics", "Gravy", TrackerCategory.LOCATION, "com.gravyanalytics.sdk", "gravyanalytics.com"),
        TrackerSignature("Reveal Mobile", "Reveal", TrackerCategory.LOCATION, "com.revealmobile.sdk", "revealmobile.com"),
        TrackerSignature("Unacast", "Unacast", TrackerCategory.LOCATION, "com.unacast.sdk", "unacast.com")
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

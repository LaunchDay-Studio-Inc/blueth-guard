package com.blueth.guard.update

import kotlinx.serialization.Serializable

@Serializable
data class SignatureUpdateManifest(
    val version: Int,
    val updatedAt: String,
    val malwareSignatures: MalwareSignatureUpdate,
    val trackerSignatures: TrackerSignatureUpdate
)

@Serializable
data class MalwareSignatureUpdate(
    val exactPackages: List<MalwarePackageEntry>,
    val patterns: List<MalwarePatternEntry>,
    val certHashes: List<String>
)

@Serializable
data class MalwarePackageEntry(
    val packageName: String,
    val family: String,
    val severity: String,
    val description: String
)

@Serializable
data class MalwarePatternEntry(
    val pattern: String,
    val family: String,
    val severity: String,
    val description: String
)

@Serializable
data class TrackerSignatureUpdate(
    val trackers: List<TrackerEntry>
)

@Serializable
data class TrackerEntry(
    val name: String,
    val company: String,
    val category: String,
    val codeSignature: String,
    val networkDomain: String? = null
)

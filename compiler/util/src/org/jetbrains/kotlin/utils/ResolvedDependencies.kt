/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * ID of [ResolvedDependency].
 */
class ResolvedDependencyId private constructor(val uniqueNames: Set<String>) {
    constructor(vararg uniqueNames: String) : this(uniqueNames.toSortedSet())
    constructor(uniqueNames: Iterable<String>) : this(uniqueNames.toSortedSet())

    init {
        check(uniqueNames.isNotEmpty())
    }

    override fun toString() = buildString {
        uniqueNames.forEachIndexed { index, uniqueName ->
            when (index) {
                0 -> append(uniqueName)
                else -> {
                    when (index) {
                        1 -> append(" (")
                        /* 2+ */ else -> append(", ")
                    }
                    append(uniqueName)
                    if (index == uniqueNames.size - 1) append(")")
                }
            }
        }
    }

    override fun equals(other: Any?) = (other as? ResolvedDependencyId)?.uniqueNames == uniqueNames
    override fun hashCode() = uniqueNames.hashCode()

    fun withVersion(version: ResolvedDependencyVersion): String = buildString {
        append(this@ResolvedDependencyId.toString())
        if (!version.isEmpty()) append(": ").append(version)
    }

    companion object {
        val SOURCE_CODE_MODULE_ID = ResolvedDependencyId("/")
    }
}

/**
 * A representation of a particular module (component, dependency) in the project. Keeps the information about the module version and
 * the place of the module in the project dependencies hierarchy.
 *
 * - [id] identifier of the module
 * - [selectedVersion] actual version of the module that is used in the project
 * - [requestedVersionsByIncomingDependencies] the mapping between ID of the dependee module (i.e. the module that depends on this module)
 *   and the version (requested version) that the dependee module requires from this module. The requested version can be different
 *   for different dependee modules, which is absolutely legal. The [selectedVersion] is always equal to one of the requested versions
 *   (the one that wins among others, which is typically handled inside of the build system). If the module is the top-level module,
 *   then it has [ResolvedDependencyId.SOURCE_CODE_MODULE_ID] in the mapping.
 * - [artifactPaths] absolute paths to every artifact represented by this module
 */
class ResolvedDependency(
    val id: ResolvedDependencyId,
    var visibleAsFirstLevelDependency: Boolean = true,
    var selectedVersion: ResolvedDependencyVersion,
    val requestedVersionsByIncomingDependencies: MutableMap<ResolvedDependencyId, ResolvedDependencyVersion>,
    val artifactPaths: MutableSet<ResolvedDependencyArtifactPath>
) {
    val moduleIdWithVersion: String
        get() = id.withVersion(selectedVersion)

    override fun toString() = moduleIdWithVersion
}

object ResolvedDependenciesSupport {
    fun serialize(modules: Collection<ResolvedDependency>): String {
        val moduleIdToIndex = mutableMapOf(ResolvedDependencyId.SOURCE_CODE_MODULE_ID to 0).apply {
            modules.forEachIndexed { index, module -> this[module.id] = index + 1 }
        }

        return buildString {
            modules.forEach { module ->
                val moduleIndex = moduleIdToIndex.getValue(module.id)
                append(moduleIndex.toString()).append(' ')
                append(module.id.uniqueNames.joinTo(this, separator = ","))
                append('[').append(module.selectedVersion).append(']')
                module.requestedVersionsByIncomingDependencies.entries.joinTo(
                    this,
                    separator = ""
                ) { (incomingDependencyId, requestedVersion) ->
                    val incomingDependencyIndex = moduleIdToIndex.getValue(incomingDependencyId)
                    " #$incomingDependencyIndex[$requestedVersion]"
                }
                append('\n')
                module.artifactPaths.joinTo(this, separator = "") { artifactPath -> "\t$artifactPath\n" }
            }
        }
    }

    fun deserialize(source: String, onMalformedLine: (lineNo: Int, line: String) -> Unit): Collection<ResolvedDependency> {
        val moduleIndexToId: MutableMap<Int, ResolvedDependencyId> = mutableMapOf(0 to ResolvedDependencyId.SOURCE_CODE_MODULE_ID)
        val requestedVersionsByIncomingDependenciesIndices: MutableMap<ResolvedDependencyId, Map<Int, ResolvedDependencyVersion>> =
            mutableMapOf()

        val modules = mutableListOf<ResolvedDependency>()

        source.lines().forEachIndexed { lineNo, line ->
            fun malformedLine(): Collection<ResolvedDependency> {
                onMalformedLine(lineNo, line)
                return emptyList()
            }

            when {
                line.isBlank() -> return@forEachIndexed
                line[0].isWhitespace() -> {
                    val currentModule = modules.lastOrNull()
                    val artifactPath = line.trimStart { it.isWhitespace() }
                    if (currentModule != null && artifactPath.isNotBlank())
                        currentModule.artifactPaths += ResolvedDependencyArtifactPath(artifactPath)
                    else
                        return malformedLine()
                }
                else -> {
                    val result = DEPENDENCY_MODULE_REGEX.matchEntire(line) ?: return malformedLine()
                    val moduleIndex = result.groupValues[1].toInt()
                    val moduleId = ResolvedDependencyId(result.groupValues[2].split(','))
                    val selectedVersion = ResolvedDependencyVersion(result.groupValues[3])

                    if (result.groupValues.size > 4) {
                        val requestedVersions: Map<Int, ResolvedDependencyVersion> = result.groupValues[4].trimStart()
                            .split(' ')
                            .associate { token ->
                                val tokenResult = REQUESTED_VERSION_BY_INCOMING_DEPENDENCY_REGEX.matchEntire(token)
                                    ?: return malformedLine()
                                val incomingDependencyIndex = tokenResult.groupValues[1].toInt()
                                val requestedVersion = ResolvedDependencyVersion(tokenResult.groupValues[2])
                                incomingDependencyIndex to requestedVersion
                            }
                        requestedVersionsByIncomingDependenciesIndices[moduleId] = requestedVersions
                    }

                    moduleIndexToId[moduleIndex] = moduleId
                    modules += ResolvedDependency(
                        id = moduleId,
                        selectedVersion = selectedVersion,
                        requestedVersionsByIncomingDependencies = mutableMapOf(), // To be filled later.
                        artifactPaths = mutableSetOf() // To be filled on the next iterations.
                    )
                }
            }
        }

        // Stamp incoming dependencies & requested versions.
        modules.forEach { module ->
            requestedVersionsByIncomingDependenciesIndices[module.id]?.forEach { (incomingDependencyIndex, requestedVersion) ->
                val incomingDependencyId = moduleIndexToId.getValue(incomingDependencyIndex)
                module.requestedVersionsByIncomingDependencies[incomingDependencyId] = requestedVersion
            }
        }

        return modules
    }

    private val DEPENDENCY_MODULE_REGEX = Regex("^(\\d+) ([^\\[]+)\\[([^]]+)](.*)?$")
    private val REQUESTED_VERSION_BY_INCOMING_DEPENDENCY_REGEX = Regex("^#(\\d+)\\[(.*)]$")
}

data class ResolvedDependencyVersion(val version: String) {
    fun isEmpty() = version.isEmpty()
    override fun toString() = version

    companion object {
        val EMPTY = ResolvedDependencyVersion("")
    }
}

data class ResolvedDependencyArtifactPath(val path: String) {
    override fun toString() = path
}

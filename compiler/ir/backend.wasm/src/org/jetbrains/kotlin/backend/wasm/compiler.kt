/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.wasm.codegen.IrModuleToWasm
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.MainModule
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

data class WasmCompilerResult(val wat: String, val js: String)

fun compileWasm(
    project: Project,
    files: List<KtFile>,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    exportedDeclarations: Set<FqName> = emptySet()
): WasmCompilerResult {
    val irFactory = PersistentIrFactory() // TODO IrFactoryImpl?

    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(
            project, MainModule.SourceFiles(files), analyzer, configuration, allDependencies, friendDependencies,
            irFactory
        )

    val moduleDescriptor = moduleFragment.descriptor
    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration, irFactory)

    // Load declarations referenced during `context` initialization
    dependencyModules.forEach {
        val irProviders = generateTypicalIrProviderList(it.descriptor, irBuiltIns, symbolTable, deserializer)
        ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings)
            .generateUnboundSymbolsAsDependencies()
    }

    val irFiles = dependencyModules.flatMap { it.files } + moduleFragment.files

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    // Create stubs
    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()
    moduleFragment.patchDeclarationParents()
    deserializer.postProcess()

    wasmPhases.invokeToplevel(phaseConfig, context, moduleFragment)

    return IrModuleToWasm(context).generateModule(moduleFragment)
}

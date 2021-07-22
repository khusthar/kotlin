/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeUnionType
import org.jetbrains.kotlin.fir.types.coneType

object FirUnionTypeInTypeOperatorCallChecker : FirTypeOperatorCallChecker() {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation !in FirOperation.TYPES)
            return
        
        val conversionTypeRef = expression.conversionTypeRef
        if (conversionTypeRef.coneType.fullyExpandedType(context.session) is ConeUnionType) {
            reporter.reportOn(conversionTypeRef.source, FirErrors.UNION_TYPE_PROHIBITED_POSITION, context)
        }
    }
}
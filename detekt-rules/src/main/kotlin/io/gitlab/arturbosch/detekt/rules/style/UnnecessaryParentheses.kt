package io.gitlab.arturbosch.detekt.rules.style

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Reports unnecessary parentheses around expressions.
 *
 * Added in v1.0.0.RC4
 */
class UnnecessaryParentheses(config: Config = Config.empty) : Rule(config) {

	override val issue = Issue("UnnecessaryParentheses", Severity.Style,
			"These parentheses are unnecessary and can be removed.")

	override fun visitParenthesizedExpression(expression: KtParenthesizedExpression) {
		super.visitParenthesizedExpression(expression)

		if (KtPsiUtil.areParenthesesUseless(expression)) {
			val description = "Parentheses in ${expression.text} are unnecessary and can be replaced with: " +
					"${KtPsiUtil.deparenthesize(expression)?.text}"
			report(CodeSmell(issue.copy(description = description), Entity.from(expression)))
		}
	}

	override fun visitArgument(argument: KtValueArgument) {
		super.visitArgument(argument)
		val isLambdaExpression = argument.children.any { it is KtLambdaExpression }
		val isOnlyArgument = argument.parent.children.size == 1

		val isSuperTypeCallEntry = argument.parent.parent is KtSuperTypeCallEntry

		if (isLambdaExpression && isOnlyArgument && !isSuperTypeCallEntry) {
			val description = "Parentheses around the lambda ${argument.parent.text} are unnecessary and can be removed."
			report(CodeSmell(issue.copy(description = description), Entity.from(argument.parent)))
		}
	}
}

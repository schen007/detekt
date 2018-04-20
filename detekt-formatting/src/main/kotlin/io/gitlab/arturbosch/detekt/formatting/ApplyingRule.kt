package io.gitlab.arturbosch.detekt.formatting

import com.github.shyiko.ktlint.core.EditorConfig
import com.github.shyiko.ktlint.core.KtLint
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.SingleAssign
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.api.TextLocation
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset

/**
 * @author Artur Bosch
 */
abstract class ApplyingRule(config: Config) : Rule(config) {

	abstract val wrapping: com.github.shyiko.ktlint.core.Rule

	protected fun issueFor(description: String) =
			Issue(wrapping.id, Severity.Style, description, Debt.FIVE_MINS)

	private val isAndroid by lazy { valueOrDefault("android", false) }

	private var positionByOffset: (offset: Int) -> Pair<Int, Int> by SingleAssign()
	private var root: KtFile by SingleAssign()

	override fun visit(root: KtFile) {
		this.root = root
		positionByOffset = calculateLineColByOffset(root.text).let {
			val offsetDueToLineBreakNormalization = calculateLineBreakOffset(root.text)
			return@let { offset: Int -> it(offset + offsetDueToLineBreakNormalization(offset)) }
		}
	}

	fun apply(node: ASTNode) {
		if (node is FileASTNode) {
			node.putUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY, EditorConfig.fromMap(mapOf()))
			node.putUserData(KtLint.ANDROID_USER_DATA_KEY, isAndroid)
		}
		if (ruleShouldOnlyRunOnFileNode(node)) {
			return
		}
		wrapping.visit(node, autoCorrect) { _, message, _ ->
			val (line, column) = positionByOffset(node.startOffset)
			report(CodeSmell(issue,
					Entity(node.toString(), "", "",
							Location(SourceLocation(line, column),
									TextLocation(node.startOffset, node.psi.endOffset),
									"($line, $column)",
									root.originalFilePath() ?: root.containingFile.name)),
					message))
		}
	}

	private fun ruleShouldOnlyRunOnFileNode(node: ASTNode) =
			wrapping is com.github.shyiko.ktlint.core.Rule.Modifier.RestrictToRoot
					&& node !is FileASTNode

	private fun PsiElement.originalFilePath() =
			(this.containingFile.viewProvider.virtualFile as? LightVirtualFile)?.originalFile?.name
}
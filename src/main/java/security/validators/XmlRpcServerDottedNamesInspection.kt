package security.validators

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.psi.PyBoolLiteralExpression
import com.jetbrains.python.psi.PyCallExpression
import security.Checks
import security.helpers.SecurityVisitor
import security.helpers.calleeMatches
import security.helpers.hasImportedNamespace
import security.helpers.skipDocstring

class XmlRpcServerDottedNamesInspection : PyInspection() {
    val check = Checks.XmlRpcServerDottedNamesCheck

    override fun getStaticDescription(): String? {
        return check.getStaticDescription()
    }

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean,
                              session: LocalInspectionToolSession): PsiElementVisitor = Visitor(holder, session)

    private class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) : SecurityVisitor(holder, session) {
        override fun visitPyCallExpression(node: PyCallExpression) {
            if (skipDocstring(node)) return
            if (!calleeMatches(node,"register_instance")) return
            if (!hasImportedNamespace(node.containingFile, "xmlrpc.server")) return

            if (node.arguments.isNullOrEmpty()) return
            if (node.arguments.size == 1) return

            val allowDottedNamesArg = node.getKeywordArgument("allow_dotted_names")
            if (allowDottedNamesArg != null) {
                if (allowDottedNamesArg !is PyBoolLiteralExpression) return
                if (allowDottedNamesArg.value.not()) return
            } else {
                val secondArg = node.arguments[1]
                if (secondArg !is PyBoolLiteralExpression) return
                if (secondArg.value.not()) return
            }
            holder.registerProblem(node, Checks.XmlRpcServerDottedNamesCheck.getDescription())
        }
    }
}
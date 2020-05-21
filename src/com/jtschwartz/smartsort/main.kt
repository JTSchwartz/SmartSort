package com.jtschwartz.smartsort

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil


private val getText: (AnActionEvent) -> String? = {e -> e.getData(CommonDataKeys.EDITOR)?.selectionModel?.selectedText}

private fun makeVisibleCheckCapability(e: AnActionEvent) {
	e.presentation.isVisible = true
	
	val project = e.project
	val editor = e.getData(CommonDataKeys.EDITOR)
	val psiFile = e.getData(CommonDataKeys.PSI_FILE)
	
	e.presentation.isEnabled = project != null
			&& editor != null
			&& psiFile != null
			&& editor.caretModel.caretCount == 1
}

class RecursiveSort: AnAction() {
	
	override fun actionPerformed(anActionEvent: AnActionEvent): Unit {
		val editor = anActionEvent.getData(CommonDataKeys.EDITOR)
		val psiFile = anActionEvent.getData(CommonDataKeys.PSI_FILE)
		if (editor == null || psiFile == null) return
		val offset = editor.caretModel.offset
		var element = psiFile.findElementAt(offset)
		
		while (element != null) {
			println(element)
			element = element.parent
		}
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckCapability(e)
	}
}

class SingleDepthSort: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		println("Single Depth Sort")
		println(getText(e))
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckCapability(e)
	}
}

class SelectiveDepthSort: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		println("Selective Depth Sort")
		println(getText(e))
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckCapability(e)
	}
}
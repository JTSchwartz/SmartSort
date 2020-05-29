package com.jtschwartz.smartsort

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.refactoring.suggested.startOffset

abstract class SmartSort(open var depth: Int? = null): AnAction() {
	companion object {
		var doc: Document? = null
		var editor: Editor? = null
		var project: Project? = null
		var psiFile: PsiFile? = null
		val getLineNumber: (PsiElement) -> Int = {doc!!.getLineNumber(it.startOffset)}
	}

	override fun actionPerformed(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR)
		val psiFile = e.getData(CommonDataKeys.PSI_FILE)
		if (editor == null || psiFile == null) return
		val offset = editor.caretModel.offset
		val element = psiFile.findElementAt(offset)
		val parent = element?.let {getParent(it)}
		val sortedChildren = sortPsiElements(parent!!.children.copyOf())
		
		WriteCommandAction.runWriteCommandAction(project) {
			var i = 0
			var j = 0
			while (i < parent.children.size) {
				while (parent.children[i] is PsiWhiteSpace) i++; println("i")
				while (sortedChildren[j] is PsiWhiteSpace) j++; println("j")

				parent.children[i].replace(sortedChildren[j])
				i++; j++
			}
		}
	}

	override fun update(e: AnActionEvent) {
		doc = editor?.document
		editor = e.getData(CommonDataKeys.EDITOR)
		project = e.project
		psiFile = e.getData(CommonDataKeys.PSI_FILE)

		e.presentation.isVisible = doc != null && editor != null && psiFile != null
		e.presentation.isEnabled = e.presentation.isVisible && editor!!.caretModel.caretCount == 1
	}

	private fun getParent(element: PsiElement): PsiElement? {
		var el = element

		return try {
			while (getLineNumber(el) == getLineNumber(element)) el = el.parent
			el
		} catch (e: IllegalStateException) {
			psiFile
		}
	}

	private fun mergeSort(left: Array<PsiElement>, right: Array<PsiElement>): Array<PsiElement> {
		var i = 0
		var j = 0
		val sorted: MutableList<PsiElement> = mutableListOf()

		while (i < left.count() && j < right.count()) {
			if (left[i].text.toLowerCase() <= right[j].text.toLowerCase()) {
				sorted.add(left[i].copy())
				i++
			} else {
				sorted.add(right[j].copy())
				j++
			}
		}

		while (i < left.size) {
			sorted.add(left[i].copy())
			i++
		}

		while (j < right.size) {
			sorted.add(right[j].copy())
			j++
		}

		return sorted.toTypedArray();
	}

	private fun sortPsiElements(psiArray: Array<PsiElement>): Array<PsiElement> {
		if (psiArray.size <= 1) {
			return psiArray
		}

		val middle = psiArray.size / 2
		val left = psiArray.copyOfRange(0, middle);
		val right = psiArray.copyOfRange(middle, psiArray.size);

		return mergeSort(sortPsiElements(left), sortPsiElements(right))
	}
}

class RecursiveSort: SmartSort()
class SingleDepthSort(override var depth: Int? = 1): SmartSort()
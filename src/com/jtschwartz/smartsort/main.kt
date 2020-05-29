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

abstract class SmartSort(open var sortDepth: Int = -1): AnAction() {
	companion object {
		var doc: Document? = null
		var editor: Editor? = null
		var project: Project? = null
		var psiFile: PsiFile? = null
		val getLineNumber: (PsiElement) -> Int = {doc!!.getLineNumber(it.textOffset)}
	}

	override fun actionPerformed(e: AnActionEvent) {
		val editor = e.getData(CommonDataKeys.EDITOR)
		val psiFile = e.getData(CommonDataKeys.PSI_FILE)
		if (editor == null || psiFile == null) return
		val offset = editor.caretModel.offset
		val element = psiFile.findElementAt(offset) ?: return
		sort(getParent(element), sortDepth)
	}
	
	private fun sort(element: PsiElement?, depth: Int) {
		if (depth == 0 || element == null) return
		
		if (element.children.size > 1 && isSortable(element.children)) {
			val sortedChildren = sortPsiElements(element.children.copyOf())
			
			WriteCommandAction.runWriteCommandAction(project) {
				var i = 0
				var j = 0
				while (i < element.children.size) {
					while (element.children[i] is PsiWhiteSpace) i++
					while (sortedChildren[j] is PsiWhiteSpace) j++
					
					element.children[i].replace(sortedChildren[j])
					i++; j++
				}
			}
		}

		element.children.forEach {sort(it, depth - 1)}
	}
	
	private fun isSortable(psiArray: Array<PsiElement>): Boolean {
		for (i in 1 until psiArray.size) {
			if (getLineNumber(psiArray[i - 1]) == getLineNumber(psiArray[i])) return false
		}
		return true
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
class SingleDepthSort(override var sortDepth: Int = 1): SmartSort()
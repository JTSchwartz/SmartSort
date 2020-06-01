package com.jtschwartz.smartsort

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

abstract class SmartSort(open var sortDepth: Int = -1): AnAction() {
	companion object {
		var doc: Document? = null
		var editor: Editor? = null
		var project: Project? = null
		var psiFile: PsiFile? = null
		val getLineNumber: (PsiElement) -> Int = {doc!!.getLineNumber(it.textOffset)}
		var hasSelection = false
		var selectionStart = 0
		var selectionEnd = 0
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
			var start = 0
			var end = element.children.size

			val sortedChildren = if (hasSelection) {
				while (start < end && getLineNumber(element.children[start]) < selectionStart) start++
				while (end > 0 && getLineNumber(element.children[end - 1]) > selectionEnd) end--

				if (start < end) sortPsiElements(element.children.copyOfRange(start, end)) else return
			} else {
				sortPsiElements(element.children.copyOf())
			}

			WriteCommandAction.runWriteCommandAction(project) {
				var i = start
				var j = 0
				while (i < end) {
					try {
						while (element.children[i].isWhitespace()) i++
					} catch (e: ArrayIndexOutOfBoundsException) {
						continue
					}
					
					try {
						while (sortedChildren[j].isWhitespace()) j++
					} catch (e: ArrayIndexOutOfBoundsException) {
						continue
					}
					
					element.children[i].replace(sortedChildren[j])
					i++; j++
				}
			}
			
			element.children.forEach {sort(it, depth - 1)}
		} else {
			
			element.children.forEach {sort(it, depth)}
		}
	}
	
	private fun isSortable(psiArray: Array<PsiElement>): Boolean {
		val origin = getLineNumber(psiArray[0])
		
		for (i in 1 until psiArray.size) {
			if (origin != getLineNumber(psiArray[i])) return true
		}
		
		return false
	}
	
	private fun PsiElement.isWhitespace(): Boolean {
		return this is PsiWhiteSpace || if (this.children.size == 1) this.firstChild is PsiWhiteSpace else false
	}

	override fun update(e: AnActionEvent) {
		doc = editor?.document
		editor = e.getData(CommonDataKeys.EDITOR)
		project = e.project
		psiFile = e.getData(CommonDataKeys.PSI_FILE)
		val lang = psiFile?.language

		if (lang == Language.findLanguageByID("Atom") ||
			lang == Language.findLanguageByID("HTML") ||
			lang == Language.findLanguageByID("RSS") ||
			lang == Language.findLanguageByID("SVG") ||
			lang == Language.findLanguageByID("XML")) {
			e.presentation.isEnabledAndVisible = false
			return
		}

		e.presentation.isVisible = doc != null && editor != null && psiFile != null
		e.presentation.isEnabled = e.presentation.isVisible && editor!!.caretModel.caretCount == 1
		
		editor?.let {
			hasSelection = editor!!.selectionModel.hasSelection()
			if (hasSelection) {
				selectionStart = doc!!.getLineNumber(editor!!.selectionModel.selectionStart)
				selectionEnd = doc!!.getLineNumber(editor!!.selectionModel.selectionEnd)
			}
		}
	}

	private fun getParent(element: PsiElement): PsiElement? {
		var el = element

		return try {
			while (getLineNumber(el) == getLineNumber(element)) el = el.parent
			el
		} catch (e: IllegalStateException) {
			psiFile
		} catch (e: IndexOutOfBoundsException) {
			psiFile
		}
	}

	private fun mergeSort(left: Array<PsiElement>, right: Array<PsiElement>): Array<PsiElement> {
		var i = 0
		var j = 0
		val sorted: MutableList<PsiElement> = mutableListOf()

		while (i < left.count() && j < right.count()) {
			if (left[i].text.toLowerCase() <= right[j].text.toLowerCase()) {
				sorted.add(left[i++].copy())
			} else {
				sorted.add(right[j++].copy())
			}
		}

		while (i < left.size) {
			sorted.add(left[i++].copy())
		}

		while (j < right.size) {
			sorted.add(right[j++].copy())
		}

		return sorted.toTypedArray()
	}

	private fun sortPsiElements(psiArray: Array<PsiElement>, start: Int = 0, end: Int = psiArray.size): Array<PsiElement> {
		if (psiArray.size <= 1) {
			return psiArray
		}
		
		val middle = psiArray.size / 2
		val left = psiArray.copyOfRange(start, middle)
		val right = psiArray.copyOfRange(middle, end)
		
		return mergeSort(sortPsiElements(left), sortPsiElements(right))
	}
}

class RecursiveSort: SmartSort()
class SingleDepthSort(override var sortDepth: Int = 1): SmartSort()

class SelectiveDepthSort: SmartSort() {
	
	override fun actionPerformed(e: AnActionEvent) {
		if (DepthPrompt().showAndGet()) {
			sortDepth = DepthPrompt.depthField.text.toInt()
			super.actionPerformed(e)
		}
	}
}

class DepthPrompt: DialogWrapper(true) {
	companion object {
		val depthField = JTextField()
	}
	
	init {
		init()
		title = "Set Sorting Depth"
	}
	
	override fun createCenterPanel(): JComponent? {
		val panel = JPanel(BorderLayout())
		panel.add(depthField)
		
		return panel
	}
	
}

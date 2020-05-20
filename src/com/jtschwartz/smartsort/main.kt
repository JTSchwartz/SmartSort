package com.jtschwartz.smartsort

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

private val getText: (AnActionEvent) -> String? = {e -> e.getData(CommonDataKeys.EDITOR)?.selectionModel?.selectedText }

private fun makeVisibleCheckSelection (e: AnActionEvent) {
	e.presentation.isVisible = true
	
	val project = e.project
	val editor = e.getData(CommonDataKeys.EDITOR)
	
	e.presentation.isEnabled = project != null && editor != null && editor.selectionModel.hasSelection()
}

class RecursiveSort: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		println("SmartSort")
		println(getText(e))
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckSelection(e)
	}
}

class SingleDepthSort: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		println("Single Depth Sort")
		println(getText(e))
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckSelection(e)
	}
}

class SelectiveDepthSort: AnAction() {
	
	override fun actionPerformed(e: AnActionEvent) {
		println("Selective Depth Sort")
		println(getText(e))
	}
	
	override fun update(e: AnActionEvent) {
		makeVisibleCheckSelection(e)
	}
}
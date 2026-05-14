package com.tabletaide.ide.ui

data class GitCloneUiState(
    val busy: Boolean = false,
    val progressMessage: String? = null,
    val errorMessage: String? = null,
)

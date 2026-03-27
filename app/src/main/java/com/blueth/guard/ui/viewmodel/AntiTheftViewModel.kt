package com.blueth.guard.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.blueth.guard.antitheft.AntiTheftManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AntiTheftViewModel @Inject constructor(
    val antiTheftManager: AntiTheftManager
) : ViewModel()

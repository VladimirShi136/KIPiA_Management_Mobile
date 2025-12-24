package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.SchemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchemesViewModel @Inject constructor(
    private val schemeRepository: SchemeRepository
) : ViewModel() {

    val allSchemes = schemeRepository.getAllSchemes().asLiveData()

    fun addScheme(scheme: Scheme) {
        viewModelScope.launch {
            schemeRepository.insertScheme(scheme)
        }
    }

    fun updateScheme(scheme: Scheme) {
        viewModelScope.launch {
            schemeRepository.updateScheme(scheme)
        }
    }

    fun deleteScheme(scheme: Scheme) {
        viewModelScope.launch {
            schemeRepository.deleteScheme(scheme)
        }
    }

    suspend fun getSchemeById(id: Int): Scheme? {
        return schemeRepository.getSchemeById(id)
    }
}
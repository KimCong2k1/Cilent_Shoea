package com.fpoly.shoes_app.framework.presentation.ui.shoes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpoly.shoes_app.framework.domain.model.Category
import com.fpoly.shoes_app.framework.domain.model.Shoes
import com.fpoly.shoes_app.framework.domain.usecase.GetCategoriesUseCase
import com.fpoly.shoes_app.framework.domain.usecase.GetShoesUseCase
import com.fpoly.shoes_app.utility.GET_ALL_POPULAR_SHOES
import com.fpoly.shoes_app.utility.GET_POPULAR_SHOES_ALL
import com.fpoly.shoes_app.utility.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ShoesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getShoesUseCase: GetShoesUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShoesUiState())
    val uiState: StateFlow<ShoesUiState> get() = _uiState

    init {
        getDataCategories()
    }

    private fun getDataCategories() {
        flow {
            emit(getCategoriesUseCase.invoke())
        }.onEach { resource ->
            when (resource.status) {
                Status.SUCCESS -> _uiState.update { state ->
                    state.copy(
                        categoriesSelected = updateCategoriesSelectedList(resource.data?.body()).map {
                            if (it.id.isNullOrEmpty()) it to true
                            else it to false
                        })
                }

                Status.ERROR -> Log.e(
                    "HomeViewModel", "getDataCategories: Error ${resource.message}"
                )

                else -> {}
            }
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.onCompletion {
            _uiState.update { it.copy(isLoading = false) }
        }.launchIn(viewModelScope)
    }

    private fun updateCategoriesSelectedList(categories: List<Category>?): List<Category> {
        val all = Category(
            name = "All"
        )
        val mutableCategoriesSelected = categories.orEmpty().toMutableList()
        mutableCategoriesSelected.add(0, all)
        return mutableCategoriesSelected
    }

    fun handleClickCategoriesSelected(category: Category) {
        val mutableCategoriesSelected = mutableListOf<Pair<Category, Boolean>>()
        uiState.value.categoriesSelected?.forEach {
            if (it.first == category) {
                mutableCategoriesSelected.add(Pair(category, true))
            } else {
                mutableCategoriesSelected.add(Pair(it.first, false))
            }
        }
        _uiState.update { state ->
            state.copy(categoriesSelected = mutableCategoriesSelected)
        }
    }

    fun getDataShoes(category: String? = GET_POPULAR_SHOES_ALL, type: String?) {
        flow {
            emit(getShoesUseCase.invoke())
        }.onEach { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    _uiState.update { state ->
                        val popularShoes = resource.data?.body()
                            ?.filterByTypeAndCategory(type, category)
                            ?.sortBySoldCount(type)
                        state.copy(popularShoes = popularShoes)
                    }
                }

                Status.ERROR -> Log.e("HomeViewModel", "getDataShoes: Error ${resource.message}")
                else -> {}
            }
        }.onStart {
            _uiState.update { it.copy(isLoading = true) }
        }.onCompletion {
            _uiState.update { it.copy(isLoading = false) }
        }.launchIn(viewModelScope)
    }

    // Extension function to filter shoes based on type and category
    private fun List<Shoes>.filterByTypeAndCategory(type: String?, category: String?): List<Shoes> {
        return this.filter { shoe ->
            if (type == GET_ALL_POPULAR_SHOES) {
                (shoe.sold
                    ?: 0) > 0 && (category == GET_POPULAR_SHOES_ALL || shoe.category?.name == category)
            } else {
                shoe.category?.name == type
            }
        }
    }

    // Extension function to sort shoes by sold count
    private fun List<Shoes>.sortBySoldCount(type: String?): List<Shoes> {
        return this.sortedByDescending { shoe ->
            if (type == GET_ALL_POPULAR_SHOES) shoe.sold else null
        }
    }
}
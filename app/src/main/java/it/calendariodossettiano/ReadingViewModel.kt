package it.calendariodossettiano

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import it.calendariodossettiano.bible.BibleText
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class ReadingState {
    object Loading : ReadingState()
    data class Success(val date: LocalDate, val reference: String, val bibleText: BibleText) : ReadingState()
    data class NoReading(val date: LocalDate) : ReadingState()
    data class Error(val cause: String, val reference: String?) : ReadingState()
}

class ReadingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarioRepository(application)

    private val _state = MutableLiveData<ReadingState>()
    val state: LiveData<ReadingState> = _state

    fun loadDate(date: LocalDate) {
        _state.value = ReadingState.Loading
        viewModelScope.launch {
            val reference = repository.referenceFor(date)
            if (reference == null) {
                _state.postValue(ReadingState.NoReading(date))
                return@launch
            }
            try {
                val text = repository.fetchText(reference)
                _state.postValue(ReadingState.Success(date, reference, text))
            } catch (e: Exception) {
                _state.postValue(ReadingState.Error(e.message ?: "Errore sconosciuto", reference))
            }
        }
    }
}

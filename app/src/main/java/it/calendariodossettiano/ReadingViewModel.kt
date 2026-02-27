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
                val text = repository.fetchText(reference, date)
                _state.postValue(ReadingState.Success(date, reference, text))
            } catch (e: Exception) {
                _state.postValue(ReadingState.Error(e.message ?: "Errore sconosciuto", reference))
            }
        }
    }

    /** Returns the set of dates that are already cached on disk. Fast — just lists a directory. */
    fun getCachedDates(): Set<LocalDate> = repository.cachedDates()

    /** Fires-and-forgets a background pre-fetch of the 7 days after [from]. No UI state changes. */
    fun silentPrefetch(from: LocalDate) {
        viewModelScope.launch {
            repository.prefetchWeek(from)
        }
    }
}

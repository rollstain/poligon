package ru.urent.qatools.core_base_feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.urent.qatools.core_logger.AppLog

abstract class BaseScreenModel<STATE : Any, SIDE_EFFECT : Any, EVENT : Any>(
    componentContext: ComponentContext,
    initialState: STATE,
    private val log: AppLog,
) {
    private val mutableState = MutableStateFlow(initialState)
    private val sideEffects = Channel<SIDE_EFFECT>(Channel.UNLIMITED)
    private val screenName = this::class.simpleName?.removeSuffix(SCREEN_MODEL_SUFFIX).orEmpty()

    val stateFlow: StateFlow<STATE> = mutableState.asStateFlow()
    val sideEffectFlow: Flow<SIDE_EFFECT> = sideEffects.receiveAsFlow()

    protected val state: STATE
        get() = mutableState.value

    protected val screenModelScope: CoroutineScope = componentScope(componentContext.lifecycle)

    abstract fun dispatch(event: EVENT)

    protected abstract fun onFetchData()

    protected fun onFetchDataScope(call: suspend CoroutineScope.() -> Unit): Job {
        log.info("Открыт экран $screenName")
        return screenModelScope.launch { call() }
    }

    protected fun screenModelScope(call: suspend CoroutineScope.(STATE) -> Unit): Job = screenModelScope.launch { call(state) }

    protected fun updateState(call: (STATE) -> STATE) = mutableState.update(call)

    protected suspend fun postSideEffect(effect: SIDE_EFFECT) = sideEffects.send(effect)

    @Composable
    fun collectAsState(): State<STATE> = mutableState.collectAsState()

    @Composable
    fun collectSideEffect(onSideEffect: (SIDE_EFFECT) -> Unit) {
        LaunchedEffect(Unit) { sideEffectFlow.collect { effect -> onSideEffect(effect) } }
    }

    private companion object {
        const val SCREEN_MODEL_SUFFIX = "ScreenModel"
    }
}

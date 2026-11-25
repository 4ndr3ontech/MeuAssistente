package com.example.meuassistente

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.meuassistente.data.local.datastore.UserPreferences
import com.example.meuassistente.scheduler.NotificationHelper
import com.example.meuassistente.scheduler.RescheduleWorker
import com.example.meuassistente.ui.navigation.MainDestination
import com.example.meuassistente.ui.screen.AssistantScreen
import com.example.meuassistente.ui.screen.home.HomeHubScreen
import com.example.meuassistente.ui.screen.modules.AgendaScreen
import com.example.meuassistente.ui.screen.modules.BackupScreen
import com.example.meuassistente.ui.screen.modules.FinanceScreen
import com.example.meuassistente.ui.screen.modules.ListsAndNotesScreen
import com.example.meuassistente.ui.screen.modules.QuickAddScreen
import com.example.meuassistente.ui.screen.modules.RoutinesScreen
import com.example.meuassistente.ui.screen.modules.StatisticsScreen
import com.example.meuassistente.ui.screen.modules.TasksHubScreen
import com.example.meuassistente.ui.screen.more.MoreHubScreen
import com.example.meuassistente.ui.screen.more.MoreModuleDestination
import com.example.meuassistente.ui.theme.MeuAssistenteTheme
import com.example.meuassistente.ui.viewmodel.TaskViewModel
import com.example.meuassistente.widget.TaskWidgetProvider

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createNotificationChannel(this)
        RescheduleWorker.enqueue(this)
        TaskWidgetProvider.refreshAll(this)

        if (intent?.getBooleanExtra("open_new_task", false) == true) {
            taskViewModel.openNewTaskDialog()
        }

        val appContainer = (application as MeuAssistenteApp).container

        enableEdgeToEdge()
        setContent {
            val prefs by appContainer.preferencesStore.preferencesFlow.collectAsState(
                initial = UserPreferences()
            )
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: MainDestination.HOME.route
            val snapshotFlow = remember { appContainer.observeHomeProductivitySnapshot() }
            val occurrencesFlow = remember {
                appContainer.database.taskSeriesDao().observeOccurrencesBetween(0L, Long.MAX_VALUE)
            }
            var activeSubModule by remember { mutableStateOf<MoreModuleDestination?>(null) }
            var showLegacyTasks by remember { mutableStateOf(false) }

            MeuAssistenteTheme(
                themeMode = prefs.themeMode,
                fontScale = prefs.fontScale
            ) {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            MainDestination.entries.forEach { destination ->
                                val isSelected = currentRoute == destination.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (destination != MainDestination.MORE) {
                                            activeSubModule = null
                                        }
                                        if (destination != MainDestination.TASKS) {
                                            showLegacyTasks = false
                                        }
                                        if (destination == MainDestination.ADD) {
                                            taskViewModel.openNewTaskDialog()
                                        }
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) {
                                                destination.selectedIcon
                                            } else {
                                                destination.unselectedIcon
                                            },
                                            contentDescription = destination.label
                                        )
                                    },
                                    label = { Text(text = destination.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = MainDestination.HOME.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(MainDestination.HOME.route) {
                            HomeHubScreen(
                                snapshotFlow = snapshotFlow,
                                extractDraftUseCase = appContainer.extractLocalDraftProposalUseCase,
                                createTaskSeriesUseCase = appContainer.createTaskSeriesUseCase,
                                saveCalendarEventUseCase = appContainer.saveCalendarEventUseCase,
                                createObligationUseCase = appContainer.createFinancialObligationUseCase,
                                saveNoteUseCase = appContainer.saveNoteUseCase,
                                onNavigateToTasks = {
                                    navController.navigate(MainDestination.TASKS.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToAdd = {
                                    taskViewModel.openNewTaskDialog()
                                    navController.navigate(MainDestination.ADD.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onNavigateToMore = {
                                    activeSubModule = null
                                    navController.navigate(MainDestination.MORE.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(MainDestination.AGENDA.route) {
                            AgendaScreen(
                                observeAgendaRangeUseCase = appContainer.observeAgendaRangeUseCase,
                                saveCalendarEventUseCase = appContainer.saveCalendarEventUseCase,
                                rescheduleCalendarEventUseCase = appContainer.rescheduleCalendarEventUseCase,
                                cancelCalendarEventUseCase = appContainer.cancelCalendarEventUseCase
                            )
                        }
                        composable(MainDestination.ADD.route) {
                            QuickAddScreen(
                                extractDraftUseCase = appContainer.extractLocalDraftProposalUseCase,
                                createTaskSeriesUseCase = appContainer.createTaskSeriesUseCase,
                                saveCalendarEventUseCase = appContainer.saveCalendarEventUseCase,
                                createObligationUseCase = appContainer.createFinancialObligationUseCase,
                                saveNoteUseCase = appContainer.saveNoteUseCase,
                                onDone = {
                                    navController.navigate(MainDestination.HOME.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        composable(MainDestination.TASKS.route) {
                            if (showLegacyTasks) {
                                AssistantScreen(viewModel = taskViewModel)
                            } else {
                                TasksHubScreen(
                                    taskDomainRepository = appContainer.taskDomainRepository,
                                    occurrencesFlow = occurrencesFlow,
                                    createTaskSeriesUseCase = appContainer.createTaskSeriesUseCase,
                                    updateTaskSeriesRuleUseCase = appContainer.updateTaskSeriesRuleUseCase,
                                    completeOccurrenceUseCase = appContainer.completeTaskOccurrenceUseCase,
                                    snoozeOccurrenceUseCase = appContainer.snoozeTaskOccurrenceUseCase,
                                    reopenOccurrenceUseCase = appContainer.reopenTaskOccurrenceUseCase,
                                    cancelOccurrenceUseCase = appContainer.cancelOccurrenceUseCase,
                                    onOpenLegacyTasks = { showLegacyTasks = true }
                                )
                            }
                        }
                        composable(MainDestination.MORE.route) {
                            when (activeSubModule) {
                                MoreModuleDestination.FINANCE -> FinanceScreen(
                                    financeRepository = appContainer.financeRepository,
                                    createObligationUseCase = appContainer.createFinancialObligationUseCase,
                                    confirmPaymentUseCase = appContainer.confirmPaymentUseCase,
                                    summarizeObligationsUseCase = appContainer.summarizeObligationsUseCase,
                                    onBack = { activeSubModule = null }
                                )
                                MoreModuleDestination.ROUTINES -> RoutinesScreen(
                                    routineRepository = appContainer.routineRepository,
                                    saveRoutineUseCase = appContainer.saveRoutineDefUseCase,
                                    recordExecutionUseCase = appContainer.recordRoutineExecutionUseCase,
                                    onBack = { activeSubModule = null }
                                )
                                MoreModuleDestination.LISTS_NOTES -> ListsAndNotesScreen(
                                    checklistRepository = appContainer.checklistRepository,
                                    noteRepository = appContainer.noteRepository,
                                    createChecklistUseCase = appContainer.createChecklistUseCase,
                                    saveNoteUseCase = appContainer.saveNoteUseCase,
                                    onBack = { activeSubModule = null }
                                )
                                MoreModuleDestination.STATISTICS -> StatisticsScreen(
                                    snapshotFlow = snapshotFlow,
                                    onBack = { activeSubModule = null }
                                )
                                MoreModuleDestination.BACKUP -> BackupScreen(
                                    taskDomainRepository = appContainer.taskDomainRepository,
                                    financeRepository = appContainer.financeRepository,
                                    noteRepository = appContainer.noteRepository,
                                    buildBackupPayloadUseCase = appContainer.buildBackupPayloadUseCase,
                                    verifyAndParseBackupUseCase = appContainer.verifyAndParseBackupUseCase,
                                    onBack = { activeSubModule = null }
                                )
                                null -> MoreHubScreen(
                                    preferences = prefs,
                                    preferencesStore = appContainer.preferencesStore,
                                    onOpenModule = { module -> activeSubModule = module }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent?.getBooleanExtra("open_new_task", false) == true) {
            taskViewModel.openNewTaskDialog()
        }
    }
}

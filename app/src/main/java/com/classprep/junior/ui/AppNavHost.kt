package com.classprep.junior.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.classprep.junior.data.prefs.SetupProgress
import com.classprep.junior.ui.common.appContainer
import com.classprep.junior.ui.common.appViewModel
import com.classprep.junior.ui.history.HistoryDetailScreen
import com.classprep.junior.ui.history.HistoryListScreen
import com.classprep.junior.ui.history.HistoryViewModel
import com.classprep.junior.ui.parent.DateTasksScreen
import com.classprep.junior.ui.parent.DayEditorScreen
import com.classprep.junior.ui.parent.DayEditorViewModel
import com.classprep.junior.ui.parent.ItemEditorScreen
import com.classprep.junior.ui.parent.ItemEditorViewModel
import com.classprep.junior.ui.parent.ItemsLibraryScreen
import com.classprep.junior.ui.parent.LinksEditorScreen
import com.classprep.junior.ui.parent.LinksEditorViewModel
import com.classprep.junior.ui.parent.LinksIndexScreen
import com.classprep.junior.ui.parent.ParentHomeScreen
import com.classprep.junior.ui.parent.ParentRoutes
import com.classprep.junior.ui.parent.ParentViewModel
import com.classprep.junior.ui.parent.PrivacyScreen
import com.classprep.junior.ui.parent.ReminderSettingsScreen
import com.classprep.junior.ui.parent.SubjectEditorScreen
import com.classprep.junior.ui.parent.SubjectEditorViewModel
import com.classprep.junior.ui.parent.SubjectsScreen
import com.classprep.junior.ui.parent.TaskEditorScreen
import com.classprep.junior.ui.parent.TaskEditorViewModel
import com.classprep.junior.ui.parent.TimetableScreen
import com.classprep.junior.ui.planner.PlannerActions
import com.classprep.junior.ui.planner.PlannerScreen
import com.classprep.junior.ui.planner.PlannerViewModel
import com.classprep.junior.ui.review.ReadyScreen
import com.classprep.junior.ui.review.ReviewScreen
import com.classprep.junior.ui.review.ReviewViewModel
import com.classprep.junior.ui.setup.SetupScreen
import com.classprep.junior.ui.setup.SetupViewModel
import com.classprep.junior.ui.setup.WelcomeScreen
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.DayOfWeek
import java.time.LocalDate

object Routes {
    const val WELCOME = "welcome"
    const val SETUP = "setup"
    const val PLANNER = "planner"
    const val EXAMPLE = "example"
    const val PARENT = "parent"
    const val HISTORY = "history"
}

private fun NavHostController.resetTo(route: String) = navigate(route) { popUpTo(0) { inclusive = true } }

/**
 * @param pendingOpenDate a target date delivered by a reminder notification; consumed by the planner.
 */
@Composable
fun AppNavHost(setup: SetupProgress, pendingOpenDate: MutableStateFlow<LocalDate?>) {
    val nav = rememberNavController()
    val container = appContainer()
    val openDate by pendingOpenDate.collectAsStateWithLifecycle()
    val start = remember { if (setup.completed) Routes.PLANNER else Routes.WELCOME }

    // A notification tap always lands on the real planner with the planner as the root (sensible back stack).
    LaunchedEffect(openDate, setup.completed) {
        if (openDate != null && setup.completed && nav.currentDestination?.route != Routes.PLANNER) {
            nav.resetTo(Routes.PLANNER)
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                progress = setup,
                onParentSetup = { nav.navigate(Routes.SETUP) },
                onExample = { container.resetExample(); nav.navigate(Routes.EXAMPLE) },
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                vm = appViewModel { c, _ -> SetupViewModel(c) },
                parentVm = appViewModel { c, _ -> ParentViewModel(c) },
                onExit = { if (!nav.popBackStack()) nav.resetTo(Routes.WELCOME) },
                onFinished = { nav.resetTo(Routes.PLANNER) },
                onEditDay = { nav.navigate("parent/day/${it.value}") },
                onSubjectLinks = { nav.navigate(ParentRoutes.SUBJECT_LINKS) },
                onWeekdayLinks = { nav.navigate(ParentRoutes.WEEKDAY_LINKS) },
                onItemLibrary = { nav.navigate(ParentRoutes.ITEMS) },
            )
        }
        composable(Routes.PLANNER) {
            val vm = appViewModel { c, h -> PlannerViewModel(c.repository, c.clock, c.timeVersion, h, openDate) }
            PlannerScreen(
                vm = vm,
                actions = PlannerActions(
                    onReview = { d -> nav.navigate("review/$d?example=false") },
                    onHistory = { nav.navigate(Routes.HISTORY) },
                    onParents = { nav.navigate(Routes.PARENT) },
                    onExitExample = {},
                ),
                openDate = openDate,
                onOpenDateConsumed = { pendingOpenDate.value = null },
            )
        }
        composable(Routes.EXAMPLE) {
            val vm = appViewModel(key = "example") { c, h -> PlannerViewModel(c.example, c.clock, c.timeVersion, h, null) }
            PlannerScreen(
                vm = vm,
                actions = PlannerActions(
                    onReview = { d -> nav.navigate("review/$d?example=true") },
                    onHistory = {},
                    onParents = {},
                    onExitExample = { nav.popBackStack() },
                ),
                openDate = null,
                onOpenDateConsumed = {},
            )
        }
        composable(
            "review/{date}?example={example}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }, navArgument("example") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val date = LocalDate.parse(entry.arguments!!.getString("date")!!)
            val example = entry.arguments!!.getBoolean("example")
            val vm = appViewModel { c, _ -> ReviewViewModel(if (example) c.example else c.repository, c.clock, date) }
            ReviewScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onConfirmed = {
                    nav.navigate("ready/$date?example=$example") { popUpTo("review/{date}?example={example}") { inclusive = true } }
                },
                onParentSetup = { nav.navigate(Routes.PARENT) },
            )
        }
        composable(
            "ready/{date}?example={example}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }, navArgument("example") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            val date = LocalDate.parse(entry.arguments!!.getString("date")!!)
            val example = entry.arguments!!.getBoolean("example")
            val vm = appViewModel { c, _ -> ReviewViewModel(if (example) c.example else c.repository, c.clock, date) }
            ReadyScreen(vm = vm, onBackToPlanner = { nav.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryListScreen(appViewModel { c, _ -> HistoryViewModel(c.repository) }, canDelete = false, onBack = { nav.popBackStack() }) {
                nav.navigate("history/$it")
            }
        }
        composable(ParentRoutes.HISTORY) {
            HistoryListScreen(appViewModel { c, _ -> HistoryViewModel(c.repository) }, canDelete = true, onBack = { nav.popBackStack() }) {
                nav.navigate("history/$it")
            }
        }
        composable("history/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            HistoryDetailScreen(appViewModel { c, _ -> HistoryViewModel(c.repository) }, id) { nav.popBackStack() }
        }

        // ----- Parent area -----
        composable(Routes.PARENT) {
            ParentHomeScreen(
                vm = appViewModel { c, _ -> ParentViewModel(c) },
                onBack = { nav.popBackStack() },
                onNavigate = { nav.navigate(it) },
                onDataCleared = { nav.resetTo(Routes.WELCOME) },
            )
        }
        composable(ParentRoutes.SUBJECTS) {
            SubjectsScreen(appViewModel { c, _ -> ParentViewModel(c) }, onBack = { nav.popBackStack() }) { nav.navigate("parent/subject/$it") }
        }
        composable("parent/subject/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            SubjectEditorScreen(appViewModel { c, _ -> SubjectEditorViewModel(c, id) }) { nav.popBackStack() }
        }
        composable(ParentRoutes.TIMETABLE) {
            TimetableScreen(appViewModel { c, _ -> ParentViewModel(c) }, onBack = { nav.popBackStack() }) { nav.navigate("parent/day/${it.value}") }
        }
        composable("parent/day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
            val day = DayOfWeek.of(entry.arguments!!.getInt("day"))
            DayEditorScreen(appViewModel { c, _ -> DayEditorViewModel(c, day) }) { nav.popBackStack() }
        }
        composable(ParentRoutes.ITEMS) {
            ItemsLibraryScreen(
                appViewModel { c, _ -> ParentViewModel(c) },
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate("parent/item/$it?suggestion=-1") },
                onAddFromSuggestion = { nav.navigate("parent/item/0?suggestion=$it") },
            )
        }
        composable(
            "parent/item/{id}?suggestion={suggestion}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("suggestion") { type = NavType.IntType; defaultValue = -1 }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            val suggestion = entry.arguments!!.getInt("suggestion")
            ItemEditorScreen(appViewModel { c, _ -> ItemEditorViewModel(c, id, suggestion) }) { nav.popBackStack() }
        }
        composable(ParentRoutes.SUBJECT_LINKS) {
            LinksIndexScreen(appViewModel { c, _ -> ParentViewModel(c) }, bySubject = true, onBack = { nav.popBackStack() },
                onOpenSubject = { nav.navigate("parent/links/subject/$it") }, onOpenDay = {})
        }
        composable(ParentRoutes.WEEKDAY_LINKS) {
            LinksIndexScreen(appViewModel { c, _ -> ParentViewModel(c) }, bySubject = false, onBack = { nav.popBackStack() },
                onOpenSubject = {}, onOpenDay = { nav.navigate("parent/links/day/${it.value}") })
        }
        composable("parent/links/subject/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            LinksEditorScreen(appViewModel { c, _ -> LinksEditorViewModel(c, id, null) }, onClose = { nav.popBackStack() }) {
                nav.navigate("parent/item/0?suggestion=-1")
            }
        }
        composable("parent/links/day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
            val day = DayOfWeek.of(entry.arguments!!.getInt("day"))
            LinksEditorScreen(appViewModel { c, _ -> LinksEditorViewModel(c, null, day) }, onClose = { nav.popBackStack() }) {
                nav.navigate("parent/item/0?suggestion=-1")
            }
        }
        composable(ParentRoutes.TASKS) {
            DateTasksScreen(appViewModel { c, _ -> ParentViewModel(c) }, onBack = { nav.popBackStack() }) { id, date ->
                nav.navigate("parent/task/$id?date=$date")
            }
        }
        composable(
            "parent/task/{id}?date={date}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }, navArgument("date") { type = NavType.StringType; defaultValue = "" }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            val date = entry.arguments!!.getString("date")?.takeIf { it.isNotEmpty() }?.let(LocalDate::parse) ?: container.clock.today().plusDays(1)
            TaskEditorScreen(appViewModel { c, _ -> TaskEditorViewModel(c, id, date) }) { nav.popBackStack() }
        }
        composable(ParentRoutes.REMINDERS) {
            ReminderSettingsScreen(appViewModel { c, _ -> ParentViewModel(c) }) { nav.popBackStack() }
        }
        composable(ParentRoutes.PRIVACY) { PrivacyScreen { nav.popBackStack() } }
    }
}

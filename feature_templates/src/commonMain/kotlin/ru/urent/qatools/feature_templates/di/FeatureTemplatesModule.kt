package ru.urent.qatools.feature_templates.di

import com.arkivanov.decompose.ComponentContext
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.urent.qatools.base_stand.StandSessionInteractor
import ru.urent.qatools.feature_templates.presentation.editor.mvi.TemplateScreenModel
import ru.urent.qatools.feature_templates.presentation.editor.ui.TemplateScreen
import ru.urent.qatools.feature_templates.presentation.list.mvi.TemplatesScreenModel
import ru.urent.qatools.feature_templates.presentation.list.ui.TemplatesScreen
import ru.urent.qatools.navigation.Screen
import ru.urent.qatools.navigation.Screens

class FeatureTemplatesModule {
    val module = module {
        factory { (context: ComponentContext, screen: Screens.Templates) ->
            TemplatesScreenModel(
                componentContext = context,
                standBaseUrl = screen.standBaseUrl,
                templateInteractor = get(),
                locationInteractor = get(),
                runner = get(),
                log = get(),
            )
        }
        factory<Screen>(named<Screens.Templates>()) { (context: ComponentContext, screen: Screens.Templates) ->
            TemplatesScreen(arguments = screen, screenModel = get { parametersOf(context, screen) })
        }
        factory { (context: ComponentContext, screen: Screens.Template) ->
            TemplateScreenModel(
                componentContext = context,
                standBaseUrl = screen.standBaseUrl,
                templateId = screen.templateId,
                templateInteractor = get(),
                locationInteractor = get(),
                bikeSetupInteractor = get(),
                runner = get(),
                tileSource = get<StandSessionInteractor>().session.value?.config?.tileSource,
                reconnections = get<StandSessionInteractor>().reconnections,
                log = get(),
            )
        }
        factory<Screen>(named<Screens.Template>()) { (context: ComponentContext, screen: Screens.Template) ->
            TemplateScreen(screenModel = get { parametersOf(context, screen) }, tileLoader = get())
        }
    }
}

package com.mitteloupe.cag.cleanarchitecturegenerator.settings.versioncatalog

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import com.mitteloupe.cag.cleanarchitecturegenerator.CleanArchitectureGeneratorBundle
import com.mitteloupe.cag.cleanarchitecturegenerator.settings.AppSettingsService
import com.mitteloupe.cag.git.Git
import java.io.File
import javax.swing.JLabel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import com.mitteloupe.cag.cleanarchitecturegenerator.model.DependencyInjection as PluginDependencyInjection

class RootConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable = RootConfigurable()
}

private class RootConfigurable :
    BoundSearchableConfigurable(
        CleanArchitectureGeneratorBundle.message("settings.display.name"),
        "com.mitteloupe.cag.settings.Settings"
    ) {
    private val serviceAutoAddToGit: Boolean
        get() = AppSettingsService.getInstance().autoAddGeneratedFilesToGit
    private val serviceGitPath: String?
        get() = AppSettingsService.getInstance().gitPath
    private val serviceDefaultDependencyInjection: PluginDependencyInjection
        get() =
            PluginDependencyInjection.fromString(AppSettingsService.getInstance().defaultDependencyInjection)

    private var autoAddToGit: Boolean = serviceAutoAddToGit
    private var gitPath: String = serviceGitPath.orEmpty()
    private var defaultDependencyInjection: PluginDependencyInjection = serviceDefaultDependencyInjection

    override fun createPanel() =
        panel {
            var warningLabel: JLabel? = null

            fun isGitAvailableForState(pathText: String): Boolean =
                Git(gitBinaryPath = pathText.nullIfEmpty()).isAvailable(File(System.getProperty("user.home")))

            fun updateWarning(currentPath: String) {
                val showWarning = !isGitAvailableForState(currentPath.trim())
                warningLabel?.isVisible = showWarning
            }
            row {
                text(CleanArchitectureGeneratorBundle.message("settings.root.description"))
            }

            row {
                checkBox(CleanArchitectureGeneratorBundle.message("settings.auto.add.to.git.label"))
                    .applyToComponent {
                        toolTipText = CleanArchitectureGeneratorBundle.message("settings.auto.add.to.git.tooltip")
                        addChangeListener { updateWarning(gitPath) }
                    }.bindSelected(::autoAddToGit)
            }

            row(CleanArchitectureGeneratorBundle.message("settings.dependency.injection.label")) {
                comboBox(PluginDependencyInjection.entries)
                    .bindItem(
                        { defaultDependencyInjection },
                        { value -> value?.let { defaultDependencyInjection = it } }
                    )
            }

            row(CleanArchitectureGeneratorBundle.message("settings.git.path.label")) {
                val descriptor = FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                textFieldWithBrowseButton(
                    project = null,
                    fileChooserDescriptor = descriptor,
                    fileChosen = { it.path }
                ).applyToComponent {
                    toolTipText = CleanArchitectureGeneratorBundle.message("settings.git.path.tooltip")
                    textField.document.addDocumentListener(
                        object : DocumentListener {
                            override fun insertUpdate(event: DocumentEvent) {
                                updateWarning(text)
                            }

                            override fun removeUpdate(event: DocumentEvent) {
                                updateWarning(text)
                            }

                            override fun changedUpdate(evenet: DocumentEvent) {
                                updateWarning(text)
                            }
                        }
                    )
                }.bindText(::gitPath)
            }

            row {
                label(CleanArchitectureGeneratorBundle.message("settings.git.not.found.warning")).applyToComponent {
                    icon = AllIcons.General.Warning
                    warningLabel = this
                    isVisible = false
                }
            }

            onApply {
                updateWarning(gitPath)
            }
            onReset {
                updateWarning(gitPath)
            }
            onIsModified {
                updateWarning(gitPath)
                false
            }

            onApply {
                AppSettingsService.getInstance().autoAddGeneratedFilesToGit = autoAddToGit
                AppSettingsService.getInstance().gitPath = gitPath.ifBlank { null }
                println("Saving default DI setting: ${defaultDependencyInjection.name}")
                AppSettingsService.getInstance().defaultDependencyInjection = defaultDependencyInjection.name
            }
            onReset {
                autoAddToGit = serviceAutoAddToGit
                gitPath = serviceGitPath ?: ""
                defaultDependencyInjection = serviceDefaultDependencyInjection
                println("Resetting default DI setting to: ${defaultDependencyInjection.name}")
            }
            onIsModified {
                val serviceDefaultDI = serviceDefaultDependencyInjection
                autoAddToGit != serviceAutoAddToGit ||
                    gitPath != (serviceGitPath ?: "") ||
                    defaultDependencyInjection != serviceDefaultDI
            }
        }
}

package com.abcd.todoplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Action to show/activate the Kotlin TODO tool window
 */
public class ShowTodoAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowTodoAction.class);

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            Project project = e.getProject();
            if (project == null) {
                return;
            }

            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow("KotlinTODOs");

            if (toolWindow != null) {
                toolWindow.activate(null);

                // Update state
                KotlinTodoState.getInstance().setToolWindowVisible(true);

                // Trigger a refresh of the current file
                KotlinTodoService service = project.getService(KotlinTodoService.class);
                if (service != null) {
                    service.refresh();
                }
            }
        } catch (Exception ex) {
            LOG.warn("Error showing TODO tool window", ex);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable the action only when we have a project
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null);
    }
}
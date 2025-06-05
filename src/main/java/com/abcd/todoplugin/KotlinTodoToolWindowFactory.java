package com.abcd.todoplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the Kotlin TODO tool window
 */
public class KotlinTodoToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the main panel
        KotlinTodoPanel todoPanel = new KotlinTodoPanel(project);

        // Create content wrapper using the modern API
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(todoPanel, "", false);

        // Add content to tool window
        toolWindow.getContentManager().addContent(content);

        // Get the service and set up file monitoring
        KotlinTodoService service = project.getService(KotlinTodoService.class);
        if (service == null) {
            // Create service if it doesn't exist
            service = new KotlinTodoService(project);
        }
        service.setTodoPanel(todoPanel);

        // Initial scan of current file
        service.scanCurrentFile();

        // Set tool window visibility state
        KotlinTodoState state = KotlinTodoState.getInstance();
        toolWindow.setAvailable(true, null);
        if (state.isToolWindowVisible()) {
            toolWindow.activate(null);
        }
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
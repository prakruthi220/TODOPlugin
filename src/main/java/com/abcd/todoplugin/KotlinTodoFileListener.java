package com.abcd.todoplugin;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for file editor events to trigger TODO scanning
 */
public class KotlinTodoFileListener implements FileEditorManagerListener {
    private static final Logger LOG = Logger.getInstance(KotlinTodoFileListener.class);

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            Project project = source.getProject();
            if (project != null && KotlinTodoScanner.isKotlinFile(file)) {
                KotlinTodoService service = project.getService(KotlinTodoService.class);
                if (service != null) {
                    service.scanFile(file);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error handling file opened event", e);
        }
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        try {
            VirtualFile newFile = event.getNewFile();
            if (newFile != null && KotlinTodoScanner.isKotlinFile(newFile)) {
                FileEditorManager manager = event.getManager();
                Project project = manager.getProject();
                KotlinTodoService service = project.getService(KotlinTodoService.class);
                if (service != null) {
                    service.scanFile(newFile);
                }
            }
        } catch (Exception e) {
            LOG.warn("Error handling selection changed event", e);
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            // When a Kotlin file is closed, scan the next active file
            Project project = source.getProject();
            if (project != null && KotlinTodoScanner.isKotlinFile(file)) {
                KotlinTodoService service = project.getService(KotlinTodoService.class);
                if (service != null) {
                    // Scan current file after a short delay to ensure UI is updated
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        service.scanCurrentFile();
                    });
                }
            }
        } catch (Exception e) {
            LOG.warn("Error handling file closed event", e);
        }
    }
}
package com.abcd.todoplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing TODO scanning and file monitoring
 */
@Service(Service.Level.PROJECT)
public final class KotlinTodoService {
    private static final Logger LOG = Logger.getInstance(KotlinTodoService.class);

    private final Project project;
    private KotlinTodoPanel todoPanel;
    private List<TodoItem> currentTodos = new ArrayList<>();

    public KotlinTodoService(Project project) {
        this.project = project;
    }

    public void setTodoPanel(KotlinTodoPanel panel) {
        this.todoPanel = panel;
    }

    /**
     * Scans the currently active file for TODOs
     */
    public void scanCurrentFile() {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                VirtualFile currentFile = getCurrentKotlinFile();
                if (currentFile != null) {
                    scanFile(currentFile);
                } else {
                    updateTodoPanel(new ArrayList<>());
                }
            } catch (Exception e) {
                LOG.warn("Error scanning current file", e);
                updateTodoPanel(new ArrayList<>());
            }
        });
    }

    /**
     * Scans a specific file for TODOs
     */
    public void scanFile(VirtualFile file) {
        if (file == null || !KotlinTodoScanner.isKotlinFile(file)) {
            updateTodoPanel(new ArrayList<>());
            return;
        }

        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    List<TodoItem> todos = KotlinTodoScanner.scanDocumentForTodos(document, file);
                    updateTodoPanel(todos);
                } else {
                    LOG.warn("Could not get document for file: " + file.getName());
                    updateTodoPanel(new ArrayList<>());
                }
            } catch (Exception e) {
                LOG.error("Error scanning file: " + file.getName(), e);
                updateTodoPanel(new ArrayList<>());
            }
        });
    }

    /**
     * Scans all Kotlin files in the project for TODOs
     */
    public void scanAllFiles() {
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                List<TodoItem> allTodos = new ArrayList<>();
                List<VirtualFile> kotlinFiles = KotlinTodoScanner.findAllKotlinFilesInProject(project);

                for (VirtualFile file : kotlinFiles) {
                    Document document = FileDocumentManager.getInstance().getDocument(file);
                    if (document != null) {
                        List<TodoItem> fileTodos = KotlinTodoScanner.scanDocumentForTodos(document, file);
                        allTodos.addAll(fileTodos);
                    }
                }

                updateTodoPanel(allTodos);
            } catch (Exception e) {
                LOG.error("Error scanning all files", e);
                updateTodoPanel(new ArrayList<>());
            }
        });
    }

    /**
     * Refreshes the current scan (rescans current file or all files based on last operation)
     */
    public void refresh() {
        scanCurrentFile(); // Default to current file scan
    }

    /**
     * Clears all TODOs from the panel
     */
    public void clearTodos() {
        updateTodoPanel(new ArrayList<>());
    }

    /**
     * Navigates to a specific TODO item in the editor
     */
    public void navigateToTodo(TodoItem todoItem) {
        if (todoItem == null) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                FileEditorManager editorManager = FileEditorManager.getInstance(project);
                editorManager.openFile(todoItem.getFile(), true);

                // Navigate to the specific line
                Document document = FileDocumentManager.getInstance().getDocument(todoItem.getFile());
                if (document != null && todoItem.getLineNumber() > 0) {
                    Editor editor = editorManager.getSelectedTextEditor();
                    if (editor != null) {
                        int lineIndex = Math.max(0, todoItem.getLineNumber() - 1);
                        if (lineIndex < document.getLineCount()) {
                            int offset = document.getLineStartOffset(lineIndex);
                            editor.getCaretModel().moveToOffset(offset);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.warn("Error navigating to TODO", e);
            }
        });
    }

    /**
     * Updates the TODO panel with the given list of TODOs
     */
    private void updateTodoPanel(List<TodoItem> todos) {
        this.currentTodos = new ArrayList<>(todos);

        if (todoPanel != null) {
            // Update on EDT (Event Dispatch Thread) since we're updating UI
            ApplicationManager.getApplication().invokeLater(() -> {
                todoPanel.updateTodos(todos);
            });
        }
    }

    /**
     * Gets the currently active Kotlin file in the editor
     * @return the current Kotlin file or null if no Kotlin file is active
     */
    private VirtualFile getCurrentKotlinFile() {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        VirtualFile[] selectedFiles = editorManager.getSelectedFiles();

        if (selectedFiles.length > 0) {
            VirtualFile currentFile = selectedFiles[0];
            if (KotlinTodoScanner.isKotlinFile(currentFile)) {
                return currentFile;
            }
        }

        return null;
    }

    /**
     * Gets the current list of TODOs
     * @return copy of current TODOs list
     */
    public List<TodoItem> getCurrentTodos() {
        return new ArrayList<>(currentTodos);
    }

    /**
     * Gets the project associated with this service
     * @return the project instance
     */
    public Project getProject() {
        return project;
    }

    /**
     * Checks if the service has any TODOs
     * @return true if there are TODOs, false otherwise
     */
    public boolean hasTodos() {
        return !currentTodos.isEmpty();
    }

    /**
     * Gets the count of current TODOs
     * @return number of TODOs
     */
    public int getTodoCount() {
        return currentTodos.size();
    }

    /**
     * Filters TODOs by priority level
     * @param priority the priority to filter by
     * @return list of TODOs with the specified priority
     */
    public List<TodoItem> getTodosByPriority(TodoItem.Priority priority) {
        return currentTodos.stream()
                .filter(todo -> todo.getPriority() == priority)
                .collect(Collectors.toList());
    }

    /**
     * Searches TODOs by text content
     * @param searchText the text to search for
     * @return list of TODOs containing the search text
     */
    public List<TodoItem> searchTodos(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return getCurrentTodos();
        }

        String lowerSearchText = searchText.toLowerCase();
        return currentTodos.stream()
                .filter(todo -> todo.getText().toLowerCase().contains(lowerSearchText))
                .collect(Collectors.toList());
    }
}
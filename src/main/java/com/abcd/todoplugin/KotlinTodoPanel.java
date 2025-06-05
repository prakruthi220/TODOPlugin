package com.abcd.todoplugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;



public class KotlinTodoPanel extends JPanel implements KotlinTodoState.TodoStateListener {
    private static final Logger LOG = Logger.getInstance(KotlinTodoPanel.class);

    private final Project project;
    private final JBTextField filterField;
    private final JBList<TodoItem> todoList;
    private final DefaultListModel<TodoItem> listModel;
    private final JLabel statusLabel;

    private List<TodoItem> allTodos = new ArrayList<>();
    private String currentFilter = "";

    public KotlinTodoPanel(Project project) {
        this.project = project;
        this.listModel = new DefaultListModel<>();
        this.todoList = new JBList<>(listModel);
        this.filterField = new JBTextField();
        this.statusLabel = new JLabel("No TODOs found");

        initializeUI();
        setupEventHandlers();

        // Register for state changes
        KotlinTodoState.getInstance().addStateListener(this);

        // Restore filter from previous session
        String savedFilter = KotlinTodoState.getInstance().getLastFilterKeyword();
        if (!savedFilter.isEmpty()) {
            filterField.setText(savedFilter);
            currentFilter = savedFilter;
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create filter panel
        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        // Configure the list
        todoList.setCellRenderer(new TodoListCellRenderer());
        todoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create scroll pane for the list
        JBScrollPane scrollPane = new JBScrollPane(todoList);
        scrollPane.setPreferredSize(new Dimension(300, 400));
        add(scrollPane, BorderLayout.CENTER);

        // Status panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setLabelFor(filterField);

        panel.add(filterLabel, BorderLayout.WEST);
        panel.add(filterField, BorderLayout.CENTER);

        // Add clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            filterField.setText("");
            currentFilter = "";
            applyFilter();
        });
        panel.add(clearButton, BorderLayout.EAST);

        return panel;
    }

    private void setupEventHandlers() {
        // Filter field listener
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }
        });

        // List double-click handler
        todoList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    navigateToSelectedTodo();
                }
            }
        });

        // List enter key handler
        todoList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    navigateToSelectedTodo();
                }
            }
        });
    }

    private void updateFilter() {
        SwingUtilities.invokeLater(() -> {
            currentFilter = filterField.getText();
            applyFilter();

            // Save filter to state
            KotlinTodoState.getInstance().setLastFilterKeyword(currentFilter);
        });
    }

    private void applyFilter() {
        try {
            listModel.clear();

            List<TodoItem> filtered = KotlinTodoScanner.filterTodos(allTodos, currentFilter);

            for (TodoItem todo : filtered) {
                listModel.addElement(todo);
            }

            updateStatusLabel(filtered.size(), allTodos.size());

        } catch (Exception e) {
            LOG.warn("Error applying filter", e);
            statusLabel.setText("Error applying filter");
        }
    }

    private void updateStatusLabel(int filteredCount, int totalCount) {
        if (totalCount == 0) {
            statusLabel.setText("No TODOs found");
        } else if (currentFilter.trim().isEmpty()) {
            statusLabel.setText(String.format("%d TODO%s found",
                    totalCount, totalCount == 1 ? "" : "s"));
        } else {
            statusLabel.setText(String.format("%d of %d TODO%s (filtered)",
                    filteredCount, totalCount, totalCount == 1 ? "" : "s"));
        }
    }

    private void navigateToSelectedTodo() {
        TodoItem selected = todoList.getSelectedValue();
        if (selected == null) {
            return;
        }

        try {
            VirtualFile file = selected.getFile();
            if (file != null && file.isValid()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        OpenFileDescriptor descriptor = new OpenFileDescriptor(
                                project, file, selected.getStartOffset()
                        );
                        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                    } catch (Exception e) {
                        LOG.warn("Error navigating to TODO", e);
                    }
                });
            }
        } catch (Exception e) {
            LOG.warn("Error in navigateToSelectedTodo", e);
        }
    }

    public void updateTodos(List<TodoItem> todos) {
        SwingUtilities.invokeLater(() -> {
            this.allTodos = new ArrayList<>(todos);
            applyFilter();
        });
    }

    public void refreshTodos() {
        // This will be called by the file listener when files change
        // For now, we'll just reapply the current filter
        applyFilter();
    }

    @Override
    public void onStateChanged() {
        // React to state changes if needed
        SwingUtilities.invokeLater(() -> {
            String savedFilter = KotlinTodoState.getInstance().getLastFilterKeyword();
            if (!savedFilter.equals(filterField.getText())) {
                filterField.setText(savedFilter);
                currentFilter = savedFilter;
                applyFilter();
            }
        });
    }

    private static class TodoListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof TodoItem) {
                TodoItem todo = (TodoItem) value;
                setText(todo.getDisplayText());
                setToolTipText(todo.getText());
            }

            return this;
        }
    }
}
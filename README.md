Kotlin TODO Scanner Plugin
A comprehensive IntelliJ IDEA plugin that scans Kotlin files for TODO comments, highlights them in the editor, and provides a convenient sidebar panel for navigation and management.
Features

Automatic TODO Detection: Scans currently opened Kotlin files for TODO comments
Inline Highlighting: Highlights TODO comments directly in the editor with custom styling
Sidebar Panel: Dedicated tool window showing all TODOs with file and line information
Click Navigation: Double-click any TODO in the sidebar to jump directly to its location
Keyword Filtering: Filter TODOs by keyword with real-time search
State Persistence: Remembers filter settings and window state between IDE restarts
Multiple Comment Styles: Supports // TODO, /* TODO */, and /** TODO */ formats

Clone or download the plugin source code
Open the project in IntelliJ IDEA
Build the plugin:
bash./gradlew buildPlugin

Install the plugin:

In IntelliJ IDEA, go to File → Settings → Plugins
Click the gear icon → Install Plugin from Disk...
Select the generated JAR file from build/distributions/
Restart IntelliJ IDEA

Development Setup

Prerequisites:

IntelliJ IDEA 2023.2 or later
Java 17 or later
Kotlin plugin enabled

Open the project in IntelliJ IDEA
Sync Gradle dependencies
Run the plugin:
bash./gradlew runIde


Usage
Basic Usage

Open any Kotlin file (.kt or .kts) in your project
The TODO panel will automatically appear on the right side
TODOs are highlighted in the editor with a light yellow background
Click any TODO in the sidebar panel to navigate to its location

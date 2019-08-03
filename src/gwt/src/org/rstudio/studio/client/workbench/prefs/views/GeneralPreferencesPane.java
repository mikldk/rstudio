/*
 * GeneralPreferencesPane.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.ApplicationQuit.QuitContext;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.application.model.RVersionsInfo;
import org.rstudio.studio.client.application.ui.RVersionSelectWidget;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.projects.Projects;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class GeneralPreferencesPane extends PreferencesPane
{
   @Inject
   public GeneralPreferencesPane(RemoteFileSystemContext fsContext,
                                 FileDialogs fileDialogs,
                                 UserPrefs prefs,
                                 Session session,
                                 GlobalDisplay globalDisplay,
                                 WorkbenchContext context,
                                 EventBus events,
                                 ApplicationQuit quit)
   {
      fsContext_ = fsContext;
      fileDialogs_ = fileDialogs;
      prefs_ = prefs;
      session_ = session;
      globalDisplay_ = globalDisplay;
      events_ = events;
      quit_ = quit;
      
      RVersionsInfo versionsInfo = context.getRVersionsInfo();
      VerticalTabPanel basic = new VerticalTabPanel(ElementIds.GENERAL_BASIC_PREFS);
      
      basic.add(headerLabel("R Sessions"));
      if (BrowseCap.isWindowsDesktop())
      {
         rVersion_ = new TextBoxWithButton(
               "R version:",
               "Change...",
               new ClickHandler()
               {
                  @Override
                  public void onClick(ClickEvent event)
                  {
                     Desktop.getFrame().chooseRVersion(ver ->
                     {
                        if (!StringUtil.isNullOrEmpty(ver))
                        {
                           rVersion_.setText(ver);
                     
                           globalDisplay.showMessage(MessageDialog.INFO,
                                 "Change R Version",
                                 "You need to quit and re-open RStudio " +
                                       "in order for this change to take effect.");
                        }
                     });
                  }
               });
         rVersion_.setWidth("100%");
         rVersion_.setText("Loading...");
         Desktop.getFrame().getRVersion(version -> {
            rVersion_.setText(version);
         });
         spaced(rVersion_);
         basic.add(rVersion_);
      }
      if (versionsInfo.isMultiVersion())
      {
         rServerRVersion_ = new RVersionSelectWidget(
                                       versionsInfo.getAvailableRVersions());
         basic.add(tight(rServerRVersion_));
         
         rememberRVersionForProjects_ = 
                        new CheckBox("Restore last used R version for projects");
         
         rememberRVersionForProjects_.setValue(true);
         Style style = rememberRVersionForProjects_.getElement().getStyle();
         style.setMarginTop(5, Unit.PX);
         style.setMarginBottom(12, Unit.PX);
         basic.add(rememberRVersionForProjects_);
      }

      basic.add(dirChooser_ = new DirectoryChooserTextBox(
            "Default working directory (when not in a project):",
            null,
            fileDialogs_,
            fsContext_));
      spaced(dirChooser_);
      nudgeRight(dirChooser_);
      textBoxWithChooser(dirChooser_);

      restoreLastProject_ = new CheckBox("Restore most recently opened project at startup");
      lessSpaced(restoreLastProject_);
      basic.add(restoreLastProject_);
      
      basic.add(checkboxPref("Restore previously open source documents at startup", prefs_.restoreSourceDocuments()));
        
      rProfileOnResume_ = new CheckBox("Run Rprofile when resuming suspended session");
      if (!Desktop.isDesktop())
         basic.add(rProfileOnResume_);
           
      basic.add(spacedBefore(headerLabel("Workspace")));
      basic.add(loadRData_ = new CheckBox("Restore .RData into workspace at startup"));
      lessSpaced(loadRData_); 
      
      saveWorkspace_ = new SelectWidget(
            "Save workspace to .RData on exit:",
            new String[]
            {
               "Always",
               "Never",
               "Ask"
            },
            new String[]
            {
               UserPrefs.SAVE_WORKSPACE_ALWAYS,
               UserPrefs.SAVE_WORKSPACE_NEVER,
               UserPrefs.SAVE_WORKSPACE_ASK
            }, false);
      spaced(saveWorkspace_);
      basic.add(saveWorkspace_);
      
      basic.add(headerLabel("History"));
      alwaysSaveHistory_ = new CheckBox(
            "Always save history (even when not saving .RData)");
      lessSpaced(alwaysSaveHistory_);
      basic.add(alwaysSaveHistory_);
      
      removeHistoryDuplicates_ = new CheckBox(
                                 "Remove duplicate entries in history");
      basic.add(removeHistoryDuplicates_);

      basic.add(spacedBefore(headerLabel("Other")));

      basic.add(checkboxPref(
            "Wrap around when navigating to previous/next tab",
            prefs_.wrapTabNavigation(),
            true /*defaultSpaced*/));
      
      // provide check for updates option in desktop mode when not
      // already globally disabled
      if (Desktop.isDesktop() && 
          !session.getSessionInfo().getDisableCheckForUpdates())
      {
         basic.add(checkboxPref("Automatically notify me of updates to RStudio",
                   prefs_.checkForUpdates(), true /*defaultSpaced*/));
      }

      // crash reporting - only show in desktop mode
      enableCrashReporting_ = checkboxPref("Send automated crash reports to RStudio",
            prefs_.submitCrashReports());
      if (Desktop.isDesktop())
      {
         lessSpaced(enableCrashReporting_);
         enableCrashReporting_.setEnabled(session.getSessionInfo().getCrashHandlerSettingsModifiable());
         basic.add(enableCrashReporting_);
      }

      VerticalTabPanel advanced = new VerticalTabPanel(ElementIds.GENERAL_ADVANCED_PREFS);

      showServerHomePage_ = new SelectWidget(
            "Show server home page:",
            new String[] {
                  "Multiple active sessions",
                  "Always",
                  "Never"
            },
            new String[] {
                 "sessions",
                 "always",
                 "never"
            },
            false,
            true,
            false);
      
      reuseSessionsForProjectLinks_ = new CheckBox("Re-use idle sessions for project links");
      lessSpaced(reuseSessionsForProjectLinks_);
      boolean firstHeader = true;

      if (!Desktop.hasDesktopFrame())
      {
         if (session_.getSessionInfo().getShowUserHomePage() ||
             session_.getSessionInfo().getMultiSession())
         {
            Label homePageLabel = headerLabel("Home Page");
            if (!firstHeader)
               spacedBefore(homePageLabel);
            advanced.add(homePageLabel);
            firstHeader = false;
         }
         if (session_.getSessionInfo().getShowUserHomePage())
         {
            tight(showServerHomePage_);
            advanced.add(showServerHomePage_);
         }
         if (session_.getSessionInfo().getMultiSession())
            advanced.add(reuseSessionsForProjectLinks_);
      }
      
      // The error handler features require source references; if this R
      // version doesn't support them, don't show these options. 
      if (session_.getSessionInfo().getHaveSrcrefAttribute())
      {
         Label debuggingLabel = headerLabel("Debugging");
         if (!firstHeader)
            spacedBefore(debuggingLabel);
         advanced.add(debuggingLabel);
         firstHeader = false;
         advanced.add(checkboxPref(
               "Use debug error handler only when my code contains errors", 
               prefs_.handleErrorsInUserCodeOnly()));
         advanced.add(spaced(checkboxPref(
               "Automatically expand tracebacks in error inspector", 
               prefs_.autoExpandErrorTracebacks(),
               false /*defaultSpaced*/)));
      }

      if (Desktop.hasDesktopFrame())
      {
         Label osLabel = headerLabel("OS Integration");
         if (!firstHeader)
            spacedBefore(osLabel);
         advanced.add(osLabel);
         firstHeader = false;
         
         renderingEngineWidget_ = new SelectWidget("Rendering engine:", new String[] {});
         renderingEngineWidget_.addChoice("Auto-detect (recommended)", ENGINE_AUTO);
         renderingEngineWidget_.addChoice("Desktop OpenGL", ENGINE_DESKTOP);
         if (BrowseCap.isLinuxDesktop())
         {
            renderingEngineWidget_.addChoice("OpenGL for Embedded Systems", ENGINE_GLES);
         }
         renderingEngineWidget_.addChoice("Software", ENGINE_SOFTWARE);
         advanced.add(spaced(renderingEngineWidget_));
         
         Desktop.getFrame().desktopRenderingEngine((String engine) -> {
            if (StringUtil.isNullOrEmpty(engine))
               return;
            renderingEngineWidget_.setValue(engine);
            renderingEngine_ = engine;
         });
         
         useGpuBlacklist_ = new CheckBox("Use GPU blacklist (recommended)");
         advanced.add(lessSpaced(useGpuBlacklist_));
         Desktop.getFrame().getIgnoreGpuBlacklist((Boolean ignore) -> {
            desktopIgnoreGpuBlacklist_ = ignore;
            useGpuBlacklist_.setValue(!ignore);
         });
         
         useGpuDriverBugWorkarounds_ = new CheckBox("Use GPU driver bug workarounds (recommended)");
         advanced.add(lessSpaced(useGpuDriverBugWorkarounds_));
         Desktop.getFrame().getDisableGpuDriverBugWorkarounds((Boolean disable) -> {
            desktopDisableGpuDriverBugWorkarounds_ = disable;
            useGpuDriverBugWorkarounds_.setValue(!disable);
         });
         
         enableAccessibility_ = new CheckBox("Enable DOM accessibility");
         advanced.add(lessSpaced(enableAccessibility_));
         Desktop.getFrame().getEnableAccessibility(enabled -> 
         {
            desktopAccessibility_ = enabled;
            enableAccessibility_.setValue(enabled);
         });
         
         if (BrowseCap.isLinuxDesktop())
         {
            clipboardMonitoring_ = new CheckBox("Enable X11 clipboard monitoring");
            advanced.add(lessSpaced(clipboardMonitoring_));
            Desktop.getFrame().getClipboardMonitoring(monitoring ->
            {
               desktopMonitoring_ = monitoring;
               clipboardMonitoring_.setValue(monitoring);
            });
         }
      }
      
      Label otherLabel = headerLabel("Other");
      if (!firstHeader)
         spacedBefore(otherLabel);
      advanced.add(otherLabel);
      firstHeader = false;

      showLastDotValue_ = new CheckBox("Show .Last.value in environment listing");
      lessSpaced(showLastDotValue_);
      advanced.add(showLastDotValue_);

      advanced.add(spaced(checkboxPref(
            "Double-click to select words in Console pane", 
            prefs_.consoleDoubleClickSelect())));
      
      showServerHomePage_.setEnabled(false);
      reuseSessionsForProjectLinks_.setEnabled(false);
      saveWorkspace_.setEnabled(false);
      loadRData_.setEnabled(false);
      dirChooser_.setEnabled(false);
      alwaysSaveHistory_.setEnabled(false);
      removeHistoryDuplicates_.setEnabled(false);
      rProfileOnResume_.setEnabled(false);
      showLastDotValue_.setEnabled(false);
      restoreLastProject_.setEnabled(false);

      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("General");
      tabPanel.setSize("435px", "498px");
      tabPanel.add(basic, "Basic", basic.getBasePanelId());
      tabPanel.add(advanced, "Advanced", advanced.getBasePanelId());
      tabPanel.selectTab(0);
      add(tabPanel);
   }
   
   @Override
   protected void initialize(UserPrefs prefs)
   {
      boolean isLauncherSession = session_.getSessionInfo().getLauncherSession();
      showServerHomePage_.setEnabled(!isLauncherSession);
      
      reuseSessionsForProjectLinks_.setEnabled(true);
      saveWorkspace_.setEnabled(true);
      loadRData_.setEnabled(true);
      dirChooser_.setEnabled(true);
      
      if (!isLauncherSession)
         showServerHomePage_.setValue(prefs.showUserHomePage().getValue());
      else
         showServerHomePage_.setValue(UserPrefs.SHOW_USER_HOME_PAGE_ALWAYS);
      
      reuseSessionsForProjectLinks_.setValue(prefs.reuseSessionsForProjectLinks().getValue());
      
      int saveWorkspaceIndex;
      switch (prefs.saveWorkspace().getValue())
      {
         case UserPrefs.SAVE_WORKSPACE_NEVER: 
            saveWorkspaceIndex = 1; 
            break;
         case UserPrefs.SAVE_WORKSPACE_ALWAYS: 
            saveWorkspaceIndex = 0; 
            break; 
         case UserPrefs.SAVE_WORKSPACE_ASK:
         default: 
            saveWorkspaceIndex = 2; 
            break; 
      }
      saveWorkspace_.getListBox().setSelectedIndex(saveWorkspaceIndex);

      loadRData_.setValue(prefs.loadWorkspace().getValue());
      dirChooser_.setText(prefs.initialWorkingDirectory().getValue());
        
      alwaysSaveHistory_.setEnabled(true);
      removeHistoryDuplicates_.setEnabled(true);
      
      alwaysSaveHistory_.setValue(prefs.alwaysSaveHistory().getValue());
      removeHistoryDuplicates_.setValue(prefs.removeHistoryDuplicates().getValue());
      
      rProfileOnResume_.setValue(prefs.runRprofileOnResume().getValue());
      rProfileOnResume_.setEnabled(true);
      
      showLastDotValue_.setValue(prefs.showLastDotValue().getValue());
      showLastDotValue_.setEnabled(true);
      
      if (rServerRVersion_ != null)
         rServerRVersion_.setRVersion(prefs.defaultRVersion().getValue().cast());
      
      if (rememberRVersionForProjects_ != null)
      {
         rememberRVersionForProjects_.setValue(
                                   prefs.restoreProjectRVersion().getValue());
      }

      enableCrashReporting_.setValue(prefs.submitCrashReports().getValue());
     
      // projects prefs
      restoreLastProject_.setEnabled(true);
      restoreLastProject_.setValue(prefs.restoreLastProject().getValue());
   }
   

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconR2x());
   }

   @Override
   public boolean onApply(UserPrefs prefs)
   {
      boolean uiReloadRequired = super.onApply(prefs);
      boolean restartRequired = false;

      if (enableAccessibility_ != null &&
          desktopAccessibility_ != enableAccessibility_.getValue())
      {
         // set accessibility property if changed
         restartRequired = true;
         boolean desktopAccessibility = enableAccessibility_.getValue();
         desktopAccessibility_ = desktopAccessibility;
         Desktop.getFrame().setEnableAccessibility(desktopAccessibility);
      }
      
      if (clipboardMonitoring_ != null &&
          desktopMonitoring_ != clipboardMonitoring_.getValue())
      {
         // set monitoring property if changed
         restartRequired = true;
         boolean desktopMonitoring = clipboardMonitoring_.getValue();
         desktopMonitoring_ = desktopMonitoring;
         Desktop.getFrame().setClipboardMonitoring(desktopMonitoring);
      }
      
      if (renderingEngineWidget_ != null &&
          !StringUtil.equals(renderingEngineWidget_.getValue(), renderingEngine_))
      {
         // set desktop renderer when changed
         restartRequired = true;
         String renderingEngine = renderingEngineWidget_.getValue();
         renderingEngine_ = renderingEngine;
         Desktop.getFrame().setDesktopRenderingEngine(renderingEngine);
      }
      
      if (useGpuBlacklist_ != null &&
          desktopIgnoreGpuBlacklist_ != !useGpuBlacklist_.getValue())
      {
         restartRequired = true;
         boolean ignore = !useGpuBlacklist_.getValue();
         desktopIgnoreGpuBlacklist_ = ignore;
         Desktop.getFrame().setIgnoreGpuBlacklist(ignore);
      }
      
      if (useGpuDriverBugWorkarounds_ != null &&
          desktopDisableGpuDriverBugWorkarounds_ != !useGpuDriverBugWorkarounds_.getValue())
      {
         restartRequired = true;
         boolean disable = !useGpuDriverBugWorkarounds_.getValue();
         desktopDisableGpuDriverBugWorkarounds_ = disable;
         Desktop.getFrame().setDisableGpuDriverBugWorkarounds(disable);
      }
 
      if (saveWorkspace_.isEnabled())
      {
         prefs.saveWorkspace().setGlobalValue(saveWorkspace_.getValue());
      }

      prefs.loadWorkspace().setGlobalValue(loadRData_.getValue());
      prefs.runRprofileOnResume().setGlobalValue(rProfileOnResume_.getValue());
      prefs.initialWorkingDirectory().setGlobalValue(dirChooser_.getText());
      prefs.showLastDotValue().setGlobalValue(showLastDotValue_.getValue());
      prefs.alwaysSaveHistory().setGlobalValue(alwaysSaveHistory_.getValue());
      prefs.removeHistoryDuplicates().setGlobalValue(removeHistoryDuplicates_.getValue());
      prefs.restoreLastProject().setGlobalValue(restoreLastProject_.getValue());
      
      // Pro specific
      if (showServerHomePage_ != null && showServerHomePage_.isEnabled())
         prefs.showUserHomePage().setGlobalValue(showServerHomePage_.getValue());
      if (reuseSessionsForProjectLinks_ != null && reuseSessionsForProjectLinks_.isEnabled())
         prefs.reuseSessionsForProjectLinks().setGlobalValue(reuseSessionsForProjectLinks_.getValue());
      if (rServerRVersion_ != null && rServerRVersion_.isEnabled())
         prefs.defaultRVersion().setGlobalValue(rServerRVersion_.getRVersion());
      if (rememberRVersionForProjects_ != null && rememberRVersionForProjects_.isEnabled())
         prefs.restoreProjectRVersion().setGlobalValue(rememberRVersionForProjects_.getValue());
      
      if (restartRequired)
      {
         globalDisplay_.showYesNoMessage(
               GlobalDisplay.MSG_QUESTION,
               "Restart Required",
               "You need to restart RStudio in order for these changes to take effect. " +
               "Do you want to do this now?",
               () -> forceClosed(() -> restart()),
               true);
         
      }

      return uiReloadRequired;
   }

   @Override
   public String getName()
   {
      return "General";
   }
   
   private RVersionSpec getDefaultRVersion()
   {
      if (rServerRVersion_ != null)
         return rServerRVersion_.getRVersion();
      else
         return RVersionSpec.createEmpty();
   }
   
   private boolean getRestoreProjectRVersion()
   {
      if (rememberRVersionForProjects_ != null)
         return rememberRVersionForProjects_.getValue();
      else
         return false;
   }
   
   private void restart()
   {
      quit_.prepareForQuit(
            "Restarting RStudio",
            new QuitContext()
            {
               @Override
               public void onReadyToQuit(boolean saveChanges)
               {
                  String project = session_.getSessionInfo().getActiveProjectFile();
                  if (project == null)
                     project = Projects.NONE;
                  
                  final String finalProject = project;
                  quit_.performQuit(null, saveChanges, () -> {
                     events_.fireEvent(new OpenProjectNewWindowEvent(finalProject, null));
                  });
               }
            });
   }
   
   private static final String ENGINE_AUTO        = "auto";
   private static final String ENGINE_DESKTOP     = "desktop";
   private static final String ENGINE_GLES        = "gles";
   private static final String ENGINE_SOFTWARE    = "software";
   
   private boolean desktopAccessibility_ = false;
   private boolean desktopMonitoring_ = false;
   private boolean desktopIgnoreGpuBlacklist_ = false;
   private boolean desktopDisableGpuDriverBugWorkarounds_ = false;
   
   private final FileSystemContext fsContext_;
   private final FileDialogs fileDialogs_;
   private RVersionSelectWidget rServerRVersion_ = null;
   private CheckBox rememberRVersionForProjects_ = null;
   private CheckBox reuseSessionsForProjectLinks_ = null;
   private CheckBox enableAccessibility_ = null;
   private CheckBox clipboardMonitoring_ = null;
   private CheckBox useGpuBlacklist_ = null;
   private CheckBox useGpuDriverBugWorkarounds_ = null;
   private SelectWidget renderingEngineWidget_ = null;
   private String renderingEngine_ = null;
   private SelectWidget showServerHomePage_;
   private SelectWidget saveWorkspace_;
   private TextBoxWithButton rVersion_;
   private TextBoxWithButton dirChooser_;
   private CheckBox loadRData_;
   private final CheckBox alwaysSaveHistory_;
   private final CheckBox removeHistoryDuplicates_;
   private CheckBox restoreLastProject_;
   private CheckBox rProfileOnResume_;
   private CheckBox showLastDotValue_;
   private CheckBox enableCrashReporting_;
   private final UserPrefs prefs_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
   private final EventBus events_;
   private final ApplicationQuit quit_;
   
}

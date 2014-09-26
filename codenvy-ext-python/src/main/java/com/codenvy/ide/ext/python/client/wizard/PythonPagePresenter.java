/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.python.client.wizard;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.GenerateDescriptor;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.event.OpenProjectEvent;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.api.wizard.AbstractWizardPage;
import com.codenvy.ide.api.wizard.WizardContext;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.python.shared.ProjectAttributes;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.Unmarshallable;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/** @author Vladyslav Zhukovskii */
@Singleton
public class PythonPagePresenter extends AbstractWizardPage implements PythonPageView.ActionDelegate {

    private PythonPageView         view;
    private ProjectServiceClient   projectServiceClient;
    private EventBus               eventBus;
    private DtoFactory             dtoFactory;
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;

    private WizardContext.Key<String> PROJECT_TYPE = new WizardContext.Key<>("projectType");

    @Inject
    public PythonPagePresenter(PythonPageView view, ProjectServiceClient projectServiceClient, EventBus eventBus, DtoFactory dtoFactory,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory) {
        super("Python project settings", null);
        this.view = view;
        this.projectServiceClient = projectServiceClient;
        this.eventBus = eventBus;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        view.setDelegate(this);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getNotice() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCompleted() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void focusComponent() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public void removeOptions() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public boolean canSkip() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        view.reset();

        ProjectDescriptor project = wizardContext.getData(ProjectWizard.PROJECT);
        if (project != null) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    // wait for client perform all actions to continue
                }
            });
        }
    }


    @Override
    public void setProjectType(String projectType) {
        wizardContext.putData(PROJECT_TYPE, projectType);
    }

    /** {@inheritDoc} */
    @Override
    public void commit(@NotNull final CommitCallback callback) {
        final ProjectDescriptor projectDescriptorToUpdate = dtoFactory.createDto(ProjectDescriptor.class);
        projectDescriptorToUpdate.withProjectTypeId(wizardContext.getData(ProjectWizard.PROJECT_TYPE).getProjectTypeId());

        projectDescriptorToUpdate.setVisibility(getProjectVisibility());
        final String name = wizardContext.getData(ProjectWizard.PROJECT_NAME);
        final ProjectDescriptor project = wizardContext.getData(ProjectWizard.PROJECT);
        if (project != null) {
            if (project.getName().equals(name)) {
                updateProject(project, projectDescriptorToUpdate, callback);
            } else {
                projectServiceClient.rename(project.getPath(), name, null, new AsyncRequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        project.setName(name);

                        updateProject(project, projectDescriptorToUpdate, callback);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        } else {
            generateProject(name, getProjectVisibility(), callback);
        }
    }

    private String getProjectVisibility() {
        Boolean visibility = wizardContext.getData(ProjectWizard.PROJECT_VISIBILITY);
        if (visibility != null && visibility) {
            return "public";
        }

        return "private";
    }

    private void updateProject(final ProjectDescriptor project, ProjectDescriptor projectDescriptorToUpdate,
                               final CommitCallback callback) {
        Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient
                .updateProject(project.getPath(), projectDescriptorToUpdate, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
                    @Override
                    protected void onSuccess(ProjectDescriptor result) {
                        eventBus.fireEvent(new OpenProjectEvent(result.getName()));
                        wizardContext.putData(ProjectWizard.PROJECT, result);
                        callback.onSuccess();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
    }

    private void generateProject(final String name, String visibility, final CommitCallback callback) {
        GenerateDescriptor generateDescriptor = dtoFactory.createDto(GenerateDescriptor.class);

        String projectType = wizardContext.getData(PROJECT_TYPE);

        switch (projectType) {
            case "web":
                generateDescriptor.setGeneratorName(ProjectAttributes.PYTHON_WEB_DEF_GENERATOR);
                break;
            default:
                generateDescriptor.setGeneratorName(ProjectAttributes.PYTHON_STANDALONE_DEF_GENERATOR);
                break;
        }

        generateDescriptor.setProjectVisibility(visibility);

        projectServiceClient.generateProject(name, generateDescriptor,
                                             new AsyncRequestCallback<ProjectDescriptor>(
                                                     dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class)) {
                                                 @Override
                                                 protected void onSuccess(ProjectDescriptor result) {
                                                     eventBus.fireEvent(new OpenProjectEvent(result.getName()));
                                                     wizardContext.putData(ProjectWizard.PROJECT, result);
                                                     callback.onSuccess();
                                                 }

                                                 @Override
                                                 protected void onFailure(Throwable exception) {
                                                     callback.onFailure(exception);
                                                 }
                                             }
                                            );
    }

}

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
package com.codenvy.ide.ext.python.client;

import com.codenvy.ide.api.extension.Extension;
import com.codenvy.ide.api.icon.Icon;
import com.codenvy.ide.api.icon.IconRegistry;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projecttype.wizard.ProjectTypeWizardRegistry;
import com.codenvy.ide.api.projecttype.wizard.ProjectWizard;
import com.codenvy.ide.ext.python.client.wizard.PythonPagePresenter;
import com.codenvy.ide.ext.python.shared.ProjectAttributes;
import com.codenvy.ide.extension.runner.client.wizard.SelectRunnerPagePresenter;
import com.google.gwt.resources.client.ClientBundle;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.vectomatic.dom.svg.ui.SVGResource;

/** @author Vladyslav Zhukovskii */
@Singleton
@Extension(title = "Python", version = "3.0.0")
public class PythonExtension {
    public interface ParserResource extends ClientBundle {
        @Source("com/codenvy/ide/ext/python/client/image/python.svg")
        SVGResource pythonCategoryIcon();
    }

    @Inject
    public PythonExtension(Provider<PythonPagePresenter> pythonPagePresenterProvider, Provider<SelectRunnerPagePresenter> runnerPagePresenter,
                           NotificationManager notificationManager, ProjectTypeWizardRegistry projectTypeWizardRegistry,
                           ParserResource parserResource, IconRegistry iconRegistry) {
        ProjectWizard wizard = new ProjectWizard(notificationManager);
        wizard.addPage(pythonPagePresenterProvider);
        wizard.addPage(runnerPagePresenter);

        projectTypeWizardRegistry.addWizard(ProjectAttributes.PYTHON_ID, wizard);

        iconRegistry.registerIcon(new Icon(ProjectAttributes.PYTHON_CATEGORY + ".samples.category.icon", parserResource.pythonCategoryIcon()));
    }
}
